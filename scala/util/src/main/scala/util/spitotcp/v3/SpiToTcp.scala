package util.spitotcp.v3

import java.net.ServerSocket
import java.util.concurrent.{ LinkedBlockingQueue, Executors }
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.HOURS
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.auto.{autoRefineV, autoUnwrap}
import fc.device.controller.spi.{ SpiAddress, SpiController }

object SpiToTcp {

  def main(args: Array[String]): Unit = {
    val gps = SpiAddress(busNumber = 0, chipSelect = 0)
    val spiController = SpiController()

    val maxBytesToTransfer: Int Refined Positive = 100
    val delayMs = 100L

    val serverSocket = new ServerSocket(args(0).toInt, 0)
    println(s"Listening on ${serverSocket.getLocalPort}")
    println(s"\tReceive buffer size (bytes): ${serverSocket.getReceiveBufferSize}")
    println(s"\tSocket timeout (milliseconds): ${serverSocket.getSoTimeout}")
    println(s"\tSO_REUSEADDR is enabled: ${serverSocket.getReuseAddress}")
    println(Seq.fill(50)("-").mkString)

    val clientSocket = serverSocket.accept()
    println(s"Connected to ${clientSocket.getInetAddress.getHostAddress}:${clientSocket.getPort}")
    println(s"\tSO_KEEPALIVE enabled: ${clientSocket.getKeepAlive}")
    println(s"\tTCP_NODELAY enabled: ${clientSocket.getTcpNoDelay}")
    println(s"\tTraffic class: ${clientSocket.getTrafficClass}")

    val clientInput = clientSocket.getInputStream
    val clientOutput = clientSocket.getOutputStream

    val spiInputQueue = new LinkedBlockingQueue[Byte]()
    val spiOutputQueue = new LinkedBlockingQueue[Byte]()

    val executor = Executors.newCachedThreadPool()

    def task1(): Runnable = new Runnable{
      def run(): Unit = {
        val dataFromTcp = clientInput.read.toByte
        spiInputQueue.add(dataFromTcp)
        executor.submit(task1())
      }
    }

    def task2(): Runnable = new Runnable{
      def run(): Unit = {
        val dataFromQueue = Option(spiInputQueue.poll(delayMs, MILLISECONDS))

        val dataFromSpi = dataFromQueue.fold(
          spiController.receive(gps, maxBytesToTransfer)
        )(
          data => spiController.transfer(gps, Seq(data))
        ).fold(l => throw new RuntimeException(l.toString), identity)

        dataFromSpi foreach spiOutputQueue.add
        executor.submit(task2())
      }
    }

    def task3(): Runnable = new Runnable{
      def run(): Unit = {
        val dataFromQueue = spiOutputQueue.take()
        clientOutput.write(Array(dataFromQueue))
        executor.submit(task3())
      }
    }

    executor.submit(task1())
    executor.submit(task2())
    executor.submit(task3())

    // Clearly this isn't a long-term solution
    executor.awaitTermination(1, HOURS)
  }

}

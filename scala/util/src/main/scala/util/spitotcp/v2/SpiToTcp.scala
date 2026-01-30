package util.spitotcp.v2

import java.net.ServerSocket
import java.util.concurrent.{ LinkedBlockingQueue }
import java.util.concurrent.TimeUnit.MILLISECONDS
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

    while (true) {
      val clientSocket = serverSocket.accept()
      println(s"Connected to ${clientSocket.getInetAddress.getHostAddress}:${clientSocket.getPort}")
      println(s"\tSO_KEEPALIVE enabled: ${clientSocket.getKeepAlive}")
      println(s"\tTCP_NODELAY enabled: ${clientSocket.getTcpNoDelay}")
      println(s"\tTraffic class: ${clientSocket.getTrafficClass}")

      val clientInput = clientSocket.getInputStream
      val clientOutput = clientSocket.getOutputStream

      val spiInputQueue = new LinkedBlockingQueue[Byte]()
      val spiOutputQueue = new LinkedBlockingQueue[Byte]()

      val thread1 = new Thread(new Runnable{
        def run(): Unit = {
          while(true) {
            val dataFromTcp = clientInput.read.toByte
            spiInputQueue.add(dataFromTcp)
          }
        }
      })

      val thread2 = new Thread(new Runnable{
        def run(): Unit = {
          while(true) {
            val dataFromQueue = Option(spiInputQueue.poll(delayMs, MILLISECONDS))

            val dataFromSpi = dataFromQueue.fold(
              spiController.receive(gps, maxBytesToTransfer)
            )(
              data => spiController.transfer(gps, Seq(data))
            ).fold(l => throw new RuntimeException(l.toString), identity)

            dataFromSpi foreach spiOutputQueue.add
          }
        }
      })

      val thread3 = new Thread(new Runnable{
        def run(): Unit = {
          while(true) {
            val dataFromQueue = spiOutputQueue.take()
            clientOutput.write(Array(dataFromQueue))
          }
        }
      })

      thread1.start()
      thread2.start()
      thread3.start()

      thread1.join()
      thread2.join()
      thread3.join()

    } // while(true)
  }

}

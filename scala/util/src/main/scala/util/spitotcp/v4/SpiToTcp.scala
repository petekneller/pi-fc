package util.spitotcp.v4

import java.net.ServerSocket
import java.util.concurrent.{ LinkedBlockingQueue, Executors }
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.HOURS
import scala.concurrent.{ ExecutionContext, Future }
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
    implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(executor)

    def task1()(implicit ec: ExecutionContext): Unit = Future {
      val dataFromTcp = clientInput.read.toByte
      spiInputQueue.add(dataFromTcp)
    } foreach { _ => task1() }

    def task2()(implicit ec: ExecutionContext): Unit = Future {
      val dataFromQueue = Option(spiInputQueue.poll(delayMs, MILLISECONDS))

      val dataFromSpi = dataFromQueue.fold(
        spiController.receive(gps, maxBytesToTransfer)
      )(
        data => spiController.transfer(gps, Seq(data))
      ).fold(l => throw new RuntimeException(l.toString), identity)

      dataFromSpi foreach spiOutputQueue.add
    } foreach { _ => task2() }

    def task3()(implicit ec: ExecutionContext): Unit = Future {
      val dataFromQueue = spiOutputQueue.take()
      clientOutput.write(Array(dataFromQueue))
    } foreach { _ => task3() }

    task1()
    task2()
    task3()

    // Clearly this isn't a long-term solution
    val _ = executor.awaitTermination(1, HOURS)
  }

}

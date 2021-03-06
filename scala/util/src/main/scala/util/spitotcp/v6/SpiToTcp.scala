package util.spitotcp.v6

import java.net.ServerSocket
import java.util.concurrent.{ LinkedBlockingQueue, Executors }
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.HOURS
import scala.math.min
import scala.concurrent.ExecutionContext
import cats.instances.list._
import cats.syntax.apply._
import cats.syntax.parallel._
import cats.effect.IO
import fs2.Stream
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.auto.{autoRefineV, autoUnwrap}
import fc.device.controller.spi.{ SpiAddress, SpiController }

object SpiToTcp {

  def main(args: Array[String]): Unit = {
    val gps = SpiAddress(busNumber = 0, chipSelect = 0)
    val spiController = SpiController()

    val maxBytesToTransfer: Int Refined Positive = 100
    val delayMs = 100

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
    val ec = ExecutionContext.fromExecutor(executor)
    implicit val cs = IO.contextShift(ec)

    val task1: Stream[IO, Unit] = Stream.eval_(IO.delay{
      val dataFromTcp = clientInput.read.toByte
      spiInputQueue.add(dataFromTcp)
    }).repeat

    val task2: Stream[IO, Unit] = Stream.eval(IO.delay {
      val dataFromQueue = Option(spiInputQueue.poll(delayMs, MILLISECONDS))

      val dataFromSpi = dataFromQueue.fold(
        spiController.receive(gps, maxBytesToTransfer)
      )(
        data => spiController.transfer(gps, Seq(data))
      ).fold(l => throw new RuntimeException(l.toString), identity)

      dataFromSpi foreach spiOutputQueue.add
    }).repeat

    val task3: Stream[IO, Unit] = Stream.eval(IO.delay {
      val dataFromQueue = spiOutputQueue.take()
      clientOutput.write(Array(dataFromQueue))
    }).repeat

    Stream(task1, task2, task3).parJoinUnbounded.compile.drain.unsafeRunSync()
  }

}

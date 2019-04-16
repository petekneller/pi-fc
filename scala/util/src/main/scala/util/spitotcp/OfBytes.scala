package util.spitotcp

import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.nio.channels.AsynchronousChannelGroup
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import eu.timepit.refined.W
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Interval
import eu.timepit.refined.auto.{ autoRefineV, autoUnwrap }
import cats.effect.{ IO, Timer }
import fs2.{ Stream, Pipe, Chunk }
import fs2.concurrent.SignallingRef
import fs2.io.tcp.Socket
import v7.SpiToTcp.{ spiTransfer, spiReceive, delay, receiveFromClient, transmitToClient }

object OfBytes {
  type Port = Int Refined Interval.Closed[W.`1`.T, W.`65535`.T]

  implicit val t: Timer[IO] = IO.timer(ExecutionContext.global)
  implicit val cs = IO.contextShift(ExecutionContext.global)
  val blockingIO = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

  def apply(port: Port): Unit = {

    implicit val acg = AsynchronousChannelGroup.withThreadPool(Executors.newCachedThreadPool())
    val app = for {
      clientResource <- Socket.server[IO](new InetSocketAddress("0.0.0.0", port))
      clientSocket <- Stream.resource(clientResource)
      receiveMetric <- Stream.eval(SignallingRef[IO, Int](0))
      tcpStream = handlePeer(clientSocket, receiveMetric)
      metricStream = printMetrics(receiveMetric)
      _ <- Stream(tcpStream, metricStream).parJoinUnbounded
    } yield ()

    app.compile.drain.unsafeRunSync()
  }

  def handlePeer(client: Socket[IO], receiveMetric: SignallingRef[IO, Int]): Stream[IO, Unit] = {
    val ping = Stream.awakeEvery[IO](delay)

    val chunksFromClient = receiveFromClient(client).chunks.map(_.toArray: Seq[Byte])
    val withReceiveMetric = for {
      bytes <-chunksFromClient
      _ <- Stream.eval(receiveMetric.set(bytes.length))
    } yield bytes

    val transferViaSpi: Pipe[IO, Either[Seq[Byte], FiniteDuration], Byte] = upstream => {
      upstream flatMap {
        case Left(clientBytes) => Stream.eval(cs.evalOn(blockingIO)(spiTransfer(clientBytes)))
        case Right(_) => Stream.eval(cs.evalOn(blockingIO)(spiReceive()))
      } flatMap { bytes => Stream.chunk(Chunk.seq(bytes))}
    }

    (withReceiveMetric either ping) through transferViaSpi through transmitToClient(client)
  }

  def printMetrics(receiveMetric: SignallingRef[IO, Int]): Stream[IO, Unit] = {
    receiveMetric.discrete.flatMap(receiveBytes => Stream.eval(cs.evalOn(blockingIO)(IO.delay{ println(s"Received $receiveBytes bytes") })))
  }
}

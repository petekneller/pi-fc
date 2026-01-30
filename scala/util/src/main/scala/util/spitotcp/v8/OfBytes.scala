package util.spitotcp.v8

import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.NANOSECONDS
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import org.slf4j.LoggerFactory
import eu.timepit.refined.W
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.{ Interval, Positive }
import eu.timepit.refined.auto.{autoRefineV, autoUnwrap}
import cats.effect.{ Blocker, IO, Timer }
import fs2.{ Stream, Pipe, Chunk }
import fs2.concurrent.SignallingRef
import fs2.io.tcp.{Socket, SocketGroup}
import fc.device.controller.spi.{ SpiAddress, SpiController }
import cats.effect.ContextShift

object OfBytes {
  type Port = Int Refined Interval.Closed[W.`1`.T, W.`65535`.T]

  val logger = LoggerFactory.getLogger(this.getClass)

  implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)
  implicit val cs: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
  val blockingIO = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

  def apply(port: Port): Unit = {

    val app = for {
      blocker <- Stream.resource(Blocker[IO])
      socketGroup <- Stream.resource(SocketGroup[IO](blocker))
      clientResource <- socketGroup.server[IO](new InetSocketAddress("0.0.0.0", port))
      clientSocket <- Stream.resource(clientResource)
      receiveMetric <- Stream.eval(SignallingRef[IO, Int](0))
      transferDurationMetric <- Stream.eval(SignallingRef[IO, FiniteDuration](0.nanos))
      tcpStream = handlePeer(clientSocket, receiveMetric, transferDurationMetric)
      metricStream = printMetrics(receiveMetric, transferDurationMetric)
      _ <- Stream(tcpStream, metricStream).parJoinUnbounded
    } yield ()

    app.compile.drain.unsafeRunSync()
  }

  def handlePeer(client: Socket[IO],
                   receiveMetric: SignallingRef[IO, Int],
                   transferDurationMetric: SignallingRef[IO, FiniteDuration]): Stream[IO, Unit] = {

    val ping = Stream.awakeEvery[IO](delay)

    val chunksFromClient = receiveFromClient(client).chunks.map(_.toArray.toSeq)
    val withReceiveMetric = for {
      bytes <- chunksFromClient
      _ <- Stream.eval(receiveMetric.set(bytes.length))
    } yield bytes

    val transferViaSpi: Pipe[IO, Either[Seq[Byte], FiniteDuration], Byte] = upstream => {
      upstream flatMap {
        case Left(clientBytes) => Stream.eval(cs.evalOn(blockingIO)(withDuration(spiTransfer(clientBytes))))
        case Right(_) => Stream.eval(cs.evalOn(blockingIO)(withDuration(spiReceive())))
      } flatMap {
        case (duration, gpsBytes) => Stream.eval(transferDurationMetric.set(duration)) >>
                                   Stream.chunk(Chunk.seq(gpsBytes))
      }
    }

    (withReceiveMetric either ping) through transferViaSpi through transmitToClient(client)
  }

  def printMetrics(receiveMetric: SignallingRef[IO, Int],
                     transferDurationMetric: SignallingRef[IO, FiniteDuration]): Stream[IO, Unit] =
    ((receiveMetric.discrete) either (transferDurationMetric.discrete)) map {
      case Left(receiveBytes) => s"Last receive was $receiveBytes bytes"
      case Right(transferDuration) => s"Last transfer took ${transferDuration.toMicros} microseconds"
    } flatMap { message => Stream.eval(cs.evalOn(blockingIO)(IO.delay{
      logger.info(message)
    })) }

  def withDuration[A](ioa: IO[A]): IO[(FiniteDuration, A)] =
    for {
      begin <- timer.clock.monotonic(NANOSECONDS)
      a <- ioa
      end <- timer.clock.monotonic(NANOSECONDS)
    } yield (FiniteDuration(end - begin, NANOSECONDS), a)

  val gps = SpiAddress(busNumber = 0, chipSelect = 0)
  val spiController = SpiController()

  val maxBytesToTransfer: Int Refined Positive = 100
  val delay = 100.milliseconds

  def spiTransfer(bytes: Seq[Byte]): IO[Seq[Byte]] = IO.delay{ spiController.transfer(gps, bytes).toOption.get }

  def spiReceive(): IO[Seq[Byte]] = IO.delay{ spiController.receive(gps, maxBytesToTransfer).toOption.get }

  def receiveFromClient(client: Socket[IO]): Stream[IO, Byte] =
    client.reads(maxBytesToTransfer).onFinalize(client.endOfOutput)

  def transmitToClient(client: Socket[IO]): Pipe[IO, Byte, Unit] = (input: Stream[IO, Byte]) =>
    input.chunks.flatMap{ bytes => Stream.eval_(client.write(bytes)) }
}

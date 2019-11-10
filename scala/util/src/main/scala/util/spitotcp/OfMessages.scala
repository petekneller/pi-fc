package util.spitotcp

import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.nio.channels.AsynchronousChannelGroup
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicReference
import java.time.{ Instant, Duration }
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.math.max
import org.slf4j.LoggerFactory
import eu.timepit.refined.W
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.{ Interval, Positive }
import eu.timepit.refined.auto.{autoRefineV, autoUnwrap}
import cats.effect.{ IO, Timer }
import fs2.{ Stream, Pipe, Chunk, Pull }
import fs2.concurrent.SignallingRef
import fs2.io.tcp.Socket
import fc.device.controller.spi.{ SpiAddress, SpiController }
import fc.device.gps.{ Message, MessageParser, CompositeParser }
import MessageParser._
import fc.device.gps.ublox.UbxParser
import fc.device.gps.nmea.NmeaParser

object OfMessages {
  type Port = Int Refined Interval.Closed[W.`1`.T, W.`65535`.T]

  val logger = LoggerFactory.getLogger(this.getClass)

  implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)
  implicit val cs = IO.contextShift(ExecutionContext.global)
  val blockingIO = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

  def apply(port: Port): Unit = {

    implicit val acg = AsynchronousChannelGroup.withThreadPool(Executors.newCachedThreadPool())
    val app = for {
      clientResource <- Socket.server[IO](new InetSocketAddress("0.0.0.0", port))
      clientSocket <- Stream.resource(clientResource)
      tcpStream = handlePeer(clientSocket)
      metricStream = Stream.awakeEvery[IO](1.second) map { _ => println(metrics.observe()) }
      _ <- Stream(tcpStream, metricStream).parJoinUnbounded
    } yield ()

    app.compile.drain.unsafeRunSync()
  }

  def handlePeer(client: Socket[IO]): Stream[IO, Unit] = {
    val ping = Stream.awakeEvery[IO](delay)

    val bytesFromClient = receiveFromClient(client)
    val messagesFromClient = bytesFromClient through messagePrinting("<-")

    val transferViaSpi: Pipe[IO, Either[Message, FiniteDuration], Byte] = upstream => {
      upstream flatMap {
        case Left(clientMsg) => Stream.eval(cs.evalOn(blockingIO)(spiTransfer(clientMsg.toBytes)))
        case Right(_) => Stream.eval(cs.evalOn(blockingIO)(spiReceive()))
      } flatMap {
        case gpsBytes => Stream.chunk(Chunk.seq(gpsBytes))
      }
    }

    (messagesFromClient either ping) through
      transferViaSpi through
      messagePrinting("->") flatMap { msg => Stream.chunk(Chunk.seq(msg.toBytes)) } through
      transmitToClient(client)
  }

  def messagePrinting(dir: String): Pipe[IO, Byte, Message] = s => printMessages(s, dir, newParser()).stream

  def newParser() = CompositeParser(NmeaParser(), UbxParser())

  def printMessages[A <: Message](s: Stream[IO, Byte], dir: String, parser: MessageParser[A]): Pull[IO, Message, Unit] = {
    s.pull.uncons1.flatMap {
      case None => Pull.pure(None)
      case Some((byte, rest)) => parser.consume(byte) match {
        case Unconsumed(_) => printMessages(rest, dir, parser)
        case Proceeding(nextParser) => printMessages(rest, dir, nextParser)
        case Done(msg) => Pull.output1(msg) >> Pull.eval(IO.delay{ logger.info(s"$dir ${msg.toString}") }) >>
          printMessages(rest, dir, newParser())
        case Failed(cause) => Pull.eval(IO.delay{ logger.error(cause) }) >>
          printMessages(rest, dir, newParser())
      }
    }
  }

  val gps = SpiAddress(busNumber = 0, chipSelect = 0)
  val spiController = SpiController()

  val maxBytesToTransfer: Int Refined Positive = 100
  val delay = 100.milliseconds

  val metrics = new Metrics()

  def spiTransfer(bytes: Seq[Byte]): IO[Seq[Byte]] = IO.delay{
    withSpiMetrics(bytes.size){ () =>
      spiController.transfer(gps, bytes).right.get
    }
  }

  def spiReceive(): IO[Seq[Byte]] = IO.delay{
    withSpiMetrics(0){ () =>
      spiController.receive(gps, maxBytesToTransfer).right.get
    }
  }

  def withSpiMetrics(writeBytes: Int)(spiFunction: () => Seq[Byte]): Seq[Byte] = {
    val begin = Instant.now()
    val readBytes = spiFunction()
    val end = Instant.now()
    val duration = FiniteDuration(Duration.between(begin, end).toMillis, MILLISECONDS)
    metrics.record(SpiEvent(begin, duration, writeBytes, readBytes.size))
    readBytes
  }

  def receiveFromClient(client: Socket[IO]): Stream[IO, Byte] =
    client.reads(maxBytesToTransfer).onFinalize(client.endOfOutput)

  def transmitToClient(client: Socket[IO]): Pipe[IO, Byte, Unit] = (input: Stream[IO, Byte]) =>
    input.chunks.flatMap{ bytes => Stream.eval_(client.write(bytes)) }

  case class SpiEvent(timestamp: Instant, duration: FiniteDuration, writeBytes: Int, readBytes: Int)

  case class SpiMetrics(rate: Double, duration: StatisticalMeasures[FiniteDuration], writeBytes: StatisticalMeasures[Int], readBytes: StatisticalMeasures[Int])

  class Metrics() {
    private val size: Int = 10
    private val buffer: AtomicReference[Vector[SpiEvent]] = new AtomicReference(Vector.empty)
    def record(event: SpiEvent): Unit = {
      buffer.updateAndGet{ buffer => (buffer :+ event).takeRight(size) }
    }

    def observe(): SpiMetrics = {
      val events = buffer.get()

      val rate = (for {
        oldest <- events.headOption
        newest <- events.lastOption
      } yield {
        val spanOfEvents = newest.timestamp.toEpochMilli() - oldest.timestamp.toEpochMilli()
        events.size / (spanOfEvents.toDouble / 1000.0)
      }).getOrElse(0.0)

      val duration = StatisticalMeasures(events.map(_.duration), FiniteDuration(0, MILLISECONDS))
      val writeBytes = StatisticalMeasures(events.map(_.writeBytes), 0)
      val readBytes = StatisticalMeasures(events.map(_.readBytes), 0)

      SpiMetrics(rate, duration, writeBytes, readBytes)
    }
  }

  case class StatisticalMeasures[A](median: A, p95: A, max: A)

  object StatisticalMeasures {
    def apply[A: Ordering](data: Seq[A], empty: A): StatisticalMeasures[A] = {
      val size = data.size
      if (size == 0)
        StatisticalMeasures(empty, empty, empty)

      val ordered = data.sorted
      StatisticalMeasures(
        ordered.apply(toIndex(0.5 * size)),
        ordered.apply(toIndex(0.9 * size)),
        ordered.apply(toIndex(1.0 * size))
      )
    }

    private def toIndex(idx: Double): Int = max(idx.floor.toInt - 1, 0)
  }
}

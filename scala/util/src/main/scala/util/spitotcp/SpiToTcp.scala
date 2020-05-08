package util.spitotcp

import java.net.InetSocketAddress
import java.util.concurrent.{ Executors, BlockingQueue }
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
import fc.device.gps.{ Message, MessageParser, CompositeParser, CompositeMessage }
import MessageParser._
import fc.device.gps.ublox.{ UbxParser, UbxMessage }
import fc.device.gps.nmea.{ NmeaParser, NmeaMessage }
import fc.device.gps.fs2.Gps
import fc.metrics.{ StatisticalMeasures, AggregationBuffer }

object SpiToTcp {

  type Msg = CompositeMessage[NmeaMessage, UbxMessage]
  type Port = Int Refined Interval.Closed[W.`1`.T, W.`65535`.T]

  def apply(port: Port): Unit = {
    val (gpsInput, gpsOutput) = Gps(
      SpiAddress(busNumber = 0, chipSelect = 0),
      100.milliseconds,
      maxBytesToTransfer,
      newParser _,
      blockingIO
    )(
      spiController,
      timer,
      cs
    )

    val app = for {
      clientSocket <- createSocket(port)
      inputStream = receiveFromClient(clientSocket, gpsInput)
      outputStream = gpsOutput through transmitToClient(clientSocket)
      metricStream = Stream.awakeEvery[IO](1.second) map { _ => println(spiController.observeMetrics()) }
      _ <- Stream(inputStream, outputStream, metricStream).parJoinUnbounded
    } yield ()

    app.compile.drain.unsafeRunSync()
  }

  private def createSocket(port: Port): Stream[IO, Socket[IO]] = {
    implicit val acg = AsynchronousChannelGroup.withThreadPool(Executors.newCachedThreadPool())
    for {
      clientResource <- Socket.server[IO](new InetSocketAddress("0.0.0.0", port))
      clientSocket <- Stream.resource(clientResource)
    } yield clientSocket
  }

  private def receiveFromClient(client: Socket[IO], gpsInput: BlockingQueue[Msg]): Stream[IO, Unit] = {
    val bytesFromClient = client.reads(maxBytesToTransfer).onFinalize(client.endOfOutput)
    val messagesFromClient = bytesFromClient through parseMessages()

    messagesFromClient flatMap {
      msg => Stream.eval_(IO.delay{ gpsInput.put(msg) })
    }
  }

  private def parseMessages(): Pipe[IO, Byte, Msg] = s => parseMessages0(s, newParser()).stream

  private def newParser() = CompositeParser(NmeaParser(), UbxParser())

  private def parseMessages0(s: Stream[IO, Byte], parser: MessageParser[Msg]): Pull[IO, Msg, Unit] = {
    s.pull.uncons1.flatMap {
      case None => Pull.pure(None)
      case Some((byte, rest)) => parser.consume(byte) match {
        case Unconsumed(_) => parseMessages0(rest, parser)
        case Proceeding(nextParser) => parseMessages0(rest, nextParser)
        case Done(msg) => Pull.output1(msg) >> parseMessages0(rest, newParser())
        case Failed(cause) => Pull.eval(IO.delay{ logger.error(cause) }) >>
          parseMessages0(rest, newParser())
      }
    }
  }

  private def transmitToClient(client: Socket[IO]): Pipe[IO, Msg, Unit] = (input: Stream[IO, Msg]) =>
    input flatMap {
      msg => Stream.chunk(Chunk.seq(msg.toBytes))
    } through client.writes()

  private implicit val timer = IO.timer(ExecutionContext.global)
  private implicit val cs = IO.contextShift(ExecutionContext.global)
  private val blockingIO = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
  private val spiController = SpiController()
  private val maxBytesToTransfer: Int Refined Positive = 100
  private val logger = LoggerFactory.getLogger(this.getClass)
}

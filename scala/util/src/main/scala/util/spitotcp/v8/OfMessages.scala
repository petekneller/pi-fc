package util.spitotcp.v8

import java.net.InetSocketAddress
import java.util.concurrent.Executors
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import org.slf4j.LoggerFactory
import eu.timepit.refined.W
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.{ Interval, Positive }
import eu.timepit.refined.auto.{autoRefineV, autoUnwrap}
import cats.effect.{ Blocker, IO, Timer }
import fs2.{ Stream, Pipe, Chunk, Pull }
import fs2.io.tcp.{Socket, SocketGroup}
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

  val spiController = SpiController()

  def apply(port: Port): Unit = {

    val app = for {
      blocker <- Stream.resource(Blocker[IO])
      socketGroup <- Stream.resource(SocketGroup[IO](blocker))
      clientResource <- socketGroup.server[IO](new InetSocketAddress("0.0.0.0", port))
      clientSocket <- Stream.resource(clientResource)
      _ <- handlePeer(clientSocket)
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
      case None => Pull.done
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

  val maxBytesToTransfer: Int Refined Positive = 100
  val delay = 100.milliseconds

  def spiTransfer(bytes: Seq[Byte]): IO[Seq[Byte]] = IO.delay{
    spiController.transfer(gps, bytes).right.get
  }

  def spiReceive(): IO[Seq[Byte]] = IO.delay{
    spiController.receive(gps, maxBytesToTransfer).right.get
  }

  def receiveFromClient(client: Socket[IO]): Stream[IO, Byte] =
    client.reads(maxBytesToTransfer).onFinalize(client.endOfOutput)

  def transmitToClient(client: Socket[IO]): Pipe[IO, Byte, Unit] = (input: Stream[IO, Byte]) =>
    input.chunks.flatMap{ bytes => Stream.eval_(client.write(bytes)) }

}

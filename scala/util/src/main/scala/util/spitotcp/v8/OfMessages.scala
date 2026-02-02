package util.spitotcp.v8

import scala.concurrent.duration._
import org.slf4j.LoggerFactory
import eu.timepit.refined.W
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.{ Interval, Positive }
import eu.timepit.refined.auto.{autoRefineV, autoUnwrap}
import com.comcast.ip4s._
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import fs2.{ Stream, Pipe, Chunk, Pull }
import fs2.io.net.{ Network, Socket }
import fc.device.controller.spi.{ SpiAddress, SpiController }
import fc.device.gps.{ Message, MessageParser, CompositeParser }
import MessageParser._
import fc.device.gps.ublox.UbxParser
import fc.device.gps.nmea.NmeaParser

object OfMessages {
  type Port = Int Refined Interval.Closed[W.`1`.T, W.`65535`.T]

  val logger = LoggerFactory.getLogger(this.getClass)

  val spiController = SpiController()

  def apply(port: Port): Unit = {
    val p = com.comcast.ip4s.Port.fromInt(port).get

    val app = Network[IO].server(address = Some(ip"0.0.0.0"), port = Some(p)).flatMap { clientSocket =>
      handlePeer(clientSocket)
    }

    app.compile.drain.unsafeRunSync()
  }

  def handlePeer(client: Socket[IO]): Stream[IO, Unit] = {
    val ping = Stream.awakeEvery[IO](delay)

    val bytesFromClient = receiveFromClient(client)
    val messagesFromClient = bytesFromClient through messagePrinting("<-")

    val transferViaSpi: Pipe[IO, Either[Message, FiniteDuration], Byte] = upstream => {
      upstream flatMap {
        case Left(clientMsg) => Stream.eval(spiTransfer(clientMsg.toBytes))
        case Right(_) => Stream.eval(spiReceive())
      } flatMap {
        case gpsBytes => Stream.chunk(Chunk.from(gpsBytes))
      }
    }

    (messagesFromClient either ping) through
      transferViaSpi through
      messagePrinting("->") flatMap { msg => Stream.chunk(Chunk.from(msg.toBytes)) } through
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
        case Done(msg) => Pull.output1(msg) >> Pull.eval(IO.blocking{ logger.info(s"$dir ${msg.toString}") }) >>
          printMessages(rest, dir, newParser())
        case Failed(cause) => Pull.eval(IO.blocking{ logger.error(cause) }) >>
          printMessages(rest, dir, newParser())
      }
    }
  }

  val gps = SpiAddress(busNumber = 0, chipSelect = 0)

  val maxBytesToTransfer: Int Refined Positive = 100
  val delay = 100.milliseconds

  def spiTransfer(bytes: Seq[Byte]): IO[Seq[Byte]] = IO.blocking{
    spiController.transfer(gps, bytes).toOption.get
  }

  def spiReceive(): IO[Seq[Byte]] = IO.blocking{
    spiController.receive(gps, maxBytesToTransfer).toOption.get
  }

  def receiveFromClient(client: Socket[IO]): Stream[IO, Byte] =
    client.reads.onFinalize(client.endOfOutput)

  def transmitToClient(client: Socket[IO]): Pipe[IO, Byte, Unit] = (input: Stream[IO, Byte]) =>
    input.chunks.flatMap{ bytes => Stream.exec(client.write(bytes)) }

}

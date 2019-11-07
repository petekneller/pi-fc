package util.spitotcp

import java.net.InetSocketAddress
import java.util.concurrent.Executors
import java.nio.channels.AsynchronousChannelGroup
import java.util.concurrent.TimeUnit.NANOSECONDS
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import org.slf4j.LoggerFactory
import eu.timepit.refined.auto.{ autoRefineV, autoUnwrap }
import cats.effect.{ IO, Timer }
import fs2.{ Stream, Pipe, Chunk, Pull }
import fs2.concurrent.SignallingRef
import fs2.io.tcp.Socket
import fc.device.gps.{ Message, MessageParser, CompositeParser }
import MessageParser._
import fc.device.gps.ublox.UbxParser
import fc.device.gps.nmea.NmeaParser
import v7.SpiToTcp.{ spiTransfer, spiReceive, delay, receiveFromClient, transmitToClient }
import OfBytes.{ Port }

object OfMessages {

  val logger = LoggerFactory.getLogger(this.getClass)

  implicit val timer: Timer[IO] = IO.timer(ExecutionContext.global)
  implicit val cs = IO.contextShift(ExecutionContext.global)
  val blockingIO = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

  def apply(port: Port): Unit = {

    implicit val acg = AsynchronousChannelGroup.withThreadPool(Executors.newCachedThreadPool())
    val app = for {
      clientResource <- Socket.server[IO](new InetSocketAddress("0.0.0.0", port))
      clientSocket <- Stream.resource(clientResource)
      _ <- handlePeer(clientSocket)
    } yield ()

    app.compile.drain.unsafeRunSync()
  }

  def handlePeer(client: Socket[IO]): Stream[IO, Unit] = {
    val ping = Stream.awakeEvery[IO](delay)

    val bytesFromClient = receiveFromClient(client)

    val chunksFromClient = (bytesFromClient through messagePrinting).chunks.map(_.toArray: Seq[Byte])

    val transferViaSpi: Pipe[IO, Either[Seq[Byte], FiniteDuration], Byte] = upstream => {
      upstream flatMap {
        case Left(clientBytes) => Stream.eval(cs.evalOn(blockingIO)(spiTransfer(clientBytes)))
        case Right(_) => Stream.eval(cs.evalOn(blockingIO)(spiReceive()))
      } flatMap {
        case gpsBytes => Stream.chunk(Chunk.seq(gpsBytes))
      }
    }

    (chunksFromClient either ping) through transferViaSpi through messagePrinting through transmitToClient(client)
  }

  val messagePrinting: Pipe[IO, Byte, Byte] = s => printMessages(s, newParser()).stream

  def newParser() = CompositeParser(NmeaParser(), UbxParser())

  def printMessages[A <: Message](s: Stream[IO, Byte], parser: MessageParser[A]): Pull[IO, Byte, Unit] = {
    s.pull.uncons1.flatMap {
      case None => Pull.pure(None)
      case Some((byte, rest)) => parser.consume(byte) match {
        case Unconsumed(_) => Pull.output1(byte) >> printMessages(rest, parser)
        case Proceeding(nextParser) => Pull.output1(byte) >> printMessages(rest, nextParser)
        case Done(msg) => Pull.output1(byte) >> Pull.eval(IO.delay{ logger.info(msg.toString) }) >>
          printMessages(rest, newParser())
        case Failed(cause) => Pull.output1(byte) >> Pull.eval(IO.delay{ logger.error(cause) }) >>
          printMessages(rest, newParser())
      }
    }
  }
}

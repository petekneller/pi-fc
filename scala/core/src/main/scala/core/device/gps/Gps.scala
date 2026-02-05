package core.device.gps

import java.util.concurrent.{ BlockingQueue, LinkedBlockingQueue }
import scala.concurrent.duration.FiniteDuration
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.auto.autoRefineV
import org.slf4j.LoggerFactory
import cats.effect.IO
import fs2.{ Stream, Pull, Pipe, Chunk }
import core.device.controller.spi.{ SpiFullDuplexController, SpiAddress }
import MessageParser.{ Unconsumed, Proceeding, Done, Failed }
import scala.concurrent.duration.DurationInt

/*
 * GPS-specific equivalent of the `Device` - a utility that binds GPS message parsing to the appropriate controller and address.
 * FS2 provides the layer to manage the parsing state between GPS accesses.
 */
trait Gps[M <: Message] {

  val input: BlockingQueue[M]
  val output: Stream[IO, M]
}

object Gps {

  def apply[M <: Message](
    address: SpiAddress,
    newParser: () => MessageParser[M],
    pollInterval: FiniteDuration = 100.milliseconds,
    numPollingBytes: Int Refined Positive = 100
  )(
    implicit spi: SpiFullDuplexController
  ): Gps[M] = new Gps[M] {

    override val input = new LinkedBlockingQueue[M]()
    val inputStream = Stream.eval(IO.blocking{ input.take() }).repeat

    val polling = Stream.awakeEvery[IO](pollInterval)

    override val output = (inputStream either polling) flatMap {
      case Left(msg) => Stream.eval(IO.blocking{ spi.transfer(address, msg.toBytes) })
      case Right(_) => Stream.eval(IO.blocking{ spi.receive(address, numPollingBytes) })
    } flatMap {
      case Left(cause) => Stream.exec(IO.delay{ logger.error(s"Device exception reading GPS: ${cause.toString}") })
      case Right(bytes) => Stream.chunk(Chunk.from(bytes))
    } through parseStream(newParser)
  }

  def parseStream[M <: Message](newParser: () => MessageParser[M]): Pipe[IO, Byte, M] = {
    def parse0(s: Stream[IO, Byte], parser: MessageParser[M]): Pull[IO, M, Unit] = {
      s.pull.uncons1 flatMap {
        case None => Pull.done
        case Some((byte, rest)) => parser.consume(byte) match {
          case Unconsumed(_) => parse0(rest, newParser())
          case Proceeding(next) => parse0(rest, next)
          case Done(msg) => Pull.output1(msg) >>
            parse0(rest, newParser())
          case Failed(cause) => Pull.eval(IO.delay{ logger.error(s"Message parsing failed: ${cause}") }) >>
            parse0(rest, newParser())
        }
      }
    }

    s => parse0(s, newParser()).stream
  }

  private val logger = LoggerFactory.getLogger(this.getClass)
}

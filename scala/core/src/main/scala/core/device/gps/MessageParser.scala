package core.device.gps

import org.slf4j.LoggerFactory
import cats.effect.IO
import fs2.{ Stream, Pipe, Pull }

trait Message {
  def toBytes: Seq[Byte]
}

trait MessageParser[Msg <: Message] {
  def consume(byte: Byte): MessageParser.ParseState[Msg]
}

/*
 * [a = new MessageParser] -> a.consume(byte) -> [Proceeding(b)] -> b.consume(byte) -> [Done(msg)]
 *                                  |                 ^                   |
 *      (start byte not recognised) |                 |                   |
 *                                  |                  -------------------
 *                             [Unconsumed]
 */

object MessageParser {
  sealed trait ParseState[Msg]
  case class Unconsumed[Msg <: Message](bytes: Seq[Byte]) extends ParseState[Msg]
  case class Proceeding[Msg <: Message](next: MessageParser[Msg]) extends ParseState[Msg]
  case class Done[Msg <: Message](message: Msg) extends ParseState[Msg]
  case class Failed[Msg <: Message](cause: String) extends ParseState[Msg]

  def pipe[M <: Message](newParser: () => MessageParser[M]): Pipe[IO, Byte, M] = {
    def parse0(s: Stream[IO, Byte], parser: MessageParser[M]): Pull[IO, M, Unit] = {
      s.pull.uncons1 flatMap {
        case None => Pull.done
        case Some((byte, rest)) => parser.consume(byte) match {
          case Unconsumed(_) => parse0(rest, newParser())
          case Proceeding(next) => parse0(rest, next)
          case Done(msg) => Pull.output1(msg) >>
            parse0(rest, newParser())
          case Failed(cause) => Pull.eval(IO.blocking{ logger.error(s"Message parsing failed: ${cause}") }) >>
            parse0(rest, newParser())
        }
      }
    }

    s => parse0(s, newParser()).stream
  }

  private val logger = LoggerFactory.getLogger(this.getClass)
}

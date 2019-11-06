package fc.device.gps

trait Message

trait MessageParser {
  def consume(byte: Byte): MessageParser.ParseState
}

/*
 * [a = new MessageParser] -> a.consume(byte) -> [Proceeding(b)] -> b.consume(byte) -> [Done(msg)]
 *                                  |                 ^                   |
 *      (start byte not recognised) |                 |                   |
 *                                  |                  -------------------
 *                             [Unconsumed]
 */

object MessageParser {
  sealed trait ParseState
  case class Unconsumed(bytes: Seq[Byte]) extends ParseState
  case class Proceeding(next: MessageParser) extends ParseState
  case class Done(message: Message) extends ParseState
  case class Failed(cause: String) extends ParseState
}

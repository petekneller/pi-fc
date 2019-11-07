package fc.device.gps

trait Message

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
}

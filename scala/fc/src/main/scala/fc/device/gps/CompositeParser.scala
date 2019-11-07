package fc.device.gps

import MessageParser._

class CompositeParser(a: MessageParser, b: MessageParser) extends MessageParser {
  def consume(byte: Byte): ParseState = a.consume(byte) match {
    case Unconsumed(_) => b.consume(byte)
    case state => state
  }
}

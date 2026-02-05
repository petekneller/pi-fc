package core.device.gps

import MessageParser._

sealed trait CompositeMessage[A <: Message, B <: Message] extends Message
case class CLeft[A <: Message, B <: Message](msg: A) extends CompositeMessage[A, B] {
  def toBytes: Seq[Byte] = msg.toBytes
}
case class CRight[A <: Message, B <: Message](msg: B) extends CompositeMessage[A, B] {
  def toBytes: Seq[Byte] = msg.toBytes
}

object CompositeParser {
  def apply[A <: Message, B <: Message](a: MessageParser[A], b: MessageParser[B]): CompositeParser[A, B] =
    new CompositeParser(a, b)

  private class WrapperA[A <: Message, B <: Message](delegate: MessageParser[A]) extends MessageParser[CompositeMessage[A, B]] {
    def consume(byte: Byte): ParseState[CompositeMessage[A, B]] = delegate.consume(byte) match {
      case Unconsumed(bytes) => unconsumed(bytes)
      case Proceeding(next) => Proceeding(new WrapperA(next))
      case Done(msg) => Done(CLeft(msg))
      case Failed(cause) => failed(cause)
    }
  }

  private class WrapperB[A <: Message, B <: Message](delegate: MessageParser[B]) extends MessageParser[CompositeMessage[A, B]] {
    def consume(byte: Byte): ParseState[CompositeMessage[A, B]] = delegate.consume(byte) match {
      case Unconsumed(bytes) => unconsumed(bytes)
      case Proceeding(next) => Proceeding(new WrapperB(next))
      case Done(msg) => Done(CRight(msg))
      case Failed(cause) => failed(cause)
    }
  }

  private def unconsumed[A <: Message, B <: Message](bytes: Seq[Byte]): ParseState[CompositeMessage[A, B]] = Unconsumed(bytes)

  private def failed[A <: Message, B <: Message](cause: String): ParseState[CompositeMessage[A, B]] = Failed(cause)
}

class CompositeParser[A <: Message, B <: Message](a: MessageParser[A], b: MessageParser[B]) extends MessageParser[CompositeMessage[A, B]] {
  import CompositeParser._

  def consume(byte: Byte): ParseState[CompositeMessage[A, B]] = a.consume(byte) match {
    case Unconsumed(_) => b.consume(byte) match {
      case Unconsumed(bytes) => unconsumed(bytes)
      case Proceeding(next) => Proceeding(new WrapperB(next))
      case Done(msg) => Done(CRight(msg))
      case Failed(cause) => failed(cause)
    }
    case Proceeding(next) => Proceeding(new WrapperA(next))
    case Done(msg) => Done(CLeft(msg))
    case Failed(cause) => failed(cause)
  }
}

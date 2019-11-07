package fc.device.gps

import MessageParser._

class CompositeParser[A <: Message, B <: Message](a: MessageParser[A], b: MessageParser[B]) extends MessageParser[CompositeMessage[A, B]] {
  def consume(byte: Byte): ParseState[CompositeMessage[A, B]] = a.consume(byte) match {
    case Unconsumed(_) => b.consume(byte) match {
      case Unconsumed(bytes) => Unconsumed[CompositeMessage[A, B]](bytes)
      case Proceeding(next) => Proceeding(new WrapperB(next))
      case Done(msg) => Done(Right(msg))
      case Failed(cause) => Failed[CompositeMessage[A, B]](cause)
    }
    case Proceeding(next) => Proceeding(new WrapperA(next))
    case Done(msg) => Done(Left(msg))
    case Failed(cause) => Failed[CompositeMessage[A, B]](cause)
  }
}

class WrapperA[A <: Message, B <: Message](delegate: MessageParser[A]) extends MessageParser[CompositeMessage[A, B]] {
  def consume(byte: Byte): ParseState[CompositeMessage[A, B]] = delegate.consume(byte) match {
    case Unconsumed(bytes) => Unconsumed[CompositeMessage[A, B]](bytes)
    case Proceeding(next) => Proceeding(new WrapperA(next))
    case Done(msg) => Done(Left(msg))
    case Failed(cause) => Failed[CompositeMessage[A, B]](cause)
  }
}

class WrapperB[A <: Message, B <: Message](delegate: MessageParser[B]) extends MessageParser[CompositeMessage[A, B]] {
  def consume(byte: Byte): ParseState[CompositeMessage[A, B]] = delegate.consume(byte) match {
    case Unconsumed(bytes) => Unconsumed[CompositeMessage[A, B]](bytes)
    case Proceeding(next) => Proceeding(new WrapperB(next))
    case Done(msg) => Done(Right(msg))
    case Failed(cause) => Failed[CompositeMessage[A, B]](cause)
  }
}

sealed trait CompositeMessage[A <: Message, B <: Message] extends Message
case class Left[A <: Message, B <: Message](msg: A) extends CompositeMessage[A, B]
case class Right[A <: Message, B <: Message](msg: B) extends CompositeMessage[A, B]

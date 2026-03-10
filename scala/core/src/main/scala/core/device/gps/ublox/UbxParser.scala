package core.device.gps.ublox

import cats.syntax.eq._
import cats.instances.int._
import ioctl.syntax._
import core.device.gps.MessageParser
import MessageParser._

sealed trait UbxParser extends MessageParser[UbxMessage]

object UbxParser {
  def apply(): UbxParser = Empty

  val preamble1 = 0xB5.toByte
  val preamble2 = 0x62.toByte
}

object Empty extends UbxParser {
  def consume(byte: Byte): ParseState[UbxMessage] = byte match {
    case UbxParser.preamble1 => Proceeding(AwaitingPreamble2)
    case _ => Unconsumed(Seq(byte))
  }
}

object AwaitingPreamble2 extends UbxParser {
  def consume(byte: Byte): ParseState[UbxMessage] = byte match {
    case UbxParser.preamble2 => Proceeding(AwaitingClass)
    case _ => Unconsumed(Seq(UbxParser.preamble1, byte))
  }
}

object AwaitingClass extends UbxParser {
  def consume(byte: Byte): ParseState[UbxMessage] = Proceeding(AwaitingId(byte))
}

case class AwaitingId(clazz: Byte) extends UbxParser {
  def consume(id: Byte): ParseState[UbxMessage] = Proceeding(AwaitingLength1(clazz, id))
}

case class AwaitingLength1(clazz: Byte, id: Byte) extends UbxParser {
  def consume(length1: Byte): ParseState[UbxMessage] = Proceeding(AwaitingLength2(clazz, id, length1))
}

case class AwaitingLength2(clazz: Byte, id: Byte, length1: Byte) extends UbxParser {
  def consume(length2: Byte): ParseState[UbxMessage] = {
    val length = (length2.unsigned << 8) + length1.unsigned
    if (length === 0)
      Proceeding(AwaitingCkA(clazz, id, Seq.empty[Byte]))
    else
      Proceeding(ConsumingPayload(clazz, id, length, Seq.empty[Byte]))
  }
}

case class ConsumingPayload(clazz: Byte, id: Byte, payloadBytesUnread: Int, payload: Seq[Byte]) extends UbxParser {
  def consume(byte: Byte): ParseState[UbxMessage] =
    if (payloadBytesUnread > 1)
      Proceeding(ConsumingPayload(clazz, id, payloadBytesUnread - 1, payload :+ byte))
    else
      Proceeding(AwaitingCkA(clazz, id, payload :+ byte))

}

case class AwaitingCkA(clazz: Byte, id: Byte, payload: Seq[Byte]) extends UbxParser {
  def consume(ckA: Byte): ParseState[UbxMessage] = Proceeding(AwaitingCkB(clazz, id, payload, ckA))
}

case class AwaitingCkB(clazz: Byte, id: Byte, payload: Seq[Byte], ckA: Byte) extends UbxParser {
  def consume(ckB: Byte): ParseState[UbxMessage] = {
    val expectedChecksum = UbxChecksum(clazz, id, payload)
    val actualChecksum = UbxChecksum(ckA, ckB)
    if (actualChecksum != expectedChecksum)
      return Failed("Checksum not correct")

    UbxMessage.parse(clazz, id, payload) match {
      case Left(cause) => Failed(cause)
      case Right(msg) => Done(msg)
    }
  }
}

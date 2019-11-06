package fc.device.gps.ublox

import cats.syntax.eq._
import cats.instances.int._
import ioctl.syntax._
import fc.device.gps.MessageParser
import MessageParser._

sealed trait UbxParser extends MessageParser

object UbxParser {
  def apply(): UbxParser = Empty

  val preamble1 = 0xB5.toByte
  val preamble2 = 0x62.toByte
}

object Empty extends UbxParser {
  def consume(byte: Byte): ParseState = byte match {
    case UbxParser.preamble1 => Proceeding(AwaitingPreamble2)
    case _ => Unconsumed(Seq(byte))
  }
}

object AwaitingPreamble2 extends UbxParser {
  import UbxParser._
  def consume(byte: Byte): ParseState = byte match {
    case UbxParser.preamble2 => Proceeding(AwaitingClass)
    case _ => Unconsumed(Seq(UbxParser.preamble1, byte))
  }
}

object AwaitingClass extends UbxParser {
  def consume(byte: Byte): ParseState = Proceeding(AwaitingId(byte))
}

case class AwaitingId(clazz: Byte) extends UbxParser {
  def consume(id: Byte): ParseState = Proceeding(AwaitingLength1(clazz, id))
}

case class AwaitingLength1(clazz: Byte, id: Byte) extends UbxParser {
  def consume(length1: Byte): ParseState = Proceeding(AwaitingLength2(clazz, id, length1))
}

case class AwaitingLength2(clazz: Byte, id: Byte, length1: Byte) extends UbxParser {
  def consume(length2: Byte): ParseState = {
    val length = (length2.unsigned << 8) + length1.unsigned
    if (length === 0)
      Proceeding(AwaitingChecksum1(clazz, id, Seq.empty[Byte]))
    else
      Proceeding(ConsumingPayload(clazz, id, length, Seq.empty[Byte]))
  }
}

case class ConsumingPayload(clazz: Byte, id: Byte, payloadBytesUnread: Int, payload: Seq[Byte]) extends UbxParser {
  def consume(byte: Byte): ParseState =
    if (payloadBytesUnread > 1)
      Proceeding(ConsumingPayload(clazz, id, payloadBytesUnread - 1, payload :+ byte))
    else
      Proceeding(AwaitingChecksum1(clazz, id, payload :+ byte))

}

case class AwaitingChecksum1(clazz: Byte, id: Byte, payload: Seq[Byte]) extends UbxParser {
  def consume(checksum1: Byte): ParseState = Proceeding(AwaitingChecksum2(clazz, id, payload, checksum1))
}

case class AwaitingChecksum2(clazz: Byte, id: Byte, payload: Seq[Byte], checksum1: Byte) extends UbxParser {
  def consume(checksum2: Byte): ParseState = Done(Unknown(clazz, id, payload, checksum1, checksum2))
}

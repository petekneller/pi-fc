package core.device.gps.ublox

import core.device.gps.Message

sealed trait UbxMessage extends Message {
  val clazz: Byte
  val id: Byte
  def payloadLength: Int
  def checksum: (Byte, Byte)
}

object UbxMessage {
  def parse(clazz: Byte, id: Byte, payload: Seq[Byte], checksum1: Byte, checksum2: Byte): Either[String, UbxMessage] = (clazz, id) match {
    case (RxBufferPoll.clazz, RxBufferPoll.id) => Right(RxBufferPoll)
    case (TxBufferPoll.clazz, TxBufferPoll.id) => Right(TxBufferPoll)
    case _ => Right(Unknown(clazz, id, payload, checksum1, checksum2))
  }
}

case class Unknown(clazz: Byte, id: Byte, payload: Seq[Byte], checksum1: Byte, checksum2: Byte) extends UbxMessage {
  override def toString(): String = s"Unknown[UBX](class=$clazz, id=$id)"
  def payloadLength: Int = payload.length
  def checksum: (Byte, Byte) = (checksum1, checksum2)

  import UbxParser.{ preamble1, preamble2 }
  def toBytes: Seq[Byte] = {
    val length1 = (payload.length & 0xFF).toByte
    val length2 = ((payload.length >> 8) & 0xFF).toByte
    (preamble1 :: preamble2 :: clazz :: id :: length1 :: length2 :: payload.toList) :+ checksum1 :+ checksum2
  }
}

case object RxBufferPoll extends UbxMessage {
  val clazz: Byte = 0x0A.toByte
  val id: Byte = 0x07.toByte
  def payloadLength: Int = 0
  def checksum: (Byte, Byte) = (0x11.toByte, 0x3D.toByte)
  def toBytes: Seq[Byte] = Unknown(clazz, id, Seq.empty[Byte], checksum._1, checksum._2).toBytes
}

case object TxBufferPoll extends UbxMessage {
  val clazz: Byte = 0x0A.toByte
  val id: Byte = 0x08.toByte
  def payloadLength: Int = 0
  def checksum: (Byte, Byte) = (0x12.toByte, 0x40.toByte)
  def toBytes: Seq[Byte] = Unknown(clazz, id, Seq.empty[Byte], checksum._1, checksum._2).toBytes
}

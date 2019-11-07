package fc.device.gps.ublox

import fc.device.gps.Message

sealed trait UbxMessage extends Message {
  def clazz: Byte
  def id: Byte
  def payloadLength: Int
  def checksum: (Byte, Byte)
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

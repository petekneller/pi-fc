package fc.device.gps.ublox

import fc.device.gps.Message

sealed trait UbxMessage extends Message
case class Unknown(clazz: Byte, id: Byte, payload: Seq[Byte], checksum1: Byte, checksum2: Byte) extends UbxMessage

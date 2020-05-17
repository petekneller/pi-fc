package fc.device.gps.ublox

object examples {

  val UbxConfigPowerPoll = {
    val clazz = 0x06.toByte
    val id = 0x57.toByte
    val payloadLength = (0x00.toByte, 0x00.toByte)
    val checksum = (0x5D.toByte, 0x1D.toByte)
    Example(
      bytes = Seq(0xB5, 0x62, 0x06, 0x57, 0x00, 0x00, 0x5D, 0x1D).map(_.toByte),
      clazz = clazz,
      id = id,
      payload = Seq.empty,
      payloadLength = payloadLength,
      checksum = checksum,
      msg = Unknown(clazz, id, Seq.empty, checksum._1, checksum._2)
    )
  }

  val UbxConfigPower = {
    // B5 62 06 57 08 00 F2 17 00 00 00 40 00 00 AE 46
    val clazz = 0x06.toByte
    val id = 0x57.toByte
    val payload = Seq(0xF2, 0x17, 0x00, 0x00, 0x00, 0x40, 0x00, 0x00).map(_.toByte)
    val payloadLength = (0x08.toByte, 0x00.toByte)
    val checksum = (0xAE.toByte, 0x46.toByte)
    Example(
      bytes = Seq(0xB5, 0x62, 0x06, 0x57, 0x08, 0x00, 0xF2, 0x17, 0x00, 0x00, 0x00, 0x40, 0x00, 0x00, 0xAE, 0x46).map(_.toByte),
      clazz = clazz,
      id = id,
      payload = payload,
      payloadLength = payloadLength,
      checksum = checksum,
      msg = Unknown(clazz, id, payload, checksum._1, checksum._2)
    )
  }

  val UbxAckAck = {
    // B5 62 05 01 02 00 06 57 65 8E
    val clazz = 0x05.toByte
    val id = 0x01.toByte
    val payload = Seq(0x06, 0x57).map(_.toByte)
    val payloadLength = (0x02.toByte, 0x00.toByte)
    val checksum = (0x65.toByte, 0x8E.toByte)
    Example(
      bytes = Seq(0xB5, 0x62, 0x05, 0x01, 0x02, 0x00, 0x06, 0x57, 0x65, 0x8E).map(_.toByte),
      clazz = clazz,
      id = id,
      payload = payload,
      payloadLength = payloadLength,
      checksum = checksum,
      msg = Unknown(clazz, id, payload, checksum._1, checksum._2)
    )
  }

}

case class Example(
  bytes: Seq[Byte],
  clazz: Byte,
  id: Byte,
  payloadLength: (Byte, Byte),
  payload: Seq[Byte],
  checksum: (Byte, Byte),
  msg: UbxMessage
)

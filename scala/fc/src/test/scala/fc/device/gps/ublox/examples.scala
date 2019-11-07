package fc.device.gps.ublox

object examples {

  object UbxConfigPowerPoll {
    val bytes: Seq[Byte] = Seq(0xB5, 0x62, 0x06, 0x57, 0x00, 0x00, 0x5D, 0x1D).map(_.toByte)
    val clazz = 0x06.toByte
    val id = 0x57.toByte
    val payload = Seq.empty[Byte]
    val checksum1 = 0x5D.toByte
    val checksum2 = 0x1D.toByte
    val msg = Unknown(clazz, id, payload, checksum1, checksum2)
  }

  object UbxConfigPower {
    // B5 62 06 57 08 00 F2 17 00 00 00 40 00 00 AE 46
    val bytes: Seq[Byte] = Seq(0xB5, 0x62, 0x06, 0x57, 0x08, 0x00, 0xF2, 0x17, 0x00, 0x00, 0x00, 0x40, 0x00, 0x00, 0xAE, 0x46).map(_.toByte)
    val clazz = 0x06.toByte
    val id = 0x57.toByte
    val payload: Seq[Byte] = Seq(0xF2, 0x17, 0x00, 0x00, 0x00, 0x40, 0x00, 0x00).map(_.toByte)
    val checksum1 = 0xAE.toByte
    val checksum2 = 0x46.toByte
    val msg = Unknown(clazz, id, payload, checksum1, checksum2)
  }

  object UbxAckAck {
    // B5 62 05 01 02 00 06 57 65 8E
    val bytes: Seq[Byte] = Seq(0xB5, 0x62, 0x05, 0x01, 0x02, 0x00, 0x06, 0x57, 0x65, 0x8E).map(_.toByte)
    val clazz = 0x05.toByte
    val id = 0x01.toByte
    val payload: Seq[Byte] = Seq(0x06, 0x57).map(_.toByte)
    val checksum1 = 0x65.toByte
    val checksum2 = 0x8E.toByte
    val msg = Unknown(clazz, id, payload, checksum1, checksum2)
  }

}

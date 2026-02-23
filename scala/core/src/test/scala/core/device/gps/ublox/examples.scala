package core.device.gps.ublox

import core.device.gps.ExampleMessage

object examples {

  object unknown {
    val clazz = 0x01.toByte
    val id = 0x02.toByte
    val checksum1 = 0x06.toByte
    val checksum2 = 0x07.toByte
    val payload = Seq(0x03, 0x04, 0x05).map(_.toByte)
    val payloadLength1 = 0x03.toByte
    val payloadLength2 = 0x00.toByte
    val bytes = Seq(0xB5.toByte, 0x62.toByte, clazz, id, payloadLength1, payloadLength2) ++ payload ++ Seq(checksum1, checksum2)
    val msg = Unknown(clazz, id, payload, checksum1, checksum2)
  }

  val all: Seq[ExampleMessage[UbxMessage]] = Seq(
    ex(
      "UBX-CFG-PWR poll",
      Unknown(0x06.toByte, 0x57.toByte, Seq.empty, 0x5D.toByte, 0x1D.toByte),
      Seq(0xB5, 0x62, 0x06, 0x57, 0x00, 0x00, 0x5D, 0x1D).map(_.toByte)
    ),
    ex(
      "UBX-CFG-PWR", // B5 62 06 57 08 00 F2 17 00 00 00 40 00 00 AE 46
      Unknown(0x06.toByte, 0x57.toByte, Seq(0xF2, 0x17, 0x00, 0x00, 0x00, 0x40, 0x00, 0x00).map(_.toByte), 0xAE.toByte, 0x46.toByte),
      Seq(0xB5, 0x62, 0x06, 0x57, 0x08, 0x00, 0xF2, 0x17, 0x00, 0x00, 0x00, 0x40, 0x00, 0x00, 0xAE, 0x46).map(_.toByte)
    ),
    ex(
      "UBX-ACK-ACK", // B5 62 05 01 02 00 06 57 65 8E
      Unknown(0x05.toByte, 0x01.toByte, Seq(0x06, 0x57).map(_.toByte), 0x65.toByte, 0x8E.toByte),
      Seq(0xB5, 0x62, 0x05, 0x01, 0x02, 0x00, 0x06, 0x57, 0x65, 0x8E).map(_.toByte)
    )
  )

  // unused for now here on down

  val UbxMonitorRxBufferPoll =
    ex(
      "UBX-MON-RXBUF", // B5 62 0A 07 00 00 11 3D
      RxBufferPoll,
      Seq(0xB5, 0x62, 0x0A, 0x07, 0x00, 0x00, 0x11, 0x3D).map(_.toByte)
    )

  val UbxMonitorTxBufferPoll =
    ex(
      "UBX-MON-TXBUF",
      TxBufferPoll,
      Seq(0xB5, 0x62, 0x0A, 0x08, 0x00, 0x00, 0x12, 0x40).map(_.toByte)
    )

  val UbxMonitorRxBuffer = {
    // B5 62 0A 07 18 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 29 45
  }

  val UbxMonitorTxBuffer = {
    // B5 62 0A 08 1C 00 00 00 00 00 00 00 00 00 9F 02 00 00 00 00 00 00 0A 00 00 00 00 00 1B 00 0A 1B 00 00 19 A5
    // spi: ~25% ~10% 671
    // total: "
  }


  private def ex(name: String, msg: UbxMessage, bytes: Seq[Byte]) = ExampleMessage.apply[UbxMessage](name, msg, bytes)
}

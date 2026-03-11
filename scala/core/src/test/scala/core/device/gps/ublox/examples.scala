package core.device.gps.ublox

import core.device.gps.ExampleMessage
import UbxMessage.monitor._

object examples {

  val M = UbxMessage
  val C = UbxClass

  object unknown {
    val clazz = 0x01.toByte
    val id = 0x02.toByte
    val checksum1 = 0x12.toByte
    val checksum2 = 0x38.toByte
    val payload = Seq(0x03, 0x04, 0x05).map(_.toByte)
    val payloadLength1 = 0x03.toByte
    val payloadLength2 = 0x00.toByte
    val bytes = Seq(0xB5.toByte, 0x62.toByte, clazz, id, payloadLength1, payloadLength2) ++ payload ++ Seq(checksum1, checksum2)
    val msg = M.Unknown(C.Unknown(clazz), id, payload)
  }

  val UbxMonitorTxBufferPoll =
    ex(
      "UBX-MON-TXBUF(poll)",
      TxBufferPoll, // B5 62 0A 08 00 00 12 40
      Seq(0xB5, 0x62, 0x0A, 0x08, 0x00, 0x00, 0x12, 0x40).map(_.toByte)
    )

  val UbxConfigPower =
    ex(
      "UBX-CFG-PWR", // B5 62 06 57 08 00 F2 17 00 00 00 40 00 00 AE 46
      M.Unknown(C.Unknown(0x06.toByte), 0x57.toByte, Seq(0xF2, 0x17, 0x00, 0x00, 0x00, 0x40, 0x00, 0x00).map(_.toByte)),
      Seq(0xB5, 0x62, 0x06, 0x57, 0x08, 0x00, 0xF2, 0x17, 0x00, 0x00, 0x00, 0x40, 0x00, 0x00, 0xAE, 0x46).map(_.toByte)
    )

  val UbxConfigPowerPoll =
    ex(
      "UBX-CFG-PWR poll",
      M.Unknown(C.Unknown(0x06.toByte), 0x57.toByte, Seq.empty),
      Seq(0xB5, 0x62, 0x06, 0x57, 0x00, 0x00, 0x5D, 0x1D).map(_.toByte)
    )

  val all: Seq[ExampleMessage[UbxMessage]] = Seq(
    UbxConfigPowerPoll,
    UbxConfigPower,
    ex(
      "UBX-ACK-ACK", // B5 62 05 01 02 00 06 57 65 8E
      M.Unknown(C.Unknown(0x05.toByte), 0x01.toByte, Seq(0x06, 0x57).map(_.toByte)),
      Seq(0xB5, 0x62, 0x05, 0x01, 0x02, 0x00, 0x06, 0x57, 0x65, 0x8E).map(_.toByte)
    ),
    UbxMonitorTxBufferPoll,
    ex(
      "UBX-MON-TXBUF",
      // B5 62 0A 08 1C 00 00 00 00 00 00 00 00 00 9F 02 00 00 00 00 00 00 0A 00 00 00 00 00 1B 00 0A 1B 00 00 19 A5
      // spi: 10% usage last period, 27% peak, 671 bytes waiting
      // total: "
      TxBuffer(671, 10, 27, 0x00.toByte, 0x00.toByte),
      Seq(0xB5, 0x62, 0x0A, 0x08, 0x1C, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x9F, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0A, 0x00, 0x00, 0x00, 0x00, 0x00, 0x1B, 0x00, 0x0A, 0x1B, 0x00, 0x00, 0x19, 0xA5).map(_.toByte)
    )
  )

  private def ex(name: String, msg: UbxMessage, bytes: Seq[Byte]) = ExampleMessage.apply[UbxMessage](name, msg, bytes)
}

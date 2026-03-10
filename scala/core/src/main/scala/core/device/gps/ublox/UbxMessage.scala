package core.device.gps.ublox

import core.device.gps.Message
import UbxTypes._

sealed trait UbxMessage extends Message {
  val clazz: Byte
  val id: Byte
  def payload: Seq[Byte]

  override def toBytes: Seq[Byte] = {
    val Seq(length1, length2) = UbxMessage.payloadLength.toBytes(payload)
    val checksum = UbxChecksum(clazz, id, payload)

    import UbxParser.{ preamble1, preamble2 }
    (preamble1 +: preamble2 +: clazz +: id +: length1 +: length2 +: payload) :+ checksum.ckA :+ checksum.ckB
  }
}

object UbxMessage {
  def parse(clazz: Byte, id: Byte, payload: Seq[Byte]): Either[String, UbxMessage] = (clazz, id) match {
    case (TxBufferPoll.clazz, TxBufferPoll.id) if (payload.length == 0) => Right(TxBufferPoll)
    case (TxBuffer.clazz, TxBuffer.id) => Right(TxBuffer(payload))
    case _ => Right(Unknown(clazz, id, payload))
  }

  object payloadLength {
    def toBytes(payload: Seq[Byte]): Seq[Byte] = {
      val length1 = (payload.length & 0xFF).toByte
      val length2 = ((payload.length >> 8) & 0xFF).toByte
      Seq(length1, length2)
    }
  }

  private[ublox] val zero: Byte = 0x00.toByte
}

case class Unknown(clazz: Byte, id: Byte, payload: Seq[Byte]) extends UbxMessage {
  override def toString(): String = s"Unknown[UBX](class=$clazz, id=$id)"
}

case object TxBufferPoll extends UbxMessage {
  val clazz: Byte = 0x0A.toByte
  val id: Byte = 0x08.toByte
  def payload: Seq[Byte] = Seq.empty[Byte]
}

case class TxBuffer(bytesWaiting: Int, usageLastPeriod: Int, usagePeak: Int, errors: Byte, reserved: Byte) extends UbxMessage {
  val clazz: Byte = TxBuffer.clazz
  val id: Byte = TxBuffer.id

  def payload: Seq[Byte] = {
    val bw = U2.toBytes(bytesWaiting)
    import UbxMessage.zero
    Seq(
      // bytes waiting
      zero, zero, // I2C
      zero, zero, // UART1
      zero, zero, // UART2
      zero, zero, // USB
      bw._1, bw._2, // SPI
      zero, zero, // ??
      // usage last period %
      zero, // I2C
      zero, // UART1
      zero, // UART2
      zero, // USB
      U1.toBytes(usageLastPeriod), // SPI
      zero, // ??
      // peak usage %
      zero, // I2C
      zero, // UART1
      zero, // UART2
      zero, // USB
      U1.toBytes(usagePeak), // SPI
      zero, // ??
      // usage last period all interfaces %
      U1.toBytes(usageLastPeriod),
      // peak usage all interfaces %
      U1.toBytes(usagePeak),
      errors,
      reserved
    )
  }
}

object TxBuffer {
  val clazz: Byte = 0x0A.toByte
  val id: Byte = 0x08.toByte

  def apply(payload: Seq[Byte]): TxBuffer = {
    // only extracting values for the SPI interface, as that's all that's connected on the Navio2
    val bytesWaiting = U2.parse(payload(8), payload(9))
    val usageLastPeriod = U1.parse(payload(16))
    val usagePeak = U1.parse(payload(22))
    val errors = payload(26)
    val reserved = payload(27)
    TxBuffer(bytesWaiting, usageLastPeriod, usagePeak, errors, reserved)
  }
}

package core.device.gps.ublox

case class UbxChecksum(ckA: Byte, ckB: Byte)

object UbxChecksum {
  def apply(clazz: Byte, id: Byte, payload: Seq[Byte]): UbxChecksum = {
    val checksumInput = clazz +: id +: UbxMessage.payloadLength.toBytes(payload) ++: payload
    val (ckA, ckB) = checksumInput.foldLeft((0.toByte, 0.toByte)){ case ((prev_ckA, prev_ckB), byte) =>
      val ckA = (prev_ckA + byte).toByte
      val ckB = (prev_ckB + ckA).toByte
      (ckA, ckB)
    }
    UbxChecksum(ckA, ckB)
  }
}

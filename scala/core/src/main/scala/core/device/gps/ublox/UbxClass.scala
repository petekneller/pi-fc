package core.device.gps.ublox

sealed trait UbxClass {
  val byte: Byte
}

object UbxClass {
  def apply(byte: Byte): UbxClass = {
    byte match {
      case Monitor.byte => Monitor
      case _ => Unknown(byte)
    }
  }

  case class Unknown(byte: Byte) extends UbxClass

  case object Monitor extends UbxClass {
    val byte: Byte = 0x0A.toByte
  }

}

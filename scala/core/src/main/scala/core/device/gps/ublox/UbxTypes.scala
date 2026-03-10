package core.device.gps.ublox

import ioctl.syntax.ToUnsignedByte

object UbxTypes {

  object U1 {
    def parse(byte: Byte): Int = byte.unsigned.toInt

    def toBytes(value: Int): Byte = (value & 0xFF).toByte
  }

  object U2 {
    def parse(byte1: Byte, byte2: Byte): Int = {
      (byte2.unsigned << 8) + byte1.unsigned
    }

    def toBytes(value: Int): (Byte, Byte) = {
      val lsb = value & 0xFF
      val msb = (value >> 8) & 0xFF
      (lsb.toByte, msb.toByte)
    }
  }

}

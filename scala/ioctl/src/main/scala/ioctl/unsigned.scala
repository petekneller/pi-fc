package ioctl

package object unsigned {

  implicit class ToUnsignedInt(i: Int) {
    def unsigned: Long = i.toLong & 0xFFFFFFFFL
  }

  implicit class ToUnsignedShort(s: Short) {
    def unsigned: Int = s.toInt & 0xFFFF
  }

  implicit class ToUnsignedByte(b: Byte) {
    def unsigned: Short = (b.toInt & 0xFF).toShort
  }

}

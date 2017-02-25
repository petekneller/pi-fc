package ioctl

import java.nio.ByteBuffer

package object syntax {

  implicit class ToUnsignedInt(i: Int) {
    def unsigned: Long = i.toLong & 0xFFFFFFFFL
  }

  implicit class ToUnsignedShort(s: Short) {
    def unsigned: Int = s.toInt & 0xFFFF
  }

  implicit class ToUnsignedByte(b: Byte) {
    def unsigned: Short = (b.toInt & 0xFF).toShort
  }

  implicit class ByteBufferOps(bb: ByteBuffer) {
    def toSeq: Seq[Byte] = (0 until bb.limit) map (bb.get(_))
  }

}

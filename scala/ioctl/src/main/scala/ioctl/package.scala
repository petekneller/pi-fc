import com.sun.jna.Native

package object ioctl {
  Native.register(classOf[IOCtl], "c")
  Native.setPreserveLastError(true)

  val IOCtl = new IOCtl

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

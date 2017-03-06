import com.sun.jna.Native

package object ioctl {
  Native.register(classOf[IOCtlImpl], "c")
  Native.setPreserveLastError(true)

  val IOCtl = new IOCtlImpl()

}

import com.sun.jna.Native

package object ioctl {
  Native.register(classOf[IOCtlImpl], "c")

  val IOCtl = new IOCtlImpl()

}

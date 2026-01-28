import com.sun.jna.Native

package object spidev {
  Native.register(classOf[IOCtlImpl], "c")

  val IOCtl = new IOCtlImpl()
}

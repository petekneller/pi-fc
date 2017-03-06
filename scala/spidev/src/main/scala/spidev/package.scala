import com.sun.jna.Native

package object spidev {
  Native.register(classOf[IOCtlImpl], "c")
  Native.setPreserveLastError(true)

  val IOCtl = new IOCtlImp()
}

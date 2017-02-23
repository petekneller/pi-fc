import com.sun.jna.Native

package object spidev {
  Native.register(classOf[IOCtl], "c")
  Native.setPreserveLastError(true)

  val IOCtl = new IOCtl()
}

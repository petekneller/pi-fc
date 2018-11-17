package fc.device.controller.spi

import cats.syntax.either._
import ioctl.syntax._
import fc.device.api._

case class ShortRx(loByteRegister: Byte, hiByteRegister: Byte) extends Rx {
  type T = Short
  type Ctrl = SpiController

  def read(device: SpiAddress)(implicit controller: SpiController): DeviceResult[Short] = for {
    hiByte <- hiRx.read(device)
    loByte <- loRx.read(device)
  } yield ((hiByte << 8) | loByte.unsigned).toShort // if the low byte isn't unsigned before widening, any signing high bits will
                                                    // wipe away the data in the high register, but this isn't true the other way around
  private val hiRx = ByteRx(hiByteRegister)
  private val loRx = ByteRx(loByteRegister)
}

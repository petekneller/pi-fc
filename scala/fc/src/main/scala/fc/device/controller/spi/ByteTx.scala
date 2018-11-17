package fc.device.controller.spi

import fc.device.api._

case class ByteTx(destinationRegister: Byte) extends Tx {
  type T = Byte
  type Ctrl = SpiController

  def write(device: SpiAddress, value: Byte)(implicit controller: Ctrl) = controller.transmit(device, destinationRegister, Seq(value))
}

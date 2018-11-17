package fc.device.controller.spi

import fc.device.api._

case class ByteTx(destinationRegister: Byte) extends Tx {
  type T = Byte
  type Ctrl = SpiRegisterController

  def write(device: SpiAddress, value: Byte)(implicit controller: SpiRegisterController) = controller.transmit(device, destinationRegister, Seq(value))
}

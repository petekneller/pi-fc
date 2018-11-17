package fc.device.controller.spi

import fc.device.api._

object ByteTx {

  def apply[A](destinationRegister: Byte) = new Tx {
    type T = Byte
    type Ctrl = SpiController

    def write(device: SpiAddress, value: Byte)(implicit controller: Ctrl) = controller.transmit(device, destinationRegister, Seq(value))
  }

}

package fc.device.controller.spi

import fc.device.api._

case class ByteConfiguration(register: Byte) extends Configuration {
  type T = Byte
  type Ctrl = SpiController

  def read(device: SpiAddress)(implicit controller: SpiController): DeviceResult[Byte] =
    rx.read(device)

  def write(device: SpiAddress, value: Byte)(implicit controller: SpiController): DeviceResult[Unit] =
    tx.write(device, value)

  private val rx = ByteRx(register)
  private val tx = ByteTx(register)
}

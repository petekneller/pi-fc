package fc.device.controller.spi

import fc.device.api._

case class ByteConfiguration(register: Byte) extends Configuration {
  type T = Byte
  type Ctrl = SpiRegisterController

  def read(device: SpiAddress)(implicit controller: SpiRegisterController): DeviceResult[Byte] =
    rx.read(device)

  def write(device: SpiAddress, value: Byte)(implicit controller: SpiRegisterController): DeviceResult[Unit] =
    tx.write(device, value)

  private val rx = ByteRx(register)
  private val tx = ByteTx(register)
}

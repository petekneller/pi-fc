package core.device.controller.filesystem

import core.device.api._

case class NumericTx[A](register: FileSystemRegister, f: A => Long = identity[Long] _) extends Tx {
  type T = A
  type Ctrl = FileSystemController

  def write(device: FileSystemAddress, value: T)(implicit controller: FileSystemController): DeviceResult[Unit] = tx.write(device, f(value).toString)

  private val tx = StringTx(register)
}

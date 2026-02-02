package core.device.controller.filesystem

import core.device.api._

case class StringTx(register: FileSystemRegister) extends Tx {
  type T = String
  type Ctrl = FileSystemController

  def write(device: FileSystemAddress, value: String)(implicit controller: FileSystemController): DeviceResult[Unit] = for {
    _ <- controller.transmit(device, register, value.toCharArray.toIndexedSeq.map(_.toByte))
  } yield ()
}

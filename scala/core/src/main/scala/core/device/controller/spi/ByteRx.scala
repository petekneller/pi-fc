package core.device.controller.spi

import eu.timepit.refined.auto.autoRefineV
import core.device.api._

case class ByteRx(sourceRegister: Byte) extends Rx {
  type T = Byte
  type Ctrl = SpiRegisterController

  def read(device: SpiAddress)(implicit controller: SpiRegisterController) = bytesRx.read(device) map (_.head)

  private val bytesRx = BytesRx(sourceRegister, 1)
}

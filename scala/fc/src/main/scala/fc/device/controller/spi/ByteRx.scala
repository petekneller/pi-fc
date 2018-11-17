package fc.device.controller.spi

import cats.syntax.either._
import eu.timepit.refined.auto.autoRefineV
import fc.device.api._

case class ByteRx(sourceRegister: Byte) extends Rx {
  type T = Byte
  type Ctrl = SpiController

  def read(device: SpiAddress)(implicit controller: SpiController) = bytesRx.read(device) map (_.head)

  private val bytesRx = BytesRx(sourceRegister, 1)
}

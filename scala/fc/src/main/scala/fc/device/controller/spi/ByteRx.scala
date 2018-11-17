package fc.device.controller.spi

import cats.syntax.either._
import eu.timepit.refined.auto.autoRefineV
import fc.device.api._

object ByteRx {

  def apply(sourceRegister: Byte) = new Rx {
    type T = Byte
    type Ctrl = SpiController

    def read(device: SpiAddress)(implicit controller: SpiController) = controller.receive(device, sourceRegister, 1) map (_.head)
  }

}

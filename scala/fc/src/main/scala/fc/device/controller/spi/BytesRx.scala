package fc.device.controller.spi

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.auto.autoRefineV
import fc.device.api._

object BytesRx {

  def apply(sourceRegister: Byte, numBytes: Int Refined Positive) = new Rx {
    type T = Seq[Byte]
    type Ctrl = SpiController

    def read(device: SpiAddress)(implicit controller: SpiController): DeviceResult[Seq[Byte]] = controller.receive(device, sourceRegister, numBytes)
  }

}

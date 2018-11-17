package fc.device.controller.spi

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.auto.autoRefineV
import fc.device.api._

case class BytesRx(sourceRegister: Byte, numBytes: Int Refined Positive) extends Rx {
  type T = Seq[Byte]
  type Ctrl = SpiRegisterController

  def read(device: SpiAddress)(implicit controller: SpiRegisterController): DeviceResult[Seq[Byte]] = controller.receive(device, sourceRegister, numBytes)
}

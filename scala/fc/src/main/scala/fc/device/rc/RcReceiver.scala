package fc.device.rc

import eu.timepit.refined.auto.{autoRefineV, autoUnwrap}
import fc.device.api._
import fc.device.controller.filesystem._

trait RcReceiver extends Device {
  type Ctrl = FileSystemController

  def readChannel(channel: RcChannel): DeviceResult[RcInput] =
    NumericRx[RcInput](channel.register, { l => RcInput.fromPpm(l.toInt, channel.min, channel.max, channel.mid) }).read(address)
}

object RcReceiver {
  def apply(a: FileSystemAddress)(implicit c: FileSystemController): RcReceiver = new RcReceiver {
    val address: FileSystemAddress = a
    implicit val controller: FileSystemController = c
  }
}

case class RcChannel(
  number: Int,
  min: PpmValue = 1000,
  max: PpmValue = 2000,
  mid: PpmValue = 1500
) {
  val register: String = s"ch${number}"
}

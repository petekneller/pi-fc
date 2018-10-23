package fc.device.rc

import eu.timepit.refined.auto.{autoRefineV, autoUnwrap}
import fc.device.api._

trait RcReceiver extends Device {
  type Register = String

  def readChannel(channel: RcChannel): DeviceResult[RcInput] =
    RxString.numeric[RcInput](channel.register, { l => RcInput.fromPpm(l.toInt, channel.min, channel.max, channel.mid) }).read(address)
}

object RcReceiver {
  def apply(a: Address)(implicit c: Controller { type Bus = a.Bus; type Register = String }): RcReceiver = new RcReceiver {
    val address: Address { type Bus = a.Bus } = a
    implicit val controller: Controller { type Bus = address.Bus; type Register = String } = c
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

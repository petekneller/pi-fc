package fc.device.input

import squants.time.{Time, Nanoseconds}
import fc.device._
import fc.device.file.File
import fc.device.rc._

trait RcReceiver extends Device {
  type Register = String

  def readChannel(channel: RcChannel): DeviceResult[Time] = RxString.numeric[Time](channel.register, { l => Nanoseconds(l) }).read(address)
}

object RcReceiver {
  def apply(a: Address)(implicit c: Controller { type Bus = a.Bus; type Register = String }): RcReceiver = new RcReceiver {
    val address: Address { type Bus = a.Bus } = a
    implicit val controller: Controller { type Bus = address.Bus; type Register = String } = c
  }

  object channels {
    val one = RcChannel(0)
    val two = RcChannel(1)
    val three = RcChannel(2)
    val four = RcChannel(3)
    val six = RcChannel(5)
  }

}

case class RcChannel(number: Int) {
  val register: String = s"ch${number}"
}

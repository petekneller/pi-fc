package fc.device.input

import fc.device._
import fc.device.file.File
import fc.device.rc._

trait RcReceiver extends Device {
  type Register = String

  def readChannel(channel: RcChannel): DeviceResult[Long] = RxString.numeric(channel.register).read(address)
}

object RcReceiver {
  def apply()(implicit c: Controller { type Bus = File; type Register = String }): RcReceiver = new RcReceiver {
    val address = RcAddress
    implicit val controller = c
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

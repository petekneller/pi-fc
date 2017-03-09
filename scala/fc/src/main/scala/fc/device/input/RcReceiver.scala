package fc.device.input

import fc.device._
import fc.device.rc._
import fc.device.configuration.NumericConfiguration

trait RcReceiver extends Device {
  type Register = String

  def readChannel(channel: Channel): DeviceResult[Long] = NumericConfiguration(channel.register).read(address)
}

object RcReceiver {
  def apply()(implicit c: Controller { type Bus = Rc; type Register = String }): RcReceiver = new RcReceiver {
    val address = RcAddress
    implicit val controller = c
  }

  object channels {
    val one = Channel(1)
    val two = Channel(2)
    val three = Channel(3)
    val four = Channel(4)
    val five = Channel(5)
  }

}

case class Channel(number: Int) {
  val register: String = s"ch${number}"
}

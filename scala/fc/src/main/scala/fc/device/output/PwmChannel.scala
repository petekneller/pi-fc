package fc.device.output

import fc.device._
import fc.device.pwm._

trait PwmChannel extends Device {
  type Register = String
}

object PwmChannel {
  def apply(chipNumber: Int, channelNumber: Int)(implicit c: Controller { type Bus = Pwm; type Register = String }): PwmChannel = new PwmChannel {
    val address = PwmAddress(chipNumber, channelNumber)
    implicit val controller = c
  }

  object registers {
    val enable = "enable"
    val period = "period"
    val dutyCycle = "duty_cycle"
  }
}

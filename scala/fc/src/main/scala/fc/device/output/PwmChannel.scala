package fc.device.output

import squants.time.{Time, Nanoseconds, Frequency, Gigahertz}
import fc.device._
import fc.device.file.File
import fc.device.pwm._
import fc.device.configuration._

trait PwmChannel extends Device {
  type Register = String
}

object PwmChannel {
  def apply(chipNumber: Int, channelNumber: Int)(implicit c: Controller { type Bus = File; type Register = String }): PwmChannel = new PwmChannel {
    val address = PwmAddress(chipNumber, channelNumber)
    implicit val controller = c
  }

  object configs {
    val enable = BooleanConfiguration(registers.enable)
    val period = NumericConfiguration[Time](registers.period, { l => Nanoseconds(l) }, {t => t.toNanoseconds.round})

    // despite the filesystem register being named 'duty_cycle' this next config really controls the
    // high-time of the pulse (ie. the pulse width)
    val pulseWidth = NumericConfiguration[Time](registers.dutyCycle, { l => Nanoseconds(l) }, {t => t.toNanoseconds.round})

    // complement to the period
    val frequency = NumericConfiguration[Frequency](registers.period, { l => Gigahertz(1/l.toDouble) }, { f => (1 / f.toGigahertz).round})
  }

  object registers {
    val enable = "enable"
    val period = "period"
    val dutyCycle = "duty_cycle"
  }

}

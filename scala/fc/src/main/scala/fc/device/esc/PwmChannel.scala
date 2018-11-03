package fc.device.esc

import eu.timepit.refined.W
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.GreaterEqual
import squants.time.{Time, Nanoseconds, Frequency, Gigahertz}
import fc.device.api._
import fc.device.controller.filesystem._

trait PwmChannel extends Device {
  type Ctrl = FileSystemController
}

object PwmChannel {
  def apply(chipNumber: Int Refined GreaterEqual[W.`0`.T], channelNumber: Int Refined GreaterEqual[W.`0`.T])(implicit c: FileSystemController): PwmChannel = new PwmChannel {
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

package fc.device.output

import cats.syntax.either._
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
    val periodNanoseconds = NumericConfiguration(registers.period)

    // despite the filesystem register being named 'duty_cycle' this next config really controls the
    // high-time of the pulse (ie. the pulse width)
    val pulseWidthNanoseconds = NumericConfiguration(registers.dutyCycle)

    // complement to the period
    val frequencyHz = new Configuration {
      type Register = String
      type T = Long

      def read(device: Address)(implicit controller: Controller { type Bus = device.Bus; type Register = String }): DeviceResult[Long] = configs.periodNanoseconds.read(device).map(period => (1e9 / period).round)

      def write(device: Address, frequencyHz: Long)(implicit controller: Controller { type Bus = device.Bus; type Register = String }): DeviceResult[Unit] = configs.periodNanoseconds.write(device, (1e9 / frequencyHz).round)
      }
  }

  object registers {
    val enable = "enable"
    val period = "period"
    val dutyCycle = "duty_cycle"
  }

}

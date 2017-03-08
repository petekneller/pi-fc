package fc.device.output

import cats.syntax.either._
import fc.device._

case class ESC(pwmChannel: PwmChannel) {

  def init(): DeviceResult[Unit] = for {
    _ <- disable()
    _ <- pwmChannel.write(PwmChannel.configs.frequencyHz)(50)
    _ <- setPulseWidthMicroseconds(900) // below 1000 us is a safe 'off' throttle for most ESCs
  } yield ()

  def enable(value: Boolean = true): DeviceResult[Unit] = pwmChannel.write(PwmChannel.configs.enable)(value)
  def disable(): DeviceResult[Unit] = enable(false)

  def getPulseWidthMicroseconds(): DeviceResult[Long] = pwmChannel.read(PwmChannel.configs.pulseWidthNanoseconds).map(microseconds => (microseconds / 1e3).round)

  def setPulseWidthMicroseconds(pulseWidth: Long): DeviceResult[Unit] = pwmChannel.write(PwmChannel.configs.pulseWidthNanoseconds)((pulseWidth * 1e9).round)
}

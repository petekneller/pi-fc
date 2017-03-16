package fc.device.output

import cats.syntax.either._
import squants.time.{Hertz, Microseconds}
import fc.device._

case class ESC(
  pwmChannel: PwmChannel,
  armPulseMicroseconds: Long = 900L,
  disarmPulseMicroseconds: Long = 0L,
  minPulseMicroseconds: Long = 1100L,
  maxPulseMicroseconds: Long = 1900L
) {

  def init(): DeviceResult[Unit] = for {
    _ <- pwmChannel.write(PwmChannel.configs.frequencyHz)(Hertz(50))
    _ <- disarm()
  } yield ()

  def arm(arming: Boolean = true): DeviceResult[Long] = setPulseWidthMicroseconds(if (arming) armPulseMicroseconds else disarmPulseMicroseconds)
  def disarm(): DeviceResult[Long] = arm(false)

  def run(pulseWidthMicroseconds: Long): DeviceResult[Long] = {
    val boundedPulseWidth = pulseWidthMicroseconds.max(minPulseMicroseconds).min(maxPulseMicroseconds)
    setPulseWidthMicroseconds(boundedPulseWidth)
  }

  private def setPulseWidthMicroseconds(pulseWidth: Long): DeviceResult[Long] = {
    pwmChannel.write(PwmChannel.configs.pulseWidthNanoseconds)(Microseconds(pulseWidth)) map (_ => pulseWidth)
  }

}

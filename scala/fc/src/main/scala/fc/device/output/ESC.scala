package fc.device.output

import cats.syntax.either._
import fc.device._

case class ESC(
  pwmChannel: PwmChannel,
  armPulseMicroseconds: Long = 900L,
  disarmPulseMicroseconds: Long = 0L,
  minPulseMicroseconds: Long = 1100L,
  maxPulseMicroseconds: Long = 1900L
) {

  def init(): DeviceResult[Unit] = for {
    _ <- disarm()
    _ <- pwmChannel.write(PwmChannel.configs.frequencyHz)(50)
  } yield ()

  def arm(arming: Boolean = true): DeviceResult[Long] = setPulseWidthMicroseconds(if (arming) armPulseMicroseconds else disarmPulseMicroseconds)
  def disarm(): DeviceResult[Long] = arm(false)

  def setPulseWidthMicroseconds(pulseWidth: Long): DeviceResult[Long] = {
    val pulseToSet = pulseWidth.max(minPulseMicroseconds).min(maxPulseMicroseconds)
    pwmChannel.write(PwmChannel.configs.pulseWidthNanoseconds)((pulseWidth * 1e9).round) map (_ => pulseToSet)
  }
}

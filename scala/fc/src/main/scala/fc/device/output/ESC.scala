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
    _ <- pwmChannel.write(PwmChannel.configs.frequencyHz)(50)
    _ <- disarm()
  } yield ()

  def arm(arming: Boolean = true): DeviceResult[Long] = setPulseWidthMicroseconds(if (arming) armPulseMicroseconds else disarmPulseMicroseconds)
  def disarm(): DeviceResult[Long] = arm(false)

  def run(pulseWidthMicroseconds: Long): DeviceResult[Long] = {
    val pulseToSet = pulseWidthMicroseconds.max(minPulseMicroseconds).min(maxPulseMicroseconds)
    setPulseWidthMicroseconds(pulseToSet)
  }

  private def setPulseWidthMicroseconds(pulseWidth: Long): DeviceResult[Long] = {
    pwmChannel.write(PwmChannel.configs.pulseWidthNanoseconds)((pulseWidth * 1e3).round) map (_ => pulseWidth)
  }

}

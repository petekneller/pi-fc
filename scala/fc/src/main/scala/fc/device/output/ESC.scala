package fc.device.output

import cats.syntax.either._
import eu.timepit.refined.auto.{autoRefineV, autoUnwrap}
import squants.time.{Hertz, Microseconds}
import fc.device._
import fc.device.rc.PpmValue

case class ESC(
  pwmChannel: PwmChannel,
  armValue: PpmValue = 900,
  disarmValue: PpmValue = 0,
  minValue: PpmValue = 1100,
  maxValue: PpmValue = 1900
) {

  def init(): DeviceResult[Unit] = for {
    _ <- pwmChannel.write(PwmChannel.configs.frequency)(Hertz(50))
    _ <- disarm()
  } yield ()

  def arm(arming: Boolean = true): DeviceResult[Int] = setValue(if (arming) armValue else disarmValue)
  def disarm(): DeviceResult[Int] = arm(false)

  def run(throttle: Double): DeviceResult[Int] = {
    val boundedThrottle = throttle.min(1.0).max(0.0)
    val ppmValue = minValue + (boundedThrottle * pulseRange).round
    setValue(ppmValue.toInt)
  }

  private def setValue(pulseWidth: Int): DeviceResult[Int] = {
    pwmChannel.write(PwmChannel.configs.pulseWidth)(Microseconds(pulseWidth)) map (_ => pulseWidth)
  }

  private val pulseRange = maxValue - minValue

}

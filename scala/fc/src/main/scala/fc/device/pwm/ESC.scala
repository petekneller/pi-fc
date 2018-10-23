package fc.device.pwm

import cats.syntax.either._
import eu.timepit.refined.auto.{autoRefineV, autoUnwrap}
import squants.time.{Hertz, Microseconds}
import fc.device.api._
import fc.device.rc.PpmValue

case class ESC(
  name: String,
  pwmChannel: PwmChannel,
  armValue: PpmValue = 900,
  disarmValue: PpmValue = 0,
  minValue: PpmValue = 1100,
  maxValue: PpmValue = 1900
) {

  import ESC.{Command, Disarm, Arm, Run}

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

  def run(command: Command): DeviceResult[Int] = command match {
    case Disarm => disarm()
    case Arm => arm()
    case Run(value) => run(value)
  }

  private def setValue(pulseWidth: Int): DeviceResult[Int] = {
    pwmChannel.write(PwmChannel.configs.pulseWidth)(Microseconds(pulseWidth)) map (_ => pulseWidth)
  }

  private val pulseRange = maxValue - minValue

}

object ESC {
  sealed trait Command
  case object Disarm extends Command
  case object Arm extends Command
  case class Run(value: Double) extends Command
}

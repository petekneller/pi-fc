package fc.device.esc

import eu.timepit.refined.auto.{autoRefineV, autoUnwrap}
import squants.time.{Hertz, Microseconds}
import cats.effect.IO
import fs2.Stream
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

  def init(): Stream[IO, DeviceResult[Unit]] = {
    val configFreq = Stream.eval(IO.blocking{ pwmChannel.write(PwmChannel.configs.frequency)(Hertz(50)) })
    configFreq ++ disarm().map(_.map(_ => ()))
  }

  def arm(arming: Boolean = true): Stream[IO, DeviceResult[Int]] = setValue(if (arming) armValue else disarmValue)
  def disarm(): Stream[IO, DeviceResult[Int]] = arm(false)

  def run(throttle: Double): Stream[IO, DeviceResult[Int]] = {
    val boundedThrottle = throttle.min(1.0).max(0.0)
    val ppmValue = minValue + (boundedThrottle * pulseRange).round
    setValue(ppmValue.toInt)
  }

  def run(command: Command): Stream[IO, DeviceResult[Int]] = command match {
    case Disarm => disarm()
    case Arm => arm()
    case Run(value) => run(value)
  }

  private def setValue(pulseWidth: Int): Stream[IO, DeviceResult[Int]] = Stream.eval(IO.blocking{
    pwmChannel.write(PwmChannel.configs.pulseWidth)(Microseconds(pulseWidth)) map (_ => pulseWidth)
  })

  private val pulseRange = maxValue - minValue

}

object ESC {
  sealed trait Command
  case object Disarm extends Command
  case object Arm extends Command
  case class Run(value: Double) extends Command
}

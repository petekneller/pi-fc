package fc

import cats.syntax.either._
import device.spi.{SpiController, SpiAddress}
import device.file.FileController
import device.input.{Mpu9250, RcReceiver}
import device.output.{PwmChannel, ESC}

object Navio2 {

  implicit val spiController = SpiController()
  implicit val fileController = FileController()

  val mpu9250 = Mpu9250(SpiAddress(busNumber = 0, chipSelect = 1))

  val receiver = RcReceiver()

  val esc1 = ESC(PwmChannel(chipNumber = 0, channelNumber = 1)) // pin 2
  val esc2 = ESC(PwmChannel(chipNumber = 0, channelNumber = 3)) // pin 4
  val esc3 = ESC(PwmChannel(chipNumber = 0, channelNumber = 5)) // pin 6
  val esc4 = ESC(PwmChannel(chipNumber = 0, channelNumber = 7)) // pin 8

  /* Quick and nasty flight experiments */

  def motorTest() = task.fs2.motorsTest(esc1, esc2, esc3, esc4)

  import RcReceiver.channels

  val arm = channels.six
  val throttle = channels.three
  val roll = channels.one
  val pitch = channels.two
  val yaw = channels.four

  val motorLF = esc3
  val motorRF = esc1
  val motorLR = esc2
  val motorRR = esc4


  def displayRc() = task.fs2.getRcInputs(receiver, arm, throttle, pitch, roll, yaw).
    map{ _.right.map{ inputs => inputs.copy(_1 = task.fs2.isArmed(inputs._1) ) } }.
    flatMap { dr =>
      dr.fold(
        ex => task.fs2.printToConsole(ex.toString),
        chs => task.fs2.printToConsole(formatInputs(chs))
      )
  }

  def displayMixer(mixer: ((Boolean, Long, Long, Long, Long)) => (Long, Long, Long, Long)) = task.fs2.getRcInputs(receiver, arm, throttle, pitch, roll, yaw).
    map{ _.right.map{ inputs => inputs.copy(_1 = task.fs2.isArmed(inputs._1)) } }.
    map{ _.right.map{ inputs => inputs -> mixer(inputs) } }.
    flatMap { dr =>
      dr.fold(
        ex => task.fs2.printToConsole(ex.toString),
        { case (inputs, outputs) =>
          task.fs2.printToConsole(s"${formatInputs(inputs)} | ${formatOutputs(outputs)}")
        }
      )
  }

  def motorsArmAll(arm: Boolean) = task.fs2.motorsArm(arm, motorLF, motorRF, motorLR, motorRR)

  def runMixer(mixer: ((Boolean, Long, Long, Long, Long)) => (Long, Long, Long, Long)) = task.fs2.getRcInputs(receiver, arm, throttle, pitch, roll, yaw).
    map{ _.right.map{ inputs => inputs.copy(_1 = task.fs2.isArmed(inputs._1)) } }.
    map{ _.right.map{ inputs => inputs -> mixer(inputs) } }.
    flatMap { dr =>
      dr.fold(
        ex => motorsArmAll(false),
        {
          case (inputs@(arm, thr, _, _, _), outputs@(lf, rf, lr, rr)) =>
            val motorTasks = if (arm && thr > 1000L)
              task.fs2.motorRun(motorLF, lf) ++
              task.fs2.motorRun(motorRF, rf) ++
              task.fs2.motorRun(motorLR, lr) ++
              task.fs2.motorRun(motorRR, rr)
            else if (arm && thr <= 1000L)
              motorsArmAll(true)
            else
              motorsArmAll(false)

            motorTasks.flatMap(_ => fs2.Stream.empty) ++ task.fs2.printToConsole(s"${formatInputs(inputs)} | ${formatOutputs(outputs)}")
        }
      )
  }

  val fmt = "%4d"

  def formatInputs(inputs: (Boolean, Long, Long, Long, Long)): String = s"ARM: ${"%4s".format(inputs._1.toString)} -- THR: ${fmt.format(inputs._2)} -- PIT: ${fmt.format(inputs._3)} -- ROL: ${fmt.format(inputs._4)} -- YAW: ${fmt.format(inputs._5)}"

  def formatOutputs(outputs: (Long, Long, Long, Long)): String = s"LF: ${fmt.format(outputs._1)} -- RF: ${fmt.format(outputs._2)} -- LR: ${fmt.format(outputs._3)} -- RR: ${fmt.format(outputs._4)}"

  def throttlePassThru(inputs: (Boolean, Long, Long, Long, Long)): (Long, Long, Long, Long) = {
    val (_, throttle, _, _, _) = inputs
    (throttle, throttle, throttle, throttle)
  }
  /* End quick and nasty */

}

package fc

import cats.syntax.either._
import device.spi.{SpiController, SpiAddress}
import device.file.FileController
import device.rc.RcAddress
import device.input.{Mpu9250, RcReceiver, RcChannel}
import device.output.{PwmChannel, ESC}
import task.{fs2 => tasks}

object Navio2 {

  implicit val spiController = SpiController()
  implicit val fileController = FileController()

  /* Devices */

  val mpu9250 = Mpu9250(SpiAddress(busNumber = 0, chipSelect = 1))

  val receiver = RcReceiver(RcAddress("/sys/kernel/rcio/rcin"))

  object rcChannels {
    val one =   RcChannel(0)
    val two =   RcChannel(1)
    val three = RcChannel(2)
    val four =  RcChannel(3)
    val six =   RcChannel(5)
  }

  // It's convenient to have low-level access to an ESC for testing configuration
  val pwmChannel1 = PwmChannel(chipNumber = 0, channelNumber = 1)

  object escs {
    val one =   ESC("1", pwmChannel1) // pin 2
    val two =   ESC("2", PwmChannel(chipNumber = 0, channelNumber = 3)) // pin 4
    val three = ESC("3", PwmChannel(chipNumber = 0, channelNumber = 5)) // pin 6
    val four =  ESC("4", PwmChannel(chipNumber = 0, channelNumber = 7)) // pin 8
  }

  /* Tasks */

  def initESCs = tasks.initESCs(escs.one, escs.two, escs.three, escs.four) to tasks.printToConsole

  def motorsTest = tasks.motorsTest(escs.one, escs.two, escs.three, escs.four) to tasks.printToConsole

  def displayRcChannels =
    tasks.zip5(tasks.readChannel(receiver, rcChannels.one),
               tasks.readChannel(receiver, rcChannels.two),
               tasks.readChannel(receiver, rcChannels.three),
               tasks.readChannel(receiver, rcChannels.four),
               tasks.readChannel(receiver, rcChannels.six)) map (dr => dr.map((tasks.formatRcChannels _).tupled)) through tasks.addLoopTime to tasks.printToConsole

  def displayGyro = tasks.readGyro(mpu9250) map (dr => dr map((tasks.formatGyro _).tupled)) through tasks.addLoopTime to tasks.printToConsole

  def displayFlightLoop(feedbackController: tasks.FeedbackController = tasks.PControllerTargetZero(0.01), mixer: tasks.Mixer = tasks.BasicMixer()) = tasks.zip3(
    tasks.readChannel(receiver, rcChannels.six) map (dr => dr map (tasks.isArmed _)),
    tasks.readChannel(receiver, rcChannels.one),
    tasks.readGyro(mpu9250)) map (dr => dr.map {
      case (armed, throttle, gyro) =>
        (armed, throttle, gyro, (feedbackController.run _).tupled(gyro))
    }) map (dr => dr map {
      case (armed, throttle, gyro, controlSignals@(x, y, z)) =>
        (armed, throttle, gyro, controlSignals, mixer.run(throttle.ppm, x, y, z))
    }) map( dr => dr map {
      case (armed, throttle, gyro, controlSignals, escOutput@(esc1, esc2, esc3, esc4)) =>
        (armed, throttle, gyro, controlSignals, tasks.armingOverride(armed, throttle, esc1, esc2, esc3, esc4))
    }) map (dr => dr.map {
      case (armed, throttle, gyro, controlSignals, escOutput) =>
        s"ARM: $armed | THR: [${"%4d".format(throttle.ppm)}] | ${(tasks.formatGyro _).tupled(gyro)} | ${(tasks.formatGyro _).tupled(controlSignals)} | ${(tasks.formatOutputs _).tupled(escOutput)}"
    }) through tasks.addLoopTime to tasks.printToConsole

}

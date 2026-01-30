package fc

import eu.timepit.refined.auto.autoRefineV
import fs2.Stream
import device.controller.filesystem.FileSystemController
import device.controller.spi.{SpiController, SpiAddress}
import device.rc.{RcAddress, RcReceiver, RcChannel}
import device.sensor.Mpu9250
import device.esc.{PwmChannel, ESC}
import task.{fs2 => tasks}
import fc.device.controller.spi.SpiRegisterController

object Navio2 {

  implicit val spiController: SpiRegisterController = SpiController()
  implicit val fileController: FileSystemController = FileSystemController()

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

  def initESCs = tasks.initESCs(escs.one, escs.two, escs.three, escs.four) through tasks.printToConsole

  def motorsTest = tasks.motorsTest(escs.one, escs.two, escs.three, escs.four) through tasks.printToConsole

  def displayRcChannels = tasks.zip6(
    tasks.looptime().map(Right(_)),
    tasks.readChannel(receiver, rcChannels.one),
    tasks.readChannel(receiver, rcChannels.two),
    tasks.readChannel(receiver, rcChannels.three),
    tasks.readChannel(receiver, rcChannels.four),
    tasks.readChannel(receiver, rcChannels.six)
  ) map (dr => dr.map {
    case (looptime, one, two, three, four, six) =>
      s"${tasks.formatLooptime(looptime)} | ${tasks.formatRcChannels(one, two, three, four, six)}"
  }) through tasks.printToConsole

  def displayGyro = tasks.zip2(
    tasks.looptime().map(Right(_)),
    tasks.readGyro(mpu9250)
  ) map (dr => dr map {
    case (looptime, (x, y, z)) =>
      s"${tasks.formatLooptime(looptime)} | ${tasks.formatGyro(x, y, z)}"
  }) through tasks.printToConsole

  def flightLoop(feedbackController: tasks.FeedbackController, mixer: tasks.Mixer) =
    tasks.zip4(
      tasks.looptime().map(Right(_)),
      tasks.readChannel(receiver, rcChannels.six) map (dr => dr map (tasks.isArmed _)),
      tasks.readChannel(receiver, rcChannels.one),
      tasks.readGyro(mpu9250)
    ) map (dr => dr.map {
      case (looptime, armed, throttle, gyro) =>
        (looptime, armed, throttle, gyro, (feedbackController.run _).tupled(gyro))
    }) map (dr => dr map {
      case (looptime, armed, throttle, gyro, controlSignals@(x, y, z)) =>
        (looptime, armed, throttle, gyro, controlSignals, mixer.run(throttle, x, y, z))
    }) map( dr => dr map {
      case (looptime, armed, throttle, gyro, controlSignals, (esc1, esc2, esc3, esc4)) =>
        (looptime, armed, throttle, gyro, controlSignals, tasks.armingOverride(armed, throttle, esc1, esc2, esc3, esc4))
    })

  def displayFlightLoop(feedbackController: tasks.FeedbackController = tasks.PControllerTargetZero(0.01), mixer: tasks.Mixer = tasks.BasicMixer()) =
    flightLoop(feedbackController, mixer) map (dr => dr.map {
      case (looptime, armed, throttle, gyro, controlSignals, escOutput) =>
        s"${tasks.formatLooptime(looptime)} | ARM: $armed | THR: [${"%4d".format(throttle.ppm)}] | ${(tasks.formatGyro _).tupled(gyro)} | ${(tasks.formatGyro _).tupled(controlSignals)} | ${(tasks.formatOutputs _).tupled(escOutput)}"
    }) through tasks.printToConsole

  def runFlightLoop(feedbackController: tasks.FeedbackController = tasks.PControllerTargetZero(0.01), mixer: tasks.Mixer = tasks.BasicMixer()) =
    flightLoop(feedbackController, mixer) flatMap (dr => dr.fold({
      failure => Stream(failure.toString)
    }, {
      case (looptime, armed, throttle, gyro, controlSignals, escOutput@(esc1, esc2, esc3, esc4)) =>
        tasks.motorRun(escs.one, esc1).map(_ => "") ++
        tasks.motorRun(escs.two, esc2).map(_ => "") ++
        tasks.motorRun(escs.three, esc3).map(_ => "") ++
        tasks.motorRun(escs.four, esc4).map(_ => "") ++
        Stream(s"${tasks.formatLooptime(looptime)} | ARM: $armed | THR: [${"%4d".format(throttle.ppm)}] | ${(tasks.formatGyro _).tupled(gyro)} | ${(tasks.formatGyro _).tupled(controlSignals)} | ${(tasks.formatOutputs _).tupled(escOutput)}")
    })) through tasks.printToConsole

}

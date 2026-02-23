package fc

import fs2.Stream
import core.Navio2

object FlightController {

  import Navio2.escs
  import Navio2.rcChannels

  def initESCs = tasks.Motors.initESCs(escs.one, escs.two, escs.three, escs.four) through tasks.Util.printToConsole

  def motorsTest = tasks.Motors.testMotors(escs.one, escs.two, escs.three, escs.four) through tasks.Util.printToConsole

  def displayRcChannels = tasks.Util.zip6(
    tasks.Util.looptime().map(Right(_)),
    tasks.RC.readChannel(rcChannels.one),
    tasks.RC.readChannel(rcChannels.two),
    tasks.RC.readChannel(rcChannels.three),
    tasks.RC.readChannel(rcChannels.four),
    tasks.RC.readChannel(rcChannels.six)
  ) map (dr => dr.map {
    case (looptime, one, two, three, four, six) =>
      s"${tasks.Util.formatLooptime(looptime)} | ${tasks.RC.formatRcChannels(one, two, three, four, six)}"
  }) through tasks.Util.printToConsole

  def displayGyro = tasks.Util.zip2(
    tasks.Util.looptime().map(Right(_)),
    tasks.Gyro.readGyro(Navio2.mpu9250)
  ) map (dr => dr map {
    case (looptime, (x, y, z)) =>
      s"${tasks.Util.formatLooptime(looptime)} | ${tasks.Gyro.formatGyro(x, y, z)}"
  }) through tasks.Util.printToConsole

  def flightLoop(feedbackController: algo.FeedbackController, mixer: algo.Mixer, ahrs: algo.Ahrs = algo.MahonyAhrs()) =
    tasks.Util.zip4(
      tasks.Util.looptime().map(Right(_)),
      tasks.RC.readChannel(rcChannels.six) map (dr => dr map (tasks.Motors.isArmed _)),
      tasks.RC.readChannel(rcChannels.one),
      tasks.Imu.readGyroAccel(Navio2.mpu9250)
    ).mapAccumulate(algo.AhrsState.identity) {
      case (ahrsState, dr) =>
        val result = dr.map {
          case (looptime, armed, throttle, (gyro, accel)) =>
            val newAhrsState = ahrs.update(ahrsState, gyro, accel, looptime.toSeconds)
            val (roll, pitch, yaw) = newAhrsState.attitude
            val controlSignals = feedbackController.run(roll, pitch, yaw, gyro._1, gyro._2, gyro._3)
            (newAhrsState, looptime, armed, throttle, gyro, (roll, pitch, yaw), controlSignals)
        }
        val nextState = result.fold(_ => ahrsState, { case (s, _, _, _, _, _, _) => s })
        (nextState, result.map { case (_, looptime, armed, throttle, gyro, attitude, controlSignals) =>
          (looptime, armed, throttle, gyro, attitude, controlSignals)
        })
    } map { case (_, dr) => dr } map (dr => dr map {
      case (looptime, armed, throttle, gyro, attitude, controlSignals@(x, y, z)) =>
        (looptime, armed, throttle, gyro, attitude, controlSignals, mixer.run(throttle, x, y, z))
    }) map (dr => dr map {
      case (looptime, armed, throttle, gyro, attitude, controlSignals, (esc1, esc2, esc3, esc4)) =>
        (looptime, armed, throttle, gyro, attitude, controlSignals, tasks.Motors.armingOverride(armed, throttle, esc1, esc2, esc3, esc4))
    })

  def formatAttitude(roll: Double, pitch: Double, yaw: Double): String = {
    val r2d = 180.0 / math.Pi
    val fmt = "%s: [%8.2f]"
    (fmt.format("R", roll * r2d) :: fmt.format("P", pitch * r2d) :: fmt.format("Y", yaw * r2d) :: Nil).mkString(" | ")
  }

  def displayFlightLoop(feedbackController: algo.FeedbackController = algo.PControllerTargetZero(0.01), mixer: algo.Mixer = algo.BasicMixer(), ahrs: algo.Ahrs = algo.MahonyAhrs()) =
    flightLoop(feedbackController, mixer, ahrs) map (dr => dr.map {
      case (looptime, armed, throttle, gyro, attitude, controlSignals, escOutput) =>
        s"${tasks.Util.formatLooptime(looptime)} | ARM: $armed | THR: [${"%4d".format(throttle.ppm)}] | ${(tasks.Gyro.formatGyro _).tupled(gyro)} | ${(formatAttitude _).tupled(attitude)} | ${(tasks.Gyro.formatGyro _).tupled(controlSignals)} | ${(tasks.Motors.formatOutputs _).tupled(escOutput)}"
    }) through tasks.Util.printToConsole

  def runFlightLoop(feedbackController: algo.FeedbackController = algo.PControllerTargetZero(0.01), mixer: algo.Mixer = algo.BasicMixer(), ahrs: algo.Ahrs = algo.MahonyAhrs()) =
    flightLoop(feedbackController, mixer, ahrs) flatMap (dr => dr.fold({
      failure => Stream(failure.toString)
    }, {
      case (looptime, armed, throttle, gyro, attitude, controlSignals, escOutput@(esc1, esc2, esc3, esc4)) =>
        escs.one.run(esc1).map(_ => "") ++
        escs.two.run(esc2).map(_ => "") ++
        escs.three.run(esc3).map(_ => "") ++
        escs.four.run(esc4).map(_ => "") ++
        Stream(s"${tasks.Util.formatLooptime(looptime)} | ARM: $armed | THR: [${"%4d".format(throttle.ppm)}] | ${(tasks.Gyro.formatGyro _).tupled(gyro)} | ${(formatAttitude _).tupled(attitude)} | ${(tasks.Gyro.formatGyro _).tupled(controlSignals)} | ${(tasks.Motors.formatOutputs _).tupled(escOutput)}")
    })) through tasks.Util.printToConsole

}

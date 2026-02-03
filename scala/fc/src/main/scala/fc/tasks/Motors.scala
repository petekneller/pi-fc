package fc.tasks

import cats.effect.IO
import _root_.fs2.Stream
import squants.time.Seconds
import core.device.api.DeviceResult
import core.device.rc.RcInput
import core.device.esc.ESC
import ESC.{ Command, Run, Arm, Disarm }

object Motors {

  def initESCs(escs: ESC*): Stream[IO, String] = {
    def initESC(esc: ESC): Stream[IO, String] = esc.init() map ( dr => dr.fold(_.toString, _ => s"ESC ${esc.name} initialized") )

    escs.foldLeft(Stream.empty.covaryAll[IO, String])((stream, esc) => stream ++ initESC(esc))
  }

  def testMotor(esc: ESC): Stream[IO, String] = {
    def sleep = Stream.emit("sleep for 0.5 seconds") ++ Util.sleep(Seconds(0.5))
    def throttleMessage(ppm: DeviceResult[Int]) = s"ESC [${esc.name}] ppm: ${ppm.toString}"
    def armMessage(ppm: DeviceResult[Int]) = s"ESC [${esc.name}] armed: ${ppm.toString}"
    val throttleCommand = Run(0.05)

    esc.arm(true).map(armMessage) ++ sleep ++
    esc.run(throttleCommand).map(throttleMessage) ++ sleep ++
    esc.run(throttleCommand).map(throttleMessage) ++ sleep ++
    esc.run(throttleCommand).map(throttleMessage) ++ sleep ++
    esc.arm(false).map(armMessage) ++ sleep
  }

  def testMotors(escs: ESC*): Stream[IO, String] =
    escs.foldLeft(Stream.empty.covaryAll[IO, String])((stream, esc) => stream ++ testMotor(esc))

  def formatOutputs(esc1: Command, esc2: Command, esc3: Command, esc4: Command): String =
    s"ESC1: [$esc1] | ESC2: [$esc2] | ESC3: [$esc3] | ESC4: [$esc4]"

  def isArmed(armChannel: RcInput): Boolean = armChannel.ppm > 1500

  def armingOverride(armed: Boolean, throttle: RcInput, esc1: Command, esc2: Command, esc3: Command, esc4: Command): (Command, Command, Command, Command) =
    if (armed && throttle.ppm > 1000)
      (esc1, esc2, esc3, esc4)
    else if (armed && throttle.ppm <= 1000)
      (Arm, Arm, Arm, Arm)
    else
      (Disarm, Disarm, Disarm, Disarm)

}

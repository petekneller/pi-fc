package fc.task

import cats.syntax.either._
import _root_.fs2.{Stream, Task}
import fc.device.DeviceResult
import fc.device.input.{RcReceiver, RcChannel}
import fc.device.output.ESC

package object fs2 {

  def motorArm(esc: ESC, arm: Boolean): Stream[Task, DeviceResult[Long]] = Stream.eval(Task.delay{ esc.arm(arm) })

  def motorsArm(arm: Boolean, escs: ESC*): Stream[Task, DeviceResult[Long]] =
    escs.foldLeft(Stream.empty[Task, DeviceResult[Long]])((stream, esc) => stream ++ motorArm(esc, arm))

  def motorRun(esc: ESC, pulseMicroseconds: Long): Stream[Task, DeviceResult[Long]] = Stream.eval(Task.delay{ esc.run(pulseMicroseconds) })

  def sleep(millis: Long): Stream[Task, Nothing] = Stream.eval(Task.delay{ Thread.sleep(millis) }).flatMap(_ => Stream.empty)

  def motorTest(esc: ESC): Stream[Task, DeviceResult[Long]] =
    motorArm(esc, true) ++ sleep(500) ++
    motorRun(esc, 1100L) ++ sleep(500) ++
    motorRun(esc, 1100L) ++ sleep(500) ++
    motorRun(esc, 1100L) ++ sleep(500) ++
    motorArm(esc, false) ++ sleep(500)

  def motorsTest(escs: ESC*): Stream[Task, DeviceResult[Long]] =
    escs.foldLeft(Stream.empty[Task, DeviceResult[Long]])((stream, esc) => stream ++ motorTest(esc))

  def printToConsole(s: String): Stream[Task, Unit] = Stream.eval(Task.delay{ println(s) })

  def readChannel(receiver: RcReceiver, channel: RcChannel): Stream[Task, DeviceResult[Long]] = Stream.eval(Task.delay{ receiver.readChannel(channel) })

  def readArm(receiver: RcReceiver, channel: RcChannel): Stream[Task, DeviceResult[Boolean]] = readChannel(receiver, channel).map(_.map(_ > 1500L))

  def runRc(receiver: RcReceiver, armChannel: RcChannel, throttleChannel: RcChannel, motor: ESC): Stream[Task, Unit] = {
    val inputs = readArm(receiver, armChannel).zip(readChannel(receiver, throttleChannel))
    val output = inputs.flatMap{ case (armInput, throttleInput) =>
      val outputPulseWidth = for {
        arm <- armInput
        throttle <- throttleInput
      } yield {
        if (arm && throttle > 1000L)
          motorRun(motor, throttle)
        else if (arm && throttle <= 1000L)
          motorArm(motor, true)
        else
          motorArm(motor, false)
      }
      outputPulseWidth.fold(_ => motorArm(motor, false), a => a)
    }
    output.flatMap{ pulseWidth => printToConsole(pulseWidth.toString) }.repeat
  }

  def getRcInputs(receiver: RcReceiver, ch1: RcChannel, ch2: RcChannel, ch3: RcChannel, ch4: RcChannel, ch5: RcChannel): Stream[Task, DeviceResult[(Long, Long, Long, Long, Long)]] = {
    val inputs = readChannel(receiver, ch1) zip
    readChannel(receiver, ch2) zip
    readChannel(receiver, ch3) zip
    readChannel(receiver, ch4) zip
    readChannel(receiver, ch5)

    inputs map { case ((((ch1in, ch2in), ch3in), ch4in), ch6in) =>
      for {
        ch1position <- ch1in
        ch2position <- ch2in
        ch3position <- ch3in
        ch4position <- ch4in
        ch6position <- ch6in
      } yield (ch1position, ch2position, ch3position, ch4position, ch6position)
    }
  }

  def isArmed(armChannel: Long): Boolean = armChannel > 1500L

}

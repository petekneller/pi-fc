package fc.task

import cats.syntax.either._
import _root_.fs2.{Stream, Task, Sink}
import squants.time.{Time, Seconds}
import fc.device.DeviceResult
import fc.device.output.ESC

package object fs2 {

  def motorArm(esc: ESC, arm: Boolean): Stream[Task, DeviceResult[Int]] = Stream.eval(Task.delay{ esc.arm(arm) })

  def motorRun(esc: ESC, throttle: Double): Stream[Task, DeviceResult[Int]] = Stream.eval(Task.delay{ esc.run(throttle) })

  def sleep(period: Time): Stream[Task, Nothing] = Stream.eval(Task.delay{ Thread.sleep(period.toMilliseconds.toInt) }).flatMap(_ => Stream.empty)

  def motorTest(esc: ESC): Stream[Task, String] = {
    def sleep = fs2.sleep(Seconds(0.5)).map(_ => s"sleep for 0.5 seconds")
    def throttleMessage(ppm: DeviceResult[Int]) = s"ESC [${esc.name}] ppm: ${ppm.toString}"
    def armMessage(ppm: DeviceResult[Int]) = s"ESC [${esc.name}] armed: ${ppm.toString}"

    motorArm(esc, true).map(armMessage) ++ sleep ++
    motorRun(esc, 0.05).map(throttleMessage) ++ sleep ++
    motorRun(esc, 0.05).map(throttleMessage) ++ sleep ++
    motorRun(esc, 0.05).map(throttleMessage) ++ sleep ++
    motorArm(esc, false).map(armMessage) ++ sleep
  }

  def motorsTest(escs: ESC*): Stream[Task, String] =
    escs.foldLeft(Stream.empty[Task, String])((stream, esc) => stream ++ motorTest(esc))

  def printToConsole[A]: Sink[Task, A] = s => s.flatMap(a => Stream.eval(Task.delay{ println(a.toString) }))

}

package fc.task

import cats.syntax.either._
import _root_.fs2.{Stream, Task}
import fc.device.DeviceResult
import fc.device.output.ESC

package object fs2 {

  def motorArm(esc: ESC, arm: Boolean): Stream[Task, DeviceResult[Long]] = Stream.eval(Task.delay{ esc.arm(arm) })

  def motorRun(esc: ESC, pulseMicroseconds: Long): Stream[Task, DeviceResult[Long]] = Stream.eval(Task.delay{ esc.run(pulseMicroseconds) })

  def sleep(millis: Long): Stream[Task, Nothing] = Stream.eval(Task.delay{ Thread.sleep(millis) }).flatMap(_ => Stream.empty)

  def motorTest(esc: ESC): Stream[Task, DeviceResult[Long]] =
    motorArm(esc, true) ++ sleep(500) ++
    motorRun(esc, 1100L) ++ sleep(500) ++
    motorRun(esc, 1100L) ++ sleep(500) ++
    motorRun(esc, 1100L) ++ sleep(500) ++
    motorArm(esc, false)

  def motorsTest(escs: ESC*): Stream[Task, DeviceResult[Long]] = escs.foldLeft(Stream.empty[Task, DeviceResult[Long]])((stream, esc) => stream ++ motorTest(esc))

}

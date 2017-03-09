package fc.task

import cats.syntax.either._
import _root_.fs2.{Stream, Task}
import fc.device.output.ESC

package object fs2 {

  def motorArm(esc: ESC, arm: Boolean): Stream[Task, Unit] = Stream.eval(Task.delay{ esc.arm(arm) } map (_ => ()) )

  def motorRun(esc: ESC, pulseMicroseconds: Long): Stream[Task, Unit] = Stream.eval(Task.delay{ esc.setPulseWidthMicroseconds(1100) } map (_ => ()) )

  def sleep(millis: Long): Stream[Task, Unit] = Stream.eval(Task.delay{ Thread.sleep(millis) })

  def motorTest(esc: ESC): Stream[Task, Unit] = {
    motorArm(esc, true) ++ sleep(500) ++ motorRun(esc, 1100L) ++ sleep(500) ++ motorRun(esc, 1100L) ++ sleep(500) ++ motorArm(esc, false)
  }

  def motorsTest(escs: ESC*): Stream[Task, Unit] = escs.foldLeft(Stream.empty[Task, Unit])((stream, esc) => stream ++ motorTest(esc))

}

package fc.task

import cats.syntax.either._
import _root_.fs2.{Stream, Task, Sink}
import squants.time.{Time, Seconds}
import fc.device.DeviceResult
import fc.device.rc.RcInput
import fc.device.input.{RcReceiver, RcChannel, Mpu9250}
import fc.device.output.ESC

package object fs2 {

  def initESCs(escs: ESC*): Stream[Task, String] = {
    def initESC(esc: ESC): Stream[Task, String] = Stream.eval(Task.delay{ esc.init() }) map ( dr => dr.fold(_.toString, _ => s"ESC ${esc.name} initialized") )

    escs.foldLeft(Stream.empty[Task, String])((stream, esc) => stream ++ initESC(esc))
  }

  def motorArm(esc: ESC, arm: Boolean): Stream[Task, DeviceResult[Int]] = Stream.eval(Task.delay{ esc.arm(arm) })

  def motorRun(esc: ESC, throttle: Double): Stream[Task, DeviceResult[Int]] = Stream.eval(Task.delay{ esc.run(throttle) })

  def sleep(period: Time): Stream[Task, Nothing] = Stream.eval(Task.delay{ Thread.sleep(period.toMilliseconds.toLong) }).flatMap(_ => Stream.empty)

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

  def zip5[A, B, C, D, E](a: Stream[Task, DeviceResult[A]], b: Stream[Task, DeviceResult[B]], c: Stream[Task, DeviceResult[C]], d: Stream[Task, DeviceResult[D]], e: Stream[Task, DeviceResult[E]]) =
    (a zip b zip c zip d zip e) map { case ((((aDR, bDR), cDR), dDR), eDR) =>
      for {
        a <- aDR
        b <- bDR
        c <- cDR
        d <- dDR
        e <- eDR
      } yield (a, b, c, d, e)
    }

  def readChannel(receiver: RcReceiver, channel: RcChannel): Stream[Task, DeviceResult[RcInput]] = Stream.eval(Task.delay{ receiver.readChannel(channel) })

  def readGyro(mpu: Mpu9250): Stream[Task, DeviceResult[(Double, Double, Double)]] = Stream.eval(Task.delay{ mpu.readGyro(Mpu9250.enums.GyroFullScale.dps500) })

}

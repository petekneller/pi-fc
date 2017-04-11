package fc.task

import cats.syntax.either._
import _root_.fs2.{Stream, Task, Sink, Pipe, Pull, Handle}
import squants.time.{Time, Seconds, Microseconds}
import java.time.LocalTime
import java.time.temporal.ChronoUnit.MICROS
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

  def zip2[A, B](a: Stream[Task, DeviceResult[A]], b: Stream[Task, DeviceResult[B]]): Stream[Task, DeviceResult[(A, B)]] =
    (a zip b) map { case (aDR, bDR) =>
      for {
        a <- aDR
        b <- bDR
      } yield (a, b)
    }

  def zip3[A, B, C](a: Stream[Task, DeviceResult[A]], b: Stream[Task, DeviceResult[B]], c: Stream[Task, DeviceResult[C]]): Stream[Task, DeviceResult[(A, B, C)]] =
    (a zip b zip c) map { case ((aDR, bDR), cDR) =>
      for {
        a <- aDR
        b <- bDR
        c <- cDR
      } yield (a, b, c)
    }

  def zip4[A, B, C, D](a: Stream[Task, DeviceResult[A]], b: Stream[Task, DeviceResult[B]], c: Stream[Task, DeviceResult[C]], d: Stream[Task, DeviceResult[D]]) =
    (a zip b zip c zip d) map { case (((aDR, bDR), cDR), dDR) =>
      for {
        a <- aDR
        b <- bDR
        c <- cDR
        d <- dDR
      } yield (a, b, c, d)
    }

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

  def zip6[A, B, C, D, E, F](a: Stream[Task, DeviceResult[A]], b: Stream[Task, DeviceResult[B]], c: Stream[Task, DeviceResult[C]], d: Stream[Task, DeviceResult[D]], e: Stream[Task, DeviceResult[E]],
    f: Stream[Task, DeviceResult[F]]) =
    (a zip b zip c zip d zip e zip f) map { case (((((aDR, bDR), cDR), dDR), eDR), fDR) =>
      for {
        a <- aDR
        b <- bDR
        c <- cDR
        d <- dDR
        e <- eDR
        f <- fDR
      } yield (a, b, c, d, e, f)
    }

  def readChannel(receiver: RcReceiver, channel: RcChannel): Stream[Task, DeviceResult[RcInput]] = Stream.eval(Task.delay{ receiver.readChannel(channel) })

  def readGyro(mpu: Mpu9250): Stream[Task, DeviceResult[(Double, Double, Double)]] = Stream.eval(Task.delay{ mpu.readGyro(Mpu9250.enums.GyroFullScale.dps250) })

  def addLoopTime[A]: Pipe[Task, DeviceResult[String], DeviceResult[String]] = {
    @volatile var time = System.currentTimeMillis
    stream => stream map { dr => dr map { string =>
      val previous = time
      time = System.currentTimeMillis
      val delta = time - previous
      s"Looptime: [$delta ms] | $string"
    }}
  }

  def formatRcChannels(one: RcInput, two: RcInput, three: RcInput, four: RcInput, six: RcInput):String = {
    val fmt = "CH %d: [%4d]"
    (fmt.format(1, one.ppm) :: fmt.format(2, two.ppm) :: fmt.format(3, three.ppm) :: fmt.format(4, four.ppm) :: fmt.format(6, six.ppm) :: Nil).mkString(" | ")
  }

  def formatGyro(x: Double, y: Double, z: Double): String = {
    val fmt = "%s: [%10f]"
    (fmt.format("X", x) :: fmt.format("Y", y) :: fmt.format("Z", z) :: Nil).mkString(" | ")
  }

  def formatOutputs(esc1: Long, esc2: Long, esc3: Long, esc4: Long): String = {
    val fmt = "%s: [%4d]"
    (fmt.format("ESC1", esc1) :: fmt.format("ESC2", esc2) :: fmt.format("ESC3", esc3) :: fmt.format("ESC4", esc4) :: Nil).mkString(" | ")
  }
  def isArmed(armChannel: RcInput): Boolean = armChannel.ppm > 1500

  def armingOverride(armed: Boolean, throttle: RcInput, esc1: Long, esc2: Long, esc3: Long, esc4: Long): (Long, Long, Long, Long) =
    if (armed && throttle.ppm > 1000)
      (esc1, esc2, esc3, esc4)
    else if (armed && throttle.ppm <= 1000)
      (900, 900, 900, 900)
    else
      (0, 0, 0, 0)

  def timestamp(): Stream[Task, LocalTime] = Stream.eval(Task.delay{ LocalTime.now() })

  def looptime(): Stream[Task, Time] = {
    def computeTimeDelta(tMinus1: LocalTime)(h: Handle[Task, LocalTime]): Pull[Task, Time, Nothing] =
      for {
        (t, h) <- h.await1
        _ <- Pull.output1(Microseconds(tMinus1.until(t, MICROS)))
        r <- computeTimeDelta(t)(h)
      } yield r

    timestamp().pull(computeTimeDelta(LocalTime.now()))
  }

  def formatLooptime(looptime: Time): String = s"Looptime: [${looptime.toMilliseconds.toString} ms]"

}

package fs2 {

  trait FeedbackController {
    def run(gx: Double, gy: Double, gz: Double): (Double, Double, Double)
  }

  case class PControllerTargetZero(gain: Double) extends FeedbackController {
    def run(qx: Double, qy: Double, qz: Double) = {
      val dx = qx // direction?
      val dy = qy // direction?
      val dz = qz // direction?
      (dx * gain, dy * gain, dz * gain)
    }
  }

  trait Mixer {
    def run(throttle: Int, pitchIn: Double, rollIn: Double, yawIn: Double): (Long, Long, Long, Long)
  }

  case class BasicMixer() extends Mixer {
    def run(throttleIn: Int, pitchIn: Double, rollIn: Double, yawIn: Double): (Long, Long, Long, Long) = {
      val throttleGain = 1.0
      val throttle = throttleIn * throttleGain

      val pitchGain = 0.3
      // pitch is low for pitch down, high for pitch up
      val pitchAdjustment = (pitchIn - 1500L) * pitchGain

      val rollGain = 0.3
      // roll is low for roll left, high for roll right
      val rollAdjustment = (rollIn - 1500L) * rollGain

      val yawGain = 0.3
      // yaw is low for yaw left, high for yaw right
      val yawAdjustment = (yawIn - 1500L) * yawGain

      val motorLF = throttle + pitchAdjustment + rollAdjustment - yawAdjustment
      val motorRF = throttle + pitchAdjustment - rollAdjustment + yawAdjustment
      val motorLR = throttle - pitchAdjustment + rollAdjustment + yawAdjustment
      val motorRR = throttle - pitchAdjustment - rollAdjustment - yawAdjustment

      (motorLF.toLong, motorRF.toLong, motorLR.toLong, motorRR.toLong)
    }
  }

}

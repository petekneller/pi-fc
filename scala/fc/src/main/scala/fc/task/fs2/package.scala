package fc.task

import java.time.LocalTime
import java.time.temporal.ChronoUnit.MICROS
import cats.effect.IO
import _root_.fs2.{ Stream, Pipe, Pull }
import squants.time.{ Time, Seconds, Microseconds }
import fc.device.api.DeviceResult
import fc.device.rc.{ RcInput, RcReceiver, RcChannel }
import fc.device.sensor.Mpu9250
import fc.device.esc.ESC
import ESC.{ Command, Run, Arm, Disarm }

package object fs2 {

  def initESCs(escs: ESC*): Stream[IO, String] = {
    def initESC(esc: ESC): Stream[IO, String] = Stream.eval(IO.delay{ esc.init() }) map ( dr => dr.fold(_.toString, _ => s"ESC ${esc.name} initialized") )

    escs.foldLeft(Stream.empty.covaryAll[IO, String])((stream, esc) => stream ++ initESC(esc))
  }

  def motorArm(esc: ESC, arm: Boolean): Stream[IO, DeviceResult[Int]] = Stream.eval(IO.delay{ esc.arm(arm) })

  def motorRun(esc: ESC, command: Command): Stream[IO, DeviceResult[Int]] = Stream.eval(IO.delay{ esc.run(command) })

  def sleep(period: Time): Stream[IO, Nothing] = Stream.eval(IO.delay{ Thread.sleep(period.toMilliseconds.toLong) }).flatMap(_ => Stream.empty.covaryAll[IO, Nothing])

  def motorTest(esc: ESC): Stream[IO, String] = {
    def sleep = fs2.sleep(Seconds(0.5)).map(_ => s"sleep for 0.5 seconds")
    def throttleMessage(ppm: DeviceResult[Int]) = s"ESC [${esc.name}] ppm: ${ppm.toString}"
    def armMessage(ppm: DeviceResult[Int]) = s"ESC [${esc.name}] armed: ${ppm.toString}"
    val throttleCommand = Run(0.05)

    motorArm(esc, true).map(armMessage) ++ sleep ++
    motorRun(esc, throttleCommand).map(throttleMessage) ++ sleep ++
    motorRun(esc, throttleCommand).map(throttleMessage) ++ sleep ++
    motorRun(esc, throttleCommand).map(throttleMessage) ++ sleep ++
    motorArm(esc, false).map(armMessage) ++ sleep
  }

  def motorsTest(escs: ESC*): Stream[IO, String] =
    escs.foldLeft(Stream.empty.covaryAll[IO, String])((stream, esc) => stream ++ motorTest(esc))

  def printToConsole[A]: Pipe[IO, A, Unit] = s => s.flatMap(a => Stream.eval(IO.delay{ println(a.toString) }))

  def zip2[A, B](a: Stream[IO, DeviceResult[A]], b: Stream[IO, DeviceResult[B]]): Stream[IO, DeviceResult[(A, B)]] =
    (a zip b) map { case (aDR, bDR) =>
      for {
        a <- aDR
        b <- bDR
      } yield (a, b)
    }

  def zip3[A, B, C](a: Stream[IO, DeviceResult[A]], b: Stream[IO, DeviceResult[B]], c: Stream[IO, DeviceResult[C]]): Stream[IO, DeviceResult[(A, B, C)]] =
    (a zip b zip c) map { case ((aDR, bDR), cDR) =>
      for {
        a <- aDR
        b <- bDR
        c <- cDR
      } yield (a, b, c)
    }

  def zip4[A, B, C, D](a: Stream[IO, DeviceResult[A]], b: Stream[IO, DeviceResult[B]], c: Stream[IO, DeviceResult[C]], d: Stream[IO, DeviceResult[D]]) =
    (a zip b zip c zip d) map { case (((aDR, bDR), cDR), dDR) =>
      for {
        a <- aDR
        b <- bDR
        c <- cDR
        d <- dDR
      } yield (a, b, c, d)
    }

  def zip5[A, B, C, D, E](a: Stream[IO, DeviceResult[A]], b: Stream[IO, DeviceResult[B]], c: Stream[IO, DeviceResult[C]], d: Stream[IO, DeviceResult[D]], e: Stream[IO, DeviceResult[E]]) =
    (a zip b zip c zip d zip e) map { case ((((aDR, bDR), cDR), dDR), eDR) =>
      for {
        a <- aDR
        b <- bDR
        c <- cDR
        d <- dDR
        e <- eDR
      } yield (a, b, c, d, e)
    }

  def zip6[A, B, C, D, E, F](a: Stream[IO, DeviceResult[A]], b: Stream[IO, DeviceResult[B]], c: Stream[IO, DeviceResult[C]], d: Stream[IO, DeviceResult[D]], e: Stream[IO, DeviceResult[E]],
    f: Stream[IO, DeviceResult[F]]) =
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

  def readChannel(receiver: RcReceiver, channel: RcChannel): Stream[IO, DeviceResult[RcInput]] = Stream.eval(IO.delay{ receiver.readChannel(channel) }).repeat

  def readGyro(mpu: Mpu9250): Stream[IO, DeviceResult[(Double, Double, Double)]] = Stream.eval(IO.delay{ mpu.readGyro(Mpu9250.enums.GyroFullScale.dps250) }).repeat

  def formatRcChannels(one: RcInput, two: RcInput, three: RcInput, four: RcInput, six: RcInput):String = {
    val fmt = "CH %d: [%4d]"
    (fmt.format(1, one.ppm) :: fmt.format(2, two.ppm) :: fmt.format(3, three.ppm) :: fmt.format(4, four.ppm) :: fmt.format(6, six.ppm) :: Nil).mkString(" | ")
  }

  def formatGyro(x: Double, y: Double, z: Double): String = {
    val fmt = "%s: [%10f]"
    (fmt.format("X", x) :: fmt.format("Y", y) :: fmt.format("Z", z) :: Nil).mkString(" | ")
  }

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

  def timestamp(): Stream[IO, LocalTime] = Stream.eval(IO.delay{ LocalTime.now() }).repeat

  def computeTimeDelta(tMinus1: LocalTime, s: Stream[IO, LocalTime]): Pull[IO, Time, Unit] =
    s.pull.uncons1.flatMap {
      case None => Pull.pure(None)
      case Some((t, rest)) => Pull.output1(Microseconds(tMinus1.until(t, MICROS))) >> computeTimeDelta(t, rest)
    }

  def looptime(): Stream[IO, Time] = computeTimeDelta(LocalTime.now(), timestamp()).stream

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
    def run(throttle: RcInput, pitchIn: Double, rollIn: Double, yawIn: Double): (Command, Command, Command, Command)
  }

  case class BasicMixer() extends Mixer {
    def run(throttleIn: RcInput, pitchIn: Double, rollIn: Double, yawIn: Double): (Command, Command, Command, Command) = {
      val throttleGain = 1.0
      val throttle = throttleIn.fromZero * throttleGain

      val pitchGain = 0.3
      // pitch is low for pitch down, high for pitch up
      val pitchAdjustment = pitchIn * pitchGain

      val rollGain = 0.3
      // roll is low for roll left, high for roll right
      val rollAdjustment = rollIn * rollGain

      val yawGain = 0.3
      // yaw is low for yaw left, high for yaw right
      val yawAdjustment = yawIn * yawGain

      val motorLF = throttle + pitchAdjustment + rollAdjustment - yawAdjustment
      val motorRF = throttle + pitchAdjustment - rollAdjustment + yawAdjustment
      val motorLR = throttle - pitchAdjustment + rollAdjustment + yawAdjustment
      val motorRR = throttle - pitchAdjustment - rollAdjustment - yawAdjustment

      (Run(motorLF), Run(motorRF), Run(motorLR), Run(motorRR))
    }
  }

}

package fc.tasks

import java.time.LocalTime
import java.time.temporal.ChronoUnit.MICROS
import cats.effect.IO
import _root_.fs2.{ Stream, Pipe, Pull }
import squants.time.{ Time, Microseconds }
import core.device.api.DeviceResult

object Util {

  def sleep(period: Time): Stream[IO, Nothing] = Stream.eval(IO.delay{ Thread.sleep(period.toMilliseconds.toLong) }).flatMap(_ => Stream.empty.covaryAll[IO, Nothing])

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

  def timestamp(): Stream[IO, LocalTime] = Stream.eval(IO.delay{ LocalTime.now() }).repeat

  def computeTimeDelta(tMinus1: LocalTime, s: Stream[IO, LocalTime]): Pull[IO, Time, Unit] =
    s.pull.uncons1.flatMap {
      case None => Pull.done
      case Some((t, rest)) => Pull.output1(Microseconds(tMinus1.until(t, MICROS))) >> computeTimeDelta(t, rest)
    }

  def looptime(): Stream[IO, Time] = computeTimeDelta(LocalTime.now(), timestamp()).stream

  def formatLooptime(looptime: Time): String = s"Looptime: [${looptime.toMilliseconds.toString} ms]"

}

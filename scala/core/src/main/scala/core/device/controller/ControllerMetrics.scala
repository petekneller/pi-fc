package core.device.controller

import java.time.Instant
import scala.concurrent.duration._
import eu.timepit.refined.auto.autoRefineV
import squants.information.BytesPerSecond
import squants.information.DataRate
import squants.time.Frequency
import squants.time.Hertz
import cats.effect.IO
import fs2.Stream
import core.metrics.{ AggregationBuffer, StatisticalMeasures }

/*
 * This file needs some work - it contains types at different levels of abstraction (that are too far apart).
 * `trait ControllerMetrics` is very close to the controller API but the object of the same name is much higher level.
 */

case class TransferEvent(timestamp: Instant, duration: FiniteDuration, writeBytes: Int, readBytes: Int)

trait ControllerMetrics {
  def addTransferCallback(callback: TransferEvent => Unit): Unit
}

case class TransfersObservation(
  transferRate: Frequency,
  duration: StatisticalMeasures[FiniteDuration],
  writeBytes: StatisticalMeasures[Int],
  writeRate: DataRate,
  readBytes: StatisticalMeasures[Int],
  readRate: DataRate
) {
  override def toString(): String =
    s"TransfersObservation - transferRate: $transferRate; duration: ${formatStats(duration)}; writeBytes: ${formatStats(writeBytes)}; writeRate: $writeRate; readBytes: ${formatStats(readBytes)}; readRate: $readRate"

  private def formatStats[A](stats: StatisticalMeasures[A]): String =
    s"[${stats.min};${stats.median};${stats.p90};${stats.max}]"
}

object ControllerMetrics {

  def apply(controller: ControllerMetrics, frequency: FiniteDuration): Stream[IO, TransfersObservation] = {
    val observationsBuffer = AggregationBuffer[TransferEvent](10)
    controller.addTransferCallback(observationsBuffer.record _)

    Stream.awakeEvery[IO](frequency) >> { Stream.eval(IO.blocking{
      observeTransfers(observationsBuffer)
    })}
  }

  private def observeTransfers(observations: AggregationBuffer[TransferEvent]): TransfersObservation = {
    val events = observations.retrieve

    val timeSpanOfEventsDoubleSeconds = (for {
      oldest <- events.headOption
      newest <- events.lastOption
    } yield {
      val span = newest.timestamp.toEpochMilli() - oldest.timestamp.toEpochMilli()
      span.toDouble / 1000.0
    })

    val transferRate = Hertz(timeSpanOfEventsDoubleSeconds.map(span => events.size.toDouble / span).getOrElse(0.0))

    val duration = StatisticalMeasures(events.map(_.duration), FiniteDuration(0, MILLISECONDS))
    val writeBytes = StatisticalMeasures(events.map(_.writeBytes), 0)
    val writeRate = BytesPerSecond(timeSpanOfEventsDoubleSeconds.map(span => events.map(_.writeBytes).sum.toDouble / span).getOrElse(0.0))
    val readBytes = StatisticalMeasures(events.map(_.readBytes), 0)
    val readRate = BytesPerSecond(timeSpanOfEventsDoubleSeconds.map(span => events.map(_.readBytes).sum.toDouble / span).getOrElse(0.0))

    TransfersObservation(transferRate, duration, writeBytes, writeRate, readBytes, readRate)
  }

}

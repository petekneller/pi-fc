package core.device.gps.ublox

import java.util.concurrent.BlockingQueue
import scala.concurrent.duration._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.auto.autoRefineV
import cats.effect.IO
import fs2.Stream
import core.device.controller.spi.{ SpiFullDuplexController, SpiAddress }
import core.device.gps.{ Gps, CompositeMessage, CompositeParser, CRight => UbxMsg }
import Gps.OutgoingMessagesObservation
import core.device.gps.nmea.{ NmeaMessage, NmeaParser }
import UbxMessage.monitor.TxBufferPoll
import core.metrics.AggregationBuffer
import core.device.gps.ublox.UbxMessage.monitor.TxBuffer
import core.metrics.StatisticalMeasures
import core.device.gps.ublox.UbloxM8N.TxBufferObservation

trait UbloxM8N {
  import UbloxM8N.Message

  val input: BlockingQueue[Message]
  val output: Stream[IO, Message]
  val metricStreams: (Stream[IO, TxBufferObservation], Stream[IO, OutgoingMessagesObservation])
}

object UbloxM8N {
  type Message = CompositeMessage[NmeaMessage, UbxMessage]

  def apply(
    address: SpiAddress,
    pollInterval: FiniteDuration,
    numPollingBytes: Int Refined Positive,
    metricInterval: FiniteDuration
  )(
    implicit controller: SpiFullDuplexController
  ): UbloxM8N = new UbloxM8N {

    def newParser() = CompositeParser(NmeaParser(), UbxParser())

    val gps = Gps(
      address,
      newParser _,
      pollInterval,
      numPollingBytes,
      metricInterval
    )

    override val input = gps.input
    override val output = gps.output through recordTxBuffer

    // metrics
    private val txBufferPolling = Stream.awakeEvery[IO](100.milliseconds) >> {
      Stream.exec(IO.blocking{
        gps.input.put(UbxMsg(TxBufferPoll))
      })
    }

    private val txBufferObservations = AggregationBuffer[TxBufferRecord](20)

    private def recordTxBuffer(input: Stream[IO, Message]): Stream[IO, Message] =
      input flatMap { message =>
        (message match {
          case UbxMsg(TxBuffer(_, usageLastPeriod, usagePeak, errors, _)) =>
            val obs = TxBufferRecord(usageLastPeriod, usagePeak, errors.toInt)
            Stream.exec(IO.blocking(txBufferObservations.record(obs)))
          case _ => Stream.empty
        }) ++ Stream(message)
      }

    private val txBufferMetrics = Stream.awakeEvery[IO](1.second) >> {
      Stream.eval(IO.blocking( observeTxBuffer(txBufferObservations) ))
    }

    private val txBufferStream: Stream[IO, TxBufferObservation] =
      (txBufferPolling either txBufferMetrics) flatMap { _ match {
        case Right(obs) => Stream(obs)
        case _ => Stream.empty
      }}

    override val metricStreams = (txBufferStream, gps.metricStream)
  }

  private case class TxBufferRecord(usageLastPeriod: Int, usagePeak: Int, errors: Int)
  case class TxBufferObservation(usage: StatisticalMeasures[Int], usagePeak: Int)

  private def observeTxBuffer(buffer: AggregationBuffer[TxBufferRecord]): TxBufferObservation = {
    val events = buffer.retrieve
    TxBufferObservation(StatisticalMeasures(events.map(_.usageLastPeriod), 0), events.map(_.usagePeak).max)
  }


}

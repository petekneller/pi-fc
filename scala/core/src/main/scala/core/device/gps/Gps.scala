package core.device.gps

import java.util.concurrent.{ BlockingQueue, LinkedBlockingQueue }
import scala.concurrent.duration.FiniteDuration
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.auto.autoRefineV
import org.slf4j.LoggerFactory
import cats.effect.IO
import fs2.{ Stream, Chunk }
import core.device.controller.spi.{ SpiFullDuplexController, SpiAddress }
import core.metrics.AggregationBuffer
import java.time.Instant
import core.metrics.StatisticalMeasures
import squants.information.DataRate
import squants.time.Frequency
import squants.time.Hertz
import squants.information.BytesPerSecond
import core.device.gps.Gps.OutgoingMessagesObservation

/*
 * GPS-specific equivalent of the `Device` - a utility that binds GPS message parsing to the appropriate controller and address.
 * FS2 provides the layer to manage the parsing state between GPS accesses.
 */
trait Gps[M <: Message] {
  val input: BlockingQueue[M]
  val output: Stream[IO, M]
  val metricStream: Stream[IO, OutgoingMessagesObservation]
}

object Gps {

  def apply[M <: Message](
    address: SpiAddress,
    newParser: () => MessageParser[M],
    pollInterval: FiniteDuration,
    numPollingBytes: Int Refined Positive,
    metricInterval: FiniteDuration
  )(
    implicit spi: SpiFullDuplexController
  ): Gps[M] = new Gps[M] {

    override val input = new LinkedBlockingQueue[M]()
    val inputStream = Stream.eval(IO.blocking{ input.take() }).repeat

    val inputPolling = Stream.awakeEvery[IO](pollInterval)

    override val output = (inputStream either inputPolling) through
      transferBytes through
      MessageParser.pipe(newParser) through
      recordMessage

    private def transferBytes(input: Stream[IO, Either[M, FiniteDuration]]): Stream[IO, Byte] =
      input flatMap {
        case Left(msg) => Stream.eval(IO.blocking{ spi.transfer(address, msg.toBytes) })
        case Right(_) => Stream.eval(IO.blocking{ spi.receive(address, numPollingBytes) })
      } flatMap {
        case Left(cause) => Stream.exec(IO.blocking{ logger.error(s"Device exception reading GPS: ${cause.toString}") })
        case Right(bytes) => Stream.chunk(Chunk.from(bytes))
      }

    // metrics

    private val messageObservationsBuffer = AggregationBuffer[MessageOutgoing](10)

    private def recordMessage(messages: Stream[IO, M]): Stream[IO, M] =
      messages flatMap {
        message =>
          Stream.exec(IO.blocking{
            messageObservationsBuffer.record(MessageOutgoing(Instant.now(), message.toBytes.length))
          }) ++
          Stream(message)
      }

    override val metricStream = Stream.awakeEvery[IO](metricInterval) >> {
      Stream.eval(IO.blocking{ observeOutgoingMessages(messageObservationsBuffer) })
    }

  }

  private val logger = LoggerFactory.getLogger(this.getClass)

  // metrics

  private case class MessageOutgoing(timestamp: Instant, size: Int)
  case class OutgoingMessagesObservation(messageRate: Frequency, dataRate: DataRate, size: StatisticalMeasures[Int])

  private def observeOutgoingMessages(buffer: AggregationBuffer[MessageOutgoing]): OutgoingMessagesObservation = {
    val events = buffer.retrieve

    val timeSpanOfEventsDoubleSeconds = (for {
      oldest <- events.headOption
      newest <- events.lastOption
    } yield {
      val span = newest.timestamp.toEpochMilli() - oldest.timestamp.toEpochMilli()
      span.toDouble / 1000.0
    })

    val messageRate = Hertz(timeSpanOfEventsDoubleSeconds.map(span => events.size.toDouble / span).getOrElse(0.0))
    val dataRate = BytesPerSecond(timeSpanOfEventsDoubleSeconds.map(span => events.map(_.size).sum.toDouble / span).getOrElse(0.0))
    val size = StatisticalMeasures(events.map(_.size), 0)
    OutgoingMessagesObservation(messageRate, dataRate, size)
  }

}

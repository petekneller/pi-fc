package util.spitotcp

import java.util.concurrent.BlockingQueue
import java.time.Instant
import scala.concurrent.duration._
import eu.timepit.refined.W
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Interval
import eu.timepit.refined.auto.{autoRefineV, autoUnwrap}
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.comcast.ip4s._
import fs2.{ Stream, Pipe, Chunk }
import fs2.io.net.{ Network, Socket }
import core.device.controller.spi.SpiAddress
import core.device.gps.{ MessageParser, CompositeParser, CompositeMessage, CRight => UbxMsg }
import core.device.gps.ublox.{ UbxParser, UbxMessage, RxBufferPoll, TxBufferPoll }
import core.device.gps.nmea.{ NmeaParser, NmeaMessage }
import core.device.gps.Gps
import core.metrics.{ StatisticalMeasures, AggregationBuffer }
import squants.information.{ DataRate, BytesPerSecond }
import squants.time.{ Frequency, Hertz }
import core.device.controller.spi.SpiFullDuplexController
import core.Navio2

/*
 * Remimder of how to run this from the ammonite repl:
 *   import eu.timepit.refined.auto.autoRefineV
 *   _root_.util.spitotcp.SpiToTcp.apply(3000)
 */

object SpiToTcp {

  type Msg = CompositeMessage[NmeaMessage, UbxMessage]
  type Port = Int Refined Interval.Closed[W.`1`.T, W.`65535`.T]

  private def newParser() = CompositeParser(NmeaParser(), UbxParser())

  def apply(port: Port): Unit = {
    val gps = Gps(
      SpiAddress(busNumber = 0, chipSelect = 0),
      newParser _
    )(
      spiController
    )

    val p = com.comcast.ip4s.Port.fromInt(port).get
    val app = Network[IO].server(address = Some(ip"0.0.0.0"), port = Some(p)).flatMap { clientSocket =>
      val inputStream = receiveFromClient(clientSocket, gps.input)
      val outputStream = gps.output through transmitToClient(clientSocket)
      Stream((inputStream :: outputStream :: metricStreams(gps.input)): _*).parJoinUnbounded
    }

    app.compile.drain.unsafeRunSync()
  }

  private def receiveFromClient(client: Socket[IO], gpsInput: BlockingQueue[Msg]): Stream[IO, Unit] = {
    val bytesFromClient = client.reads.onFinalize(client.endOfOutput)
    val messagesFromClient = bytesFromClient through MessageParser.pipe(newParser _)

    messagesFromClient flatMap {
      msg => Stream.exec(IO.blocking{ gpsInput.put(msg) })
    }
  }

  private def transmitToClient(client: Socket[IO]): Pipe[IO, Msg, Unit] = (input: Stream[IO, Msg]) =>
    input flatMap { msg =>
      val bytes = msg.toBytes
      Stream.exec(IO.blocking{ messageObservationsBuffer.record(MessageOutgoing(Instant.now(), bytes.length)) }) ++
        Stream.chunk(Chunk.from(bytes))
    } through client.writes

  private def metricStreams(gpsInput: BlockingQueue[Msg]): List[Stream[IO, Unit]] = {
    val gpsStatusPolling = Stream.awakeEvery[IO](100.milliseconds) >> { Stream.exec(IO.blocking{
      gpsInput.put(UbxMsg(RxBufferPoll))
      gpsInput.put(UbxMsg(TxBufferPoll))
    }) }
    val messageObservations = Stream.awakeEvery[IO](1.second) >> { Stream.exec(IO.blocking{
      println(observeOutgoingMessages())
    })}
    val spiObservations = Navio2.spiMetrics flatMap { observation =>
      Stream.exec(IO.blocking{ println(observation) })
    }

    gpsStatusPolling :: messageObservations :: spiObservations :: Nil
  }

  private case class MessageOutgoing(timestamp: Instant, size: Int)
  private case class OutgoingMessagesObservation(messageRate: Frequency, dataRate: DataRate, size: StatisticalMeasures[Int])

  private def observeOutgoingMessages(): OutgoingMessagesObservation = {
    val events = messageObservationsBuffer.retrieve

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

  private val spiController: SpiFullDuplexController = Navio2.spiController
  private val messageObservationsBuffer = AggregationBuffer[MessageOutgoing](10)
}

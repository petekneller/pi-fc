package util.spitotcp

import java.net.InetSocketAddress
import java.util.concurrent.{ Executors, BlockingQueue }
import java.nio.channels.AsynchronousChannelGroup
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicReference
import java.time.{ Instant, Duration }
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.math.max
import org.slf4j.LoggerFactory
import eu.timepit.refined.W
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.{ Interval, Positive }
import eu.timepit.refined.auto.{autoRefineV, autoUnwrap}
import cats.effect.{ IO, Timer }
import fs2.{ Stream, Pipe, Chunk, Pull }
import fs2.concurrent.SignallingRef
import fs2.io.tcp.Socket
import fc.device.controller.spi.{ SpiAddress, SpiController }
import SpiController.{ TransferEvent => SpiTransferEvent }
import fc.device.gps.{ Message, MessageParser, CompositeParser, CompositeMessage }
import MessageParser._
import fc.device.gps.ublox.{ UbxParser, UbxMessage }
import fc.device.gps.nmea.{ NmeaParser, NmeaMessage }
import fc.device.gps.fs2.Gps
import fc.metrics.{ StatisticalMeasures, AggregationBuffer }
import squants.information.{ DataRate, BytesPerSecond }
import squants.time.{ Frequency, Hertz }

object SpiToTcp {

  type Msg = CompositeMessage[NmeaMessage, UbxMessage]
  type Port = Int Refined Interval.Closed[W.`1`.T, W.`65535`.T]

  def apply(port: Port): Unit = {
    val (gpsInput, gpsOutput) = Gps(
      SpiAddress(busNumber = 0, chipSelect = 0),
      100.milliseconds,
      maxBytesToTransfer,
      newParser _,
      blockingIO
    )(
      spiController,
      timer,
      cs
    )

    spiController.addTransferCallback(spiObservationsBuffer.record _)

    val app = for {
      clientSocket <- createSocket(port)
      inputStream = receiveFromClient(clientSocket, gpsInput)
      outputStream = gpsOutput through transmitToClient(clientSocket)
      metricStream = Stream.awakeEvery[IO](1.second) >> { Stream.eval_(IO.delay{
        println(observeSpiTransfers())
        println(observeOutgoingMessages())
      })}
      _ <- Stream(inputStream, outputStream, metricStream).parJoinUnbounded
    } yield ()

    app.compile.drain.unsafeRunSync()
  }

  private def createSocket(port: Port): Stream[IO, Socket[IO]] = {
    implicit val acg = AsynchronousChannelGroup.withThreadPool(Executors.newCachedThreadPool())
    for {
      clientResource <- Socket.server[IO](new InetSocketAddress("0.0.0.0", port))
      clientSocket <- Stream.resource(clientResource)
    } yield clientSocket
  }

  private def receiveFromClient(client: Socket[IO], gpsInput: BlockingQueue[Msg]): Stream[IO, Unit] = {
    val bytesFromClient = client.reads(maxBytesToTransfer).onFinalize(client.endOfOutput)
    val messagesFromClient = bytesFromClient through parseMessages()

    messagesFromClient flatMap {
      msg => Stream.eval_(IO.delay{ gpsInput.put(msg) })
    }
  }

  private def newParser() = CompositeParser(NmeaParser(), UbxParser())
  private def parseMessages(): Pipe[IO, Byte, Msg] = Gps.parseStream(newParser _)

  private def transmitToClient(client: Socket[IO]): Pipe[IO, Msg, Unit] = (input: Stream[IO, Msg]) =>
    input flatMap { msg =>
      val bytes = msg.toBytes
      Stream.eval_(IO.delay{ messageObservationsBuffer.record(MessageOutgoing(Instant.now(), bytes.length)) }) ++
        Stream.chunk(Chunk.seq(bytes))
    } through client.writes()

  private case class SpiTransfersObservation(transferRate: Frequency, duration: StatisticalMeasures[FiniteDuration], writeBytes: StatisticalMeasures[Int], writeRate: DataRate, readBytes: StatisticalMeasures[Int], readRate: DataRate) {
    override def toString(): String =
      s"SpiTransfersObservation - transferRate: $transferRate; duration: ${formatStats(duration)}; writeBytes: ${formatStats(writeBytes)}; writeRate: $writeRate; readBytes: ${formatStats(readBytes)}; readRate: $readRate"
  }

  private def formatStats[A](stats: StatisticalMeasures[A]): String =
    s"[${stats.min};${stats.median};${stats.p90};${stats.max}]"

  private def observeSpiTransfers(): SpiTransfersObservation = {
    val events = spiObservationsBuffer.retrieve

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

    SpiTransfersObservation(transferRate, duration, writeBytes, writeRate, readBytes, readRate)
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

  private implicit val timer = IO.timer(ExecutionContext.global)
  private implicit val cs = IO.contextShift(ExecutionContext.global)
  private val blockingIO = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())
  private val spiController = SpiController()
  private val maxBytesToTransfer: Int Refined Positive = 100
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val spiObservationsBuffer = AggregationBuffer[SpiTransferEvent](10)
  private val messageObservationsBuffer = AggregationBuffer[MessageOutgoing](10)
}

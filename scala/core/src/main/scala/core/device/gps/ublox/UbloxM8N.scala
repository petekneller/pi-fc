package core.device.gps.ublox

import java.util.concurrent.BlockingQueue
import scala.concurrent.duration._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import cats.effect.IO
import fs2.Stream
import core.device.controller.spi.{ SpiFullDuplexController, SpiAddress }
import core.device.gps.{ Gps, CompositeMessage, CompositeParser }
import Gps.OutgoingMessagesObservation
import core.device.gps.nmea.{ NmeaMessage, NmeaParser }

trait UbloxM8N {
  import UbloxM8N.Message

  val input: BlockingQueue[Message]
  val output: Stream[IO, Message]
  val metricStream: Stream[IO, OutgoingMessagesObservation]
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
    override val output = gps.output
    override val metricStream = gps.metricStream
  }
}

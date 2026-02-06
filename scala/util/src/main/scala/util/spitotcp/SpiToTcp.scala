package util.spitotcp

import java.util.concurrent.BlockingQueue
import scala.concurrent.duration._
import eu.timepit.refined.W
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Interval
import eu.timepit.refined.auto.{ autoUnwrap, autoRefineV }
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.comcast.ip4s._
import fs2.{ Stream, Pipe, Chunk }
import fs2.io.net.{ Network, Socket }
import core.device.controller.spi.SpiAddress
import core.device.gps.{ MessageParser, CompositeParser, CRight => UbxMsg }
import core.device.gps.ublox.{ UbxParser, RxBufferPoll, TxBufferPoll, UbloxM8N }
import core.device.gps.nmea.NmeaParser
import core.Navio2

/*
 * Remimder of how to run this from the ammonite repl:
 *   import eu.timepit.refined.auto.autoRefineV
 *   _root_.util.spitotcp.SpiToTcp.apply(3000)
 */

object SpiToTcp {

  type Port = Int Refined Interval.Closed[W.`1`.T, W.`65535`.T]

  def apply(port: Port): Unit = {
    val gps = UbloxM8N(
      SpiAddress(busNumber = 0, chipSelect = 0),
      pollInterval = 100.milliseconds,
      numPollingBytes = 100,
      metricInterval = 1.seconds
    )(
      Navio2.spiController
    )

    val p = com.comcast.ip4s.Port.fromInt(port).get
    val app = Network[IO].server(address = Some(ip"0.0.0.0"), port = Some(p)).flatMap { clientSocket =>
      val inputStream = receiveFromClient(clientSocket, gps.input)
      val outputStream = gps.output through transmitToClient(clientSocket)
      Stream((inputStream :: outputStream :: metricStreams(gps)): _*).parJoinUnbounded
    }

    app.compile.drain.unsafeRunSync()
  }

  private def newParser() = CompositeParser(NmeaParser(), UbxParser())
  private def receiveFromClient(client: Socket[IO], gpsInput: BlockingQueue[UbloxM8N.Message]): Stream[IO, Unit] = {
    val bytesFromClient = client.reads.onFinalize(client.endOfOutput)
    val messagesFromClient = bytesFromClient through MessageParser.pipe(newParser _)

    messagesFromClient flatMap {
      msg => Stream.exec(IO.blocking{ gpsInput.put(msg) })
    }
  }

  private def transmitToClient(client: Socket[IO]): Pipe[IO, UbloxM8N.Message, Unit] = input =>
    input flatMap { msg =>
      Stream.chunk(Chunk.from(msg.toBytes))
    } through client.writes

  private def metricStreams(gps: UbloxM8N): List[Stream[IO, Unit]] = {
    val gpsStatusPolling = Stream.awakeEvery[IO](100.milliseconds) >> {
      Stream.exec(IO.blocking{
        gps.input.put(UbxMsg(RxBufferPoll))
        gps.input.put(UbxMsg(TxBufferPoll))
      })
    }
    val messageObservations = gps.metricStream flatMap { observation =>
      Stream.exec(IO.blocking{ println(observation) })
    }
    val spiObservations = Navio2.spiMetrics flatMap { observation =>
      Stream.exec(IO.blocking{ println(observation) })
    }

    gpsStatusPolling :: messageObservations :: spiObservations :: Nil
  }

}

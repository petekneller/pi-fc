package util.spitotcp.v7

import scala.concurrent.duration._
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import fs2.{ Stream, Pipe, Chunk }
import fs2.io.net.{ Network, Socket }
import com.comcast.ip4s._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.auto.autoRefineV
import core.device.controller.spi.{ SpiAddress, SpiController }

object SpiToTcp {

  def main(args: Array[String]): Unit = {

    def transferToPeer(client: Socket[IO]): Stream[IO, Unit] = {
      val ping = Stream.awakeEvery[IO](delay)

      val transferViaSpi: Pipe[IO, Either[Seq[Byte], FiniteDuration], Byte] = fromClient => {
        fromClient flatMap {
          case Left(clientBytes) => Stream.eval(spiTransfer(clientBytes))
          case Right(_) => Stream.eval(spiReceive())
        } flatMap { bytes => Stream.chunk(Chunk.from(bytes))}
      }

      (receiveFromClient(client).chunks.map(_.toArray.toSeq) either ping) through transferViaSpi through transmitToClient(client)
    }

    val port = Port.fromInt(args(0).toInt).get
    val app = Network[IO].server(address = Some(ip"0.0.0.0"), port = Some(port)).flatMap { clientSocket =>
      transferToPeer(clientSocket)
    }
    app.compile.drain.unsafeRunSync()
  }

  val gps = SpiAddress(busNumber = 0, chipSelect = 0)
  val spiController = SpiController()

  val maxBytesToTransfer: Int Refined Positive = 100
  val delay = 100.milliseconds

  def spiTransfer(bytes: Seq[Byte]): IO[Seq[Byte]] = IO.blocking{ spiController.transfer(gps, bytes).toOption.get }

  def spiReceive(): IO[Seq[Byte]] = IO.blocking{ spiController.receive(gps, maxBytesToTransfer).toOption.get }

  def receiveFromClient(client: Socket[IO]): Stream[IO, Byte] =
    client.reads.onFinalize(client.endOfOutput)

  def transmitToClient(client: Socket[IO]): Pipe[IO, Byte, Unit] = (input: Stream[IO, Byte]) =>
    input.chunks.flatMap{ bytes => Stream.exec(client.write(bytes)) }
}

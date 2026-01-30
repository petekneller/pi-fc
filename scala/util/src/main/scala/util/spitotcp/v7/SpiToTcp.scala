package util.spitotcp.v7

import java.net.InetSocketAddress
import java.util.concurrent.Executors
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import cats.effect.{ Blocker, IO, Timer }
import fs2.{ Stream, Pipe, Chunk }
import fs2.io.tcp.{Socket, SocketGroup}
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.auto.{autoRefineV, autoUnwrap}
import fc.device.controller.spi.{ SpiAddress, SpiController }

object SpiToTcp {

  def main(args: Array[String]): Unit = {

    implicit val t: Timer[IO] = IO.timer(ExecutionContext.global)
    implicit val cs = IO.contextShift(ExecutionContext.global)
    val blockingIO = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

    def transferToPeer(client: Socket[IO]): Stream[IO, Unit] = {
      val ping = Stream.awakeEvery[IO](delay)

      val transferViaSpi: Pipe[IO, Either[Seq[Byte], FiniteDuration], Byte] = fromClient => {
        fromClient flatMap { _ match {
            case Left(clientBytes) => Stream.eval(cs.evalOn(blockingIO)(spiTransfer(clientBytes)))
            case Right(_) => Stream.eval(cs.evalOn(blockingIO)(spiReceive()))
          }
        } flatMap { bytes => Stream.chunk(Chunk.seq(bytes))}
      }

      (receiveFromClient(client).chunks.map(_.toArray.toSeq) either ping) through transferViaSpi through transmitToClient(client)
    }

    val app = for {
      blocker <- Stream.resource(Blocker[IO])
      socketGroup <- Stream.resource(SocketGroup[IO](blocker))
      clientResource <- socketGroup.server[IO](new InetSocketAddress("0.0.0.0", args(0).toInt))
      clientSocket <- Stream.resource(clientResource)
      _ <- transferToPeer(clientSocket)
    } yield ()
    app.compile.drain.unsafeRunSync()
  }

  val gps = SpiAddress(busNumber = 0, chipSelect = 0)
  val spiController = SpiController()

  val maxBytesToTransfer: Int Refined Positive = 100
  val delay = 100.milliseconds

  def spiTransfer(bytes: Seq[Byte]): IO[Seq[Byte]] = IO.delay{ spiController.transfer(gps, bytes).right.get }

  def spiReceive(): IO[Seq[Byte]] = IO.delay{ spiController.receive(gps, maxBytesToTransfer).right.get }

  def receiveFromClient(client: Socket[IO]): Stream[IO, Byte] =
    client.reads(maxBytesToTransfer).onFinalize(client.endOfOutput)

  def transmitToClient(client: Socket[IO]): Pipe[IO, Byte, Unit] = (input: Stream[IO, Byte]) =>
    input.chunks.flatMap{ bytes => Stream.eval_(client.write(bytes)) }
}

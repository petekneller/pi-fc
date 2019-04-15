package util.spitotcp.v7

import java.net.InetSocketAddress
import java.util.concurrent.{ LinkedBlockingQueue, Executors }
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.HOURS
import java.nio.channels.AsynchronousChannelGroup
import scala.math.min
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import cats.instances.list._
import cats.syntax.apply._
import cats.syntax.parallel._
import cats.effect.{ IO, Timer }
import fs2.{ Stream, Pipe, Chunk }
import fs2.io.tcp.Socket
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.auto.{autoRefineV, autoUnwrap}
import fc.device.controller.spi.{ SpiAddress, SpiController }

object SpiToTcp {

  def main(args: Array[String]): Unit = {
    val gps = SpiAddress(busNumber = 0, chipSelect = 0)
    val spiController = SpiController()

    val maxBytesToTransfer: Int Refined Positive = 100
    val delay = 100.milliseconds

    implicit val t: Timer[IO] = IO.timer(ExecutionContext.global)
    implicit val cs = IO.contextShift(ExecutionContext.global)
    val blockingIO = ExecutionContext.fromExecutor(Executors.newCachedThreadPool())

    def transferToPeer(client: Socket[IO]): Stream[IO, Unit] = {
      val fromClient = client.reads(maxBytesToTransfer).onFinalize(client.endOfOutput)

      val ping = Stream.awakeEvery[IO](delay)

      val transferViaSpi: Pipe[IO, Either[Seq[Byte], FiniteDuration], Byte] = fromClient => {
        fromClient flatMap { _ match {
            case Left(clientBytes) => Stream.eval(cs.evalOn(blockingIO)(IO.delay{ spiController.transfer(gps, clientBytes).right.get }))
            case Right(_) => Stream.eval(cs.evalOn(blockingIO)(IO.delay{ spiController.receive(gps, maxBytesToTransfer).right.get }))
          }
        } flatMap { bytes => Stream.chunk(Chunk.seq(bytes))}
      }

      val sendToClient: Pipe[IO, Byte, Unit] = fromSpi => {
        fromSpi.chunks.flatMap{ bytes => Stream.eval_(client.write(bytes)) }
      }

      (fromClient.chunks.map(_.toArray: Seq[Byte]) either ping) through transferViaSpi through sendToClient
    }

    implicit val acg = AsynchronousChannelGroup.withThreadPool(Executors.newCachedThreadPool())

    val app = for {
      clientResource <- Socket.server[IO](new InetSocketAddress("0.0.0.0", args(0).toInt))
      clientSocket <- Stream.resource(clientResource)
      _ <- transferToPeer(clientSocket)
    } yield ()
    app.compile.drain.unsafeRunSync()
  }

}

package fc.device.controller.spi

import java.nio.ByteBuffer
import java.time.{ Instant, Duration }
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit.MILLISECONDS
import cats.syntax.either._
import eu.timepit.refined.api.{ Refined, RefType }
import eu.timepit.refined.numeric.{ Positive, NonNegative }
import eu.timepit.refined.auto.{ autoRefineV, autoUnwrap }
import eu.timepit.refined.refineV
import squants.time.{Frequency, Kilohertz}
import ioctl.IOCtl
import IOCtl.O_RDWR
import ioctl.syntax._
import spidev.Spidev
import fc.device.api._
import fc.metrics.Hook

case class SpiAddress(busNumber: Int, chipSelect: Int) extends Address {
  def toFilename: String = s"/dev/spidev${busNumber}.${chipSelect}"
}

trait SpiRegisterController extends RegisterController {
  type Addr = SpiAddress
  type Register = Byte
}

trait SpiFullDuplexController extends FullDuplexController {
  type Addr = SpiAddress
}

// TODO Ugh! I hate XyzImpl's. Must think of a better name
class SpiControllerImpl(api: SpiApi) extends SpiRegisterController with SpiFullDuplexController {
  override type Addr = SpiAddress

  // API for RegisterController

  def receive(device: SpiAddress, register: Byte, numBytes: Int Refined Positive): DeviceResult[Seq[Byte]] =
    withMetricRecording(0, { () =>
      withFileDescriptor(device, { fd =>
        val requisiteBufferSize = numBytes + 1
        val txBuffer = ByteBuffer.allocateDirect(requisiteBufferSize)
        val rxBuffer = ByteBuffer.allocateDirect(requisiteBufferSize)

        txBuffer.put(0, (0x80 | register).toByte)
        for {
          bytesTransferred <- transfer(fd, txBuffer, rxBuffer, requisiteBufferSize, clockSpeed)
          _ <- assertCompleteData(numBytes, bytesTransferred - 1)
        } yield {
          val result = rxBuffer.toSeq.drop(1) // first byte of receive buffer lines up with tx command, and so is empty
          (result, result.length)
        }
      })
    })

  def transmit(device: SpiAddress, register: Byte, data: Seq[Byte]): DeviceResult[Unit] =
    withMetricRecording(data.length, { () =>
      withFileDescriptor(device, { fd =>
        val requisiteBufferSize = data.length + 1
        val txBuffer = ByteBuffer.allocateDirect(requisiteBufferSize)
        val rxBuffer = ByteBuffer.allocateDirect(requisiteBufferSize)

        txBuffer.put(0, register)
        data.zipWithIndex foreach { case (b, i) => txBuffer.put(i + 1, b) }
        for {
          bytesTransferred <- transfer(fd, txBuffer, rxBuffer, requisiteBufferSize, clockSpeed)
          _ <- assertCompleteData(data.length, bytesTransferred - 1)
        } yield {
          ((), 0)
        }
      })
    })

  // API for DuplexController

  def transferN(device: Addr, dataToWrite: Seq[Byte], numBytesToRead: Int Refined NonNegative): DeviceResult[Seq[Byte]] =
    withMetricRecording(dataToWrite.length, { () =>
      withFileDescriptor(device, { fd =>
        val requisiteBufferSize = scala.math.max(dataToWrite.length, numBytesToRead)
        val txBuffer = ByteBuffer.allocateDirect(requisiteBufferSize)
        val rxBuffer = ByteBuffer.allocateDirect(requisiteBufferSize)

        dataToWrite.zipWithIndex foreach { case (b, i) => txBuffer.put(i, b) }
        for {
          bytesTransferred <- transfer(fd, txBuffer, rxBuffer, requisiteBufferSize, clockSpeed)
          _ <- assertCompleteData(requisiteBufferSize, bytesTransferred)
        } yield {
          val result = rxBuffer.toSeq
          (result, result.length)
        }
      })
    })

  def transfer(device: Addr, dataToWrite: Seq[Byte]): DeviceResult[Seq[Byte]] =
    transferN(device, dataToWrite, 0)

  def receive(device: Addr, numBytesToRead: Int Refined Positive): DeviceResult[Seq[Byte]] = {
    // This is a faff - a Positive is clearly NonNegative but refined doesn't seem to be able to infer that.
    // The correct solution is to learn how to add that inference, but I'd rather not spend the time right now.
    val numBytes: Int Refined NonNegative = refineV[NonNegative](RefType[Refined].unwrap(numBytesToRead)).toOption.get
    transferN(device, Seq.empty[Byte], numBytes)
  }

  // Metrics API
  import SpiController.TransferEvent
  def addTransferCallback(callback: TransferEvent => Unit): Unit = transferHook.callbacks.add(callback)

  // Internal API from here on down

  private val clockSpeed = Kilohertz(100)

  private def assertCompleteData(expected: Int, actual: Int): Either[IncompleteDataException, Unit] = if (expected == actual) Right(()) else Left(IncompleteDataException(expected, actual))

  private def open(device: SpiAddress): Either[DeviceUnavailableException, Int] =
    Either.catchNonFatal{ api.open(device.toFilename, O_RDWR) }.leftMap(DeviceUnavailableException(device, _))

  private def transfer(fileDescriptor: Int, txBuffer: ByteBuffer, rxBuffer: ByteBuffer, numBytes: Int, clockSpeed: Frequency): Either[TransferFailedException, Int] =
    Either.catchNonFatal{ api.transfer(fileDescriptor, txBuffer, rxBuffer, numBytes, clockSpeed.toHertz.toInt) }.leftMap(TransferFailedException(_))

  private def withFileDescriptor[A](device: SpiAddress, f: Int => DeviceResult[A]): DeviceResult[A] = for {
    fd <- open(device)
    result <- f(fd).bimap({ l => api.close(fd); l }, { r => api.close(fd); r })
  } yield result

  private val transferHook = Hook[TransferEvent]()

  private def withMetricRecording[A](numBytesToWrite: Int, actionUnderObservation: () => DeviceResult[(A, Int)]): DeviceResult[A] = {
    val begin = Instant.now()
    val result = actionUnderObservation()
    val end = Instant.now()
    val numBytesRead = result.right.toOption.map{ case (_, nb) => nb }.getOrElse(0)
    val duration = FiniteDuration(Duration.between(begin, end).toMillis, MILLISECONDS)
    transferHook.notify(TransferEvent(begin, duration, numBytesToWrite, numBytesRead))
    result.map{ case (bytes, _) => bytes }
  }

}

trait SpiApi {
  def transfer(fileDescriptor: Int, txBuffer: ByteBuffer, rxBuffer: ByteBuffer, numBytes: Int, clockSpeedHz: Int): Int
  def open(filename: String, flags: Int): Int
  def close(fileDescriptor: Int): Int
}

object SpiController {
  def apply() = new SpiControllerImpl(new SpiApi {
    def transfer(fileDescriptor: Int, txBuffer: ByteBuffer, rxBuffer: ByteBuffer, numBytes: Int, clockSpeedHz: Int) = Spidev.transfer(fileDescriptor, txBuffer, rxBuffer, numBytes, clockSpeedHz)
    def open(filename: String, flags: Int) = IOCtl.open(filename, flags)
    def close(fileDescriptor: Int) = IOCtl.close(fileDescriptor)
  })

  case class TransferEvent(timestamp: Instant, duration: FiniteDuration, writeBytes: Int, readBytes: Int)
}

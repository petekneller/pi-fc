package fc.device.controller.spi

import java.nio.ByteBuffer
import cats.syntax.either._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.auto.autoUnwrap
import squants.time.{Frequency, Kilohertz}
import ioctl.IOCtl
import IOCtl.O_RDWR
import ioctl.syntax._
import spidev.Spidev
import fc.device.api._

case class SpiAddress(busNumber: Int, chipSelect: Int) extends Address {
  def toFilename: String = s"/dev/spidev${busNumber}.${chipSelect}"
}

trait SpiRegisterController extends RegisterBasedDeviceController {
  type Addr = SpiAddress
  type Register = Byte
}

trait SpiBidirectionalController extends BidirectionalDeviceController {
  type Addr = SpiAddress
}

// TODO Ugh! I hate XyzImpl's. Must think of a better name
class SpiControllerImpl(api: SpiApi) extends SpiRegisterController with SpiBidirectionalController {
  override type Addr = SpiAddress

  def receive(device: SpiAddress, register: Byte, numBytes: Int Refined Positive): DeviceResult[Seq[Byte]] =
    withFileDescriptor(device, { fd =>
      val requisiteBufferSize = numBytes + 1
      val txBuffer = ByteBuffer.allocateDirect(requisiteBufferSize)
      val rxBuffer = ByteBuffer.allocateDirect(requisiteBufferSize)

      txBuffer.put(0, (0x80 | register).toByte)
      for {
        bytesTransferred <- transfer(fd, txBuffer, rxBuffer, requisiteBufferSize, clockSpeed)
        _ <- assertCompleteData(numBytes, bytesTransferred - 1)
      } yield {
        rxBuffer.toSeq.drop(1) // first byte of receive buffer lines up with tx command, and so is empty
      }
    })

  def transmit(device: SpiAddress, register: Byte, data: Seq[Byte]): DeviceResult[Unit] =
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
        ()
      }
    })

  def transfer(device: Addr, dataToWrite: Option[Byte]): DeviceResult[Byte] =
    withFileDescriptor(device, { fd =>
      val requisiteBufferSize = 1
      val txBuffer = ByteBuffer.allocateDirect(requisiteBufferSize)
      val rxBuffer = ByteBuffer.allocateDirect(requisiteBufferSize)

      txBuffer.put(0, dataToWrite.getOrElse(0x0))
      for {
        bytesTransferred <- transfer(fd, txBuffer, rxBuffer, requisiteBufferSize, clockSpeed)
        _ <- assertCompleteData(1, bytesTransferred)
      } yield {
        rxBuffer.toSeq.head
      }
    })

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
}

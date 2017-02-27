package fc.device.spi

import java.nio.ByteBuffer
import cats.syntax.either._
import ioctl.IOCtl
import IOCtl.O_RDWR
import ioctl.syntax._
import spidev.Spidev
import fc.device._

trait SpiBus

case class SpiAddress(busNumber: Int, chipSelect: Int) extends DeviceAddress {
  type Bus = SpiBus

  def toFilename: String = s"/dev/spidev${busNumber}.${chipSelect}"
}

class SpiController(api: SpiApi) extends Controller { self =>
  type Bus = SpiBus

  private val clockSpeed: Int = 100000

  def read(device: DeviceAddress { type Bus = self.Bus }, register: DeviceRegister, numBytes: Int): Either[DeviceError, Seq[Byte]] =
    withFileDescriptor(device, { fd =>
      val requisiteBufferSize = numBytes + 1
      val txBuffer = ByteBuffer.allocateDirect(requisiteBufferSize)
      val rxBuffer = ByteBuffer.allocateDirect(requisiteBufferSize)

      txBuffer.put(0, (0x80 | register.value).toByte)
      for {
        bytesTransferred <- transfer(fd, txBuffer, rxBuffer, requisiteBufferSize, clockSpeed)
        _ <- assertCompleteData(numBytes, bytesTransferred - 1)
      } yield {
        rxBuffer.toSeq.drop(1) // first byte of receive buffer lines up with tx command, and so is empty
      }
    })

  def write(device: DeviceAddress { type Bus = self.Bus }, register: DeviceRegister, data: Byte): Either[DeviceError, Unit] =
    withFileDescriptor(device, { fd =>
      val requisiteBufferSize = 2
      val txBuffer = ByteBuffer.allocateDirect(requisiteBufferSize)
      val rxBuffer = ByteBuffer.allocateDirect(requisiteBufferSize)

      txBuffer.put(0, register.value)
      txBuffer.put(1, data)
      for {
        bytesTransferred <- transfer(fd, txBuffer, rxBuffer, requisiteBufferSize, clockSpeed)
        _ <- assertCompleteData(requisiteBufferSize, bytesTransferred)
      } yield {
        ()
      }
    })

  // Internal API from here on down

  private def assertCompleteData(expected: Int, actual: Int): Either[IncompleteDataError, Unit] = if (expected == actual) Right(()) else Left(IncompleteDataError(expected, actual))

  private def open(device: DeviceAddress): Either[DeviceUnavailableError, Int] =
    Either.catchNonFatal{ api.open(device.toFilename, O_RDWR) }.leftMap(DeviceUnavailableError(device, _))

  private def transfer(fileDescriptor: Int, txBuffer: ByteBuffer, rxBuffer: ByteBuffer, numBytes: Int, clockSpeedHz: Int): Either[TransferFailedError, Int] =
    Either.catchNonFatal{ api.transfer(fileDescriptor, txBuffer, rxBuffer, numBytes, clockSpeedHz) }.leftMap(TransferFailedError(_))

  private def withFileDescriptor[A](device: DeviceAddress, f: Int => Either[DeviceError, A]): Either[DeviceError, A] = for {
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
  def apply() = new SpiController(new SpiApi {
    def transfer(fileDescriptor: Int, txBuffer: ByteBuffer, rxBuffer: ByteBuffer, numBytes: Int, clockSpeedHz: Int) = Spidev.transfer(fileDescriptor, txBuffer, rxBuffer, numBytes, clockSpeedHz)
    def open(filename: String, flags: Int) = IOCtl.open(filename, flags)
    def close(fileDescriptor: Int) = IOCtl.close(fileDescriptor)
  })
}

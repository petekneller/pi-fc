package fc.device.spi

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
import fc.device._

trait SpiBus

case class SpiAddress(busNumber: Int, chipSelect: Int) extends Address {
  type Bus = SpiBus

  def toFilename: String = s"/dev/spidev${busNumber}.${chipSelect}"
}

class SpiController(api: SpiApi) extends Controller { self =>
  type Bus = SpiBus
  type Register = Byte

  private val clockSpeed: Frequency = Kilohertz(100)

  def receive(device: Address { type Bus = self.Bus }, register: Byte, numBytes: Int Refined Positive): DeviceResult[Seq[Byte]] =
    withFileDescriptor(device, { fd =>
      val requisiteBufferSize = numBytes + 1
      val txBuffer = ByteBuffer.allocateDirect(requisiteBufferSize)
      val rxBuffer = ByteBuffer.allocateDirect(requisiteBufferSize)

      txBuffer.put(0, (0x80 | register).toByte)
      for {
        bytesTransferred <- transfer(fd, txBuffer, rxBuffer, requisiteBufferSize, clockSpeed.toHertz.toInt)
        _ <- assertCompleteData(numBytes, bytesTransferred - 1)
      } yield {
        rxBuffer.toSeq.drop(1) // first byte of receive buffer lines up with tx command, and so is empty
      }
    })

  def transmit(device: Address { type Bus = self.Bus }, register: Register, data: Seq[Byte]): DeviceResult[Unit] =
    withFileDescriptor(device, { fd =>
      val requisiteBufferSize = 2
      val txBuffer = ByteBuffer.allocateDirect(requisiteBufferSize)
      val rxBuffer = ByteBuffer.allocateDirect(requisiteBufferSize)

      txBuffer.put(0, register)
      data.zipWithIndex foreach { case (b, i) => txBuffer.put(i + 1, b) }
      for {
        bytesTransferred <- transfer(fd, txBuffer, rxBuffer, requisiteBufferSize, clockSpeed.toHertz.toInt)
        _ <- assertCompleteData(requisiteBufferSize, bytesTransferred)
      } yield {
        ()
      }
    })

  // Internal API from here on down

  private def assertCompleteData(expected: Int, actual: Int): Either[IncompleteDataException, Unit] = if (expected == actual) Right(()) else Left(IncompleteDataException(expected, actual))

  private def open(device: Address): Either[DeviceUnavailableException, Int] =
    Either.catchNonFatal{ api.open(device.toFilename, O_RDWR) }.leftMap(DeviceUnavailableException(device, _))

  private def transfer(fileDescriptor: Int, txBuffer: ByteBuffer, rxBuffer: ByteBuffer, numBytes: Int, clockSpeedHz: Int): Either[TransferFailedException, Int] =
    Either.catchNonFatal{ api.transfer(fileDescriptor, txBuffer, rxBuffer, numBytes, clockSpeedHz) }.leftMap(TransferFailedException(_))

  private def withFileDescriptor[A](device: Address, f: Int => DeviceResult[A]): DeviceResult[A] = for {
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

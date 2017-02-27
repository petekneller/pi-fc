package fc.device.spi

import java.nio.ByteBuffer
import cats.syntax.either._
import ioctl.IOCtl.O_RDWR
import ioctl.syntax._
import fc.device._

trait SpiBus

case class SpiAddress(busNumber: Int, chipSelect: Int) extends DeviceAddress {
  type Bus = SpiBus

  def toFilename: String = s"/dev/spidev${busNumber}.${chipSelect}"
}

// TODO a better api to the transfer function? that gives me better types or at least param names?
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
        bytesTransferred <- transfer(fd, txBuffer, rxBuffer, numBytes, clockSpeed)
      } yield {
        // TODO assert bytestransferred == expected
        rxBuffer.toSeq.drop(1) // first byte of receive buffer lines up with tx command, and so is empty
      }
    })

  def write(device: DeviceAddress { type Bus = self.Bus }, register: DeviceRegister, data: Byte): Either[DeviceError, Unit] = {
    ???
  }

  // Internal API from here on down

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

package fc.device.controller.filesystem

import java.nio.ByteBuffer
import cats.syntax.either._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.auto.autoUnwrap
import ioctl.IOCtl
import ioctl.IOCtlImpl.size_t
import fc.device.api._

trait FileSystemAddress extends Address {
  def toFilename: String
}

trait FileSystemController extends RegisterController {
  type Addr = FileSystemAddress
  type Register = String
}

// TODO Ugh! I hate XyzImpl's. Must think of a better name
class FileSystemControllerImpl(api: FileApi) extends FileSystemController {

  def receive(device: FileSystemAddress, register: String, numBytes: Int Refined Positive): DeviceResult[Seq[Byte]] =
    withFileDescriptor(device, register, IOCtl.O_RDONLY, { fd =>
      val rxBuffer = ByteBuffer.allocateDirect(numBytes)
      for {
        numBytesRead <- read(fd, rxBuffer, numBytes)
        data = (0 until numBytesRead) map { i => rxBuffer.get(i) }
      } yield data
    })

  def transmit(device: FileSystemAddress, register: String, data: Seq[Byte]): DeviceResult[Unit] =
    withFileDescriptor(device, register, IOCtl.O_WRONLY, { fd =>
      val numBytes = data.length
      val txBuffer = ByteBuffer.allocateDirect(numBytes)
      data.zipWithIndex foreach { case (b, i) => txBuffer.put(i, b) }

      for {
        numBytesWritten <- write(fd, txBuffer, numBytes)
        _ <- assertWriteComplete(numBytes, numBytesWritten)
      } yield ()
    })

  // Internal API from here on down

  private def open(device: FileSystemAddress, register: String, fileMode: Int): Either[FileUnavailableException, Int] =
    Either.catchNonFatal{ api.open(s"${device.toFilename}/${register}", fileMode) }.leftMap(FileUnavailableException(device, register, _))

  private def read(fileDescriptor: Int, rxBuffer: ByteBuffer, numBytes: Int): Either[TransferFailedException, Int] =
    Either.catchNonFatal{
      api.read(fileDescriptor, rxBuffer, new size_t(numBytes.toLong)).intValue
    }.leftMap(TransferFailedException(_))

  private def write(fileDescriptor: Int, txBuffer: ByteBuffer, numBytes: Int): Either[TransferFailedException, Int] =
    Either.catchNonFatal{
      api.write(fileDescriptor, txBuffer, new size_t(numBytes.toLong)).intValue
    }.leftMap(TransferFailedException(_))

  private def withFileDescriptor[A](device: FileSystemAddress, register: String, fileMode: Int, f: Int => DeviceResult[A]): DeviceResult[A] = for {
    fd <- open(device, register, fileMode)
    result <- f(fd).bimap({ l => api.close(fd); l }, { r => api.close(fd); r })
  } yield result

  private def assertWriteComplete(expectedNumBytes: Int, actualNumBytes: Int): Either[IncompleteDataException, Unit] = if (expectedNumBytes != actualNumBytes) Left(IncompleteDataException(expectedNumBytes, actualNumBytes)) else Right(())
}

trait FileApi {
  def open(filename: String, flags: Int): Int
  def close(fileDescriptor: Int): Int
  def read(fileDescriptor: Int, rxBuffer: ByteBuffer, maxBytes: size_t): size_t
  def write(fileDescriptor: Int, txBuffer: ByteBuffer, numBytes: size_t): size_t
}

object FileSystemController {
  def apply() = new FileSystemControllerImpl(new FileApi {
    def open(filename: String, flags: Int) = IOCtl.open(filename, flags)
    def close(fileDescriptor: Int) = IOCtl.close(fileDescriptor)
    def read(fileDescriptor: Int, rxBuffer: ByteBuffer, maxBytes: size_t) = IOCtl.read(fileDescriptor, rxBuffer, maxBytes)
    def write(fileDescriptor: Int, txBuffer: ByteBuffer, numBytes: size_t) = IOCtl.write(fileDescriptor, txBuffer, numBytes)
  })
}

case class FileUnavailableException(device: FileSystemAddress, register: String, cause: Throwable) extends DeviceException

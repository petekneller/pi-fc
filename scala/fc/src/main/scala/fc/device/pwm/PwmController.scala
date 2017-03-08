package fc.device.pwm

import cats.syntax.either._
import java.nio.ByteBuffer
import ioctl.IOCtl
import ioctl.IOCtlImpl.size_t
import fc.device._

trait Pwm

case class PwmAddress(chip: Int, channel: Int) extends Address {
  type Bus = Pwm

  def toFilename = s"/sys/class/pwm/pwmchip${chip}/pwm${channel}"
}

class PwmController(api: PwmApi) extends Controller {
  type Bus = Pwm
  type Register = String

  def receive(device: Address { type Bus = Pwm }, register: String, numBytes: Int): DeviceResult[Seq[Byte]] =
    withFileDescriptor(device, register, { fd =>
      val rxBuffer = ByteBuffer.allocateDirect(numBytes)
      for {
        numBytesRead <- read(fd, rxBuffer, numBytes)
        data = (0 until numBytesRead) map { i => rxBuffer.get(i) }
      } yield data
    })

  def transmit(device: Address { type Bus = Pwm }, register: String, data: Seq[Byte]): DeviceResult[Unit] =
    withFileDescriptor(device, register, { fd =>
      val numBytes = data.length
      val txBuffer = ByteBuffer.allocateDirect(numBytes)
      data.zipWithIndex foreach { case (b, i) => txBuffer.put(i, b) }

      for {
        numBytesWritten <- write(fd, txBuffer, numBytes)
        _ <- assertWriteComplete(numBytes, numBytesWritten)
      } yield ()
    })

  // Internal API from here on down

  private def open(device: Address { type Bus = Pwm }, register: String): Either[PwmUnavailableException, Int] =
    Either.catchNonFatal{ api.open(s"${device.toFilename}/${register}", IOCtl.O_RDWR) }.leftMap(PwmUnavailableException(device, register, _))

  private def read(fileDescriptor: Int, rxBuffer: ByteBuffer, numBytes: Int): Either[TransferFailedException, Int] =
    Either.catchNonFatal{
      api.read(fileDescriptor, rxBuffer, new size_t(numBytes.toLong)).intValue
    }.leftMap(TransferFailedException(_))

  private def write(fileDescriptor: Int, txBuffer: ByteBuffer, numBytes: Int): Either[TransferFailedException, Int] =
    Either.catchNonFatal{
      api.write(fileDescriptor, txBuffer, new size_t(numBytes.toLong)).intValue
    }.leftMap(TransferFailedException(_))

  private def withFileDescriptor[A](device: Address { type Bus = Pwm }, register: String, f: Int => DeviceResult[A]): DeviceResult[A] = for {
    fd <- open(device, register)
    result <- f(fd).bimap({ l => api.close(fd); l }, { r => api.close(fd); r })
  } yield result

  private def assertWriteComplete(expectedNumBytes: Int, actualNumBytes: Int): Either[IncompleteDataException, Unit] = if (expectedNumBytes != actualNumBytes) Left(IncompleteDataException(expectedNumBytes, actualNumBytes)) else Right(())
}

trait PwmApi {
  def open(filename: String, flags: Int): Int
  def close(fileDescriptor: Int): Int
  def read(fileDescriptor: Int, rxBuffer: ByteBuffer, maxBytes: size_t): size_t
  def write(fileDescriptor: Int, txBuffer: ByteBuffer, numBytes: size_t): size_t
}

object PwmController {
  def apply(): PwmController = new PwmController(new PwmApi {
    def open(filename: String, flags: Int) = IOCtl.open(filename, flags)
    def close(fileDescriptor: Int) = IOCtl.close(fileDescriptor)
    def read(fileDescriptor: Int, rxBuffer: ByteBuffer, maxBytes: size_t) = IOCtl.read(fileDescriptor, rxBuffer, maxBytes)
    def write(fileDescriptor: Int, txBuffer: ByteBuffer, numBytes: size_t) = IOCtl.write(fileDescriptor, txBuffer, numBytes)
  })
}

case class PwmUnavailableException(device: Address, register: String, cause: Throwable) extends DeviceException

case class RxString(register: String, maxBytesToRead: Int = 32) extends Rx {
  type T = String
  type Register = String

  def read(device: Address)(implicit controller: Controller { type Bus = device.Bus; type Register = String }): DeviceResult[String] = for {
    data <- controller.receive(device, register, maxBytesToRead)
  } yield data.map(_.toChar).filter(_ != '\n').mkString
}

case class TxString(register: String) extends Tx {
  type T = String
  type Register = String

  def write(device: Address, value: String)(implicit controller: Controller { type Bus = device.Bus; type Register = String }): DeviceResult[Unit] = for {
    _ <- controller.transmit(device, register, value.toCharArray.map(_.toByte))
  } yield ()
}

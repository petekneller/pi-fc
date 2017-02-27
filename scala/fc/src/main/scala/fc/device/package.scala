package fc

import cats.syntax.either._

package device {

  case class DeviceRegister(value: Byte)

  trait DeviceAddress {
    type Bus

    def toFilename: String
  }

  trait Controller { self =>
    type Bus

    def read(device: DeviceAddress { type Bus = self.Bus }, register: DeviceRegister, numBytes: Int): Either[DeviceError, Seq[Byte]]

    def write(device: DeviceAddress { type Bus = self.Bus }, register: DeviceRegister, data: Byte): Either[DeviceError, Unit]
  }

  trait DeviceError
  case class DeviceUnavailableError(device: DeviceAddress, cause: Throwable) extends DeviceError
  case class TransferFailedError(cause: Throwable) extends DeviceError
  case class IncompleteDataError(expected: Int, actual: Int) extends DeviceError

}

package object device {

  def readRegister(device: DeviceAddress, register: DeviceRegister)(implicit controller: Controller { type Bus = device.Bus }): Either[DeviceError, Byte] = controller.read(device, register, 1) map (_.head)

  def readRegisterN(device: DeviceAddress, register: DeviceRegister, numBytes: Int)(implicit controller: Controller { type Bus = device.Bus }): Either[DeviceError, Seq[Byte]] = controller.read(device, register, numBytes)

  def writeRegister(device: DeviceAddress, register: DeviceRegister, data: Byte)(implicit controller: Controller { type Bus = device.Bus }): Either[DeviceError, Unit] = controller.write(device, register, data)

}

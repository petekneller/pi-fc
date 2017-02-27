package fc.device.configuration

import cats.syntax.either._
import ioctl.syntax._
import fc.device._

case class ByteConfiguration(register: DeviceRegister) extends Configuration {
  type T = Byte

  def read(device: DeviceAddress)(controller: Controller { type Bus = device.Bus }): Either[DeviceError, Byte] =
    readRegister(device, register)(controller)

  def write(device: DeviceAddress, value: Byte)(controller: Controller { type Bus = device.Bus }): Either[DeviceError, Unit] =
    writeRegister(device, register, value)(controller)
}

case class FlagConfiguration(register: DeviceRegister, bit: Int) extends Configuration {
  type T = Boolean

  def read(device: DeviceAddress)(controller: Controller { type Bus = device.Bus }): Either[DeviceError, Boolean] =
    readRegister(device, register)(controller).map(registerValue => ((registerValue.unsigned >> bit) & 0x1) == 0x1)

  def write(device: DeviceAddress, value: Boolean)(controller: Controller { type Bus = device.Bus }): Either[DeviceError, Unit] = for {
    originalValue <- readRegister(device, register)(controller)
    newValue = (((if (value) 0x1 else 0x0) << bit) | originalValue).toByte
    _ <- writeRegister(device, register, newValue)(controller)
  } yield ()
}

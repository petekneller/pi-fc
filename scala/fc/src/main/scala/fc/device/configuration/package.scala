package fc.device.configuration

import cats.syntax.either._
import ioctl.syntax._
import fc.device._

case class ByteConfiguration(register: DeviceRegister) extends Configuration {
  type T = Byte

  def read(device: DeviceAddress)(controller: Controller { type Bus = device.Bus }): Either[DeviceError, Byte] =
    readRegister(device, register)(controller)
}

case class FlagConfiguration(register: DeviceRegister, bit: Int) extends Configuration {
  type T = Boolean

  def read(device: DeviceAddress)(controller: Controller { type Bus = device.Bus }): Either[DeviceError, Boolean] =
    readRegister(device, register)(controller).map(registerValue => ((registerValue.unsigned >> bit) & 0x1) == 0x1)
}

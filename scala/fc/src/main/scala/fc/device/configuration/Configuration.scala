package fc.device.configuration

import cats.syntax.either._
import ioctl.syntax._
import fc.device._

trait Configuration extends Rx with Tx

case class ByteConfiguration(register: Register) extends Configuration {
  type T = Byte

  def read(device: Address)(implicit controller: Controller { type Bus = device.Bus }): Either[DeviceException, Byte] =
    Rx.byte(register).read(device)

  def write(device: Address, value: Byte)(implicit controller: Controller { type Bus = device.Bus }): Either[DeviceException, Unit] =
    Tx.byte(register).write(device, value)
}

case class BitFlagConfiguration(register: Register, bit: Int) extends Configuration {
  type T = Boolean

  def read(device: Address)(implicit controller: Controller { type Bus = device.Bus }): Either[DeviceException, Boolean] =
    Rx.byte(register).read(device).map(registerValue => ((registerValue.unsigned >> bit) & 0x1) == 0x1)

  def write(device: Address, value: Boolean)(implicit controller: Controller { type Bus = device.Bus }): Either[DeviceException, Unit] = for {
    originalValue <- Rx.byte(register).read(device)
    bitMask = 0x1 << bit
    newValue = if (value)
      (bitMask | originalValue)
    else
      (~bitMask & originalValue)
    _ <- Tx.byte(register).write(device, newValue.toByte)
  } yield ()
}

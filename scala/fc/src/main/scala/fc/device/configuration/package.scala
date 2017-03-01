package fc.device.configuration

import cats.syntax.either._
import ioctl.syntax._
import fc.device._

trait Configuration extends Rx with Tx

case class ByteConfiguration(register: Register) extends Configuration {
  type T = Byte

  def receive(device: Address)(implicit controller: Controller { type Bus = device.Bus }): Either[DeviceException, Byte] =
    Rx.byte(register).receive(device)

  def transmit(device: Address, value: Byte)(implicit controller: Controller { type Bus = device.Bus }): Either[DeviceException, Unit] =
    Tx.byte(register).transmit(device, value)
}

case class FlagConfiguration(register: Register, bit: Int) extends Configuration {
  type T = Boolean

  def receive(device: Address)(implicit controller: Controller { type Bus = device.Bus }): Either[DeviceException, Boolean] =
    Rx.byte(register).receive(device).map(registerValue => ((registerValue.unsigned >> bit) & 0x1) == 0x1)

  def transmit(device: Address, value: Boolean)(implicit controller: Controller { type Bus = device.Bus }): Either[DeviceException, Unit] = for {
    originalValue <- Rx.byte(register).receive(device)
    newValue = (((if (value) 0x1 else 0x0) << bit) | originalValue).toByte
    _ <- Tx.byte(register).transmit(device, newValue)
  } yield ()
}

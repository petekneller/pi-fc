package fc.device.configuration

import cats.syntax.either._
import ioctl.syntax._
import fc.device._

case class ByteConfiguration(register: DeviceRegister) extends Configuration {
  type T = Byte

  def receive(device: DeviceAddress)(implicit controller: Controller { type Bus = device.Bus }): Either[DeviceError, Byte] =
    Rx.byte(register).receive(device)

  def transmit(device: DeviceAddress, value: Byte)(implicit controller: Controller { type Bus = device.Bus }): Either[DeviceError, Unit] =
    Tx.byte(register).transmit(device, value)
}

case class FlagConfiguration(register: DeviceRegister, bit: Int) extends Configuration {
  type T = Boolean

  def receive(device: DeviceAddress)(implicit controller: Controller { type Bus = device.Bus }): Either[DeviceError, Boolean] =
    Rx.byte(register).receive(device).map(registerValue => ((registerValue.unsigned >> bit) & 0x1) == 0x1)

  def transmit(device: DeviceAddress, value: Boolean)(implicit controller: Controller { type Bus = device.Bus }): Either[DeviceError, Unit] = for {
    originalValue <- Rx.byte(register).receive(device)
    newValue = (((if (value) 0x1 else 0x0) << bit) | originalValue).toByte
    _ <- Tx.byte(register).transmit(device, newValue)
  } yield ()
}

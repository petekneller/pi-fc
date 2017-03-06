package fc.device.configuration

import cats.syntax.either._
import cats.syntax.eq._
import cats.instances.byte._
import ioctl.syntax._
import fc.device._

trait Configuration extends Rx with Tx

case class ByteConfiguration(register: Register) extends Configuration {
  type T = Byte

  def read(device: Address)(implicit controller: Controller { type Bus = device.Bus }): DeviceResult[Byte] =
    Rx.byte(register).read(device)

  def write(device: Address, value: Byte)(implicit controller: Controller { type Bus = device.Bus }): DeviceResult[Unit] =
    Tx.byte(register).write(device, value)
}

case class SingleBitFlag(register: Register, bit: Int) extends Configuration {
  type T = Boolean

  def read(device: Address)(implicit controller: Controller { type Bus = device.Bus }): DeviceResult[Boolean] =
    Rx.byte(register).read(device).map(registerValue => ((registerValue.unsigned >> bit) & 0x1) == 0x1)

  def write(device: Address, value: Boolean)(implicit controller: Controller { type Bus = device.Bus }): DeviceResult[Unit] = for {
    originalValue <- Rx.byte(register).read(device)
    bitMask = 0x1 << bit
    newValue = if (value)
      (bitMask | originalValue)
    else
      (~bitMask & originalValue)
    _ <- Tx.byte(register).write(device, newValue.toByte)
  } yield ()
}

trait FlagEnumeration {
  type T <: Flag
  trait Flag {
    val value: Byte
  }
  def values: Set[T]
}

case class FlagException[A](valueFound: Byte, optionsAvailable: Set[A]) extends DeviceException

case class MultiBitFlag[E <: FlagEnumeration](register: Register, hiBit: Int, numBits: Int, options: E) extends Configuration {
  type T = E#T

  val loBit = hiBit - (numBits - 1)
  private val onesMask = (1 until numBits).fold(0x1){ (acc, _) => acc << 1 | 0x1 }
  val mask = (0 until loBit).fold(onesMask){ (acc, _) => acc << 1 }

  def read(device: Address)(implicit controller: Controller { type Bus = device.Bus }): DeviceResult[E#T] = for {
    registerValue <- Rx.byte(register).read(device)
    masked = registerValue.unsigned & mask
    enumOrdinal = (masked >> loBit).toByte
    enumValue <- options.values.find(_.value === enumOrdinal).toRight(FlagException(enumOrdinal, options.values))
  } yield enumValue

  def write(device: Address, value: E#T)(implicit controller: Controller { type Bus = device.Bus }): DeviceResult[Unit] = for {
    existingRegisterValue <- Rx.byte(register).read(device)
    existingWithConfigZeroed = existingRegisterValue.unsigned & ~mask
    newConfig = (value.value << loBit) & mask
    newRegisterValue = existingWithConfigZeroed | newConfig
    _ <- Tx.byte(register).write(device, newRegisterValue.toByte)
  } yield ()
}

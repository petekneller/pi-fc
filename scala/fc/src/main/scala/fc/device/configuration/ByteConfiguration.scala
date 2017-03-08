package fc.device.configuration

import cats.syntax.either._
import cats.syntax.eq._
import cats.instances.byte._
import ioctl.syntax._
import fc.device._

case class ByteConfiguration(register: Byte) extends Configuration {
  type T = Byte
  type Register = Byte

  def read(device: Address)(implicit controller: Controller { type Bus = device.Bus; type Register = Byte }): DeviceResult[Byte] =
    ByteRx.byte(register).read(device)

  def write(device: Address, value: Byte)(implicit controller: Controller { type Bus = device.Bus; type Register = Byte }): DeviceResult[Unit] =
    ByteTx.byte(register).write(device, value)
}

case class SingleBitFlag(register: Byte, bit: Int) extends Configuration { self =>
  type T = Boolean
  type Register = Byte

  def read(device: Address)(implicit controller: Controller { type Bus = device.Bus; type Register = self.Register }): DeviceResult[Boolean] =
    ByteRx.byte(register).read(device).map(registerValue => ((registerValue.unsigned >> bit) & 0x1) == 0x1)

  def write(device: Address, value: Boolean)(implicit controller: Controller { type Bus = device.Bus; type Register = self.Register }): DeviceResult[Unit] = for {
    originalValue <- ByteRx.byte(register).read(device)
    bitMask = 0x1 << bit
    newValue = if (value)
      (bitMask | originalValue)
    else
      (~bitMask & originalValue)
    _ <- ByteTx.byte(register).write(device, newValue.toByte)
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

case class MultiBitFlag[E <: FlagEnumeration](register: Byte, hiBit: Int, numBits: Int, options: E) extends Configuration { self =>
  type T = E#T
  type Register = Byte

  val loBit = hiBit - (numBits - 1)
  private val onesMask = (1 until numBits).fold(0x1){ (acc, _) => acc << 1 | 0x1 }
  val mask = (0 until loBit).fold(onesMask){ (acc, _) => acc << 1 }

  def read(device: Address)(implicit controller: Controller { type Bus = device.Bus; type Register = Byte }): DeviceResult[E#T] = for {
    registerValue <- ByteRx.byte(register).read(device)
    masked = registerValue.unsigned & mask
    enumOrdinal = (masked >> loBit).toByte
    enumValue <- options.values.find(_.value === enumOrdinal).toRight(FlagException(enumOrdinal, options.values))
  } yield enumValue

  def write(device: Address, value: E#T)(implicit controller: Controller { type Bus = device.Bus; type Register = Byte }): DeviceResult[Unit] = for {
    existingRegisterValue <- ByteRx.byte(register).read(device)
    existingWithConfigZeroed = existingRegisterValue.unsigned & ~mask
    newConfig = (value.value << loBit) & mask
    newRegisterValue = existingWithConfigZeroed | newConfig
    _ <- ByteTx.byte(register).write(device, newRegisterValue.toByte)
  } yield ()
}

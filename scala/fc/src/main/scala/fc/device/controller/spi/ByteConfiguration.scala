package fc.device.controller.spi

import cats.syntax.either._
import cats.syntax.eq._
import cats.instances.byte._
import eu.timepit.refined.W
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Interval
import eu.timepit.refined.auto.autoUnwrap
import ioctl.syntax._
import fc.device.api._

case class ByteConfiguration(register: Byte) extends Configuration {
  type T = Byte
  type Ctrl = SpiController

  def read(device: SpiAddress)(implicit controller: SpiController): DeviceResult[Byte] =
    rx.read(device)

  def write(device: SpiAddress, value: Byte)(implicit controller: SpiController): DeviceResult[Unit] =
    tx.write(device, value)

  private val rx = ByteRx(register)
  private val tx = ByteTx(register)
}

case class SingleBitFlag(register: Byte, bit: SingleBitFlag.BetweenZeroAndSeven) extends Configuration { self =>
  type T = Boolean
  type Ctrl = SpiController

  def read(device: SpiAddress)(implicit controller: SpiController): DeviceResult[Boolean] =
    rx.read(device).map(registerValue => ((registerValue.unsigned >> bit) & 0x1) == 0x1)

  def write(device: SpiAddress, value: Boolean)(implicit controller: SpiController): DeviceResult[Unit] = for {
    originalValue <- rx.read(device)
    bitMask = 0x1 << bit
    newValue = if (value)
      (bitMask | originalValue)
    else
      (~bitMask & originalValue)
    _ <- tx.write(device, newValue.toByte)
  } yield ()

  private val rx = ByteRx(register)
  private val tx = ByteTx(register)
}

object SingleBitFlag {
  type BetweenZeroAndSeven = Int Refined Interval.Closed[W.`0`.T, W.`7`.T]
}

trait FlagEnumeration {
  type T <: Flag
  trait Flag {
    val value: Byte
  }
  def values: Set[T]
}

case class FlagException[A](valueFound: Byte, optionsAvailable: Set[A]) extends DeviceException

case class MultiBitFlag[E <: FlagEnumeration](register: Byte, hiBit: SingleBitFlag.BetweenZeroAndSeven, numBits: Int Refined Interval.Closed[W.`1`.T, W.`8`.T], options: E) extends Configuration { self =>
  type T = E#T
  type Ctrl = SpiController

  val loBit = hiBit - (numBits - 1)
  private val onesMask = (1 until numBits).fold(0x1){ (acc, _) => acc << 1 | 0x1 }
  val mask = (0 until loBit).fold(onesMask){ (acc, _) => acc << 1 }

  def read(device: SpiAddress)(implicit controller: SpiController): DeviceResult[E#T] = for {
    registerValue <- rx.read(device)
    masked = registerValue.unsigned & mask
    enumOrdinal = (masked >> loBit).toByte
    enumValue <- options.values.find(_.value === enumOrdinal).toRight(FlagException(enumOrdinal, options.values))
  } yield enumValue

  def write(device: SpiAddress, value: E#T)(implicit controller: SpiController): DeviceResult[Unit] = for {
    existingRegisterValue <- rx.read(device)
    existingWithConfigZeroed = existingRegisterValue.unsigned & ~mask
    newConfig = (value.value << loBit) & mask
    newRegisterValue = existingWithConfigZeroed | newConfig
    _ <- tx.write(device, newRegisterValue.toByte)
  } yield ()

  private val rx = ByteRx(register)
  private val tx = ByteTx(register)
}

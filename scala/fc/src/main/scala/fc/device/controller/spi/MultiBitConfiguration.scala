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

case class MultiBitConfiguration[E <: FlagEnumeration](register: Byte, hiBit: SingleBitConfiguration.BetweenZeroAndSeven, numBits: Int Refined Interval.Closed[W.`1`.T, W.`8`.T], options: E) extends Configuration { self =>
  type T = E#T
  type Ctrl = SpiRegisterController

  val loBit = hiBit - (numBits - 1)
  private val onesMask = (1 until numBits).fold(0x1){ (acc, _) => acc << 1 | 0x1 }
  val mask = (0 until loBit).fold(onesMask){ (acc, _) => acc << 1 }

  def read(device: SpiAddress)(implicit controller: SpiRegisterController): DeviceResult[E#T] = for {
    registerValue <- rx.read(device)
    masked = registerValue.unsigned & mask
    enumOrdinal = (masked >> loBit).toByte
    enumValue <- options.values.find(_.value === enumOrdinal).toRight(FlagException(enumOrdinal, options.values))
  } yield enumValue

  def write(device: SpiAddress, value: E#T)(implicit controller: SpiRegisterController): DeviceResult[Unit] = for {
    existingRegisterValue <- rx.read(device)
    existingWithConfigZeroed = existingRegisterValue.unsigned & ~mask
    newConfig = (value.value << loBit) & mask
    newRegisterValue = existingWithConfigZeroed | newConfig
    _ <- tx.write(device, newRegisterValue.toByte)
  } yield ()

  private val rx = ByteRx(register)
  private val tx = ByteTx(register)
}

trait FlagEnumeration {
  type T <: Flag
  trait Flag {
    val value: Byte
  }
  def values: Set[T]
}

case class FlagException[A](valueFound: Byte, optionsAvailable: Set[A]) extends DeviceException

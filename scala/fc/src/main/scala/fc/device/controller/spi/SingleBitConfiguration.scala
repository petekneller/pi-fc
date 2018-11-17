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

case class SingleBitConfiguration(register: Byte, bit: SingleBitConfiguration.BetweenZeroAndSeven) extends Configuration { self =>
  type T = Boolean
  type Ctrl = SpiRegisterController

  def read(device: SpiAddress)(implicit controller: SpiRegisterController): DeviceResult[Boolean] =
    rx.read(device).map(registerValue => ((registerValue.unsigned >> bit) & 0x1) == 0x1)

  def write(device: SpiAddress, value: Boolean)(implicit controller: SpiRegisterController): DeviceResult[Unit] = for {
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

object SingleBitConfiguration {
  type BetweenZeroAndSeven = Int Refined Interval.Closed[W.`0`.T, W.`7`.T]
}

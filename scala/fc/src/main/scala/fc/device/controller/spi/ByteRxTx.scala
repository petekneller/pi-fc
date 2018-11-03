package fc.device.controller.spi

import cats.syntax.either._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import eu.timepit.refined.auto.autoRefineV
import ioctl.syntax._
import fc.device.api._

object ByteRx {
  def byte(sourceRegister: Byte) = new Rx {
    type T = Byte
    type Ctrl = SpiController

    def read(device: SpiAddress)(implicit controller: SpiController) = controller.receive(device, sourceRegister, 1) map (_.head)
  }

  def bytes(sourceRegister: Byte, numBytes: Int Refined Positive) = new Rx {
    type T = Seq[Byte]
    type Ctrl = SpiController

    def read(device: SpiAddress)(implicit controller: SpiController): DeviceResult[Seq[Byte]] = controller.receive(device, sourceRegister, numBytes)
  }

  def short(loByteRegister: Byte, hiByteRegister: Byte) = new Rx {
    type T = Short
    type Ctrl = SpiController

    def read(device: SpiAddress)(implicit controller: SpiController): DeviceResult[Short] = for {
      hiByte <- hiRx.read(device)
      loByte <- loRx.read(device)
    } yield ((hiByte << 8) | loByte.unsigned).toShort // if the low byte isn't unsigned before widening, any signing high bits will
                                                      // wipe away the data in the high register, but this isn't true the other way around
    private val hiRx = ByteRx.byte(hiByteRegister)
    private val loRx = ByteRx.byte(loByteRegister)
  }
}

object ByteTx {
  def byte[A](destinationRegister: Byte) = new Tx {
    type T = Byte
    type Ctrl = SpiController

    def write(device: SpiAddress, value: Byte)(implicit controller: Ctrl) = controller.transmit(device, destinationRegister, Seq(value))
  }
}

package fc.device

import cats.syntax.either._
import ioctl.syntax._

object ByteRx {
  def byte(sourceRegister: Byte) = new Rx {
    type T = Byte
    type Register = Byte
    def read(device: Address)(implicit controller: Controller { type Bus = device.Bus; type Register = Byte }) = controller.receive(device, sourceRegister, 1) map (_.head)
  }

  def bytes(sourceRegister: Byte, numBytes: Int) = new Rx {
    type T = Seq[Byte]
    type Register = Byte
    def read(device: Address)(implicit controller: Controller { type Bus = device.Bus; type Register = Byte }): DeviceResult[Seq[Byte]] = controller.receive(device, sourceRegister, numBytes)
  }

  def short(loByteRegister: Byte, hiByteRegister: Byte) = new Rx {
    type T = Short
    type Register = Byte
    def read(device: Address)(implicit controller: Controller { type Bus = device.Bus; type Register = Byte }): DeviceResult[Short] = for {
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
    type Register = Byte
    def write(device: Address, value: Byte)(implicit controller: Controller { type Bus = device.Bus; type Register = Byte }) = controller.transmit(device, destinationRegister, Seq(value))
  }
}

package fc.device

import cats.syntax.either._
import ioctl.syntax._

/*
 Rx and Tx represent _things_ that can be transferred to/from a device. eg. a bit, byte, word, sequence of bytes, configuration parameter/s.

 They build on the underlying Controller API and make it easier to encapsulate logical transactions that you might wish to make.
 */

trait Rx { self =>
  type T
  type Register
  def read(device: Address)(implicit controller: Controller { type Bus = device.Bus; type Register = self.Register }): DeviceResult[T]
}

object Rx {
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
      hiByte <- Rx.byte(hiByteRegister).read(device)
      loByte <- Rx.byte(loByteRegister).read(device)
    } yield ((hiByte << 8) | loByte.unsigned).toShort // if the low byte isn't unsigned before widening, any signing high bits will
                                                      // wipe away the data in the high register, but this isn't true the other way around
  }
}

trait Tx { self =>
  type T
  type Register
  def write(device: Address, value: T)(implicit controller: Controller { type Bus = device.Bus; type Register = self.Register }): DeviceResult[Unit]
}

object Tx {
  def byte[A](destinationRegister: Byte) = new Tx {
    type T = Byte
    type Register = Byte
    def write(device: Address, value: Byte)(implicit controller: Controller { type Bus = device.Bus; type Register = Byte }) = controller.transmit(device, destinationRegister, value)
  }
}

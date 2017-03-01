package fc.device

import cats.syntax.either._
import ioctl.syntax._

/*
 Rx and Tx represent _things_ that can be transferred to/from a device. eg. a bit, byte, word, sequence of bytes, configuration parameter/s.

 They build on the underlying Controller API and make it easier to encapsulate logical transactions that you might wish to make.
 */

trait Rx {
  type T
  def read(device: Address)(implicit controller: Controller { type Bus = device.Bus }): Either[DeviceException, T]
}

object Rx {
  def byte(sourceRegister: Register) = new Rx {
    type T = Byte
    def read(device: Address)(implicit controller: Controller { type Bus = device.Bus }) = controller.receive(device, sourceRegister, 1) map (_.head)
  }

  def bytes(sourceRegister: Register, numBytes: Int) = new Rx {
    type T = Seq[Byte]
    def read(device: Address)(implicit controller: Controller { type Bus = device.Bus }): Either[DeviceException, Seq[Byte]] = controller.receive(device, sourceRegister, numBytes)
  }

  def short(loByteRegister: Register, hiByteRegister: Register) = new Rx {
    type T = Short
    def read(device: Address)(implicit controller: Controller { type Bus = device.Bus }): Either[DeviceException, Short] = for {
      hiByte <- Rx.byte(hiByteRegister).read(device)
      loByte <- Rx.byte(loByteRegister).read(device)
    } yield ((hiByte << 8) | loByte.unsigned).toShort // if the low byte isn't unsigned before widening, any signing high bits will
                                                      // wipe away the data in the high register, but this isn't true the other way around
  }
}

trait Tx {
  type T
  def write(device: Address, value: T)(implicit controller: Controller { type Bus = device.Bus }): Either[DeviceException, Unit]
}

object Tx {
  def byte[A](destinationRegister: Register) = new Tx {
    type T = Byte
    def write(device: Address, value: Byte)(implicit controller: Controller { type Bus = device.Bus }) = controller.transmit(device, destinationRegister, value)
  }
}

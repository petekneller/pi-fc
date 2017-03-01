package fc.device

import cats.syntax.either._

/*
 Rx and Tx represent _things_ that can be transferred to/from a device. eg. a bit, byte, word, sequence of bytes, configuration parameter/s.

 They build on the underlying Controller API and make it easier to encapsulate logical transactions that you might wish to make.
 */

trait Tx {
  type T
  def transmit(device: Address, value: T)(implicit controller: Controller { type Bus = device.Bus }): Either[DeviceException, Unit]
}

object Tx {
  def byte[A](destinationRegister: Register) = new Tx {
    type T = Byte
    def transmit(device: Address, value: Byte)(implicit controller: Controller { type Bus = device.Bus }) = controller.transmit(device, destinationRegister, value)
  }
}

trait Rx {
  type T
  def receive(device: Address)(implicit controller: Controller { type Bus = device.Bus }): Either[DeviceException, T]
}

object Rx {
  def byte(sourceRegister: Register) = new Rx {
    type T = Byte
    def receive(device: Address)(implicit controller: Controller { type Bus = device.Bus }) = controller.receive(device, sourceRegister, 1) map (_.head)
  }

  def bytes(sourceRegister: Register, numBytes: Int) = new Rx {
    type T = Seq[Byte]
    def receive(device: Address)(implicit controller: Controller { type Bus = device.Bus }): Either[DeviceException, Seq[Byte]] = controller.receive(device, sourceRegister, numBytes)
  }
}

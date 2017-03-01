package fc.device

import cats.syntax.either._

trait Device {
  val address: Address
  implicit val controller: Controller { type Bus = address.Bus }

  def transmit(tx: Tx)(value: tx.T): Either[DeviceException, Unit] = tx.transmit(address, value)(controller)
  def receive(rx: Rx): Either[DeviceException, rx.T] = rx.receive(address)(controller)
}

object Device {
  def apply(a: Address)(c: Controller { type Bus = a.Bus }) = new Device {
    val address: Address { type Bus = a.Bus } = a
    implicit val controller: Controller { type Bus = address.Bus } = c
  }
}

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

package fc.device

import cats.syntax.either._

trait Device {
  val address: DeviceAddress
  implicit val controller: Controller { type Bus = address.Bus }

  def transmit(tx: Tx)(value: tx.T): Either[DeviceError, Unit] = tx.transmit(address, value)(controller)
  def receive(rx: Rx): Either[DeviceError, rx.T] = rx.receive(address)(controller)
}

object Device {
  def apply(a: DeviceAddress)(c: Controller { type Bus = a.Bus }) = new Device {
    val address: DeviceAddress { type Bus = a.Bus } = a
    implicit val controller: Controller { type Bus = address.Bus } = c
  }
}

trait Tx {
  type T
  def transmit(device: DeviceAddress, value: T)(implicit controller: Controller { type Bus = device.Bus }): Either[DeviceError, Unit]
}

object Tx {
  def byte[A](destinationRegister: DeviceRegister) = new Tx {
    type T = Byte
    def transmit(device: DeviceAddress, value: Byte)(implicit controller: Controller { type Bus = device.Bus }) = controller.transmit(device, destinationRegister, value)
  }
}

trait Rx {
  type T
  def receive(device: DeviceAddress)(implicit controller: Controller { type Bus = device.Bus }): Either[DeviceError, T]
}

object Rx {
  def byte(sourceRegister: DeviceRegister) = new Rx {
    type T = Byte
    def receive(device: DeviceAddress)(implicit controller: Controller { type Bus = device.Bus }) = controller.receive(device, sourceRegister, 1) map (_.head)
  }

  def bytes(sourceRegister: DeviceRegister, numBytes: Int) = new Rx {
    type T = Seq[Byte]
    def receive(device: DeviceAddress)(implicit controller: Controller { type Bus = device.Bus }): Either[DeviceError, Seq[Byte]] = controller.receive(device, sourceRegister, numBytes)
  }
}

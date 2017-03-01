package fc.device

import cats.syntax.either._

case class DeviceRegister(value: Byte)

trait DeviceAddress {
  type Bus

  def toFilename: String
}

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
    def transmit(device: DeviceAddress, value: Byte)(implicit controller: Controller { type Bus = device.Bus }) = controller.write(device, destinationRegister, value)
  }
}

trait Rx {
  type T
  def receive(device: DeviceAddress)(implicit controller: Controller { type Bus = device.Bus }): Either[DeviceError, T]
}

object Rx {
  def byte(sourceRegister: DeviceRegister) = new Rx {
    type T = Byte
    def receive(device: DeviceAddress)(implicit controller: Controller { type Bus = device.Bus }) = controller.read(device, sourceRegister, 1) map (_.head)
  }

  def bytes(sourceRegister: DeviceRegister, numBytes: Int) = new Rx {
    type T = Seq[Byte]
    def receive(device: DeviceAddress)(implicit controller: Controller { type Bus = device.Bus }): Either[DeviceError, Seq[Byte]] = controller.read(device, sourceRegister, numBytes)
  }
}

trait Controller { self =>
  type Bus

  def read(device: DeviceAddress { type Bus = self.Bus }, register: DeviceRegister, numBytes: Int): Either[DeviceError, Seq[Byte]]

  def write(device: DeviceAddress { type Bus = self.Bus }, register: DeviceRegister, data: Byte): Either[DeviceError, Unit]
}

trait DeviceError
case class DeviceUnavailableError(device: DeviceAddress, cause: Throwable) extends DeviceError
case class TransferFailedError(cause: Throwable) extends DeviceError
case class IncompleteDataError(expected: Int, actual: Int) extends DeviceError

trait Configuration extends Rx with Tx

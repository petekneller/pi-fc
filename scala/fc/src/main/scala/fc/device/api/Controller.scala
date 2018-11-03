package fc.device

package object api {
  type DeviceResult[A] = Either[DeviceException, A]
}

package api {

  /*
   Controller provides an abstraction over the different bus types, eg. SPI v I2C, at which a device can be reached at an Address
  */

  trait Address

  trait Controller {
    type Addr <: Address
  }

  trait DeviceException
  case class DeviceUnavailableException(device: Address, cause: Throwable) extends DeviceException
  case class TransferFailedException(cause: Throwable) extends DeviceException
  case class IncompleteDataException(expected: Int, actual: Int) extends DeviceException
}

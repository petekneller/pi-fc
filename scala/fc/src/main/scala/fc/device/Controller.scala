package fc.device

/*
 Controller provides an abstraction over the different bus types, eg. SPI v I2C.

 Transmit/receive is fundamentally byte-based. Transactions of other sizes (bit, word, multi-byte) are
 enabled via the Rx and Tx types, which transform more meaningful 'packets' into Controller transfers
 */
trait Controller { self =>
  type Bus

  def receive(device: DeviceAddress { type Bus = self.Bus }, register: DeviceRegister, numBytes: Int): Either[DeviceError, Seq[Byte]]

  def transmit(device: DeviceAddress { type Bus = self.Bus }, register: DeviceRegister, data: Byte): Either[DeviceError, Unit]
}

case class DeviceRegister(value: Byte)

trait DeviceAddress {
  type Bus

  def toFilename: String
}

trait DeviceError
case class DeviceUnavailableError(device: DeviceAddress, cause: Throwable) extends DeviceError
case class TransferFailedError(cause: Throwable) extends DeviceError
case class IncompleteDataError(expected: Int, actual: Int) extends DeviceError

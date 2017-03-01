package fc.device

/*
 Controller provides an abstraction over the different bus types, eg. SPI v I2C.

 Transmit/receive is fundamentally byte-based. Transactions of other sizes (bit, word, multi-byte) are
 enabled via the Rx and Tx types, which transform more meaningful 'packets' into Controller transfers
 */
trait Controller { self =>
  type Bus

  def receive(device: Address { type Bus = self.Bus }, register: Register, numBytes: Int): Either[DeviceException, Seq[Byte]]

  def transmit(device: Address { type Bus = self.Bus }, register: Register, data: Byte): Either[DeviceException, Unit]
}

case class Register(value: Byte)

trait Address {
  type Bus

  def toFilename: String
}

trait DeviceException
case class DeviceUnavailableError(device: Address, cause: Throwable) extends DeviceException
case class TransferFailedError(cause: Throwable) extends DeviceException
case class IncompleteDataError(expected: Int, actual: Int) extends DeviceException

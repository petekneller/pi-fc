package fc.device

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive

package object api {
  type DeviceResult[A] = Either[DeviceException, A]
}

package api {
  /*
   Controller provides an abstraction over the different bus types, eg. SPI v I2C.

   Transmit/receive is fundamentally byte-based. Transactions of other sizes (bit, word, multi-byte) are
   enabled via the Rx and Tx types, which transform more meaningful 'packets' into Controller transfers
   */
  trait Controller { self =>
    type Bus
    type Register

    /*
     @throws DeviceUnavailableException should something occur while trying to initially connect to the device
     @throws IncompleteDataException if the device failed to provide the requested number of bytes
     @throws TransferFailedException to signal a general low-level failure within the transer
     */
    def receive(device: Address { type Bus = self.Bus }, register: Register, numBytes: Int Refined Positive): DeviceResult[Seq[Byte]]

    /*
     @throws DeviceUnavailableException should something occur while trying to initially connect to the device
     @throws TransferFailedException to signal a general low-level failure within the transer
     */
    def transmit(device: Address { type Bus = self.Bus }, register: Register, data: Seq[Byte]): DeviceResult[Unit]
  }

  trait Address {
    type Bus

    def toFilename: String
  }

  trait DeviceException
  case class DeviceUnavailableException(device: Address, cause: Throwable) extends DeviceException
  case class TransferFailedException(cause: Throwable) extends DeviceException
  case class IncompleteDataException(expected: Int, actual: Int) extends DeviceException
}

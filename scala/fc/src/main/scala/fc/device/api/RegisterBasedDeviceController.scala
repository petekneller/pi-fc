package fc.device.api

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive

/*
 Transmission is fundamentally byte-based. Transactions of other sizes (bit, word, multi-byte) are
 enabled via the Rx and Tx types, which transform more meaningful 'packets' into Controller transfers
*/
trait RegisterBasedDeviceController extends Controller {
  type Register
  /*
   @throws DeviceUnavailableException should something occur while trying to initially connect to the device
   @throws IncompleteDataException if the device failed to provide the requested number of bytes
   @throws TransferFailedException to signal a general low-level failure within the transer
  */
  def receive(device: Addr, register: Register, numBytes: Int Refined Positive): DeviceResult[Seq[Byte]]

  /*
   @throws DeviceUnavailableException should something occur while trying to initially connect to the device
   @throws TransferFailedException to signal a general low-level failure within the transer
  */
  def transmit(device: Addr, register: Register, data: Seq[Byte]): DeviceResult[Unit]
}

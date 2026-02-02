package core.device.api

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.{ NonNegative, Positive }

/*
 * Some device buses (eg. SPI) have full-duplex communication between the master
 * and slave devices. This can mean that even a transmit-only action can cause data
 * to be returned that needs to be handled.
 */
trait FullDuplexController extends Controller {

  /*
   *  Be very careful using this method. If the size of `dataToWrite` is less than
   *  `numBytesToRead` then padding 0s will be sent to the device. If `dataToWrite`
   *  represents a partial message then the padding will cause message corruption.
   */
  @deprecated("Beware, using this method incorrectly can lead to data corruption")
  def transferN(device: Addr, dataToWrite: Seq[Byte], numBytesToRead: Int Refined NonNegative): DeviceResult[Seq[Byte]]

  def transfer(device: Addr, dataToWrite: Seq[Byte]): DeviceResult[Seq[Byte]]

  def receive(device: Addr, numBytesToRead: Int Refined Positive): DeviceResult[Seq[Byte]]
}

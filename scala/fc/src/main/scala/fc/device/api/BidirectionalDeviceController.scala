package fc.device.api

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.NonNegative

trait BidirectionalDeviceController extends Controller {

  /*
   *  Be very careful using this method. If the size of `dataToWrite` is less than
   *  `numBytesToRead` then padding 0s will be sent to the device. If `dataToWrite`
   *  represents a partial message then the padding will cause message corruption.
   */
  @deprecated("Beware, using this method incorrectly can lead to data corruption")
  def transferN(device: Addr, dataToWrite: Seq[Byte], numBytesToRead: Int Refined NonNegative): DeviceResult[Seq[Byte]]

}

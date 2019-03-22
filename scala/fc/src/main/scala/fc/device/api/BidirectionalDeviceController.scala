package fc.device.api

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.NonNegative

trait BidirectionalDeviceController extends Controller {

  def transfer(device: Addr, dataToWrite: Seq[Byte], numBytesToRead: Int Refined NonNegative): DeviceResult[Seq[Byte]]

}

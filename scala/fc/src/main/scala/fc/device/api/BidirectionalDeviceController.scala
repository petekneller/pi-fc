package fc.device.api

trait BidirectionalDeviceController extends Controller {

  def transfer(device: Addr, dataToWrite: Option[Byte]): DeviceResult[Byte]

}

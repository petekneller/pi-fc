package fc.device.api

/*
 Device is a simple convenience which bundles a device address and controller together. It's unlikely the controller
 associated with a device (address) will change over the course of an application so this makes it easier to manage.
*/
trait Device { self =>
  type Ctrl <: Controller
  implicit val controller: Ctrl
  val address: Ctrl#Addr

  def read(rx: Rx { type Ctrl >: self.Ctrl }): DeviceResult[rx.T] = rx.read(address)(controller)
  def write(tx: Tx { type Ctrl >: self.Ctrl })(value: tx.T): DeviceResult[Unit] = tx.write(address, value)(controller)
}

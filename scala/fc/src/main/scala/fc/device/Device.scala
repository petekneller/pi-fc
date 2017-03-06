package fc.device

/*
 Device is a simple convenience which bundles a device address and controller together. It's unlikely the controller
 associated with a device (address) will change over the course of an application so this makes it easier to manage.
 */
trait Device {
  val address: Address
  implicit val controller: Controller { type Bus = address.Bus }

  def read(rx: Rx): DeviceResult[rx.T] = rx.read(address)(controller)
  def write(tx: Tx)(value: tx.T): DeviceResult[Unit] = tx.write(address, value)(controller)
}

object Device {
  def apply(a: Address)(c: Controller { type Bus = a.Bus }) = new Device {
    val address: Address { type Bus = a.Bus } = a
    implicit val controller: Controller { type Bus = address.Bus } = c
  }
}

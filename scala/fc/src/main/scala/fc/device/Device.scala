package fc.device

/*
 Device is a simple convenience which bundles a device address and controller together. It's unlikely the controller
 associated with a device (address) will change over the course of an application so this makes it easier to manage.
 */
trait Device {
  val address: Address
  implicit val controller: Controller { type Bus = address.Bus }

  def transmit(tx: Tx)(value: tx.T): Either[DeviceException, Unit] = tx.transmit(address, value)(controller)
  def receive(rx: Rx): Either[DeviceException, rx.T] = rx.receive(address)(controller)
}

object Device {
  def apply(a: Address)(c: Controller { type Bus = a.Bus }) = new Device {
    val address: Address { type Bus = a.Bus } = a
    implicit val controller: Controller { type Bus = address.Bus } = c
  }
}

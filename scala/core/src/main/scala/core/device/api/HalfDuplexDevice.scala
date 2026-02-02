package core.device.api

/*
 * 'Device' is a simple convenience which bundles a device address and controller together.
 * It's unlikely the controller associated with a device (address) will change over the course of an
 * application so this makes it easier to manage.
 * The HalfDuplexDevice utilises Rx's and Tx's to abstract over the transfer protocol, which implies
 * that half-duplex transfers are available.
 */
trait HalfDuplexDevice { self =>
  type Ctrl <: HalfDuplexController
  implicit val controller: Ctrl
  val address: Ctrl#Addr

  def read(rx: Rx { type Ctrl >: self.Ctrl }): DeviceResult[rx.T] = rx.read(address)(controller)
  def write(tx: Tx { type Ctrl >: self.Ctrl })(value: tx.T): DeviceResult[Unit] = tx.write(address, value)(controller)
}

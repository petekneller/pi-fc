package fc.device.api

trait Configuration extends Rx with Tx

object JointConfiguration {
  def apply(rx: Rx)(tx: Tx { type Ctrl = rx.Ctrl; type T = rx.T  }) = new Configuration {
    type T = rx.T
    type Ctrl = rx.Ctrl

    def read(device: Ctrl#Addr)(implicit controller: Ctrl): DeviceResult[T] = rx.read(device)
    def write(device: Ctrl#Addr, value: T)(implicit controller: Ctrl): DeviceResult[Unit] = tx.write(device, value)
  }
}

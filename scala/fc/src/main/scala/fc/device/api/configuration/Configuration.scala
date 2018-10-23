package fc.device.api.configuration

import fc.device.api._

trait Configuration extends Rx with Tx

object JointConfiguration {
  def apply(rx: Rx)(tx: Tx { type Register = rx.Register; type T = rx.T  }) = new Configuration { self =>
    type Register = rx.Register
    type T = rx.T

    def read(device: Address)(implicit controller: Controller { type Bus = device.Bus; type Register = self.Register }): DeviceResult[T] = rx.read(device)

    def write(device: Address, value: T)(implicit controller: Controller { type Bus = device.Bus; type Register = self.Register }): DeviceResult[Unit] = tx.write(device, value)
  }
}

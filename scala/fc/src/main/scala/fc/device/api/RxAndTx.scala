package fc.device.api

/*
 Rx and Tx represent _things_ that can be transferred to/from a device. eg. a bit, byte, word, sequence of bytes, configuration parameter/s.

 They build on the underlying Controller API and make it easier to encapsulate logical transactions that you might wish to make.
 */

trait Rx { self =>
  type T
  type Register
  def read(device: Address)(implicit controller: Controller { type Bus = device.Bus; type Register = self.Register }): DeviceResult[T]
}

trait Tx { self =>
  type T
  type Register
  def write(device: Address, value: T)(implicit controller: Controller { type Bus = device.Bus; type Register = self.Register }): DeviceResult[Unit]
}

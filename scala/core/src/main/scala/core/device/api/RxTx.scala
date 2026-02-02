package core.device.api

/*
 * Rx and Tx represent _things_ that can be transferred to/from a device, eg. a bit, byte, word,
 * sequence of bytes, configuration parameter/s.
 * They build on the underlying Controller API and make it easier to encapsulate logical transactions
 * that you might wish to make.
 * It is implied that the underlying controller supports half-duplex comms - that reads and writes
 * can be made independently.
 */

trait Rx {
  type T
  type Ctrl <: HalfDuplexController
  def read(device: Ctrl#Addr)(implicit controller: Ctrl): DeviceResult[T]
}

trait Tx {
  type T
  type Ctrl <: HalfDuplexController
  def write(device: Ctrl#Addr, value: T)(implicit controller: Ctrl): DeviceResult[Unit]
}

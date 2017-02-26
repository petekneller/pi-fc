package fc.device.spi

import java.nio.ByteBuffer
import ioctl.IOCtl.O_RDWR
import ioctl.syntax._
import fc.device.{DeviceAddress, DeviceRegister, Controller}

case class SpiAddress(busNumber: Int, chipSelect: Int) extends DeviceAddress {
  type Bus = SpiBus

  def toFilename: String = s"/dev/spidev${busNumber}.${chipSelect}"
}

trait SpiBus

// TODO a better api to the transfer function? that gives me better types or at least param names?
class SpiController(api: SpiApi) extends Controller { self =>
  type Bus = SpiBus

  private val clockSpeed: Int = 100000

  def read(device: DeviceAddress { type Bus = self.Bus }, register: DeviceRegister, numBytes: Int): Seq[Byte] = {
    val fd = api.open(device.toFilename, O_RDWR)
    try {
      val requisiteBufferSize = numBytes + 1
      val txBuffer = ByteBuffer.allocateDirect(requisiteBufferSize)
      val rxBuffer = ByteBuffer.allocateDirect(requisiteBufferSize)

      txBuffer.put(0, (0x80 | register.value).toByte)
      val bytesTransferred = api.transfer(fd, txBuffer, rxBuffer, numBytes, clockSpeed)
      // TODO assert bytestransferred == expected
      rxBuffer.toSeq.drop(1) // first byte of receive buffer lines up with tx command, and so is empty

    } finally {
      api.close(fd)
    }
  }

  def write(device: DeviceAddress { type Bus = self.Bus }, register: DeviceRegister, data: Byte): Unit = {
    ???
  }
}

trait SpiApi {
  def transfer(fileDescriptor: Int, txBuffer: ByteBuffer, rxBuffer: ByteBuffer, numBytes: Int, clockSpeedHz: Int): Int
  def open(filename: String, flags: Int): Int
  def close(fileDescriptor: Int): Int
}

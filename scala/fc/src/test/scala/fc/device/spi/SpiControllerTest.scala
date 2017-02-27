package fc.device.spi

import java.nio.ByteBuffer
import org.scalatest.{FlatSpec, Matchers}
import org.scalactic.TypeCheckedTripleEquals
import org.scalamock.scalatest.MockFactory
import ioctl.syntax._
import fc.device._

class SpiControllerTest extends FlatSpec with Matchers with TypeCheckedTripleEquals with MockFactory {

  val mockApi = stub[SpiApi]
  val controller = new SpiController(mockApi)

  val device = SpiAddress(0, 0)
  val register = DeviceRegister(3)

  "read register" should "set a read flag in the first byte of the transmit buffer" in {
    val _ = readRegister(device, register)(controller)

    (mockApi.transfer _).verify(where { (_, txBuffer, _, _, _) => hasReadFlag(txBuffer) })
  }

  it should "set the register address in the lower 7 bits of the first byte of the transmit buffer" in {
    val _ = readRegister(device, register)(controller)

    (mockApi.transfer _).verify(where { (_, txBuffer, _, _, _) => hasRegister(txBuffer, register.value) })
  }

  it should "allocate at least 2 bytes in both the transmit and receive buffers" in {
    val _ = readRegister(device, register)(controller)

    (mockApi.transfer _).verify(where { (_, txBuffer, rxBuffer, _, _) =>
      txBuffer.limit >= 2 && rxBuffer.limit >= 2
    })
  }

  it should "write the second byte of the transmit buffer as 0x0" in {
    val _ = readRegister(device, register)(controller)

    (mockApi.transfer _).verify(where { (_, txBuffer, _, _, _) => txBuffer.get(1) == 0x0 })
  }

  it should "pass a receive buffer full of 0x0" in {
    val _ = readRegister(device, register)(controller)

    (mockApi.transfer _).verify(where { (_, _, rxBuffer, _, _) => rxBuffer.toSeq.forall(_ == 0x0) })
  }

  it should "return a 'device unavailable' error if the underlying 'open' call fails" in {
    val errorCause = new RuntimeException("")
    (mockApi.open _) when(*, *) throws errorCause

    readRegister(device, register)(controller) should === (Left(DeviceUnavailableError(device, errorCause)))
  }

  it should "return a 'transfer failed' error if the underlying 'transfer' call fails" in {
    val errorCause = new RuntimeException("")
    (mockApi.transfer _) when(*, *, *, *, *) throws errorCause

    val _ = readRegister(device, register)(controller) should === (Left(TransferFailedError(errorCause)))
  }

  it should "call 'close' even if the underlying 'transfer' call fails" in {
    (mockApi.transfer _) when(*, *, *, *, *) throws new RuntimeException("")

    val _ = readRegister(device, register)(controller)
    (mockApi.close _).verify(*).once()
  }

  def hasReadFlag(buffer: ByteBuffer): Boolean = buffer.get(0).unsigned >> 7 === 1

  def hasRegister(buffer: ByteBuffer, register: Byte): Boolean = (buffer.get(0).unsigned & 0x7).toByte === register
}

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
  val device = new Device {
    val address = SpiAddress(0, 0)
    implicit val controller = new SpiController(mockApi)
  }

  val register = DeviceRegister(3)

  "Rx.byte" should "set a read flag in the first byte of the transmit buffer" in {
    val _ = device.receive(Rx.byte(register))

    (mockApi.transfer _).verify(where { (_, txBuffer, _, _, _) => hasReadFlag(txBuffer) })
  }

  it should "set the register address in the lower 7 bits of the first byte of the transmit buffer" in {
    val _ = device.receive(Rx.byte(register))

    (mockApi.transfer _).verify(where { (_, txBuffer, _, _, _) => hasRegister(txBuffer, register.value) })
  }

  it should "allocate at least 2 bytes in both the transmit and receive buffers" in {
    val _ = device.receive(Rx.byte(register))

    (mockApi.transfer _).verify(where { (_, txBuffer, rxBuffer, _, _) =>
      txBuffer.limit >= 2 && rxBuffer.limit >= 2
    })
  }

  it should "write the second byte of the transmit buffer as 0x0" in {
    val _ = device.receive(Rx.byte(register))

    (mockApi.transfer _).verify(where { (_, txBuffer, _, _, _) => txBuffer.get(1) == 0x0 })
  }

  it should "pass a receive buffer full of 0x0" in {
    val _ = device.receive(Rx.byte(register))

    (mockApi.transfer _).verify(where { (_, _, rxBuffer, _, _) => rxBuffer.toSeq.forall(_ == 0x0) })
  }

  it should "return a 'device unavailable' error if the underlying 'open' call fails" in {
    val errorCause = new RuntimeException("")
    (mockApi.open _) when(*, *) throws errorCause

    device.receive(Rx.byte(register)) should === (Left(DeviceUnavailableError(device.address, errorCause)))
  }

  it should "return a 'transfer failed' error if the underlying 'transfer' call fails" in {
    val errorCause = new RuntimeException("")
    (mockApi.transfer _) when(*, *, *, *, *) throws errorCause

    device.receive(Rx.byte(register)) should === (Left(TransferFailedError(errorCause)))
  }

  it should "call 'close' even if the underlying 'transfer' call fails" in {
    (mockApi.transfer _) when(*, *, *, *, *) throws new RuntimeException("")

    val _ = device.receive(Rx.byte(register))
    (mockApi.close _).verify(*).once()
  }

  "Rx.bytes" should "allocate at least N+1 bytes in both the transmit and receive buffer" in {
    val _ = device.receive(Rx.bytes(register, 3))

    (mockApi.transfer _).verify(where { (_, txBuffer, rxBuffer, _, _) =>
      txBuffer.limit >= 4 && rxBuffer.limit >= 4
    })
  }

  it should "return the requested number of bytes" in {
    (mockApi.transfer _).when(*, *, *, *, *).onCall{ (_, _, rxBuffer, _, _) =>
      rxBuffer.put(0x1.toByte).put(0x2.toByte).put(0x3.toByte).put(0x4.toByte)
      4
    }

    device.receive(Rx.bytes(register, 3)) should === (Right(Seq(0x2.toByte, 0x3.toByte, 0x4.toByte)))
  }

  it should "attempt to transfer N+1 bytes" in {
    device.receive(Rx.bytes(register, 3))

    (mockApi.transfer _).verify(*, *, *, 4, *)
  }

  it should "return an 'incomplete data' error if less than N bytes could be fetched" in {
    val expectedNumBytes = 2
    val actualNumBytes = 1
    (mockApi.transfer _).when(*, *, *, expectedNumBytes+1, *).onCall{ (_, _, rxBuffer, _, _) =>
      rxBuffer.put(0x1.toByte).put(0x2.toByte)
      actualNumBytes + 1
    }

    device.receive(Rx.bytes(register, expectedNumBytes)) should === (Left(IncompleteDataError(expectedNumBytes, actualNumBytes)))
  }

  "Tx.byte" should "not set a read flag in the first byte of the transmit buffer" in {
    val _ = device.transmit(Tx.byte(register))(0x55)

    (mockApi.transfer _).verify(where { (_, txBuffer, _, _, _) => !hasReadFlag(txBuffer) })
  }

  it should "set the data byte in the second byte of the transmit buffer" in {
    val data = 0x55.toByte
    val _ = device.transmit(Tx.byte(register))(data)

    (mockApi.transfer _).verify(where { (_, txBuffer, _, _, _) => txBuffer.get(1) == data })
  }

  def hasReadFlag(buffer: ByteBuffer): Boolean = buffer.get(0).unsigned >> 7 === 1

  def hasRegister(buffer: ByteBuffer, register: Byte): Boolean = (buffer.get(0).unsigned & 0x7).toByte === register
}

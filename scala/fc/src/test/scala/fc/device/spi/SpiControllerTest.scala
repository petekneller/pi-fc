package fc.device.spi

import java.nio.ByteBuffer
import eu.timepit.refined.auto.{autoRefineV, autoUnwrap}
import eu.timepit.refined.refineMV
import eu.timepit.refined.numeric.Positive
import spire.syntax.literals._
import org.scalatest.{FlatSpec, Matchers}
import org.scalactic.TypeCheckedTripleEquals
import org.scalamock.scalatest.MockFactory
import ioctl.syntax._
import fc.device._

class SpiControllerTest extends FlatSpec with Matchers with TypeCheckedTripleEquals with MockFactory {

  val mockApi = stub[SpiApi]
  val controller = new SpiController(mockApi)
  val device = new Device {
    type Register = Byte
    val address = SpiAddress(0, 0)
    implicit val controller = new SpiController(mockApi)
  }

  val register = b"3"

  "Rx.byte" should "set a read flag in the first byte of the transmit buffer" in {
    val _ = device.read(ByteRx.byte(register))

    (mockApi.transfer _).verify(where { (_, txBuffer, _, _, _) => hasReadFlag(txBuffer) })
  }

  it should "set the register address in the lower 7 bits of the first byte of the transmit buffer" in {
    val _ = device.read(ByteRx.byte(register))

    (mockApi.transfer _).verify(where { (_, txBuffer, _, _, _) => hasRegister(txBuffer, register) })
  }

  it should "allocate at least 2 bytes in both the transmit and receive buffers" in {
    val _ = device.read(ByteRx.byte(register))

    (mockApi.transfer _).verify(where { (_, txBuffer, rxBuffer, _, _) =>
      txBuffer.limit >= 2 && rxBuffer.limit >= 2
    })
  }

  it should "write the second byte of the transmit buffer as 0x0" in {
    val _ = device.read(ByteRx.byte(register))

    (mockApi.transfer _).verify(where { (_, txBuffer, _, _, _) => txBuffer.get(1) == 0x0 })
  }

  it should "pass a receive buffer full of 0x0" in {
    val _ = device.read(ByteRx.byte(register))

    (mockApi.transfer _).verify(where { (_, _, rxBuffer, _, _) => rxBuffer.toSeq.forall(_ == 0x0) })
  }

  it should "return a 'device unavailable' error if the underlying 'open' call fails" in {
    val errorCause = new RuntimeException("")
    (mockApi.open _) when(*, *) throws errorCause

    device.read(ByteRx.byte(register)) should === (Left(DeviceUnavailableException(device.address, errorCause)))
  }

  it should "return a 'transfer failed' error if the underlying 'transfer' call fails" in {
    val errorCause = new RuntimeException("")
    (mockApi.transfer _) when(*, *, *, *, *) throws errorCause

    device.read(ByteRx.byte(register)) should === (Left(TransferFailedException(errorCause)))
  }

  it should "call 'close' even if the underlying 'transfer' call fails" in {
    (mockApi.transfer _) when(*, *, *, *, *) throws new RuntimeException("")

    val _ = device.read(ByteRx.byte(register))
    (mockApi.close _).verify(*).once()
  }

  "Rx.bytes" should "allocate at least N+1 bytes in both the transmit and receive buffer" in {
    val _ = device.read(ByteRx.bytes(register, 3))

    (mockApi.transfer _).verify(where { (_, txBuffer, rxBuffer, _, _) =>
      txBuffer.limit >= 4 && rxBuffer.limit >= 4
    })
  }

  it should "return the requested number of bytes" in {
    (mockApi.transfer _).when(*, *, *, *, *).onCall{ (_, _, rxBuffer, _, _) =>
      rxBuffer.put(b"1").put(b"2").put(b"3").put(b"4")
      4
    }

    device.read(ByteRx.bytes(register, 3)) should === (Right(Seq(b"2", b"3", b"4")))
  }

  it should "attempt to transfer N+1 bytes" in {
    device.read(ByteRx.bytes(register, 3))

    (mockApi.transfer _).verify(*, *, *, 4, *)
  }

  it should "return an 'incomplete data' error if less than N bytes could be fetched" in {
    val expectedNumBytes = refineMV[Positive](2)
    val actualNumBytes = 1
    (mockApi.transfer _).when(*, *, *, expectedNumBytes+1, *).onCall{ (_, _, rxBuffer, _, _) =>
      rxBuffer.put(b"1").put(b"2")
      actualNumBytes + 1
    }

    device.read(ByteRx.bytes(register, expectedNumBytes)) should === (Left(IncompleteDataException(expectedNumBytes, actualNumBytes)))
  }

  "Tx.byte" should "not set a read flag in the first byte of the transmit buffer" in {
    val _ = device.write(ByteTx.byte(register))(0x55)

    (mockApi.transfer _).verify(where { (_, txBuffer, _, _, _) => !hasReadFlag(txBuffer) })
  }

  it should "set the data byte in the second byte of the transmit buffer" in {
    val data = 0x55.toByte
    val _ = device.write(ByteTx.byte(register))(data)

    (mockApi.transfer _).verify(where { (_, txBuffer, _, _, _) => txBuffer.get(1) == data })
  }

  def hasReadFlag(buffer: ByteBuffer): Boolean = buffer.get(0).unsigned >> 7 === 1

  def hasRegister(buffer: ByteBuffer, register: Byte): Boolean = (buffer.get(0).unsigned & 0x7).toByte === register
}

package fc.device.controller.spi

import java.nio.ByteBuffer
import eu.timepit.refined.auto.{autoRefineV, autoUnwrap}
import eu.timepit.refined.refineV
import eu.timepit.refined.numeric.Positive
import spire.syntax.literals._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalactic.TypeCheckedTripleEquals
import org.scalamock.scalatest.MockFactory
import ioctl.syntax._
import ioctl.IOCtl.O_RDWR
import fc.device.api._

class SpiControllerTest extends AnyFlatSpec with Matchers with TypeCheckedTripleEquals with MockFactory {

  val mockApi = stub[SpiApi]
  val controller = new SpiControllerImpl(mockApi)
  val device = SpiAddress(0, 0)
  val register = b"3"
  val fd = 2

  /*
   Tests for basic file open/close, failure handling
   */

  "receive" should "open the correct file" in {
    controller.receive(device, register, 1)
    (mockApi.open _).verify("/dev/spidev0.0", *)
  }

  it should "open the underlying file read-write" in {
    controller.receive(device, register, 1)
    (mockApi.open _).verify(*, O_RDWR)
  }

  it should "return a 'device unavailable' error if the underlying 'open' call fails" in {
    val errorCause = new RuntimeException("")
    (mockApi.open _) when(*, *) throws errorCause

    controller.receive(device, register, 1) should === (Left(DeviceUnavailableException(device, errorCause)))
  }

  "transmit" should "open the correct file" in {
    controller.transmit(device, register, Seq(b"1"))
    (mockApi.open _).verify("/dev/spidev0.0", *)
  }

  it should "open the underlying file read-write" in {
    controller.transmit(device, register, Seq(b"1"))
    (mockApi.open _).verify(*, O_RDWR)
  }

  it should "return a 'device unavailable' error if the underlying 'open' call fails" in {
    val errorCause = new RuntimeException("")
    (mockApi.open _) when(*, *) throws errorCause

    controller.transmit(device, register, Seq(b"1")) should === (Left(DeviceUnavailableException(device, errorCause)))
  }

  "transferN" should "open the correct file" in {
    controller.transferN(device, Seq.empty[Byte], 0)
    (mockApi.open _).verify("/dev/spidev0.0", *)
  }

  it should "open the underlying file read-write" in {
    controller.transferN(device, Seq.empty[Byte], 0)
    (mockApi.open _).verify(*, O_RDWR)
  }

  it should "return a 'device unavailable' error if the underlying 'open' call fails" in {
    val errorCause = new RuntimeException("")
    (mockApi.open _) when(*, *) throws errorCause

    controller.transferN(device, Seq.empty[Byte], 0) should === (Left(DeviceUnavailableException(device, errorCause)))
  }

  "transfer" should "open the correct file" in {
    controller.transfer(device, Seq.empty[Byte])
    (mockApi.open _).verify("/dev/spidev0.0", *)
  }

  it should "open the underlying file read-write" in {
    controller.transfer(device, Seq.empty[Byte])
    (mockApi.open _).verify(*, O_RDWR)
  }

  it should "return a 'device unavailable' error if the underlying 'open' call fails" in {
    val errorCause = new RuntimeException("")
    (mockApi.open _) when(*, *) throws errorCause

    controller.transfer(device, Seq.empty[Byte]) should === (Left(DeviceUnavailableException(device, errorCause)))
  }

  "receive (bi-directional)" should "open the correct file" in {
    controller.receive(device, 1)
    (mockApi.open _).verify("/dev/spidev0.0", *)
  }

  it should "open the underlying file read-write" in {
    controller.receive(device, 1)
    (mockApi.open _).verify(*, O_RDWR)
  }

  it should "return a 'device unavailable' error if the underlying 'open' call fails" in {
    val errorCause = new RuntimeException("")
    (mockApi.open _) when(*, *) throws errorCause

    controller.receive(device, 1) should === (Left(DeviceUnavailableException(device, errorCause)))
  }

  "receive" should "close the underlying file even if an error occurs during transfer" in {
    (mockApi.open _).when(*, *).returns(fd)
    (mockApi.transfer _).when(*, *, *, *, *) throws new RuntimeException("")

    controller.receive(device, register, 1)
    (mockApi.close _).verify(fd).once()
  }

  it should "return a 'transfer failed' error if the underlying 'transfer' call fails" in {
    val errorCause = new RuntimeException("")
    (mockApi.transfer _).when(*, *, *, *, *).throws(errorCause)

    controller.receive(device, register, 1) should === (Left(TransferFailedException(errorCause)))
  }

  "transmit" should "close the underlying file even if an error occurs during transfer" in {
    (mockApi.open _).when(*, *).returns(fd)
    (mockApi.transfer _).when(*, *, *, *, *) throws new RuntimeException("")

    controller.transmit(device, register, Seq(b"1"))
    (mockApi.close _).verify(fd).once()
  }

  it should "return a 'transfer failed' error if the underlying 'transfer' call fails" in {
    val errorCause = new RuntimeException("")
    (mockApi.transfer _).when(*, *, *, *, *).throws(errorCause)

    controller.transmit(device, register, Seq(b"1")) should === (Left(TransferFailedException(errorCause)))
  }

  "transferN" should "close the underlying file even if an error occurs during transfer" in {
    (mockApi.open _).when(*, *).returns(fd)
    (mockApi.transfer _).when(*, *, *, *, *) throws new RuntimeException("")

    controller.transferN(device, Seq.empty[Byte], 0)
    (mockApi.close _).verify(fd).once()
  }

  it should "return a 'transfer failed' error if the underlying 'transfer' call fails" in {
    val errorCause = new RuntimeException("")
    (mockApi.transfer _).when(*, *, *, *, *).throws(errorCause)

    controller.transferN(device, Seq.empty[Byte], 0) should === (Left(TransferFailedException(errorCause)))
  }

  "transfer" should "close the underlying file even if an error occurs during transfer" in {
    (mockApi.open _).when(*, *).returns(fd)
    (mockApi.transfer _).when(*, *, *, *, *) throws new RuntimeException("")

    controller.transfer(device, Seq.empty[Byte])
    (mockApi.close _).verify(fd).once()
  }

  it should "return a 'transfer failed' error if the underlying 'transfer' call fails" in {
    val errorCause = new RuntimeException("")
    (mockApi.transfer _).when(*, *, *, *, *).throws(errorCause)

    controller.transfer(device, Seq.empty[Byte]) should === (Left(TransferFailedException(errorCause)))
  }

  "receive (bi-directional)" should "close the underlying file even if an error occurs during transfer" in {
    (mockApi.open _).when(*, *).returns(fd)
    (mockApi.transfer _).when(*, *, *, *, *) throws new RuntimeException("")

    controller.receive(device, 1)
    (mockApi.close _).verify(fd).once()
  }

  it should "return a 'transfer failed' error if the underlying 'transfer' call fails" in {
    val errorCause = new RuntimeException("")
    (mockApi.transfer _).when(*, *, *, *, *).throws(errorCause)

    controller.receive(device, 1) should === (Left(TransferFailedException(errorCause)))
  }

  /*
   Tests for the detail of byte transfers
   */

  "receive" should "attempt to transfer N+1 bytes" in {
    controller.receive(device, register, 3)
    (mockApi.transfer _).verify(*, *, *, 4, *)
  }

  it should "allocate N+1 bytes in both the transmit and receive buffers" in {
    controller.receive(device, register, 3)

    (mockApi.transfer _).verify(where { (_, txBuffer, rxBuffer, _, _) =>
      txBuffer.limit() === 4 && rxBuffer.limit() === 4
    })
  }

  "transmit" should "attempt to transfer N+1 bytes" in {
    controller.transmit(device, register, Seq(b"1", b"2", b"3"))
    (mockApi.transfer _).verify(*, *, *, 4, *)
  }

  it should "allocate N+1 bytes in both the transmit and receive buffers" in {
    controller.transmit(device, register, Seq(b"1", b"2", b"3"))

    (mockApi.transfer _).verify(where { (_, txBuffer, rxBuffer, _, _) =>
      txBuffer.limit() === 4 && rxBuffer.limit() === 4
    })
  }

  "transferN" should "attempt to transfer the maximum of the outgoing or the requested number of bytes (1)" in {
    controller.transferN(device, Seq.empty[Byte], 0)
    (mockApi.transfer _).verify(*, *, *, 0, *)
  }

  it should "attempt to transfer the maximum of the outgoing or the requested number of bytes (2)" in {
    controller.transferN(device, Seq(b"1", b"2", b"3"), 3)
    (mockApi.transfer _).verify(*, *, *, 3, *)
  }

  it should "attempt to transfer the maximum of the outgoing or the requested number of bytes (3)" in {
    controller.transferN(device, Seq.empty[Byte], 10)
    (mockApi.transfer _).verify(*, *, *, 10, *)
  }

  it should "attempt to transfer the maximum of the outgoing or the requested number of bytes (4)" in {
    controller.transferN(device, Seq(b"1", b"2", b"3"), 0)
    (mockApi.transfer _).verify(*, *, *, 3, *)
  }

  it should "allocate the same number of bytes in both the transmit and receive buffers (1)" in {
    controller.transferN(device, Seq.empty[Byte], 0)

    (mockApi.transfer _).verify(where { (_, txBuffer, rxBuffer, _, _) =>
      txBuffer.limit() === 0 && rxBuffer.limit() === 0
    })
  }

  it should "allocate the same number of bytes in both the transmit and receive buffers (2)" in {
    controller.transferN(device, Seq(b"1", b"2", b"3"), 3)

    (mockApi.transfer _).verify(where { (_, txBuffer, rxBuffer, _, _) =>
      txBuffer.limit() === 3 && rxBuffer.limit() === 3
    })
  }

  it should "allocate the same number of bytes in both the transmit and receive buffers (3)" in {
    controller.transferN(device, Seq.empty[Byte], 10)

    (mockApi.transfer _).verify(where { (_, txBuffer, rxBuffer, _, _) =>
      txBuffer.limit() === 10 && rxBuffer.limit() === 10
    })
  }

  it should "allocate the same number of bytes in both the transmit and receive buffers (4)" in {
    controller.transferN(device, Seq(b"1", b"2", b"3"), 0)

    (mockApi.transfer _).verify(where { (_, txBuffer, rxBuffer, _, _) =>
      txBuffer.limit() === 3 && rxBuffer.limit() === 3
    })
  }

  "transfer" should "attempt to transfer the number of bytes present in the input data" in {
    controller.transfer(device, Seq(b"1", b"2", b"3"))
    (mockApi.transfer _).verify(*, *, *, 3, *)
  }

  it should "allocate the same number of bytes in both the transmit and receive buffers" in {
    controller.transfer(device, Seq(b"1", b"2", b"3"))

    (mockApi.transfer _).verify(where { (_, txBuffer, rxBuffer, _, _) =>
      txBuffer.limit() === 3 && rxBuffer.limit() === 3
    })
  }

  "receive (bi-directional)" should "attempt to transfer the requested number of bytes" in {
    controller.receive(device, 3)
    (mockApi.transfer _).verify(*, *, *, 3, *)
  }

  it should "allocate the same number of bytes in both the transmit and receive buffers" in {
    controller.receive(device, 3)

    (mockApi.transfer _).verify(where { (_, txBuffer, rxBuffer, _, _) =>
      txBuffer.limit() === 3 && rxBuffer.limit() === 3
    })
  }

  "receive" should "set a read flag in the first byte of the transmit buffer" in {
    controller.receive(device, register, 1)

    (mockApi.transfer _).verify(where { (_, txBuffer, _, _, _) => hasReadFlag(txBuffer) })
  }

  it should "set the register address in the lower 7 bits of the first byte of the transmit buffer" in {
    controller.receive(device, register, 1)

    (mockApi.transfer _).verify(where { (_, txBuffer, _, _, _) => hasRegister(txBuffer, register) })
  }

  "transmit" should "set a write flag in the first byte of the transmit buffer" in {
    controller.transmit(device, register, Seq(b"1"))

    (mockApi.transfer _).verify(where { (_, txBuffer, _, _, _) => hasWriteFlag(txBuffer) })
  }

  it should "set the register address in the lower 7 bits of the first byte of the transmit buffer" in {
    controller.transmit(device, register, Seq(b"1"))

    (mockApi.transfer _).verify(where { (_, txBuffer, _, _, _) => hasRegister(txBuffer, register) })
  }

  "receive" should "write the second and subsequent bytes of the transmit buffer as 0x0" in {
    controller.receive(device, register, 3)

    (mockApi.transfer _).verify(where { (_, txBuffer, _, _, _) => txBuffer.toSeq.tail.forall(_ == 0x0) })
  }

  it should "pass a receive buffer full of 0x0" in {
    controller.receive(device, register, 5)

    (mockApi.transfer _).verify(where { (_, _, rxBuffer, _, _) => rxBuffer.toSeq.forall(_ == 0x0) })
  }

  "transmit" should "set the data bytes in the second and subsequent bytes of the transmit buffer" in {
    val data = Seq(0x55.toByte, 0x66.toByte)
    controller.transmit(device, register, data)

    (mockApi.transfer _).verify(where { (_, txBuffer, _, _, _) => txBuffer.toSeq.tail == data })
  }

  "transferN" should "set the outgoing bytes in the transmit buffer, padding the rest with zeroes if the incoming number exceeds the outgoing" in {
    controller.transferN(device, Seq(0x12, 0x34), 3)

    (mockApi.transfer _).verify(where {(_, txBuffer, _, _, _) =>
      txBuffer.get(0) === 0x12.toByte
      txBuffer.get(1) === 0x34.toByte
      txBuffer.get(2) === 0x00.toByte
    })
  }

  "transfer" should "set the outgoing bytes in the transmit buffer" in {
    controller.transfer(device, Seq(0x12, 0x34))

    (mockApi.transfer _).verify(where {(_, txBuffer, _, _, _) =>
      txBuffer.get(0) === 0x12.toByte
      txBuffer.get(1) === 0x34.toByte
    })
  }

  "receive" should "return the requested number of bytes" in {
    (mockApi.transfer _).when(*, *, *, *, *).onCall{ (_, _, rxBuffer, _, _) =>
      rxBuffer.put(b"1").put(b"2").put(b"3").put(b"4")
      4
    }

    controller.receive(device, register, 3) should === (Right(Seq(b"2", b"3", b"4")))
  }

  it should "return an 'incomplete data' error if less than N bytes could be fetched" in {
    val expectedNumBytes = 2
    val actualNumBytes = 1
    (mockApi.transfer _).when(*, *, *, expectedNumBytes+1, *).onCall{ (_, _, rxBuffer, _, _) =>
      rxBuffer.put(b"1").put(b"2")
      actualNumBytes + 1
    }

    controller.receive(device, register, refineV[Positive](expectedNumBytes).right.get) should === (Left(IncompleteDataException(expectedNumBytes, actualNumBytes)))
  }

  "transmit" should "return an error if less bytes were written than specified" in {
    val desiredNumBytes = 3
    val numBytesWritten = 3 // desired number bytes + 1 command byte
    (mockApi.transfer _).when(*, *, *, *, *).returns(numBytesWritten)

    controller.transmit(device, register, Seq(b"1", b"2", b"3")) should === (Left(IncompleteDataException(desiredNumBytes, numBytesWritten - 1)))
  }

  "transferN" should "return the maximum of the outgoing or the requested number of bytes" in {
    (mockApi.transfer _).when(*, *, *, *, *).onCall{ (_, _, rxBuffer, _, _) =>
      rxBuffer.put(b"1").put(b"2")
      2
    }

    controller.transferN(device, Seq(0x12, 0x34), 1) === Right(Seq(b"1", b"2"))
  }

  "receive (bi-directional)" should "return the requested number of bytes" in {
    (mockApi.transfer _).when(*, *, *, *, *).onCall{ (_, _, rxBuffer, _, _) =>
      rxBuffer.put(b"1").put(b"2").put(b"3")
      3
    }

    controller.receive(device, 3) should === (Right(Seq(b"1", b"2", b"3")))
  }

  it should "return an 'incomplete data' error if less than N bytes could be fetched" in {
    val expectedNumBytes = 2
    val actualNumBytes = 1
    (mockApi.transfer _).when(*, *, *, expectedNumBytes, *).onCall{ (_, _, rxBuffer, _, _) =>
      rxBuffer.put(b"1")
      actualNumBytes
    }

    controller.receive(device, refineV[Positive](expectedNumBytes).right.get) should === (Left(IncompleteDataException(expectedNumBytes, actualNumBytes)))
  }


  def hasReadFlag(buffer: ByteBuffer): Boolean = buffer.get(0).unsigned >> 7 === 1

  def hasWriteFlag(buffer: ByteBuffer): Boolean = buffer.get(0).unsigned >> 7 === 0

  def hasRegister(buffer: ByteBuffer, register: Byte): Boolean = (buffer.get(0).unsigned & 0x7).toByte === register
}

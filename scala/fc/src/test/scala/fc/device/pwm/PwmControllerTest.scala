package fc.device.pwm

import org.scalatest.{FlatSpec, Matchers}
import org.scalactic.TypeCheckedTripleEquals
import org.scalamock.scalatest.MockFactory
import ioctl.IOCtl.O_RDWR
import ioctl.IOCtlImpl.size_t
import fc.device._

class PwmControllerTest extends FlatSpec with Matchers with TypeCheckedTripleEquals with MockFactory {

  val mockPwmApi = stub[PwmApi]
  implicit val controller = new PwmController(mockPwmApi)
  val device = PwmAddress(1, 2)
  val register = "foo"
  val fd = 2

  "receive" should "open the underlying file correctly" in {
    (mockPwmApi.read _).when(*, *, *).returns(new size_t)

    controller.receive(device, "bar", 1) should === (Right(Seq.empty))
    (mockPwmApi.open _).verify("/sys/class/pwm/pwmchip1/pwm2/bar", O_RDWR)
  }

  it should "close the underlying file even if an error occurs during read" in {
    val result = 1.toByte
    val errorCause = new RuntimeException("")
    (mockPwmApi.open _).when(*, *).returns(fd)
    (mockPwmApi.read _).when(*, *, *).throws(errorCause)

    controller.receive(device, register, 1) should === (Left(TransferFailedException(errorCause)))
    (mockPwmApi.close _).verify(fd).once()
  }

  it should "succeed if the underlying device returned less bytes than were specified" in {
    val two = 2.toByte
    val three = 3.toByte
    val desiredNumBytes = 3
    (mockPwmApi.open _).when(*, *).returns(fd)
    (mockPwmApi.read _).when(*, *, *).onCall{ (_, rxBuffer, desiredNumBytes) =>
      rxBuffer.put(0, two).put(1, three)
      new size_t(2L)
    }

    controller.receive(device, register, desiredNumBytes) should === (Right(Seq(two, three)))
  }

  "transmit" should "open the underlying file correctly" in {
    (mockPwmApi.write _).when(*, *, *).returns(new size_t(1L))

    controller.transmit(device, "foo", Seq(1.toByte)) should === (Right(()))
    (mockPwmApi.open _).verify("/sys/class/pwm/pwmchip1/pwm2/foo", O_RDWR)
  }

  it should "close the underlying file even if an error occurs during write" in {
    val fd = 3
    val errorCause = new RuntimeException("")
    (mockPwmApi.open _).when(*, *).returns(fd)
    (mockPwmApi.write _).when(*, *, *).throws(errorCause)

    controller.transmit(device, register, Seq(1.toByte)) should === (Left(TransferFailedException(errorCause)))
    (mockPwmApi.close _).verify(fd).once()
  }

  it should "return an error if less bytes were written than specified" in {
    val desiredNumBytes = 3
    val numBytesWritten = 2
    (mockPwmApi.write _).when(*, *, *).returns(new size_t(numBytesWritten.toLong))

    controller.transmit(device, register, Seq(1.toByte, 2.toByte, 3.toByte)) should === (Left(IncompleteDataException(desiredNumBytes, numBytesWritten)))
  }

  "RxString" should "consider each byte in the response to be an ANSI character" in {
    val rx = RxString(register)
    (mockPwmApi.open _).when(*, *).returns(fd)
    (mockPwmApi.read _).when(*, *, *).onCall{ (_, rxBuffer, _) =>
      rxBuffer.put(0, 'f'.toByte).put(1, 'o'.toByte).put(2, 'o'.toByte)
      new size_t(3L)
    }

    rx.read(device) should === (Right("foo"))
  }

  it should "read a maximum number of bytes as specified in the constructor" in {
    val rx = RxString(register, 3)
    (mockPwmApi.open _).when(*, *).returns(fd)

    rx.read(device)
    (mockPwmApi.read _).verify(where { (_, _, numBytesToRead) => numBytesToRead.intValue == 3 })
  }

  it should "remove any trailing newlines" in {
    val rx = RxString(register)
    (mockPwmApi.open _).when(*, *).returns(fd)
    (mockPwmApi.read _).when(*, *, *).onCall{ (_, rxBuffer, _) =>
      rxBuffer.put(0, 'f'.toByte).put(1, 'o'.toByte).put(2, 'o'.toByte).put(3, '\n')
      new size_t(4L)
    }

    rx.read(device) should === (Right("foo"))
  }

  "TxString" should "convert the input string into a sequence of bytes, where each byte is an ANSI character" in {
    val tx = TxString(register)
    (mockPwmApi.open _).when(*, *).returns(fd)
    (mockPwmApi.write _).when(*, *, *).onCall{ (_, _, numBytes) => numBytes }

    tx.write(device, "bar") should === (Right(()))
    (mockPwmApi.write _).verify(where { (_, txBuffer, numBytes) =>
      txBuffer.get(0).toChar == 'b' &&
      txBuffer.get(1).toChar == 'a' &&
      txBuffer.get(2).toChar == 'r' &&
      numBytes.intValue == 3
    })
  }

}

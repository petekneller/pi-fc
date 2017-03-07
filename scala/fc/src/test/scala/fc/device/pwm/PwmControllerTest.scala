package fc.device.pwm

import org.scalatest.{FlatSpec, Matchers}
import org.scalactic.TypeCheckedTripleEquals
import org.scalamock.scalatest.MockFactory
import ioctl.IOCtl.O_RDWR
import ioctl.IOCtlImpl.size_t
import fc.device._

class PwmControllerTest extends FlatSpec with Matchers with TypeCheckedTripleEquals with MockFactory {

  val mockPwmApi = stub[PwmApi]
  val controller = new PwmController(mockPwmApi)
  val device = PwmAddress(1, 2)
  val register = "foo"

  "receive" should "open the underlying file correctly" in {
    (mockPwmApi.read _).when(*, *, *).returns(new size_t)

    controller.receive(device, "bar", 1) should === (Right(Seq.empty))
    (mockPwmApi.open _).verify("/sys/class/pwm/pwmchip1/pwm2/bar", O_RDWR)
  }

  it should "close the underlying file even if an error occurs during read" in {
    val result = 1.toByte
    val fd = 2
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
    (mockPwmApi.open _).when(*, *).returns(1)
    (mockPwmApi.read _).when(*, *, *).onCall{ (*, rxBuffer, desiredNumBytes) => rxBuffer.put(0, two).put(1, three); new size_t(2L) }

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

  // Rx.string should convert string to bytes correctly



}

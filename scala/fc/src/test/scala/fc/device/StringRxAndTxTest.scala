package fc.device

import org.scalatest.{FlatSpec, Matchers}
import org.scalactic.TypeCheckedTripleEquals
import org.scalamock.scalatest.MockFactory

class StringRxAndTxTest extends FlatSpec with Matchers with TypeCheckedTripleEquals with MockFactory with DeviceTestUtils {
  val device = new MockDeviceAddress()
  implicit val mockController = stub[MockStringController]
  val register = "foo"

  // NB. RxString.string and TxString.string are covered as part of the FileController test suite.

  "RxString.numeric" should "transform a sequence character bytes into an long value" in {
    (mockController.receive _).when(*, *, *).returns(Right(Seq('1'.toByte, '2'.toByte, '3'.toByte)))

    RxString.numeric(register).read(device) should === (Right(123L))
  }

  it should "return an error if the value in the register is not numeric" in {
    (mockController.receive _).when(*, *, *).returns(Right(Seq('a'.toByte)))

    RxString.numeric(register).read(device) should === (Left(NotNumericException("a")))
  }

  "TxString.numeric" should "transform a long into a sequence of character bytes" in {
    (mockController.transmit _).when(*, *, *).returns(Right(()))

    TxString.numeric(register).write(device, 1234L)
    (mockController.transmit _).verify(*, *, Seq('1'.toByte, '2'.toByte, '3'.toByte, '4'.toByte))
  }

  // negative numbers?

}

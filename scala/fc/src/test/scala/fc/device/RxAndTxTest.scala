package fc.device

import org.scalatest.{FlatSpec, Matchers}
import org.scalactic.TypeCheckedTripleEquals
import org.scalamock.scalatest.MockFactory

class RxAndTxTest extends FlatSpec with Matchers with TypeCheckedTripleEquals with MockFactory with DeviceTestUtils {

  val device = new MockDeviceAddress
  implicit val mockController = stub[MockController]

  val loByteRegister = 0x40.toByte
  val hiByteRegister = 0x41.toByte

  "Rx.short" should "fetch 2 8-bit registers and combine them into a 16-bit word" in {
    (mockController.receive _).when(*, loByteRegister, 1).returns(Right(Seq(0x34.toByte)))
    (mockController.receive _).when(*, hiByteRegister, 1).returns(Right(Seq(0x12.toByte)))

    Rx.short(loByteRegister, hiByteRegister).read(device) should === (Right(0x1234.toShort))
  }

  it should "handle properly a negative signed value in the low byte" in {
    (mockController.receive _).when(*, loByteRegister, 1).returns(Right(Seq(0x84.toByte)))
    (mockController.receive _).when(*, hiByteRegister, 1).returns(Right(Seq(0x12.toByte)))

    Rx.short(loByteRegister, hiByteRegister).read(device) should === (Right(0x1284.toShort))
  }

  it should "handle properly a negative signed value in the high byte" in {
    (mockController.receive _).when(*, loByteRegister, 1).returns(Right(Seq(0x34.toByte)))
    (mockController.receive _).when(*, hiByteRegister, 1).returns(Right(Seq(0x82.toByte)))

    Rx.short(loByteRegister, hiByteRegister).read(device) should === (Right(0x8234.toShort))
  }
}

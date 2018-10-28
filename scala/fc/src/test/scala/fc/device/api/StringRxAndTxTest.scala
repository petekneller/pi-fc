package fc.device.api

import eu.timepit.refined.auto.{autoRefineV, autoUnwrap}
import org.scalatest.{FlatSpec, Matchers}
import org.scalactic.TypeCheckedTripleEquals
import org.scalamock.scalatest.MockFactory

class StringRxAndTxTest extends FlatSpec with Matchers with TypeCheckedTripleEquals with MockFactory with DeviceTestUtils {
  val device = new MockDeviceAddress()
  implicit val mockController = stub[MockStringController]
  val register = "foo"

  "RxString.string" should "consider each byte in the response to be an ANSI character" in {
    val rx = RxString.string(register)
    (mockController.receive _).when(*, *, *).returns(Right(Seq('f'.toByte, 'o'.toByte, 'o'.toByte)))

    rx.read(device) should === (Right("foo"))
  }

  it should "read a maximum number of bytes as specified in the constructor" in {
    val rx = RxString.string(register, 2)
    (mockController.receive _).when(*, *, *).returns(Right(Seq('f'.toByte, 'o'.toByte, 'o'.toByte)))

    rx.read(device) should === (Right("fo"))
    (mockController.receive _).verify(where { (_, _, numBytesToRead) => (numBytesToRead: Int) == 2 })
  }

  it should "remove any trailing newlines" in {
    val rx = RxString.string(register)
    (mockController.receive _).when(*, *, *).returns(Right(Seq('f'.toByte, 'o'.toByte, 'o'.toByte, '\n'.toByte)))

    rx.read(device) should === (Right("foo"))
  }

  "TxString.string" should "convert the input string into a sequence of bytes, where each byte is an ANSI character" in {
    val tx = TxString.string(register)
    (mockController.transmit _).when(*, *, *).returns(Right(()))

    tx.write(device, "bar") should === (Right(()))
    (mockController.transmit _).verify(where { (_, _, bytes) =>
      bytes === Seq('b'.toByte, 'a'.toByte, 'r'.toByte)
    })
  }

  "RxString.numeric" should "transform a sequence character bytes into an long value" in {
    (mockController.receive _).when(*, *, *).returns(Right(Seq('1'.toByte, '2'.toByte, '3'.toByte)))

    RxString.numeric(register).read(device) should === (Right(123L))
  }

  it should "return an error if the value in the register is not numeric" in {
    (mockController.receive _).when(*, *, *).returns(Right(Seq('a'.toByte)))

    RxString.numeric(register).read(device) should === (Left(NotNumericException("a")))
  }

  it should "transform from Long to the desired result type" in {
    (mockController.receive _).when(*, *, *).returns(Right(Seq('5'.toByte)))

    RxString.numeric(register, toFoo).read(device) should === (Right(Bar))
  }

  "TxString.numeric" should "transform a long into a sequence of character bytes" in {
    (mockController.transmit _).when(*, *, *).returns(Right(()))

    TxString.numeric(register).write(device, 1234L)
    (mockController.transmit _).verify(*, *, Seq('1'.toByte, '2'.toByte, '3'.toByte, '4'.toByte))
  }

  it should "transform from the specified source type to Long" in {
    (mockController.transmit _).when(*, *, *).returns(Right(()))

    TxString.numeric(register, fromFoo).write(device, Baz)
    (mockController.transmit _).verify(*, *, Seq('1'.toByte, '0'.toByte))
  }

  // negative numbers?

  trait Foo
  object Bar extends Foo
  object Baz extends Foo

  def toFoo(l: Long): Foo = if (l < 10) Bar else Baz

  def fromFoo(f: Foo): Long = if (f == Bar) 5L else 10L

}

package fc.device.configuration

import eu.timepit.refined.refineMV
import eu.timepit.refined.numeric.Positive
import org.scalatest.{FlatSpec, Matchers}
import org.scalactic.TypeCheckedTripleEquals
import org.scalamock.scalatest.MockFactory
import fc.device.DeviceTestUtils

class StringConfigurationsTest extends FlatSpec with Matchers with TypeCheckedTripleEquals with DeviceTestUtils with MockFactory {
  val device = new MockDeviceAddress()
  implicit val mockController = stub[MockStringController]
  val register = "foo"

  "BooleanConfiguration.read" should "transform the strings '1' and '0' into a boolean value" in {

    (mockController.receive _).when(*, "foo", *).returns(Right(Seq('1'.toByte)))
    BooleanConfiguration("foo").read(device) should === (Right(true))

    (mockController.receive _).when(*, "bar", *).returns(Right(Seq('0'.toByte)))
    BooleanConfiguration("bar").read(device) should === (Right(false))
  }

  it should "only request 1 byte of data" in {
    (mockController.receive _).when(*, *, *).returns(Right(Seq('1')))

    BooleanConfiguration(register).read(device)
    (mockController.receive _).verify(*, *, refineMV[Positive](1))
  }

  it should "return an error when a value other than '1' or '0' is found" in {
    (mockController.receive _).when(*, *, *).returns(Right(Seq('a'.toByte)))

    BooleanConfiguration(register).read(device) should === (Left(NotABooleanException("a")))
  }

  "BooleanConfiguration.write" should "transform the boolean argument into '1' and '0'" in {
    (mockController.transmit _).when(*, *, *).returns(Right(()))

    BooleanConfiguration("foo").write(device, true)
    (mockController.transmit _).verify(*, "foo", Seq('1'.toByte))

    BooleanConfiguration("bar").write(device, false)
    (mockController.transmit _).verify(*, "bar", Seq('0'.toByte))
  }

  "NumericConfiguration.read" should "transform a sequence character bytes into an long value" in {
    (mockController.receive _).when(*, *, *).returns(Right(Seq('1'.toByte, '2'.toByte, '3'.toByte)))

    NumericConfiguration(register).read(device) should === (Right(123L))
  }

  it should "return an error if the value in the register is not numeric" in {
    (mockController.receive _).when(*, *, *).returns(Right(Seq('a'.toByte)))

    NumericConfiguration(register).read(device) should === (Left(NotNumericException("a")))
  }

  "NumericConfiguration.write" should "transform a long into a sequence of character bytes" in {
    (mockController.transmit _).when(*, *, *).returns(Right(()))

    NumericConfiguration(register).write(device, 1234L)
    (mockController.transmit _).verify(*, *, Seq('1'.toByte, '2'.toByte, '3'.toByte, '4'.toByte))
  }

  // negative numbers?

}

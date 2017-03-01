package fc.device.configuration

import org.scalatest.{FlatSpec, Matchers}
import org.scalactic.TypeCheckedTripleEquals
import org.scalamock.scalatest.MockFactory
import fc.device._

class ConfigurationTest extends FlatSpec with Matchers with TypeCheckedTripleEquals with MockFactory {

  val device = new MockDeviceAddress
  implicit val mockController = stub[MockController]
  val register = Register(0x35)

  "ByteConfiguration.receive" should "make a read request for that register" in {
    val configValue = 0x12.toByte
    val config = ByteConfiguration(register)

    (mockController.receive _).when(*, *, *).returns(Right(Seq(configValue)))
    config.receive(device)
    (mockController.receive _).verify(*, register, 1)
  }

  it should "return the whole register value" in {
    val configValue = 0x12.toByte
    val config = ByteConfiguration(register)

    (mockController.receive _).when(*, *, *).returns(Right(Seq(configValue)))
    config.receive(device) should === (Right(configValue))
  }

  "ByteConfiguration.transmit" should "set the whole register value" in {
    val config = ByteConfiguration(register)
    val configValue = 0x33.toByte

    config.transmit(device, configValue)
    (mockController.transmit _).verify(*, register, configValue)
  }

  "FlagConfiguration.receive" should "return a boolean that reflects the value of the bit specified in the configuration arguments" in {
    val registerValue = 0x04.toByte
    val bit1Flag = FlagConfiguration(register, 1)
    val bit2Flag = FlagConfiguration(register, 2)

    (mockController.receive _).when(*, *, *).returns(Right(Seq(registerValue)))
    bit1Flag.receive(device) should === (Right(false))
    bit2Flag.receive(device) should === (Right(true))
  }

  "FlagConfiguration.transmit" should "not affect bits in the register outside of that defined for the configuration" in {
    val bit1Flag = FlagConfiguration(register, 1)
    val originalValue = 0x71.toByte

    (mockController.receive _).when(*, *, 1).returns(Right(Seq(originalValue)))
    (mockController.transmit _).when(*, *, *)returns(Right(Unit))
    bit1Flag.transmit(device, true)
    (mockController.transmit _).verify(*, register, 0x73.toByte)
  }

  class MockDeviceAddress extends Address {
    type Bus = Boolean
    def toFilename = "unused"
  }

  trait MockController extends Controller { type Bus = Boolean }
}

package fc.device.configuration

import org.scalatest.{FlatSpec, Matchers}
import org.scalactic.TypeCheckedTripleEquals
import org.scalamock.scalatest.MockFactory
import fc.device._

class ConfigurationTest extends FlatSpec with Matchers with TypeCheckedTripleEquals with MockFactory with DeviceTestUtils {

  val device = new MockDeviceAddress
  implicit val mockController = stub[MockController]
  val register = Register(0x35)

  "ByteConfiguration.read" should "make a read request for that register" in {
    val configValue = 0x12.toByte
    val config = ByteConfiguration(register)

    (mockController.receive _).when(*, *, *).returns(Right(Seq(configValue)))
    config.read(device)
    (mockController.receive _).verify(*, register, 1)
  }

  it should "return the whole register value" in {
    val configValue = 0x12.toByte
    val config = ByteConfiguration(register)

    (mockController.receive _).when(*, *, *).returns(Right(Seq(configValue)))
    config.read(device) should === (Right(configValue))
  }

  "ByteConfiguration.write" should "set the whole register value" in {
    val config = ByteConfiguration(register)
    val configValue = 0x33.toByte

    config.write(device, configValue)
    (mockController.transmit _).verify(*, register, configValue)
  }

  "SingleBitFlag.read" should "return a boolean that reflects the value of the bit specified in the configuration arguments" in {
    val registerValue = 0x04.toByte
    val bit1Flag = SingleBitFlag(register, 1)
    val bit2Flag = SingleBitFlag(register, 2)

    (mockController.receive _).when(*, *, *).returns(Right(Seq(registerValue)))
    bit1Flag.read(device) should === (Right(false))
    bit2Flag.read(device) should === (Right(true))
  }

  "SingleBitFlag.write" should "not affect bits in the register outside of that defined for the configuration" in {
    val bit1Flag = SingleBitFlag(register, 1)

    (mockController.receive _).when(*, *, 1).returns(Right(Seq(0x71.toByte)))
    (mockController.transmit _).when(*, *, *)returns(Right(Unit))
    bit1Flag.write(device, true)
    (mockController.transmit _).verify(*, register, 0x73.toByte)
  }

  it should "be able to also unset bits" in {
    val bit1Flag = SingleBitFlag(register, 1)

    (mockController.receive _).when(*, *, 1).returns(Right(Seq(0x73.toByte)))
    (mockController.transmit _).when(*, *, *)returns(Right(Unit))
    bit1Flag.write(device, false)
    (mockController.transmit _).verify(*, register, 0x71.toByte)
  }
}

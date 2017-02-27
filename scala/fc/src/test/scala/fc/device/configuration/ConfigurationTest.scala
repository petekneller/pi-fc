package fc.device.configuration

import org.scalatest.{FlatSpec, Matchers}
import org.scalactic.TypeCheckedTripleEquals
import org.scalamock.scalatest.MockFactory
import fc.device._

class ConfigurationTest extends FlatSpec with Matchers with TypeCheckedTripleEquals with MockFactory {

  val device = new MockDeviceAddress
  val mockController = stub[MockController]
  val register = DeviceRegister(0x35)

  "readConfiguration for a byte register" should "make a read request for that register" in {
    val configValue = 0x12.toByte
    val config = ByteConfiguration(register)

    (mockController.read _).when(*, *, *).returns(Right(Seq(configValue)))
    readConfiguration(device, config)(mockController)
    (mockController.read _).verify(*, register, 1)
  }

  it should "return the whole register value" in {
    val configValue = 0x12.toByte
    val config = ByteConfiguration(register)

    (mockController.read _).when(*, *, *).returns(Right(Seq(configValue)))
    readConfiguration(device, config)(mockController) should === (Right(configValue))
  }

  "readConfiguration for a boolean" should "return a boolean that reflects the value of the bit specified in the configuration arguments" in {
    val registerValue = 0x04.toByte
    val bit2Flag = FlagConfiguration(register, 1)
    val bit3Flag = FlagConfiguration(register, 2)

    (mockController.read _).when(*, *, *).returns(Right(Seq(registerValue)))
    readConfiguration(device, bit2Flag)(mockController) should === (Right(false))
    readConfiguration(device, bit3Flag)(mockController) should === (Right(true))
  }

  class MockDeviceAddress extends DeviceAddress {
    type Bus = Boolean
    def toFilename = "unused"
  }

  trait MockController extends Controller { type Bus = Boolean }
}

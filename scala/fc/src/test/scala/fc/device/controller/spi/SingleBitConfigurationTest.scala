package fc.device.controller.spi

import eu.timepit.refined.refineMV
import eu.timepit.refined.auto.autoRefineV
import eu.timepit.refined.numeric.Positive
import spire.syntax.literals._
import org.scalatest.{FlatSpec, Matchers}
import org.scalactic.TypeCheckedTripleEquals
import org.scalamock.scalatest.MockFactory
import fc.device.api._

class SingleBitConfigurationTest extends FlatSpec with Matchers with TypeCheckedTripleEquals with MockFactory {

  val device = SpiAddress(0, 0)
  implicit val mockController = stub[SpiRegisterController]
  val register = 0x35.toByte

  "read" should "return a boolean that reflects the value of the bit specified in the configuration arguments" in {
    val registerValue = 0x04.toByte
    val bit1Flag = SingleBitConfiguration(register, 1)
    val bit2Flag = SingleBitConfiguration(register, 2)

    (mockController.receive _).when(*, *, *).returns(Right(Seq(registerValue)))
    bit1Flag.read(device) should === (Right(false))
    bit2Flag.read(device) should === (Right(true))
  }

  "write" should "not affect bits in the register outside of that defined for the configuration" in {
    val bit1Flag = SingleBitConfiguration(register, 1)

    (mockController.receive _).when(*, *, refineMV[Positive](1)).returns(Right(Seq(0x71.toByte)))
    (mockController.transmit _).when(*, *, *)returns(Right(Unit))
    bit1Flag.write(device, true)
    (mockController.transmit _).verify(*, register, Seq(0x73.toByte))
  }

  it should "be able to also unset bits" in {
    val bit1Flag = SingleBitConfiguration(register, 1)

    (mockController.receive _).when(*, *, refineMV[Positive](1)).returns(Right(Seq(0x73.toByte)))
    (mockController.transmit _).when(*, *, *)returns(Right(Unit))
    bit1Flag.write(device, false)
    (mockController.transmit _).verify(*, register, Seq(0x71.toByte))
  }

}

package fc.device.controller.spi

import eu.timepit.refined.refineMV
import eu.timepit.refined.auto.autoRefineV
import eu.timepit.refined.numeric.Positive
import spire.syntax.literals._
import org.scalatest.{FlatSpec, Matchers}
import org.scalactic.TypeCheckedTripleEquals
import org.scalamock.scalatest.MockFactory
import fc.device.api._

class ByteConfigurationTest extends FlatSpec with Matchers with TypeCheckedTripleEquals with MockFactory {

  val device = SpiAddress(0, 0)
  implicit val mockController = stub[SpiRegisterController]
  val register = 0x35.toByte

  "read" should "make a read request for that register" in {
    val configValue = 0x12.toByte
    val config = ByteConfiguration(register)

    (mockController.receive _).when(*, *, *).returns(Right(Seq(configValue)))
    config.read(device)
    (mockController.receive _).verify(*, register, refineMV[Positive](1))
  }

  it should "return the whole register value" in {
    val configValue = 0x12.toByte
    val config = ByteConfiguration(register)

    (mockController.receive _).when(*, *, *).returns(Right(Seq(configValue)))
    config.read(device) should === (Right(configValue))
  }

  "write" should "set the whole register value" in {
    val config = ByteConfiguration(register)
    val configValue = 0x33.toByte

    config.write(device, configValue)
    (mockController.transmit _).verify(*, register, Seq(configValue))
  }

}

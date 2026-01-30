package fc.device.sensor

import eu.timepit.refined.refineMV
import eu.timepit.refined.numeric.Positive
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalactic.TypeCheckedTripleEquals
import org.scalamock.scalatest.MockFactory
import fc.device.controller.spi.{SpiRegisterController, SpiAddress}

class MeasurementTest extends AnyFlatSpec with Matchers with TypeCheckedTripleEquals with MockFactory {

  val address = SpiAddress(0, 0)
  implicit val mockController = stub[SpiRegisterController]
  val register1 = 1.toByte
  val register2 = 2.toByte

  "Measurement.read" should "normalize the 16-bit register values before applying the scaling factor" in {
    (mockController.receive _).when(*, register1, refineMV[Positive](1)).returns(Right(Seq(0xFF.toByte)))
    (mockController.receive _).when(*, register2, refineMV[Positive](1)).returns(Right(Seq(0x7F.toByte)))

    val scaleOf1 = new FullScale { val factor = 1.0 }
    Measurement(register1, register2, scaleOf1).read(address).map(between(0.99, 1.01)) should === (Right(true))
  }

  "Measurement.read" should "handle signed values appropriately" in {
    (mockController.receive _).when(*, register1, refineMV[Positive](1)).returns(Right(Seq(0xFF.toByte)))
    (mockController.receive _).when(*, register2, refineMV[Positive](1)).returns(Right(Seq(0xFF.toByte)))

    val unscaled = new FullScale { val factor = 32768.0 }
    Measurement(register1, register2, unscaled).read(address).map(between(-1.01, -0.99)) should === (Right(true))
  }


  def between(lower: Double, upper: Double): Double => Boolean = (actual) => actual >= lower && actual <= upper

}

package fc.device.sensor

import org.scalatest.{FlatSpec, Matchers}
import org.scalactic.TypeCheckedTripleEquals
import org.scalamock.scalatest.MockFactory
import cats.syntax.either._
import fc.device._

class MeasurementTest extends FlatSpec with Matchers with TypeCheckedTripleEquals with MockFactory with DeviceTestUtils {

  val address = new MockDeviceAddress
  implicit val mockController = stub[MockByteController]
  val register1 = 1.toByte
  val register2 = 2.toByte

  "Meaurement.read" should "normalize the 16-bit register values before applying the scaling factor" in {
    (mockController.receive _).when(*, register1, 1).returns(Right(Seq(0xFF.toByte)))
    (mockController.receive _).when(*, register2, 1).returns(Right(Seq(0x7F.toByte)))

    val scaleOf1 = new FullScale { val factor = 1.0 }
    Measurement(register1, register2, scaleOf1).read(address).map(between(0.99, 1.01)) should === (Right(true))
  }

  "Measurement.read" should "handle signed values appropriately" in {
    (mockController.receive _).when(*, register1, 1).returns(Right(Seq(0xFF.toByte)))
    (mockController.receive _).when(*, register2, 1).returns(Right(Seq(0xFF.toByte)))

    val unscaled = new FullScale { val factor = 32768.0 }
    Measurement(register1, register2, unscaled).read(address).map(between(-1.01, -0.99)) should === (Right(true))
  }


  def between(lower: Double, upper: Double): Double => Boolean = (actual) => actual >= lower && actual <= upper

}

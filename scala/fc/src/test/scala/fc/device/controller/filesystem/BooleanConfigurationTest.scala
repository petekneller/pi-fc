package fc.device.controller.filesystem

import eu.timepit.refined.refineMV
import eu.timepit.refined.numeric.Positive
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalactic.TypeCheckedTripleEquals
import org.scalamock.scalatest.MockFactory

class BooleanConfigurationTest extends AnyFlatSpec with Matchers with TypeCheckedTripleEquals with MockFactory {

  val device = new FileSystemAddress { def toFilename = "unused" }
  implicit val mockController: FileSystemController = stub[FileSystemController]
  val register1 = FileSystemRegister("foo")
  val register2 = FileSystemRegister("bar")

  "BooleanConfiguration.read" should "transform the strings '1' and '0' into a boolean value" in {

    (mockController.receive _).when(*, register1, *).returns(Right(Seq('1'.toByte)))
    BooleanConfiguration(register1).read(device) should === (Right(true))

    (mockController.receive _).when(*, register2, *).returns(Right(Seq('0'.toByte)))
    BooleanConfiguration(register2).read(device) should === (Right(false))
  }

  it should "only request 1 byte of data" in {
    (mockController.receive _).when(*, *, *).returns(Right(Seq('1')))

    BooleanConfiguration(register1).read(device)
    (mockController.receive _).verify(*, *, refineMV[Positive](1))
  }

  it should "return an error when a value other than '1' or '0' is found" in {
    (mockController.receive _).when(*, *, *).returns(Right(Seq('a'.toByte)))

    BooleanConfiguration(register1).read(device) should === (Left(NotABooleanException("a")))
  }

  "BooleanConfiguration.write" should "transform the boolean argument into '1' and '0'" in {
    (mockController.transmit _).when(*, *, *).returns(Right(()))

    BooleanConfiguration(register1).write(device, true)
    (mockController.transmit _).verify(*, register1, Seq('1'.toByte))

    BooleanConfiguration(register2).write(device, false)
    (mockController.transmit _).verify(*, register2, Seq('0'.toByte))
  }

}

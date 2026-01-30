package fc.device.controller.filesystem

import eu.timepit.refined.auto.autoUnwrap
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalactic.TypeCheckedTripleEquals
import org.scalamock.scalatest.MockFactory

class NumericRxTest extends AnyFlatSpec with Matchers with TypeCheckedTripleEquals with MockFactory {

  val device = new FileSystemAddress { def toFilename = "unused" }
  implicit val mockController = stub[FileSystemController]
  val register = "foo"

  "NumericRx" should "transform a sequence character bytes into an long value" in {
    (mockController.receive _).when(*, *, *).returns(Right(Seq('1'.toByte, '2'.toByte, '3'.toByte)))

    NumericRx(register).read(device) should === (Right(123L))
  }

  it should "return an error if the value in the register is not numeric" in {
    (mockController.receive _).when(*, *, *).returns(Right(Seq('a'.toByte)))

    NumericRx(register).read(device) should === (Left(NotNumericException("a")))
  }

  it should "transform from Long to the desired result type" in {
    (mockController.receive _).when(*, *, *).returns(Right(Seq('5'.toByte)))

    NumericRx(register, toFoo).read(device) should === (Right(Bar))
  }

  trait Foo
  object Bar extends Foo
  object Baz extends Foo

  def toFoo(l: Long): Foo = if (l < 10) Bar else Baz

  def fromFoo(f: Foo): Long = if (f == Bar) 5L else 10L

}

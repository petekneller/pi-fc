package fc.device.controller.filesystem

import eu.timepit.refined.auto.autoUnwrap
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalactic.TypeCheckedTripleEquals
import org.scalamock.scalatest.MockFactory

class NumericTxTest extends AnyFlatSpec with Matchers with TypeCheckedTripleEquals with MockFactory {

  val device = new FileSystemAddress { def toFilename = "unused" }
  implicit val mockController: FileSystemController = stub[FileSystemController]
  val register = "foo"

  "NumericTx" should "transform a long into a sequence of character bytes" in {
    (mockController.transmit _).when(*, *, *).returns(Right(()))

    NumericTx(register).write(device, 1234L)
    (mockController.transmit _).verify(*, *, Seq('1'.toByte, '2'.toByte, '3'.toByte, '4'.toByte))
  }

  it should "transform from the specified source type to Long" in {
    (mockController.transmit _).when(*, *, *).returns(Right(()))

    NumericTx(register, fromFoo).write(device, Baz)
    (mockController.transmit _).verify(*, *, Seq('1'.toByte, '0'.toByte))
  }

  // negative numbers?

  trait Foo
  object Bar extends Foo
  object Baz extends Foo

  def toFoo(l: Long): Foo = if (l < 10) Bar else Baz

  def fromFoo(f: Foo): Long = if (f == Bar) 5L else 10L

}

package fc.device.controller.filesystem

import eu.timepit.refined.auto.{autoRefineV, autoUnwrap}
import org.scalatest.{FlatSpec, Matchers}
import org.scalactic.TypeCheckedTripleEquals
import org.scalamock.scalatest.MockFactory

class StringRxTest extends FlatSpec with Matchers with TypeCheckedTripleEquals with MockFactory {

  val device = new FileSystemAddress { def toFilename = "unused" }
  implicit val mockController = stub[FileSystemController]
  val register = "foo"

  "StringRx" should "consider each byte in the response to be an ANSI character" in {
    val rx = StringRx(register)
    (mockController.receive _).when(*, *, *).returns(Right(Seq('f'.toByte, 'o'.toByte, 'o'.toByte)))

    rx.read(device) should === (Right("foo"))
  }

  it should "read a maximum number of bytes as specified in the constructor" in {
    val rx = StringRx(register, 2)
    (mockController.receive _).when(*, *, *).returns(Right(Seq('f'.toByte, 'o'.toByte, 'o'.toByte)))

    rx.read(device) should === (Right("fo"))
    (mockController.receive _).verify(where { (_, _, numBytesToRead) => (numBytesToRead: Int) == 2 })
  }

  it should "remove any trailing newlines" in {
    val rx = StringRx(register)
    (mockController.receive _).when(*, *, *).returns(Right(Seq('f'.toByte, 'o'.toByte, 'o'.toByte, '\n'.toByte)))

    rx.read(device) should === (Right("foo"))
  }

}

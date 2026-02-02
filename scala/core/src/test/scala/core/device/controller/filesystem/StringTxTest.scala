package core.device.controller.filesystem

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalactic.TypeCheckedTripleEquals
import org.scalamock.scalatest.MockFactory

class StringTxTest extends AnyFlatSpec with Matchers with TypeCheckedTripleEquals with MockFactory {

  val device = new FileSystemAddress { def toFilename = "unused" }
  implicit val mockController: FileSystemController = stub[FileSystemController]
  val register = FileSystemRegister("foo")

  "StringTx" should "convert the input string into a sequence of bytes, where each byte is an ANSI character" in {
    val tx = StringTx(register)
    (mockController.transmit _).when(*, *, *).returns(Right(()))

    tx.write(device, "bar") should === (Right(()))
    (mockController.transmit _).verify(where { (_, _, bytes) =>
      bytes === Seq('b'.toByte, 'a'.toByte, 'r'.toByte)
    })
  }

}

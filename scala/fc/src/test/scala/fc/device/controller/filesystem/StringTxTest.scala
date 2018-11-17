package fc.device.controller.filesystem

import eu.timepit.refined.auto.{autoRefineV, autoUnwrap}
import org.scalatest.{FlatSpec, Matchers}
import org.scalactic.TypeCheckedTripleEquals
import org.scalamock.scalatest.MockFactory

class StringTxTest extends FlatSpec with Matchers with TypeCheckedTripleEquals with MockFactory {

  val device = new FileSystemAddress { def toFilename = "unused" }
  implicit val mockController = stub[FileSystemController]
  val register = "foo"

  "StringTx" should "convert the input string into a sequence of bytes, where each byte is an ANSI character" in {
    val tx = StringTx(register)
    (mockController.transmit _).when(*, *, *).returns(Right(()))

    tx.write(device, "bar") should === (Right(()))
    (mockController.transmit _).verify(where { (_, _, bytes) =>
      bytes === Seq('b'.toByte, 'a'.toByte, 'r'.toByte)
    })
  }

}

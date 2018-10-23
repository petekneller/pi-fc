package fc.device.rc

import eu.timepit.refined.auto.autoRefineV
import spire.syntax.literals._
import org.scalatest.{FlatSpec, Matchers}
import org.scalactic.TypeCheckedTripleEquals
import org.scalamock.scalatest.MockFactory
import ioctl.IOCtl.{O_RDONLY, O_WRONLY}
import ioctl.IOCtlImpl.size_t
import fc.device.controller.{FileController, FileApi}

class RcAddressTest extends FlatSpec with Matchers with TypeCheckedTripleEquals with MockFactory {

  val mockFileApi = stub[FileApi]
  implicit val controller = new FileController(mockFileApi)

  "receive" should "open the underlying file correctly" in {
    (mockFileApi.read _).when(*, *, *).returns(new size_t)

    controller.receive(RcAddress("/foo/bar"), "baz", 1) should === (Right(Seq.empty))
    (mockFileApi.open _).verify("/foo/bar/baz", O_RDONLY)
  }

  "transmit" should "open the underlying file correctly" in {
    (mockFileApi.write _).when(*, *, *).returns(new size_t(1L))

    controller.transmit(RcAddress("/foo/bar"), "baz", Seq(b"1")) should === (Right(()))
    (mockFileApi.open _).verify("/foo/bar/baz", O_WRONLY)
  }

}

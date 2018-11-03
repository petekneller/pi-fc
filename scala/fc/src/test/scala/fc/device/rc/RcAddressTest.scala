package fc.device.rc

import eu.timepit.refined.auto.autoRefineV
import spire.syntax.literals._
import org.scalatest.{FlatSpec, Matchers}
import org.scalactic.TypeCheckedTripleEquals
import org.scalamock.scalatest.MockFactory
import ioctl.IOCtl.{O_RDONLY, O_WRONLY}
import ioctl.IOCtlImpl.size_t
import fc.device.controller.filesystem.{FileSystemControllerImpl, FileApi}

class RcAddressTest extends FlatSpec with Matchers with TypeCheckedTripleEquals with MockFactory {

  val mockFileApi = stub[FileApi]
  implicit val controller = new FileSystemControllerImpl(mockFileApi)

  "receive" should "open the correct file" in {
    controller.receive(RcAddress("/foo/bar"), "baz", 1)
    (mockFileApi.open _).verify("/foo/bar/baz", *)
  }

  "transmit" should "open the correct file" in {
    controller.transmit(RcAddress("/foo/bar"), "baz", Seq(b"1"))
    (mockFileApi.open _).verify("/foo/bar/baz", *)
  }

}

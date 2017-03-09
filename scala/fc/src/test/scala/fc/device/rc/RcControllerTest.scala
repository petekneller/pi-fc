package fc.device.rc

import org.scalatest.{FlatSpec, Matchers}
import org.scalactic.TypeCheckedTripleEquals
import org.scalamock.scalatest.MockFactory
import ioctl.IOCtl.O_RDWR
import ioctl.IOCtlImpl.size_t
import fc.device.file._

class RcControllerTest extends FlatSpec with Matchers with TypeCheckedTripleEquals with MockFactory {

  val mockFileApi = stub[FileApi]
  implicit val controller = new RcController(mockFileApi)
  val device = RcAddress
  val register = "foo"
  val fd = 2

  "receive" should "open the underlying file correctly" in {
    (mockFileApi.read _).when(*, *, *).returns(new size_t)

    controller.receive(device, "bar", 1) should === (Right(Seq.empty))
    (mockFileApi.open _).verify("/sys/kernel/rcio/rcin/bar", O_RDWR)
  }

  "transmit" should "open the underlying file correctly" in {
    (mockFileApi.write _).when(*, *, *).returns(new size_t(1L))

    controller.transmit(device, "foo", Seq(1.toByte)) should === (Right(()))
    (mockFileApi.open _).verify("/sys/kernel/rcio/rcin/foo", O_RDWR)
  }

}

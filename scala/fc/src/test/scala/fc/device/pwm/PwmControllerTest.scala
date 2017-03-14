package fc.device.pwm

import eu.timepit.refined.auto.autoRefineV
import org.scalatest.{FlatSpec, Matchers}
import org.scalactic.TypeCheckedTripleEquals
import org.scalamock.scalatest.MockFactory
import ioctl.IOCtl.{O_RDONLY, O_WRONLY}
import ioctl.IOCtlImpl.size_t
import fc.device.file._

class PwmControllerTest extends FlatSpec with Matchers with TypeCheckedTripleEquals with MockFactory {

  val mockFileApi = stub[FileApi]
  implicit val controller = new FileController(mockFileApi)
  val device = PwmAddress(1, 2)
  val register = "foo"
  val fd = 2

  "receive" should "open the underlying file correctly" in {
    (mockFileApi.read _).when(*, *, *).returns(new size_t)

    controller.receive(device, "bar", 1) should === (Right(Seq.empty))
    (mockFileApi.open _).verify("/sys/class/pwm/pwmchip1/pwm2/bar", O_RDONLY)
  }

  "transmit" should "open the underlying file correctly" in {
    (mockFileApi.write _).when(*, *, *).returns(new size_t(1L))

    controller.transmit(device, "foo", Seq(1.toByte)) should === (Right(()))
    (mockFileApi.open _).verify("/sys/class/pwm/pwmchip1/pwm2/foo", O_WRONLY)
  }

}

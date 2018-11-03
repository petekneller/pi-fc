package fc.device.esc

import eu.timepit.refined.auto.autoRefineV
import spire.syntax.literals._
import org.scalatest.{FlatSpec, Matchers}
import org.scalactic.TypeCheckedTripleEquals
import org.scalamock.scalatest.MockFactory
import ioctl.IOCtl.{O_RDONLY, O_WRONLY}
import ioctl.IOCtlImpl.size_t
import fc.device.controller.filesystem.{FileSystemControllerImpl, FileApi}

class PwmAddressTest extends FlatSpec with Matchers with TypeCheckedTripleEquals with MockFactory {

  val mockFileApi = stub[FileApi]
  implicit val controller = new FileSystemControllerImpl(mockFileApi)
  val device = PwmAddress(1, 2)

  "receive" should "open the correct file" in {
    controller.receive(device, "bar", 1)
    (mockFileApi.open _).verify("/sys/class/pwm/pwmchip1/pwm2/bar", *)
  }

  "transmit" should "open the correct file" in {
    controller.transmit(device, "foo", Seq(b"1"))
    (mockFileApi.open _).verify("/sys/class/pwm/pwmchip1/pwm2/foo", *)
  }

}

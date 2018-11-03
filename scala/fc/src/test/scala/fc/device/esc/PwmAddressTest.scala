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

  "PwmAddress" should "resolve to the correct device file" in {
    PwmAddress(1, 2).toFilename should ===("/sys/class/pwm/pwmchip1/pwm2")
    PwmAddress(3, 4).toFilename should ===("/sys/class/pwm/pwmchip3/pwm4")
  }

}

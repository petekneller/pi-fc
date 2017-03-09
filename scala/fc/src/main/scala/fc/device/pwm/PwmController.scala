package fc.device.pwm

import fc.device._
import fc.device.file._

trait Pwm

case class PwmAddress(chip: Int, channel: Int) extends Address {
  type Bus = Pwm

  def toFilename = s"/sys/class/pwm/pwmchip${chip}/pwm${channel}"
}

class PwmController(api: FileApi) extends FileController(api) {
  type Bus = Pwm
}

object PwmController {
  def apply() = new PwmController(FileApi())
}

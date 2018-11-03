package fc.device.esc

import eu.timepit.refined.W
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.GreaterEqual
import eu.timepit.refined.auto.autoUnwrap
import fc.device.controller.filesystem.FileSystemAddress

case class PwmAddress(chip: Int Refined GreaterEqual[W.`0`.T], channel: Int Refined GreaterEqual[W.`0`.T]) extends FileSystemAddress {
  def toFilename = s"/sys/class/pwm/pwmchip${chip}/pwm${channel}"
}

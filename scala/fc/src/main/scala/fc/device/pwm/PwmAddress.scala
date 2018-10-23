package fc.device.pwm

import fc.device.controller.FileAddress

case class PwmAddress(chip: Int, channel: Int) extends FileAddress {
  def toFilename = s"/sys/class/pwm/pwmchip${chip}/pwm${channel}"
}

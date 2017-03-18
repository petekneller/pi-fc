package fc.device.pwm

import fc.device.file._

case class PwmAddress(chip: Int, channel: Int) extends FileAddress {
  def toFilename = s"/sys/class/pwm/pwmchip${chip}/pwm${channel}"
}

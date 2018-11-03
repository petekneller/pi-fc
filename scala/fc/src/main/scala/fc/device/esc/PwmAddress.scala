package fc.device.esc

import fc.device.controller.filesystem.FileSystemAddress

case class PwmAddress(chip: Int, channel: Int) extends FileSystemAddress {
  def toFilename = s"/sys/class/pwm/pwmchip${chip}/pwm${channel}"
}

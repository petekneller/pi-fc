package core.device.rc

import core.device.controller.filesystem.FileSystemAddress

case class RcAddress(channel: Int) extends FileSystemAddress {
  def toFilename = s"/sys/kernel/rcio/rcin/ch${channel.toString}"
}

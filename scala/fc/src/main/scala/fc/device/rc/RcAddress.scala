package fc.device.rc

import fc.device.controller.filesystem.FileSystemAddress

case class RcAddress(channel: Int) extends FileSystemAddress {
  def toFilename = s"/sys/kernel/rcio/rcin/ch${channel.toString}"
}

package fc.device.rc

import fc.device.controller.filesystem.FileSystemAddress

case class RcAddress() extends FileSystemAddress {
  def toFilename = "/sys/kernel/rcio/rcin"
}

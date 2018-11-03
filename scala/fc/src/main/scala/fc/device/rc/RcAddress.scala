package fc.device.rc

import fc.device.controller.filesystem.FileSystemAddress

case class RcAddress(deviceFile: String) extends FileSystemAddress {
  def toFilename = deviceFile
}

package fc.device.rc

import fc.device.file.FileAddress

case class RcAddress(deviceFile: String) extends FileAddress {
  def toFilename = deviceFile
}

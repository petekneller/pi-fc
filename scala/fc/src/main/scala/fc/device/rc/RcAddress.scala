package fc.device.rc

import fc.device.controller.FileAddress

case class RcAddress(deviceFile: String) extends FileAddress {
  def toFilename = deviceFile
}

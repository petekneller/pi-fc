package fc.device.rc

import fc.device.file._

object RcAddress extends FileAddress {
  def toFilename = "/sys/kernel/rcio/rcin"
}

package fc.device.rc

import fc.device._
import fc.device.file._

trait Rc

object RcAddress extends Address {
  type Bus = Rc

  def toFilename = "/sys/kernel/rcio/rcin"
}

class RcController(api: FileApi) extends FileController(api) {
  type Bus = Rc
}

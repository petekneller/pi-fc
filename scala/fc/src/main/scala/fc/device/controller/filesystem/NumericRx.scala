package fc.device.controller.filesystem

import cats.syntax.either._
import fc.device.api._

case class NotNumericException(actualValue: String) extends DeviceException

case class NumericRx[A](register: String, f: Long => A = identity[Long] _) extends Rx {
    type T = A
    type Ctrl = FileSystemController

    def read(device: FileSystemAddress)(implicit controller: FileSystemController): DeviceResult[T] = for {
      string <- rx.read(device)
      long <- Either.catchNonFatal { string.toLong }.leftMap(_ => NotNumericException(string))
    } yield f(long)

    private val rx = StringRx(register)
}

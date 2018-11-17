package fc.device.controller.filesystem

import cats.syntax.either._
import eu.timepit.refined.auto.{autoRefineV, autoUnwrap}
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import fc.device.api._

case class NumericTx[A](register: String, f: A => Long = identity[Long] _) extends Tx {
  type T = A
  type Ctrl = FileSystemController

  def write(device: FileSystemAddress, value: T)(implicit controller: FileSystemController): DeviceResult[Unit] = tx.write(device, f(value).toString)

  private val tx = StringTx(register)
}

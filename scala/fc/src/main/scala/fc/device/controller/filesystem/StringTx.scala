package fc.device.controller.filesystem

import cats.syntax.either._
import eu.timepit.refined.auto.{autoRefineV, autoUnwrap}
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import fc.device.api._

case class StringTx(register: String) extends Tx {
    type T = String
    type Ctrl = FileSystemController

    def write(device: FileSystemAddress, value: String)(implicit controller: FileSystemController): DeviceResult[Unit] = for {
      _ <- controller.transmit(device, register, value.toCharArray.map(_.toByte))
    } yield ()
}

package fc.device.controller.filesystem

import cats.syntax.either._
import eu.timepit.refined.auto.{autoRefineV, autoUnwrap}
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import fc.device.api._

object StringRx {

  def apply(register: String, maxBytesToRead: Int Refined Positive = 32) = new Rx {
    type T = String
    type Ctrl = FileSystemController

    def read(device: FileSystemAddress)(implicit controller: FileSystemController): DeviceResult[String] = for {
      data <- controller.receive(device, register, maxBytesToRead)
    } yield data.take(maxBytesToRead).map(_.toChar).filter(_ != '\n').mkString
  }

}

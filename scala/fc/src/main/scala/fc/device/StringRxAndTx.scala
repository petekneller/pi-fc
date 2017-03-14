package fc.device

import cats.syntax.either._
import eu.timepit.refined.refineMV
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive

case class RxString(register: String, maxBytesToRead: Int Refined Positive = refineMV[Positive](32)) extends Rx {
  type T = String
  type Register = String

  def read(device: Address)(implicit controller: Controller { type Bus = device.Bus; type Register = String }): DeviceResult[String] = for {
    data <- controller.receive(device, register, maxBytesToRead)
  } yield data.map(_.toChar).filter(_ != '\n').mkString
}

case class TxString(register: String) extends Tx {
  type T = String
  type Register = String

  def write(device: Address, value: String)(implicit controller: Controller { type Bus = device.Bus; type Register = String }): DeviceResult[Unit] = for {
    _ <- controller.transmit(device, register, value.toCharArray.map(_.toByte))
  } yield ()
}

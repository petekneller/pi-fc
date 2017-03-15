package fc.device

import cats.syntax.either._
import eu.timepit.refined.auto.autoRefineV
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive

object RxString {

  def string(register: String, maxBytesToRead: Int Refined Positive = 32) = new Rx {
    type T = String
    type Register = String

    def read(device: Address)(implicit controller: Controller { type Bus = device.Bus; type Register = String }): DeviceResult[String] = for {
      data <- controller.receive(device, register, maxBytesToRead)
    } yield data.map(_.toChar).filter(_ != '\n').mkString
  }

}

object TxString {

  def string(register: String) = new Tx {
    type T = String
    type Register = String

    def write(device: Address, value: String)(implicit controller: Controller { type Bus = device.Bus; type Register = String }): DeviceResult[Unit] = for {
      _ <- controller.transmit(device, register, value.toCharArray.map(_.toByte))
    } yield ()
  }

}

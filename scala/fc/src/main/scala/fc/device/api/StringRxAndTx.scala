package fc.device.api

import cats.syntax.either._
import eu.timepit.refined.auto.{autoRefineV, autoUnwrap}
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive

case class NotNumericException(actualValue: String) extends DeviceException

object RxString {

  def string(register: String, maxBytesToRead: Int Refined Positive = 32) = new Rx {
    type T = String
    type Register = String

    def read(device: Address)(implicit controller: Controller { type Bus = device.Bus; type Register = String }): DeviceResult[String] = for {
      data <- controller.receive(device, register, maxBytesToRead)
    } yield data.take(maxBytesToRead).map(_.toChar).filter(_ != '\n').mkString
  }

  def numeric[A](register: String, f: Long => A = identity[Long] _) = new Rx {
    type Register = String
    type T = A

    def read(device: Address)(implicit controller: Controller { type Bus = device.Bus; type Register = String }): DeviceResult[T] = for {
      string <- rx.read(device)
      long <- Either.catchNonFatal { string.toLong }.leftMap(_ => NotNumericException(string))
    } yield f(long)

    private val rx = RxString.string(register)
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

  def numeric[A](register: String, f: A => Long = identity[Long] _) = new Tx {
    type Register = String
    type T = A

    def write(device: Address, value: T)(implicit controller: Controller { type Bus = device.Bus; type Register = String }): DeviceResult[Unit] = tx.write(device, f(value).toString)

    private val tx = TxString.string(register)
  }

}

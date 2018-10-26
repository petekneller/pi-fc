package fc.device.api.configuration

import cats.syntax.either._
import eu.timepit.refined.refineMV
import eu.timepit.refined.numeric.Positive
import fc.device.api._

case class BooleanConfiguration(register: String) extends Rx with Tx {
  type Register = String
  type T = Boolean

  def read(device: Address)(implicit controller: Controller { type Bus = device.Bus; type Register = String }): DeviceResult[Boolean] = for {
    data <- rx.read(device)
    result <- data match {
      case "1" => Right(true)
      case "0" => Right(false)
      case _ => Left(NotABooleanException(data))
    }
  } yield result

  def write(device: Address, value: Boolean)(implicit controller: Controller { type Bus = device.Bus; type Register = String }): DeviceResult[Unit] = tx.write(device, if (value) "1" else "0")

  private val rx = RxString.string(register, refineMV[Positive](1))
  private val tx = TxString.string(register)
}

case class NotABooleanException(actualValue: String) extends DeviceException


object NumericConfiguration {
  def apply[A](register: String, map: Long => A = identity[Long] _, contramap: A => Long = identity[Long] _) = JointConfiguration(RxString.numeric(register, map))(TxString.numeric(register, contramap))
}
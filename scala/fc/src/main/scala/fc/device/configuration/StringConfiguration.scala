package fc.device.configuration

import cats.syntax.either._
import fc.device._

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

  private val rx = RxString(register, 1)
  private val tx = TxString(register)
}

case class NotABooleanException(actualValue: String) extends DeviceException

case class NumericConfiguration(register: String) extends Rx with Tx {
  type Register = String
  type T = Long

  def read(device: Address)(implicit controller: Controller { type Bus = device.Bus; type Register = String }): DeviceResult[Long] = for {
    string <- rx.read(device)
    long <- Either.catchNonFatal { string.toLong }.leftMap(_ => NotNumericException(string))
  } yield long

  def write(device: Address, value: Long)(implicit controller: Controller { type Bus = device.Bus; type Register = String }): DeviceResult[Unit] = tx.write(device, value.toString)

  private val rx = RxString(register)
  private val tx = TxString(register)
}

case class NotNumericException(actualValue: String) extends DeviceException

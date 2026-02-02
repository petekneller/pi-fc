package core.device.controller.filesystem

import eu.timepit.refined.refineMV
import eu.timepit.refined.numeric.Positive
import core.device.api._

case class BooleanConfiguration(register: FileSystemRegister) extends Configuration {
  type T = Boolean
  type Ctrl = FileSystemController

  def read(device: FileSystemAddress)(implicit controller: FileSystemController): DeviceResult[Boolean] = for {
    data <- rx.read(device)
    result <- data match {
      case "1" => Right(true)
      case "0" => Right(false)
      case _ => Left(NotABooleanException(data))
    }
  } yield result

  def write(device: FileSystemAddress, value: Boolean)(implicit controller: FileSystemController): DeviceResult[Unit] = tx.write(device, if (value) "1" else "0")

  private val rx = StringRx(register, refineMV[Positive](1))
  private val tx = StringTx(register)
}

case class NotABooleanException(actualValue: String) extends DeviceException

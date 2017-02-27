package fc.device.sensor

import cats.syntax.either._
import fc.device._

trait Mpu9250 {
  val address: DeviceAddress
  implicit val controller: Controller { type Bus = address.Bus }

  def checkCommunication(): Either[DeviceError, Unit] = readRegister(address, registers.WHOAMI).map(_ == 0x71.toByte)

}

object Mpu9250 {

  def apply(a: DeviceAddress)(implicit c: Controller { type Bus = address.Bus }) = new Mpu9250 {
    val address = a
    implicit val controller = c
  }

  object registers {
    val WHOAMI = DeviceRegister(0x75)
  }

}

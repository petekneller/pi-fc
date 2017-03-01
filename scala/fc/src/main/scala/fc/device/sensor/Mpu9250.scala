package fc.device.sensor

import cats.syntax.either._
import cats.syntax.eq._
import cats.instances.byte._
import ioctl.syntax._
import fc.{device => d}
import d._

trait Mpu9250 {
  val address: Address
  implicit val controller: Controller { type Bus = address.Bus }

  import Mpu9250._

  def checkCommunication(): Either[DeviceException, Unit] = readRegister(registers.WHOAMI).map(_ === constants.DEVICE_ID)

  def readGyro(): Either[DeviceException, (Double, Double, Double)] = for {
    x <- readRegisterPair(registers.GYRO_XOUT_L, registers.GYRO_XOUT_H)
    y <- readRegisterPair(registers.GYRO_YOUT_L, registers.GYRO_YOUT_H)
    z <- readRegisterPair(registers.GYRO_ZOUT_L, registers.GYRO_ZOUT_H)
  } yield (x.toDouble, y.toDouble, z.toDouble)

  private def readRegister(register: Register) = Rx.byte(register).read(address)

  private def readRegisterPair(loRegister: Register, hiRegister: Register): Either[DeviceException, Short] = for {
    hiByte <- readRegister(hiRegister)
    loByte <- readRegister(loRegister)
  } yield ((hiByte.unsigned << 8) | loByte).toShort

}

object Mpu9250 {

  def apply[A](a: Address { type Bus = A })(implicit c: Controller { type Bus = A }) = new Mpu9250 {
    val address = a
    implicit val controller = c
  }

  object registers {
    val WHOAMI = Register(0x75)
    val GYRO_XOUT_H = Register(0x43)
    val GYRO_XOUT_L = Register(0x44)
    val GYRO_YOUT_H = Register(0x45)
    val GYRO_YOUT_L = Register(0x46)
    val GYRO_ZOUT_H = Register(0x47)
    val GYRO_ZOUT_L = Register(0x48)
  }

  object constants {
    val DEVICE_ID: Byte = 0x71
  }

}

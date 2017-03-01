package fc.device.sensor

import cats.syntax.either._
import cats.syntax.eq._
import cats.instances.byte._
import ioctl.syntax._
import fc.device._
import fc.device.configuration._

trait Mpu9250 extends Device {
  import Mpu9250.{registers, constants, config}

  def checkCommunication(): Either[DeviceException, Boolean] = Rx.byte(registers.WHOAMI).read(address).map(_ === constants.DEVICE_ID)

  def enable(value: Boolean = true): Either[DeviceException, Unit] = config.SLEEP.write(address, !value)
  def disable(): Either[DeviceException, Unit] = enable(false)

  def reset(): Either[DeviceException, Unit] = config.H_RESET.write(address, true)

  def readGyro(): Either[DeviceException, (Short, Short, Short)] = for {
    x <- Rx.short(registers.GYRO_XOUT_L, registers.GYRO_XOUT_H).read(address)
    y <- Rx.short(registers.GYRO_YOUT_L, registers.GYRO_YOUT_H).read(address)
    z <- Rx.short(registers.GYRO_ZOUT_L, registers.GYRO_ZOUT_H).read(address)
  } yield (x, y, z)

}

object Mpu9250 {

  def apply(a: Address)(implicit c: Controller { type Bus = a.Bus }) = new Mpu9250 {
    val address: Address { type Bus = a.Bus } = a
    implicit val controller: Controller { type Bus = address.Bus } = c
  }

  object config {
    val SLEEP =   BitFlagConfiguration(registers.PWR_MGMT_1, 6)
    val H_RESET = BitFlagConfiguration(registers.PWR_MGMT_1, 7) // hardware reset - all config registers go to their default values
  }

  object constants {
    val DEVICE_ID: Byte = 0x71
  }

  object registers {
    val GYRO_XOUT_H = Register(0x43)
    val GYRO_XOUT_L = Register(0x44)
    val GYRO_YOUT_H = Register(0x45)
    val GYRO_YOUT_L = Register(0x46)
    val GYRO_ZOUT_H = Register(0x47)
    val GYRO_ZOUT_L = Register(0x48)
    val PWR_MGMT_1 =  Register(0x6B)
    val WHOAMI =      Register(0x75)
  }

}
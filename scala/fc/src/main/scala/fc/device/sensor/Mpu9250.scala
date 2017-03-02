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

  // NB. upon power-on the MPU is AWAKE and will generate measurements immediately
  def enable(value: Boolean = true): Either[DeviceException, Unit] = config.SLEEP.write(address, !value)
  def disable(): Either[DeviceException, Unit] = enable(false)

  def reset(): Either[DeviceException, Unit] = config.H_RESET.write(address, true)

  def readGyro(): Either[DeviceException, (Short, Short, Short)] =
    Measurement(
      Rx.short(registers.GYRO_XOUT_L, registers.GYRO_XOUT_H),
      Rx.short(registers.GYRO_YOUT_L, registers.GYRO_YOUT_H),
      Rx.short(registers.GYRO_ZOUT_L, registers.GYRO_ZOUT_H)
    ).read(address)

}

object Mpu9250 {

  def apply(a: Address)(implicit c: Controller { type Bus = a.Bus }) = new Mpu9250 {
    val address: Address { type Bus = a.Bus } = a
    implicit val controller: Controller { type Bus = address.Bus } = c
  }

  object config {
    val GYRO_FS_SEL = MultiBitFlag(registers.GYRO_CONFIG, 4, 2, GyroFullScale)
    val SLEEP =       SingleBitFlag(registers.PWR_MGMT_1, 6)
    val H_RESET =     SingleBitFlag(registers.PWR_MGMT_1, 7) // hardware reset - all config registers go to their default values
  }

  object constants {
    val DEVICE_ID: Byte = 0x71
  }

  object registers {
    val GYRO_CONFIG = Register(0x1B)
    val GYRO_XOUT_H = Register(0x43)
    val GYRO_XOUT_L = Register(0x44)
    val GYRO_YOUT_H = Register(0x45)
    val GYRO_YOUT_L = Register(0x46)
    val GYRO_ZOUT_H = Register(0x47)
    val GYRO_ZOUT_L = Register(0x48)
    val PWR_MGMT_1 =  Register(0x6B)
    val WHOAMI =      Register(0x75)
  }

  object GyroFullScale extends FlagEnumeration {
    type T = Val

    sealed trait Val extends Flag
    object dps250 extends Val { val value = 0x0.toByte }
    object dps500 extends Val { val value = 0x1.toByte }
    object dps1000 extends Val { val value = 0x2.toByte }
    object dps2000 extends Val { val value = 0x3.toByte }

    def values = Set(dps250, dps500, dps1000, dps2000)
  }

}

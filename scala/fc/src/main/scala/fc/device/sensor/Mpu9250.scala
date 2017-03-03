package fc.device.sensor

import cats.syntax.either._
import cats.syntax.eq._
import cats.instances.byte._
import ioctl.syntax._
import fc.device._
import fc.device.configuration._

trait Mpu9250 extends Device {
  import Mpu9250.{registers, constants, config, enums}

  def checkCommunication(): Either[DeviceException, Boolean] = Rx.byte(registers.WHOAMI).read(address).map(_ === constants.DEVICE_ID)

  // NB. upon power-on the MPU is AWAKE and will generate measurements immediately
  def enable(value: Boolean = true): Either[DeviceException, Unit] = config.SLEEP.write(address, !value)
  def disable(): Either[DeviceException, Unit] = enable(false)

  def reset(): Either[DeviceException, Unit] = config.H_RESET.write(address, true)

  def readGyro(fullScale: enums.GyroFullScale.Val): Either[DeviceException, (Double, Double, Double)] =
    Measurement3Axis(
      registers.GYRO_XOUT_L, registers.GYRO_XOUT_H,
      registers.GYRO_YOUT_L, registers.GYRO_YOUT_H,
      registers.GYRO_ZOUT_L, registers.GYRO_ZOUT_H,
      fullScale
    ).read(address)

  def readAccel(fullScale: enums.AccelFullScale.Val): Either[DeviceException, (Double, Double, Double)] =
    Measurement3Axis(
      registers.ACCEL_XOUT_L, registers.ACCEL_XOUT_H,
      registers.ACCEL_YOUT_L, registers.ACCEL_YOUT_H,
      registers.ACCEL_ZOUT_L, registers.ACCEL_ZOUT_H,
      fullScale
    ).read(address)

}

object Mpu9250 {

  def apply(a: Address)(implicit c: Controller { type Bus = a.Bus }) = new Mpu9250 {
    val address: Address { type Bus = a.Bus } = a
    implicit val controller: Controller { type Bus = address.Bus } = c
  }

  object config {
    val GYRO_FS_SEL =    MultiBitFlag(registers.GYRO_CONFIG, 4, 2, enums.GyroFullScale)
    val ACCEL_FS_SEL =   MultiBitFlag(registers.ACCEL_CONFIG_1, 4, 2, enums.AccelFullScale)
    val SLEEP =          SingleBitFlag(registers.PWR_MGMT_1, 6)
    val H_RESET =        SingleBitFlag(registers.PWR_MGMT_1, 7) // hardware reset - all config registers go to their default values
  }

  object constants {
    val DEVICE_ID: Byte = 0x71
  }

  object registers {

    val GYRO_CONFIG =      Register(27)
    val ACCEL_CONFIG_1 =   Register(28)
    val ACCEL_XOUT_H =     Register(59)
    val ACCEL_XOUT_L =     Register(60)
    val ACCEL_YOUT_H =     Register(61)
    val ACCEL_YOUT_L =     Register(62)
    val ACCEL_ZOUT_H =     Register(63)
    val ACCEL_ZOUT_L =     Register(64)
    val GYRO_XOUT_H =      Register(67)
    val GYRO_XOUT_L =      Register(68)
    val GYRO_YOUT_H =      Register(69)
    val GYRO_YOUT_L =      Register(70)
    val GYRO_ZOUT_H =      Register(71)
    val GYRO_ZOUT_L =      Register(72)
    val PWR_MGMT_1 =       Register(107)
    val WHOAMI =           Register(117)

  } // registers

  object enums {

    object GyroFullScale extends FlagEnumeration {
      type T = Val

      sealed trait Val extends Flag with FullScale
      object dps250 extends Val { val value = 0x0.toByte; val factor = 250.0 }
      object dps500 extends Val { val value = 0x1.toByte; val factor = 500.0 }
      object dps1000 extends Val { val value = 0x2.toByte; val factor = 1000.0 }
      object dps2000 extends Val { val value = 0x3.toByte; val factor = 2000.0 }

      def values = Set(dps250, dps500, dps1000, dps2000)
    }

    object AccelFullScale extends FlagEnumeration {
      type T = Val

      sealed trait Val extends Flag with FullScale
      object g2 extends Val { val value = 0x0.toByte; val factor = 2.0 }
      object g4 extends Val { val value = 0x1.toByte; val factor = 4.0 }
      object g8 extends Val { val value = 0x2.toByte; val factor = 8.0 }
      object g16 extends Val { val value = 0x3.toByte; val factor = 16.0 }

      def values = Set(g2, g4, g8, g16)
    }

  } // enums

}

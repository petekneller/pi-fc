package fc.device.input

import cats.syntax.either._
import cats.syntax.eq._
import cats.instances.byte._
import eu.timepit.refined.auto.autoRefineV
import spire.syntax.literals._
import ioctl.syntax._
import fc.device._
import fc.device.configuration._
import fc.device.sensor._

trait Mpu9250 extends Device {
  type Register = Byte
  import Mpu9250.{registers, constants, configs, enums}

  def checkCommunication(): DeviceResult[Boolean] = ByteRx.byte(registers.WHOAMI).read(address).map(_ === constants.DEVICE_ID)

  // NB. upon power-on the MPU is AWAKE and will generate measurements immediately
  def enable(value: Boolean = true): DeviceResult[Unit] = configs.SLEEP.write(address, !value)
  def disable(): DeviceResult[Unit] = enable(false)

  def reset(): DeviceResult[Unit] = configs.H_RESET.write(address, true)

  def readGyro(fullScale: enums.GyroFullScale.Val): DeviceResult[(Double, Double, Double)] =
    Measurement3Axis(
      registers.GYRO_XOUT_L, registers.GYRO_XOUT_H,
      registers.GYRO_YOUT_L, registers.GYRO_YOUT_H,
      registers.GYRO_ZOUT_L, registers.GYRO_ZOUT_H,
      fullScale
    ).read(address)

  def readAccel(fullScale: enums.AccelFullScale.Val): DeviceResult[(Double, Double, Double)] =
    Measurement3Axis(
      registers.ACCEL_XOUT_L, registers.ACCEL_XOUT_H,
      registers.ACCEL_YOUT_L, registers.ACCEL_YOUT_H,
      registers.ACCEL_ZOUT_L, registers.ACCEL_ZOUT_H,
      fullScale
    ).read(address)

}

object Mpu9250 {

  def apply(a: Address)(implicit c: Controller { type Bus = a.Bus; type Register = Byte }) = new Mpu9250 {
    val address: Address { type Bus = a.Bus } = a
    implicit val controller: Controller { type Bus = address.Bus; type Register = Byte } = c
  }

  object configs {
    val GYRO_FS_SEL =    MultiBitFlag(registers.GYRO_CONFIG, 4, 2, enums.GyroFullScale)
    val ACCEL_FS_SEL =   MultiBitFlag(registers.ACCEL_CONFIG_1, 4, 2, enums.AccelFullScale)
    val SLEEP =          SingleBitFlag(registers.PWR_MGMT_1, 6)
    val H_RESET =        SingleBitFlag(registers.PWR_MGMT_1, 7) // hardware reset - all config registers go to their default values
  }

  object constants {
    val DEVICE_ID: Byte = 0x71
  }

  object registers {

    val GYRO_CONFIG =      b"27"
    val ACCEL_CONFIG_1 =   b"28"
    val ACCEL_XOUT_H =     b"59"
    val ACCEL_XOUT_L =     b"60"
    val ACCEL_YOUT_H =     b"61"
    val ACCEL_YOUT_L =     b"62"
    val ACCEL_ZOUT_H =     b"63"
    val ACCEL_ZOUT_L =     b"64"
    val GYRO_XOUT_H =      b"67"
    val GYRO_XOUT_L =      b"68"
    val GYRO_YOUT_H =      b"69"
    val GYRO_YOUT_L =      b"70"
    val GYRO_ZOUT_H =      b"71"
    val GYRO_ZOUT_L =      b"72"
    val PWR_MGMT_1 =       b"107"
    val WHOAMI =           b"117"

  } // registers

  object enums {

    object GyroFullScale extends FlagEnumeration {
      type T = Val

      sealed trait Val extends Flag with FullScale
      object dps250 extends Val { val value = b"0"; val factor = 250.0 }
      object dps500 extends Val { val value = b"1"; val factor = 500.0 }
      object dps1000 extends Val { val value = b"2"; val factor = 1000.0 }
      object dps2000 extends Val { val value = b"3"; val factor = 2000.0 }

      def values = Set(dps250, dps500, dps1000, dps2000)
    }

    object AccelFullScale extends FlagEnumeration {
      type T = Val

      sealed trait Val extends Flag with FullScale
      object g2 extends Val { val value = b"0"; val factor = 2.0 }
      object g4 extends Val { val value = b"1"; val factor = 4.0 }
      object g8 extends Val { val value = b"2"; val factor = 8.0 }
      object g16 extends Val { val value = b"3"; val factor = 16.0 }

      def values = Set(g2, g4, g8, g16)
    }

  } // enums

}

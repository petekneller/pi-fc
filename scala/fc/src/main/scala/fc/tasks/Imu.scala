package fc.tasks

import cats.effect.IO
import _root_.fs2.Stream
import core.device.api.DeviceResult
import core.device.sensor.Mpu9250

object Imu {

  def readGyroAccel(mpu: Mpu9250): Stream[IO, DeviceResult[((Double, Double, Double), (Double, Double, Double))]] = Stream.eval(IO.delay {
    for {
      gyro  <- mpu.readGyro(Mpu9250.enums.GyroFullScale.dps250)
      accel <- mpu.readAccel(Mpu9250.enums.AccelFullScale.g4)
    } yield (gyro, accel)
  }).repeat

}

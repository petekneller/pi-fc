package fc.tasks

import cats.effect.IO
import _root_.fs2.Stream
import core.device.api.DeviceResult
import core.device.sensor.Mpu9250

object Gyro {

  def readGyro(mpu: Mpu9250): Stream[IO, DeviceResult[(Double, Double, Double)]] = Stream.eval(IO.delay{ mpu.readGyro(Mpu9250.enums.GyroFullScale.dps250) }).repeat

  def formatGyro(x: Double, y: Double, z: Double): String = {
    val fmt = "%s: [%10f]"
    (fmt.format("X", x) :: fmt.format("Y", y) :: fmt.format("Z", z) :: Nil).mkString(" | ")
  }

}

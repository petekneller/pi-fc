package fc

import device.spi._
import device.sensor.Mpu9250

object Navio2 {

  val mpu9250 = Mpu9250(SpiAddress(busNumber = 0, chipSelect = 1))

}

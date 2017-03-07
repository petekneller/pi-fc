package fc

import device.spi._
import device.input.Mpu9250

object Navio2 {

  implicit val spiController = SpiController()

  val mpu9250 = Mpu9250(SpiAddress(busNumber = 0, chipSelect = 1))

}

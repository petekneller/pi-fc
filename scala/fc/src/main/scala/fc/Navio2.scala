package fc

import device.spi.{SpiController, SpiAddress}
import device.pwm.PwmController
import device.input.Mpu9250
import device.output.PwmChannel

object Navio2 {

  implicit val spiController = SpiController()
  implicit val pwmController = PwmController()

  val mpu9250 = Mpu9250(SpiAddress(busNumber = 0, chipSelect = 1))

  val pwm0 = PwmChannel(chipNumber = 0, channelNumber = 0)
  val pwm1 = PwmChannel(chipNumber = 0, channelNumber = 1)
  val pwm2 = PwmChannel(chipNumber = 0, channelNumber = 2)
  val pwm3 = PwmChannel(chipNumber = 0, channelNumber = 3)

}

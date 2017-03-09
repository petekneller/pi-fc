package fc

import device.spi.{SpiController, SpiAddress}
import device.pwm.PwmController
import device.rc.RcController
import device.input.{Mpu9250, RcReceiver}
import device.output.{PwmChannel, ESC}

object Navio2 {

  implicit val spiController = SpiController()
  implicit val pwmController = PwmController()
  implicit val rcController = RcController()

  val mpu9250 = Mpu9250(SpiAddress(busNumber = 0, chipSelect = 1))

  val receiver = RcReceiver()

  val esc0 = ESC(PwmChannel(chipNumber = 0, channelNumber = 0))
  val esc1 = ESC(PwmChannel(chipNumber = 0, channelNumber = 1))
  val esc2 = ESC(PwmChannel(chipNumber = 0, channelNumber = 2))
  val esc3 = ESC(PwmChannel(chipNumber = 0, channelNumber = 3))

}

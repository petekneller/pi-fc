package fc

import device.spi.{SpiController, SpiAddress}
import device.file.FileController
import device.input.{Mpu9250, RcReceiver}
import device.output.{PwmChannel, ESC}

object Navio2 {

  implicit val spiController = SpiController()
  implicit val fileController = FileController()

  val mpu9250 = Mpu9250(SpiAddress(busNumber = 0, chipSelect = 1))

  val receiver = RcReceiver()

  val esc1 = ESC(PwmChannel(chipNumber = 0, channelNumber = 1)) // pin 2
  val esc2 = ESC(PwmChannel(chipNumber = 0, channelNumber = 3)) // pin 4
  val esc3 = ESC(PwmChannel(chipNumber = 0, channelNumber = 5)) // pin 6
  val esc4 = ESC(PwmChannel(chipNumber = 0, channelNumber = 7)) // pin 8

  /* Quick and nasty flight experiments */

  def motorTest() = task.fs2.motorsTest(esc1, esc2, esc3, esc4)


  /* End quick and nasty */

}

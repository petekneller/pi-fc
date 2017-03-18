package fc

import device.spi.{SpiController, SpiAddress}
import device.file.FileController
import device.rc.RcAddress
import device.input.{Mpu9250, RcReceiver}
import device.output.{PwmChannel, ESC}
import task.{fs2 => tasks}

object Navio2 {

  implicit val spiController = SpiController()
  implicit val fileController = FileController()

  /* Devices */

  val mpu9250 = Mpu9250(SpiAddress(busNumber = 0, chipSelect = 1))

  val receiver = RcReceiver(RcAddress("/sys/kernel/rcio/rcin"))

  // It's convenient to have low-level access to an ESC for testing configuration
  val pwmChannel1 = PwmChannel(chipNumber = 0, channelNumber = 1)

  val esc1 = ESC("1", pwmChannel1) // pin 2
  val esc2 = ESC("2", PwmChannel(chipNumber = 0, channelNumber = 3)) // pin 4
  val esc3 = ESC("3", PwmChannel(chipNumber = 0, channelNumber = 5)) // pin 6
  val esc4 = ESC("4", PwmChannel(chipNumber = 0, channelNumber = 7)) // pin 8

  /* Tasks */

  def motorsTest = tasks.motorsTest(esc1, esc2, esc3, esc4) to tasks.printToConsole

}

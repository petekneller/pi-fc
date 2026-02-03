package core

import eu.timepit.refined.auto.autoRefineV
import device.controller.filesystem.FileSystemController
import device.controller.spi.{SpiController, SpiAddress}
import device.rc.RcChannel
import device.sensor.Mpu9250
import device.esc.{PwmChannel, ESC}
import core.device.controller.spi.SpiRegisterController

object Navio2 {

  implicit val spiController: SpiRegisterController = SpiController()
  implicit val fileController: FileSystemController = FileSystemController()

  /* Devices */

  val mpu9250 = Mpu9250(SpiAddress(busNumber = 0, chipSelect = 1))

  object rcChannels {
    val one =   RcChannel(0)
    val two =   RcChannel(1)
    val three = RcChannel(2)
    val four =  RcChannel(3)
    val six =   RcChannel(5)
  }

  // It's convenient to have low-level access to an ESC for testing configuration
  val pwmChannel1 = PwmChannel(chipNumber = 0, channelNumber = 1)

  object escs {
    val one =   ESC("1", pwmChannel1) // pin 2
    val two =   ESC("2", PwmChannel(chipNumber = 0, channelNumber = 3)) // pin 4
    val three = ESC("3", PwmChannel(chipNumber = 0, channelNumber = 5)) // pin 6
    val four =  ESC("4", PwmChannel(chipNumber = 0, channelNumber = 7)) // pin 8
  }
}

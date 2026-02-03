package core

import scala.concurrent.duration._
import eu.timepit.refined.auto.autoRefineV
import cats.effect.IO
import fs2.Stream
import device.controller.{ ControllerMetrics, TransfersObservation }
import device.controller.filesystem.FileSystemController
import device.controller.spi.{SpiController, SpiAddress, SpiFullDuplexController, SpiRegisterController }
import device.rc.RcChannel
import device.sensor.Mpu9250
import device.esc.{PwmChannel, ESC}

object Navio2 {

  implicit val spiController: SpiRegisterController with SpiFullDuplexController with ControllerMetrics = SpiController()
  implicit val fileController: FileSystemController = FileSystemController()

  val spiMetrics: Stream[IO, TransfersObservation] = ControllerMetrics(spiController, 1.second)

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

package fc

import cats.syntax.either._
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

  import RcReceiver.channels

  val arm = channels.six
  val throttle = channels.three
  val roll = channels.one
  val pitch = channels.two
  val yaw = channels.four


  def displayRc() = task.fs2.getRcInputs(receiver, arm, throttle, pitch, roll, yaw) flatMap { dr =>
    dr.fold(
      ex => task.fs2.printToConsole(ex.toString),
      chs => task.fs2.printToConsole(s"arm: ${chs._1} -- throttle: ${chs._2} -- pitch: ${chs._3} -- roll: ${chs._4} -- yaw: ${chs._5}")
    )
  }

  /* End quick and nasty */

}

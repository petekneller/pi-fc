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

  def displayRc() = {
    val inputs = task.fs2.readChannel(receiver, channels.one) zip
    task.fs2.readChannel(receiver, channels.two) zip
    task.fs2.readChannel(receiver, channels.three) zip
    task.fs2.readChannel(receiver, channels.four) zip
    task.fs2.readChannel(receiver, channels.five)

    val outputs = inputs map { case ((((ch1in, ch2in), ch3in), ch4in), ch5in) =>
      for {
        ch1position <- ch1in
        ch2position <- ch2in
        ch3position <- ch3in
        ch4position <- ch4in
        ch5position <- ch5in
      } yield (ch1position, ch2position, ch3position, ch4position, ch5position)
    }
    outputs flatMap { dr => dr.fold(ex => task.fs2.printToConsole(ex.toString),
      chs => task.fs2.printToConsole(s"ch1: ${chs._1} -- ch2: ${chs._2} -- ch3: ${chs._3} -- ch4: ${chs._4} -- ch5: ${chs._5}")) }
  }


  /* End quick and nasty */

}

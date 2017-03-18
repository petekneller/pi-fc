package fc

import cats.syntax.either._
import device.spi.{SpiController, SpiAddress}
import device.file.FileController
import device.rc.RcAddress
import device.input.{Mpu9250, RcReceiver, RcChannel}
import device.output.{PwmChannel, ESC}
import task.{fs2 => tasks}

object Navio2 {

  implicit val spiController = SpiController()
  implicit val fileController = FileController()

  /* Devices */

  val mpu9250 = Mpu9250(SpiAddress(busNumber = 0, chipSelect = 1))

  val receiver = RcReceiver(RcAddress("/sys/kernel/rcio/rcin"))

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

  /* Tasks */

  def initESCs = tasks.initESCs(escs.one, escs.two, escs.three, escs.four)

  def motorsTest = tasks.motorsTest(escs.one, escs.two, escs.three, escs.four) to tasks.printToConsole

  def displayRcChannels =
    tasks.zip5(tasks.readChannel(receiver, rcChannels.one),
               tasks.readChannel(receiver, rcChannels.two),
               tasks.readChannel(receiver, rcChannels.three),
               tasks.readChannel(receiver, rcChannels.four),
               tasks.readChannel(receiver, rcChannels.six)) map ( dr => dr.map {
    case (one, two, three, four, six) =>
      val fmt = "CH %d: [%4d]"
      (fmt.format(1, one.ppm) :: fmt.format(2, two.ppm) :: fmt.format(3, three.ppm) :: fmt.format(4, four.ppm) :: fmt.format(6, six.ppm) :: Nil).mkString(" | ")
  }) to tasks.printToConsole

  def displayGyro = tasks.readGyro(mpu9250) map (dr => dr map {
    case (x, y, z) =>
      val fmt = "%s: [%10f]"
      (fmt.format("X", x) :: fmt.format("Y", y) :: fmt.format("Z", z) :: Nil).mkString(" | ")
  }) to tasks.printToConsole

}

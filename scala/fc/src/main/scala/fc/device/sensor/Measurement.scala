package fc.device.sensor

import cats.syntax.either._
import fc.device._

trait FullScale {
  val factor: Double
}

case class Measurement(
    xLoRegister: Register, xHiRegister: Register,
    yLoRegister: Register, yHiRegister: Register,
    zLoRegister: Register, zHiRegister: Register,
    scale: FullScale) extends Rx {

  type T = (Double, Double, Double)

  def read(device: Address)(implicit controller: Controller { type Bus = device.Bus }): Either[DeviceException, (Double, Double, Double)] = for {
    x <- xWord.read(device)
    y <- yWord.read(device)
    z <- zWord.read(device)
  } yield (scale(x), scale(y), scale(z))

  private val maxShort = 2 ^ 16

  private def scale(fromDevice: Short): Double = (fromDevice.toDouble / maxShort) * scale.factor

  private val xWord = Rx.short(xLoRegister, xHiRegister)
  private val yWord = Rx.short(yLoRegister, yHiRegister)
  private val zWord = Rx.short(zLoRegister, zHiRegister)
}

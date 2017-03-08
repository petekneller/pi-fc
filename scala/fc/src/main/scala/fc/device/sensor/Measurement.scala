package fc.device.sensor

import cats.syntax.either._
import fc.device._

trait FullScale {
  val factor: Double
}

case class Measurement(
    loRegister: Byte, hiRegister: Byte,
    scale: FullScale) extends Rx {

  type T = Double
  type Register = Byte

  def read(device: Address)(implicit controller: Controller { type Bus = device.Bus; type Register = Byte }): DeviceResult[Double] = word.read(device).map(short => (short.toDouble / maxShort) * scale.factor)

  private val maxShort = math.pow(2, 15) // well, close enough

  private val word = ByteRx.short(loRegister, hiRegister)
}

case class Measurement3Axis(
    xLoRegister: Byte, xHiRegister: Byte,
    yLoRegister: Byte, yHiRegister: Byte,
    zLoRegister: Byte, zHiRegister: Byte,
    scale: FullScale) extends Rx {

  type T = (Double, Double, Double)
  type Register = Byte

  def read(device: Address)(implicit controller: Controller { type Bus = device.Bus; type Register = Byte }): DeviceResult[(Double, Double, Double)] = for {
    x <- xMeasurement.read(device)
    y <- yMeasurement.read(device)
    z <- zMeasurement.read(device)
  } yield (x, y, z)

  private val xMeasurement = Measurement(xLoRegister, xHiRegister, scale)
  private val yMeasurement = Measurement(yLoRegister, yHiRegister, scale)
  private val zMeasurement = Measurement(zLoRegister, zHiRegister, scale)
}

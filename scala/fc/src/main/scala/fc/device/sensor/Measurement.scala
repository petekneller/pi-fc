package fc.device.sensor

import cats.syntax.either._
import fc.device._

case class Measurement(
    xWord: Rx { type T = Short },
    yWord: Rx { type T = Short },
    zWord: Rx { type T = Short }) extends Rx {

  type T = (Short, Short, Short)

  def read(device: Address)(implicit controller: Controller { type Bus = device.Bus }): Either[DeviceException, T] = for {
    x <- xWord.read(device)
    y <- yWord.read(device)
    z <- zWord.read(device)
  } yield (x, y, z)
}

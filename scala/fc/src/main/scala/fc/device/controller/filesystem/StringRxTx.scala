package fc.device.controller.filesystem

import cats.syntax.either._
import eu.timepit.refined.auto.{autoRefineV, autoUnwrap}
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import fc.device.api._

case class NotNumericException(actualValue: String) extends DeviceException

object RxString {

  def string(register: String, maxBytesToRead: Int Refined Positive = 32) = new Rx {
    type T = String
    type Ctrl = FileSystemController

    def read(device: FileSystemAddress)(implicit controller: FileSystemController): DeviceResult[String] = for {
      data <- controller.receive(device, register, maxBytesToRead)
    } yield data.take(maxBytesToRead).map(_.toChar).filter(_ != '\n').mkString
  }

  def numeric[A](register: String, f: Long => A = identity[Long] _) = new Rx {
    type T = A
    type Ctrl = FileSystemController

    def read(device: FileSystemAddress)(implicit controller: FileSystemController): DeviceResult[T] = for {
      string <- rx.read(device)
      long <- Either.catchNonFatal { string.toLong }.leftMap(_ => NotNumericException(string))
    } yield f(long)

    private val rx = RxString.string(register)
  }

}

object TxString {

  def string(register: String) = new Tx {
    type T = String
    type Ctrl = FileSystemController

    def write(device: FileSystemAddress, value: String)(implicit controller: FileSystemController): DeviceResult[Unit] = for {
      _ <- controller.transmit(device, register, value.toCharArray.map(_.toByte))
    } yield ()
  }

  def numeric[A](register: String, f: A => Long = identity[Long] _) = new Tx {
    type T = A
    type Ctrl = FileSystemController

    def write(device: FileSystemAddress, value: T)(implicit controller: FileSystemController): DeviceResult[Unit] = tx.write(device, f(value).toString)

    private val tx = TxString.string(register)
  }

}

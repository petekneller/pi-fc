package fc.device.controller.filesystem

import cats.syntax.either._
import fc.device.api._

object NumericConfiguration {
  def apply[A](register: String, map: Long => A = identity[Long] _, contramap: A => Long = identity[Long] _) = JointConfiguration(NumericRx(register, map))(NumericTx(register, contramap))
}

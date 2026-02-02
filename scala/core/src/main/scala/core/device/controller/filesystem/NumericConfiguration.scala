package core.device.controller.filesystem

import core.device.api._

object NumericConfiguration {
  def apply[A](register: FileSystemRegister, map: Long => A = identity[Long] _, contramap: A => Long = identity[Long] _) = JointConfiguration(NumericRx(register, map))(NumericTx(register, contramap))
}

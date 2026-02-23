package fc.algo

case class AhrsState(q0: Double, q1: Double, q2: Double, q3: Double, ix: Double, iy: Double, iz: Double) {

  def attitude: (Double, Double, Double) = {
    val roll  = math.atan2(2.0 * (q0 * q1 + q2 * q3), 1.0 - 2.0 * (q1 * q1 + q2 * q2))
    val sinP  = 2.0 * (q0 * q2 - q3 * q1)
    val pitch = if (math.abs(sinP) >= 1.0) math.copySign(math.Pi / 2.0, sinP) else math.asin(sinP)
    val yaw   = math.atan2(2.0 * (q0 * q3 + q1 * q2), 1.0 - 2.0 * (q2 * q2 + q3 * q3))
    (roll, pitch, yaw)
  }
}

object AhrsState {
  val identity = AhrsState(1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
}

trait Ahrs {
  def update(state: AhrsState, gyro: (Double, Double, Double), accel: (Double, Double, Double), dt: Double): AhrsState
}

package fc.algo

trait FeedbackController {
  def run(roll: Double, pitch: Double, yaw: Double, gx: Double, gy: Double, gz: Double): (Double, Double, Double)
}

case class PControllerTargetZero(gain: Double) extends FeedbackController {
  def run(roll: Double, pitch: Double, yaw: Double, qx: Double, qy: Double, qz: Double) = {
    val dx = qx
    val dy = qy
    val dz = qz
    (dx * gain, dy * gain, dz * gain)
  }
}

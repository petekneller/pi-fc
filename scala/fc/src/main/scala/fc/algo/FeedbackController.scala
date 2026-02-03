package fc.algo

trait FeedbackController {
  def run(gx: Double, gy: Double, gz: Double): (Double, Double, Double)
}

case class PControllerTargetZero(gain: Double) extends FeedbackController {
  def run(qx: Double, qy: Double, qz: Double) = {
    val dx = qx // direction?
    val dy = qy // direction?
    val dz = qz // direction?
    (dx * gain, dy * gain, dz * gain)
  }
}

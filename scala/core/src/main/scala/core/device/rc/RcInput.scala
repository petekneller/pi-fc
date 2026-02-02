package core.device.rc

case class RcInput(ppm: Int, fromZero: Double, aroundZero: Double)

object RcInput {
  def fromPpm(value: Int, minPpm: Int, maxPpm: Int, midPpm: Int): RcInput = {
    val ppmRange = maxPpm - minPpm
    val fromZero = (value - minPpm).toDouble / ppmRange
    val aroundZero = (value - midPpm).toDouble / (ppmRange.toDouble / 2)
    RcInput(value, fromZero.min(1.0).max(0.0), aroundZero.max(-1.0).min(1.0))
  }
}

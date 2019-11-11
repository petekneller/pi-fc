package fc.metrics

import scala.math.max

case class StatisticalMeasures[A](median: A, p90: A, max: A)

object StatisticalMeasures {
  def apply[A: Ordering](data: Seq[A], empty: A): StatisticalMeasures[A] = {
    val size = data.size
    if (size == 0)
      StatisticalMeasures(empty, empty, empty)
    else {
      val ordered = data.sorted
      StatisticalMeasures(
        ordered.apply(toIndex(0.5 * size)),
        ordered.apply(toIndex(0.9 * size)),
        ordered.apply(toIndex(1.0 * size))
      )
    }
  }

  private def toIndex(idx: Double): Int = max(idx.round.toInt - 1, 0)
}

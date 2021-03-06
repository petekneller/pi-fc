package fc.metrics

import scala.math.max

case class StatisticalMeasures[A](min: A, median: A, p90: A, max: A)

object StatisticalMeasures {
  def apply[A: Ordering](data: Seq[A], empty: A): StatisticalMeasures[A] = {
    val length = data.length
    if (length == 0)
      StatisticalMeasures(empty, empty, empty, empty)
    else {
      val ordered = data.sorted
      StatisticalMeasures(
        ordered.apply(toIndex(0.0 * length)),
        ordered.apply(toIndex(0.5 * length)),
        ordered.apply(toIndex(0.9 * length)),
        ordered.apply(toIndex(1.0 * length))
      )
    }
  }

  private def toIndex(idx: Double): Int = max(idx.round.toInt - 1, 0)
}

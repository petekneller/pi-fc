package fc.metrics

import java.util.concurrent.atomic.AtomicReference
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.{ Positive }
import eu.timepit.refined.auto.autoUnwrap


case class AggregationBuffer[A](size: Int Refined Positive) {
  private val buffer: AtomicReference[Vector[A]] = new AtomicReference(Vector.empty)

  def record(a: A): Unit = buffer.updateAndGet{ b => (b :+ a).takeRight(size) }

  def retrieve: Seq[A] = buffer.get()
}

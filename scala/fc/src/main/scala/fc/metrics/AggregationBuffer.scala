package fc.metrics

import java.util.concurrent.atomic.AtomicReference
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.{ Positive }
import eu.timepit.refined.auto.autoUnwrap

/*
 * A circular buffer, used as a concurrency primitive to decouple the thread on
 * which events are published from the consumer, allowing the publishing thread
 * to return as quickly as possible.
 */
case class AggregationBuffer[A](numSamples: Int Refined Positive) {
  private val buffer: AtomicReference[Vector[A]] = new AtomicReference(Vector.empty)

  def record(a: A): Unit = buffer.updateAndGet{ b => (b :+ a).takeRight(numSamples) }

  def retrieve: Seq[A] = buffer.get()
}

package fc.metrics

import java.util.function.UnaryOperator
import java.util.concurrent.atomic.AtomicReference

class Hook[A]() {
  private val _callbacks: AtomicReference[Seq[A => Unit]] = new AtomicReference(Seq.empty)
  val callbacks = new Callbacks[A] {
    def add(cb: A => Unit): Unit = _callbacks.getAndUpdate(
      new UnaryOperator[Seq[A => Unit]]{ def apply(cbs: Seq[A => Unit]) = cbs :+ cb }
    )
  }
  def notify(observation: A): Unit = _callbacks.get.foreach(cb => cb(observation))
}

trait Callbacks[A] {
  def add(callback: A => Unit): Unit
}

object Hook {
  def apply[A](): Hook[A] = new Hook()
}

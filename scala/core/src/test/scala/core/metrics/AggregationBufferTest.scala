package core.metrics

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalactic.{ TypeCheckedTripleEquals }
import eu.timepit.refined.auto.autoRefineV

class AggregationBufferTest extends AnyFlatSpec with Matchers with TypeCheckedTripleEquals {

  "an aggregation buffer" should "record up to the specified number of records" in {
    val buffer = AggregationBuffer[Int](3)
    buffer.record(1)
    buffer.record(2)
    buffer.record(3)
    buffer.retrieve should contain only (1, 2, 3)
  }

  it should "discard the _oldest_ records once full (ie. it is a FIFO)" in {
    val buffer = AggregationBuffer[Int](3)
    buffer.record(1)
    buffer.record(2)
    buffer.record(3)
    buffer.record(4)
    buffer.retrieve should contain only (2, 3, 4)
  }

  it should "be empty after the contents are retrieved" in {
    val buffer = AggregationBuffer[Int](3)
    buffer.record(1)
    buffer.record(2)
    buffer.record(3)
    val _ = buffer.retrieve
    buffer.retrieve should be(empty)
  }

}

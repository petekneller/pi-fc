package fc.metrics

import org.scalatest.{ FlatSpec, Matchers }
import org.scalactic.{ TypeCheckedTripleEquals }
import eu.timepit.refined.auto.autoRefineV

class AggregationBufferTest extends FlatSpec with Matchers with TypeCheckedTripleEquals {

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

}

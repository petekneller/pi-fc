package fc.metrics

import org.scalatest.{ FlatSpec, Matchers }
import org.scalactic.{ TypeCheckedTripleEquals }

class StatisticalMeasuresTest extends FlatSpec with Matchers with TypeCheckedTripleEquals {

  "a statistical measure" should "calculate the median of the provided values" in {
    StatisticalMeasures(Seq(1, 3, 2), 0).median should === (2)
  }

  it should "calculate the 90th percentile of the provided values" in {
    StatisticalMeasures(Seq(8, 1, 6, 3, 4, 9, 2, 7, 0, 5), 0).p90 should === (8)
  }

  it should "calculate the maximum value" in {
    StatisticalMeasures(Seq("f", "foo", "fo"), "").max should === ("foo")
  }

  it should "tolerate and empty data set by returning the empty value" in {
    val empty = StatisticalMeasures(Seq.empty[Double], 0.0)
    empty.median should === (0.0)
    empty.p90 should === (0.0)
    empty.max should === (0.0)
  }

}

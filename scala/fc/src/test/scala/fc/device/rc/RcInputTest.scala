package fc.device.rc

import org.scalatest.{FlatSpec, Matchers}
import org.scalactic.TypeCheckedTripleEquals

class RcInputTest extends FlatSpec with Matchers with TypeCheckedTripleEquals {

  "ppm" should "echo the PPM value that was originally captured" in {
    RcInput.fromPpm(1, unused, unused, unused).ppm should === (1)
  }

  it should "capture the original value regardless of whether it falls within the min/max bound" in {
    RcInput.fromPpm(10, 1, 3, unused).ppm should === (10)
  }

  "fromZero" should "return a value in the range 0 to 1 that reflects where the ppm value lies in the specified bounds" in {
    RcInput.fromPpm(10, 10, 20, unused).fromZero should === (0.0)
    RcInput.fromPpm(20, 10, 20, unused).fromZero should === (1.0)
    RcInput.fromPpm(15, 10, 20, unused).fromZero should === (0.5)
  }

  it should "limit the return value to the range 0 to 1" in {
    RcInput.fromPpm(0, 10, 20, unused).fromZero should === (0.0)
    RcInput.fromPpm(30, 10, 20, unused).fromZero should === (1.0)
  }

  "aroundZero" should "return a value in the range -1 to 1 that reflects where the ppm value lies around the centre point" in {
    RcInput.fromPpm(1, 1, 3, 2).aroundZero should === (-1.0)
    RcInput.fromPpm(2, 1, 3, 2).aroundZero should === (0.0)
    RcInput.fromPpm(3, 1, 3, 2).aroundZero should === (1.0)
  }

  it should "limit the return value to the range -1 to 1" in {
    RcInput.fromPpm(4, 1, 3, 2).aroundZero should === (1.0)
    RcInput.fromPpm(0, 1, 3, 2).aroundZero should === (-1.0)
  }

  val unused = 1

}

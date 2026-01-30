package ioctl

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalactic.TypeCheckedTripleEquals
import syntax._

class UnsignedConversionsTest extends AnyFlatSpec with Matchers with TypeCheckedTripleEquals {

  "unsigned conversion of a short" should "widen to int, preserving the original number" in {
    val doesntFitInAShort = 0x8000.toShort
    assert(doesntFitInAShort < 0)
    doesntFitInAShort.unsigned should === (32768)
  }

  "unsigned conversion of an int" should "widen to long, preserving the original number" in {
    val doesntFitInAnInt = 0x80000000
    assert(doesntFitInAnInt < 0)
    doesntFitInAnInt.unsigned should === (2147483648L)
  }

}

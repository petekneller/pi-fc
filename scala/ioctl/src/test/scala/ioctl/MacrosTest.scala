package ioctl

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalactic.TypeCheckedTripleEquals
import syntax._
import macros._

class MacrosTest extends AnyFlatSpec with Matchers with TypeCheckedTripleEquals {

  "the IOx macros" should "place the direction identifier in the highest 2 bits" in {
    val a = rand.toByte
    val b = rand.toByte
    "0x%x".format(IOR(a, b, classOf[Int]) & (3 << 30)) should === ("0x80000000")
    "0x%x".format(IOW(a, b, classOf[Int]) & (3 << 30)) should === ("0x40000000")
  }

  "the IOC macro" should "place the 'nr' identifier in the bottom 8 bits" in {
    val nr = rand.toByte
    "0x%x".format(IOC(IOC_NONE, rand.toByte, nr, 4) & 0x000000FF) should === ("0x%x".format(nr.unsigned.toInt))
  }

  it should "place the 'type' identifier in the upper 8 of the bottom 16 bits" in {
    val `type` = rand.toByte
    "0x%x".format(IOC(IOC_NONE, `type`, rand.toByte, 4) & 0x0000FF00) should === ("0x%x".format(`type`.unsigned << 8))
  }

  it should "place the 'size' in the lower 14 of the upper 16 bits" in {
    "0x%08x".format(IOC(IOC_NONE, rand.toByte, rand.toByte, 4) & 0x3FFF0000) should === ("0x00040000")
    "0x%08x".format(IOC(IOC_NONE, rand.toByte, rand.toByte, 1) & 0x3FFF0000) should === ("0x00010000")
  }

  def rand: Int = (Math.random * Int.MaxValue).toInt
}

package core.device.gps.ublox

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalactic.TypeCheckedTripleEquals

class UbxTypesTest extends AnyFlatSpec with Matchers with TypeCheckedTripleEquals {

  "U2.parse" should "should correctly parse example values" in {
    UbxTypes.U2.parse(0x9F.toByte, 0x02.toByte) should ===(671)
  }

  "U2.toBytes" should "should correctly serialize example values" in {
    UbxTypes.U2.toBytes(671) should ===((0x9F.toByte, 0x02.toByte))
  }

  "U1.parse" should "should correctly parse example values" in {
    UbxTypes.U1.parse(0x0A.toByte) should ===(10)
    UbxTypes.U1.parse(0x1B.toByte) should ===(27)
  }

  "U1.toBytes" should "should correctly serialize example values" in {
    UbxTypes.U1.toBytes(10) should ===(0x0A.toByte)
    UbxTypes.U1.toBytes(27) should ===(0x1B.toByte)
  }

}

package core.device.gps.ublox

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalactic.TypeCheckedTripleEquals
import core.device.gps.ublox.examples.UbxConfigPower
import core.device.gps.ublox.examples.UbxConfigPowerPoll

class UbxChecksumTest extends AnyFunSpec with Matchers with TypeCheckedTripleEquals {

  describe("UbxChecksum") {
    describe("should calculate the checksum correctly for a number of known examples") {
      it(UbxConfigPower.name) {
        val msg = UbxConfigPower.msg
        val expected = UbxConfigPower.bytes.takeRight(2)
        val actual = UbxChecksum(msg.clazz, msg.id, msg.payload)
        (actual.ckA, actual.ckB) should ===((expected(0), expected(1)))
      }

      it(UbxConfigPowerPoll.name) {
        val msg = UbxConfigPowerPoll.msg
        val expected = UbxConfigPowerPoll.bytes.takeRight(2)
        val actual = UbxChecksum(msg.clazz, msg.id, msg.payload)
        (actual.ckA, actual.ckB) should ===((expected(0), expected(1)))
      }
    }
  }

}

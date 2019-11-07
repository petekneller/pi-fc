package fc.device.gps.ublox

import org.scalatest.{ FlatSpec, Matchers }
import org.scalactic.TypeCheckedTripleEquals

class UbxMessagesTest extends FlatSpec with Matchers with TypeCheckedTripleEquals {

  testsForToBytes(exampleUnknown, "Unknown")

  "Unknown" should "have the lo-byte of the length as the fifth byte" in {
    exampleUnknown.toBytes(4) should === (0x03.toByte)
  }

  it should "have the hi-byte of the length as the sixth byte" in {
    exampleUnknown.toBytes(5) should === (0x00.toByte)
  }

  testsForToBytes(examples.UbxConfigPowerPoll.msg, "UBX-CFG-PWR poll")
  testsForToBytes(examples.UbxConfigPower.msg, "UBX-CFG-PWR")
  testsForToBytes(examples.UbxAckAck.msg, "UBX-ACK-ACK")

  def testsForToBytes(msg: UbxMessage, msgName: String): Unit = {
    s"${msgName}.toBytes" should "have 0xB5 as the first byte" in {
      msg.toBytes(0) should === (0xB5.toByte)
    }

    it should "have 0x62 as the second byte" in {
      msg.toBytes(1) should === (0x62.toByte)
    }

    it should "have the message class as the third byte" in {
      msg.toBytes(2) should === (msg.clazz)
    }

    it should "have the message id as the fourth byte" in {
      msg.toBytes(3) should === (msg.id)
    }

    it should "have the first checksum byte as the second to last byte" in {
      msg.toBytes.init.last should === (msg.checksum._1)
    }

    it should "have the second checksum byte as the last byte" in {
      msg.toBytes.last should === (msg.checksum._2)
    }
  }

  def exampleUnknown = Unknown(0x01, 0x02, Seq(0x03, 0x04, 0x05), 0x06, 0x07)

}

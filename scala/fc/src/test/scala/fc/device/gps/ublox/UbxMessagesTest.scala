package fc.device.gps.ublox

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalactic.TypeCheckedTripleEquals

class UbxMessagesTest extends AnyFlatSpec with Matchers with TypeCheckedTripleEquals {

  // Unknown
  testsForToBytes(exampleUnknown, "Unknown")

  "Unknown" should "have the lo-byte of the length as the fifth byte" in {
    exampleUnknown.msg.toBytes(4) should === (0x03.toByte)
  }

  it should "have the hi-byte of the length as the sixth byte" in {
    exampleUnknown.msg.toBytes(5) should === (0x00.toByte)
  }

  private lazy val exampleUnknown = {
    val clazz = 0x01.toByte
    val id = 0x02.toByte
    val checksum1 = 0x06.toByte
    val checksum2 = 0x07.toByte
    val payload = Seq(0x03, 0x04, 0x05).map(_.toByte)
    val payloadLength1 = 0x03.toByte
    val payloadLength2 = 0x00.toByte

    Example(
      bytes = Seq(0xB5.toByte, 0x62.toByte, clazz, id, payloadLength1, payloadLength2, checksum1, checksum2),
      clazz = clazz,
      id = id,
      payload = payload,
      payloadLength = (payloadLength1, payloadLength2),
      checksum = (checksum1, checksum2),
      msg = Unknown(clazz, id, payload, checksum1, checksum2)
    )
  }

  // Config/Power
  testsForToBytes(examples.UbxConfigPowerPoll, "UBX-CFG-PWR poll")

  testsForToBytes(examples.UbxConfigPower, "UBX-CFG-PWR")

  // Ack/Ack
  testsForToBytes(examples.UbxAckAck, "UBX-ACK-ACK")

  // Monitor/RxBuffer
  testsForToBytes(examples.UbxMonitorRxBufferPoll, "UBX-MON-RXBUF")

  // Monitor/TxBuffer
  testsForToBytes(examples.UbxMonitorTxBufferPoll, "UBX-MON-TXBUF")

  // Internals
  private def testsForToBytes(example: Example, msgName: String): Unit = {
    val msgBytes = example.msg.toBytes
    s"${msgName}.toBytes" should "have 0xB5 as the first byte" in {
      msgBytes(0) should === (0xB5.toByte)
    }

    it should "have 0x62 as the second byte" in {
      msgBytes(1) should === (0x62.toByte)
    }

    it should "have the message class as the third byte" in {
      msgBytes(2) should === (example.clazz)
    }

    it should "have the message id as the fourth byte" in {
      msgBytes(3) should === (example.id)
    }

    it should "have the first checksum byte as the second to last byte" in {
      msgBytes.init.last should === (example.checksum._1)
    }

    it should "have the second checksum byte as the last byte" in {
      msgBytes.last should === (example.checksum._2)
    }
  }

}

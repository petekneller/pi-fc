package core.device.gps.ublox

import spire.syntax.literals._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalactic.TypeCheckedTripleEquals
import core.device.gps.ParserTestSupport

class UbxParserTest extends AnyFlatSpec with Matchers with TypeCheckedTripleEquals with ParserTestSupport {

  // TODO checksum doesn't match

  "An empty parser" should "not consume a byte that is not 0xB5" in {
    UbxParser().consume(b) should be (unconsumed(b))
  }

  it should "consume a byte that is 0xB5 and proceed" in {
    UbxParser().consume(b5) should be (proceeding)
  }

  "A proceeding parser" should "halt when the second byte is not 0x62 and return both bytes" in {
    AwaitingPreamble2.consume(c) should be (unconsumed(b5, c))
  }

  it should "continue proceeding when the second byte is 0x62" in {
    AwaitingPreamble2.consume(sixty2) should be (proceeding)
  }

  it should "parse out the message class" in {
    AwaitingClass.consume(b) should === (Proceeding(AwaitingId(b)))
  }

  it should "parse out the message id" in {
    AwaitingId(b).consume(c) should === (Proceeding(AwaitingLength1(b, c)))
  }

  it should "parse out the payload length" in {
    consume(AwaitingLength1(b, c), Seq(b"3", b"0")) should === (Proceeding(ConsumingPayload(b, c, 3, Seq.empty[Byte])))
    consume(AwaitingLength1(b, c), Seq(b"0", b"3")) should === (Proceeding(ConsumingPayload(b, c, 768, Seq.empty[Byte])))
  }

  it should "correctly treat the first byte of the length as an unsigned value" in {
    consume(AwaitingLength1(b, c), Seq(b"4", b"0")) should === (Proceeding(ConsumingPayload(b, c, 4, Seq.empty[Byte])))
    consume(AwaitingLength1(b, c), Seq(b"129", b"0")) should === (Proceeding(ConsumingPayload(b, c, 129, Seq.empty[Byte])))
    consume(AwaitingLength1(b, c), Seq(b"0", b"1")) should === (Proceeding(ConsumingPayload(b, c, 256, Seq.empty[Byte])))
    consume(AwaitingLength1(b, c), Seq(b"0", b"129")) should === (Proceeding(ConsumingPayload(b, c, 33024, Seq.empty[Byte])))
  }

  it should "parse out the message payload" in {
    val state1 = ConsumingPayload(b, c, 3, Seq.empty[Byte]).consume(d)
    state1 should be (proceeding)

    val state2 = (state1.asInstanceOf[Proceeding]).next.consume(c)
    state2 should be (proceeding)

    val state3 = (state2.asInstanceOf[Proceeding]).next.consume(b)
    state3 should === (Proceeding(AwaitingChecksum1(b, c, Seq(d, c, b))))
  }

  it should "parse out the checksum" in {
    val state1 = AwaitingChecksum1(b, c, Seq.empty[Byte]).consume(b)
    state1 should be(proceeding)

    val state2 = state1.asInstanceOf[Proceeding].next.consume(d)
    state2 should === (Done(Unknown(b, c, Seq.empty[Byte], b, d)))
  }

  "A parser" should "successfully parse a Config Power poll message (as Unknown)" in {
    val expected = examples.UbxConfigPowerPoll
    consume(UbxParser(), expected.bytes) should === (
      Done(Unknown(expected.clazz, expected.id, expected.payload, expected.checksum._1, expected.checksum._2))
    )
  }

  it should "successfully parse a Config Power message (as Unknown)" in {
    val expected = examples.UbxConfigPower
    consume(UbxParser(), expected.bytes) should === (
      Done(Unknown(expected.clazz, expected.id, expected.payload, expected.checksum._1, expected.checksum._2))
    )
  }

  it should "successfully parse an Ack message (as Unknown)" in {
    val expected = examples.UbxAckAck
    consume(UbxParser(), expected.bytes) should === (
      Done(Unknown(expected.clazz, expected.id, expected.payload, expected.checksum._1, expected.checksum._2))
    )
  }

  type Msg = UbxMessage

  private val b5 = 0xB5.toByte
  private val sixty2 = 0x62.toByte
  private val b = 'b'.toByte
  private val c = 'c'.toByte
  private val d = 'd'.toByte

}

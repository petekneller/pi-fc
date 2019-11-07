package fc.device.gps.nmea

import org.scalatest.{ FlatSpec, Matchers }
import org.scalactic.TypeCheckedTripleEquals
import fc.device.gps.{ MessageParser, ParserTestSupport }
import MessageParser.Proceeding

class NmeaParserTest extends FlatSpec with Matchers with TypeCheckedTripleEquals with ParserTestSupport {

  // TODO checksum doesn't match

  "An empty parser" should "not consume a byte that does not represent '$'" in {
    val f = 'f'.toByte
    NmeaParser().consume(f) should be (unconsumed(f))
  }

  it should "consume a byte that represents '$' and begin parsing" in {
    NmeaParser().consume('$'.toByte) should be (proceeding)
  }

  "a Proceeding parser" should "continue to read bytes" in {
    val parser1 = NmeaParser()
    val finalParser = parser1.consume('$'.toByte) match {
      case Proceeding(parser2) => parser2.consume('a'.toByte) match {
        case Proceeding(parser3) => parser3.consume('b'.toByte) match {
          case Proceeding(parser4) => parser4
          case _ => fail("parser3 did not Proceed")
        }
        case _ => fail("parser2 did not Proceed")
      }
      case _ => fail("parser1 did not Proceed")
    }

    finalParser should === (AwaitingTrailer1(Seq('a'.toByte, 'b'.toByte)))
  }

  it should "complete after finding a <cr><lf> pair" in {
    val a = 'a'.toByte; val b = 'b'.toByte; val c = 'c'.toByte; val d = 'd'.toByte
    val bytes = Seq('$'.toByte, a, b, c, d, '\r'.toByte, '\n'.toByte)
    val finalState = consume(NmeaParser(), bytes)
    finalState should be (done)
    finalState.asInstanceOf[Done].message should === (Unknown(Seq(a, b, c, d)))
  }

  it should "continue past <cr>'s that are not followed by <lf>" in {
    val a = 'a'.toByte; val b = 'b'.toByte; val cr = '\r'.toByte
    val bytes = Seq('$'.toByte, a, cr, b)
    val finalState = consume(NmeaParser(), bytes)
    finalState should be (proceeding)
    finalState.asInstanceOf[Proceeding].next should === (AwaitingTrailer1(Seq(a, b)))
  }

  it should "fail if the total length of message (including initial and 2 trailer chars) exceeds 82 bytes" in {
    val content = (1 to 79) map (_ => 'a'.toByte)
    val bytes = '$'.toByte +: content
    val state1 = consume(NmeaParser(), bytes)

    state1 should be (proceeding)
    val state2 = state1.asInstanceOf[Proceeding].next.consume('a'.toByte)
    state2 should be (failed)
  }

  "NmeaParser" should "successfully consumer a PUBX Time Of Day Poll message (as Unknown)" in {
    consume(NmeaParser(), examples.PubxTimeOfDayPoll.bytes) should be (done)
  }

  it should "successfully consumer a PUBX Time Of Day message (as Unknown)" in {
    consume(NmeaParser(), examples.PubxTimeOfDay.bytes) should be (done)
  }

  it should "successfully consumer a GNVTG message (as Unknown)" in {
    consume(NmeaParser(), examples.GnVtg.bytes) should be (done)
  }

  type Msg = NmeaMessage

}

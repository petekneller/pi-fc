package core.device.gps.nmea

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalactic.TypeCheckedTripleEquals
import core.device.gps.{ ParserTestSupport, ExampleMessage }

class NmeaMessagesTest extends AnyFunSpec with Matchers with TypeCheckedTripleEquals with ParserTestSupport {

  // TODO verify checksum
  examples.all.foreach { case ExampleMessage(name, msg, bytes) =>
    describe(name) {
      describe(".toBytes") {
        it("should contain a start char of $"){
          msg.toBytes(0) should === ('$'.toByte)
        }

        it("should finish with a <cr><lf> pair"){
          msg.toBytes.last should === ('\n'.toByte)
          msg.toBytes.init.last should === ('\r'.toByte)
        }
      }
      describe("when parsed from test data") {
        it("should match the expected message") {
          consume(NmeaParser(), bytes) should be (done(msg))
        }
      }
    }
  }

  type Msg = NmeaMessage // ParserTestSupport
}

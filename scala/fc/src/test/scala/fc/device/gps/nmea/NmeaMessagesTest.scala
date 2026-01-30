package fc.device.gps.nmea

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalactic.TypeCheckedTripleEquals

class NmeaMessagesTest extends AnyFlatSpec with Matchers with TypeCheckedTripleEquals {

  // TODO verify checksum

  testsForToBytes(exampleUnknown, "Unknown")
  testsForToBytes(examples.PubxTimeOfDayPoll.msg, "PUBX TIME poll")
  testsForToBytes(examples.PubxTimeOfDay.msg, "PUBX TIME")
  testsForToBytes(examples.GnVtg.msg, "GN VTG")

  def testsForToBytes(msg: NmeaMessage, msgName: String): Unit = {
    s"${msgName}.toBytes" should "contain a start char of $" in {
      msg.toBytes(0) should === ('$'.toByte)
    }

    it should "finish with a <cr><lf> pair" in {
      msg.toBytes.last should === ('\n'.toByte)
      msg.toBytes.init.last should === ('\r'.toByte)
    }
  }

  def exampleUnknown = Unknown(Seq(0x01, 0x02, 0x03))

}

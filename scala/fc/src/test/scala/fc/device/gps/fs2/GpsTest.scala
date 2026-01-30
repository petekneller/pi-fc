package fc.device.gps.fs2

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalactic.TypeCheckedTripleEquals
import fs2.{ Stream, Chunk }
import fc.device.gps.nmea.{ NmeaMessage, NmeaParser }
import fc.device.gps.nmea.examples.{ PubxTimeOfDayPoll, PubxTimeOfDay }

class GpsTest extends AnyFlatSpec with Matchers with TypeCheckedTripleEquals {

  "parseStream" should "successfully parse input messages" in {
    val input = Stream.chunk(Chunk.seq(PubxTimeOfDayPoll.bytes)) ++
      Stream.chunk(Chunk.seq(PubxTimeOfDay.bytes))
    val s =  input through Gps.parseStream(() => NmeaParser())
    val msgs = s.compile.fold(Seq.empty[NmeaMessage])((acc, msg) => acc :+ msg).unsafeRunSync()
    msgs should ===(Seq(PubxTimeOfDayPoll.msg, PubxTimeOfDay.msg))
  }

}

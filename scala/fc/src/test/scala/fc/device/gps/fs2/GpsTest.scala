package fc.device.gps.fs2

import org.scalatest.{ FlatSpec, Matchers }
import org.scalactic.TypeCheckedTripleEquals
import fs2.{ Stream, Chunk }
import fc.device.gps.nmea.{ NmeaMessage, NmeaParser }
import fc.device.gps.nmea.examples.{ PubxTimeOfDayPoll, PubxTimeOfDay }

class GpsTest extends FlatSpec with Matchers with TypeCheckedTripleEquals {

  "parseStream" should "successfully parse input messages" in {
    val input = Stream.chunk(Chunk.seq(PubxTimeOfDayPoll.bytes)) ++
      Stream.chunk(Chunk.seq(PubxTimeOfDay.bytes))
    val s =  input through Gps.parseStream(() => NmeaParser())
    val msgs = s.compile.fold(Seq.empty[NmeaMessage])((acc, msg) => acc :+ msg).unsafeRunSync()
    msgs should ===(Seq(PubxTimeOfDayPoll.msg, PubxTimeOfDay.msg))
  }

}

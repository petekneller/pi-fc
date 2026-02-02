package core.device.gps.nmea

import core.device.gps.Message

sealed trait NmeaMessage extends Message
case class Unknown(content: Seq[Byte]) extends NmeaMessage {
  override def toString(): String = "Unknown[NMEA]"

  import NmeaParser.{ initialChar, trailerChar1, trailerChar2 }
  def toBytes: Seq[Byte] = initialChar +: content :+ trailerChar1 :+ trailerChar2
}

package fc.device.gps.nmea

import fc.device.gps.Message

sealed trait NmeaMessage extends Message
case class Unknown(content: Seq[Byte]) extends NmeaMessage

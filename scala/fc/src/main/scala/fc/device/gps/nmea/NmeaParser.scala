package fc.device.gps.nmea

import fc.device.gps.MessageParser
import MessageParser._

sealed trait NmeaParser extends MessageParser[NmeaMessage]

object NmeaParser {
  def apply(): NmeaParser = Empty

  private[nmea] val initialChar = '$'.toByte
  private[nmea] val trailerChar1 = '\r'.toByte
  private[nmea] val trailerChar2 = '\n'.toByte
}

case object Empty extends NmeaParser {
  def consume(byte: Byte): ParseState[NmeaMessage] = byte match {
    case b if (b == NmeaParser.initialChar) => Proceeding(AwaitingTrailer1(Seq.empty))
    case _ => Unconsumed(Seq(byte))
  }
}

case class AwaitingTrailer1(messageContent: Seq[Byte]) extends NmeaParser {
  def consume(byte: Byte): ParseState[NmeaMessage] = byte match {
    case _ if (messageContent.length) >= 79 => Failed(s"Message longer than 82 bytes (content 79). Message content so far: ${messageContent.toString}")
    case b if (b == NmeaParser.trailerChar1) => Proceeding(AwaitingTrailer2(messageContent))
    case b => Proceeding(AwaitingTrailer1(messageContent :+ b))
  }
}

case class AwaitingTrailer2(messageContent: Seq[Byte]) extends NmeaParser {
  def consume(byte: Byte): ParseState[NmeaMessage] = byte match {
    case b if (b == NmeaParser.trailerChar2) => Done(Unknown(messageContent))
    case b => Proceeding(AwaitingTrailer1(messageContent :+ b))
  }
}

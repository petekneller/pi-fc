package fc.device.gps

import cats.syntax.eq._
import cats.instances.byte._
import cats.instances.list._
import org.scalatest.matchers.{ BeMatcher, MatchResult }
import MessageParser.{ ParseState => OrigParseState, Unconsumed => OrigUnconsumed, Proceeding => OrigProceeding, Done => OrigDone, Failed => OrigFailed }

trait ParserTestSupport {

  /*
   * This trait specialises many of the MessageParser types to reduce the number of type annotations
   *  scattered throughout tests
   * While it could be argued that an API that is hard to use in tests is a bad API (and this wouldn't
   *  be an unreasonable statement) the amount of test code for these parsers will likely exceed the
   *  amount of client code, and the comparison of types that is necessary in tests is not common
   *  or necessary to use the API as a client would. So it seems sensible ease the type burden
   *  for tests.
   */

  type Msg <: Message
  type ParseState = OrigParseState[Msg]

  def unconsumed(expectedBytes: Byte*): BeMatcher[ParseState] = new BeMatcher[ParseState] {
    def apply(left: ParseState): MatchResult = {
      val success = left match {
        case OrigUnconsumed(bytes) => expectedBytes.toList === bytes.toList
        case _ => false
      }
      MatchResult(success, s"$left is not Unconsumed(${expectedBytes.mkString(",")})", s"$left is Unconsumed"
      )
    }
  }

  def proceeding = new BeMatcher[ParseState] {
    def apply(left: ParseState) = {
      val success = left match {
        case OrigProceeding(_) => true
        case _ => false
      }
      MatchResult(success, s"$left is not Proceeding", s"$left is Proceeding"
      )
    }
  }

  def done = new BeMatcher[ParseState] {
    def apply(left: ParseState) = {
      val success = left match {
        case OrigDone(_) => true
        case _ => false
      }
      MatchResult(success, s"$left is not Done", s"$left is Done"
      )
    }
  }

  def failed = new BeMatcher[ParseState] {
    def apply(left: ParseState) = {
      val success = left match {
        case OrigFailed(_) => true
        case _ => false
      }
      MatchResult(success, s"$left is not Failed", s"$left is Failed"
      )
    }
  }

}

package fc.device.gps

import cats.syntax.eq._
import cats.instances.byte._
import cats.instances.list._
import org.scalatest.matchers.{ BeMatcher, MatchResult }
import MessageParser._

trait ParserTestSupport {

  def unconsumed(expectedBytes: Byte*): BeMatcher[ParseState] = new BeMatcher[ParseState] {
    def apply(left: ParseState): MatchResult = {
      val success = left match {
        case Unconsumed(bytes) => expectedBytes.toList === bytes.toList
        case _ => false
      }
      MatchResult(success, s"$left is not Unconsumed(${expectedBytes.mkString(",")})", s"$left is Unconsumed"
      )
    }
  }

  val proceeding = new BeMatcher[ParseState] {
    def apply(left: ParseState) = {
      val success = left match {
        case Proceeding(_) => true
        case _ => false
      }
      MatchResult(success, s"$left is not Proceeding", s"$left is Proceeding"
      )
    }
  }

  val done = new BeMatcher[ParseState] {
    def apply(left: ParseState) = {
      val success = left match {
        case Done(_) => true
        case _ => false
      }
      MatchResult(success, s"$left is not Done", s"$left is Done"
      )
    }
  }

  val failed = new BeMatcher[ParseState] {
    def apply(left: ParseState) = {
      val success = left match {
        case Failed(_) => true
        case _ => false
      }
      MatchResult(success, s"$left is not Failed", s"$left is Failed"
      )
    }
  }

}

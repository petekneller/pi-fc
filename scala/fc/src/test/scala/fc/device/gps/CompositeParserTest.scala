package fc.device.gps

import org.scalatest.{ FlatSpec, Matchers }
import org.scalactic.TypeCheckedTripleEquals
import MessageParser._

class CompositeParserTest extends FlatSpec with Matchers with TypeCheckedTripleEquals with ParserTestSupport {

  "a composite parser" should "return Unconsumed if neither of its member parsers consumes the input" in {
    val p1 = parserReturning(Unconsumed(Seq(b)))
    val p2 = parserReturning(Unconsumed(Seq(b)))
    new CompositeParser(p1, p2).consume(b) should be (unconsumed(b))
  }

  it should "use the first member parser if that consumes the input" in {
    val p0 = parserReturning(Failed("boo!"))
    val p1 = parserReturning(Proceeding(p0))
    val p2 = parserReturning(Unconsumed(Seq(b)))
    val firstResult = new CompositeParser(p1, p2).consume(b)
    firstResult should be (proceeding)
    firstResult.asInstanceOf[Proceeding[CompositeMessage[Foo, Foo]]].next.consume(b) should === (Failed[CompositeMessage[Foo, Foo]]("boo!"))
  }

  it should "use the second member parser if the first does consumes the input, but the second does" in {
    val p0 = parserReturning(Failed("boo!"))
    val p1 = parserReturning(Unconsumed(Seq(b)))
    val p2 = parserReturning(Proceeding(p0))
    val firstResult = new CompositeParser(p1, p2).consume(b)
    firstResult should be (proceeding)
    firstResult.asInstanceOf[Proceeding[CompositeMessage[Foo, Foo]]].next.consume(b) should === (Failed[CompositeMessage[Foo, Foo]]("boo!"))
  }

  type Msg = CompositeMessage[Foo, Foo]

  val b = 'b'.toByte

  trait Foo extends Message
  private def parserReturning(state: MessageParser.ParseState[Foo]): MessageParser[Foo] = new MessageParser[Foo] {
    def consume(byte: Byte): MessageParser.ParseState[Foo] = state
  }
}

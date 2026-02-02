package core.device.gps

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalactic.TypeCheckedTripleEquals

class CompositeParserTest extends AnyFlatSpec with Matchers with TypeCheckedTripleEquals with ParserTestSupport {

  "a composite parser" should "return Unconsumed if neither of its member parsers consumes the input" in {
    val p1 = parserReturning(MessageParser.Unconsumed(Seq(b)))
    val p2 = parserReturning(MessageParser.Unconsumed(Seq(b)))
    new CompositeParser(p1, p2).consume(b) should be (unconsumed(b))
  }

  it should "use the first member parser if that consumes the input" in {
    val p0 = parserReturning(MessageParser.Failed("boo!"))
    val p1 = parserReturning(MessageParser.Proceeding(p0))
    val p2 = parserReturning(MessageParser.Unconsumed(Seq(b)))
    val firstResult = new CompositeParser(p1, p2).consume(b)
    firstResult should be (proceeding)
    firstResult.asInstanceOf[Proceeding].next.consume(b) should === (Failed("boo!"))
  }

  it should "use the second member parser if the first does consumes the input, but the second does" in {
    val p0 = parserReturning(MessageParser.Failed("boo!"))
    val p1 = parserReturning(MessageParser.Unconsumed(Seq(b)))
    val p2 = parserReturning(MessageParser.Proceeding(p0))
    val firstResult = new CompositeParser(p1, p2).consume(b)
    firstResult should be (proceeding)
    firstResult.asInstanceOf[Proceeding].next.consume(b) should === (Failed("boo!"))
  }

  type Msg = CompositeMessage[Foo, Foo]

  val b = 'b'.toByte

  trait Foo extends Message
  private def parserReturning(state: MessageParser.ParseState[Foo]): MessageParser[Foo] = new MessageParser[Foo] {
    def consume(byte: Byte): MessageParser.ParseState[Foo] = state
  }
}

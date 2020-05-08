package fc.metrics

import org.scalatest.{ FlatSpec, Matchers }
import org.scalactic.TypeCheckedTripleEquals
import org.scalamock.scalatest.MockFactory

class HookTest extends FlatSpec with Matchers with TypeCheckedTripleEquals with MockFactory {

  "Hook" should "pass calls to each registered observer" in {
    val hook = Hook[Foo]()

    case class Foo(foo: String)
    val foo = Foo("foo")

    val callback = mock[Function1[Foo, Unit]]
    (callback.apply _).expects(foo)

    hook.callbacks.add(callback)
    hook.notify(foo)
  }

}

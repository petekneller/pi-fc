package fc

object scratch {

  trait Address
  trait FooAddress extends Address
  trait BarAddress extends Address
  trait BazAddress extends FooAddress

  trait Controller {
    type Addr <: Address
  }

  trait FooController extends Controller {
    type Addr = FooAddress
    def foo(address: FooAddress): Seq[Byte] = ???
  }

  trait BarController extends Controller {
    type Addr = BarAddress
    def bar(address: BarAddress): Seq[Byte] = ???
  }

  trait BazController extends FooController {
    // type Addr = FooAddress
    def baz(address: FooAddress): Seq[Byte] = ???
  }

  trait Rx {
    type T
    type Ctrl <: Controller
    def receive(address: Ctrl#Addr)(controller: Ctrl): T
  }

  trait Tx {
    type T
    type Ctrl <: Controller
    def transmit(address: Ctrl#Addr, value: T)(controller: Ctrl)
  }

  trait Config extends Rx with Tx



  val stringFooRx = new Rx {
    type T = String
    type Ctrl = FooController
    def receive(address: FooAddress)(controller: FooController): String = ??? // controller.foo(address).toString
  }

  val intFooRx = new Rx {
    type T = Int
    type Ctrl = FooController
    def receive(address: FooAddress)(controller: FooController): Int = controller.foo(address).toString.toInt * 2
  }

  val stringBarRx = new Rx {
    type T = String
    type Ctrl = BarController
    def receive(address: BarAddress)(controller: BarController): String = controller.bar(address).toString
  }

  val stringBazRx = new Rx {
    type T = String
    type Ctrl = BazController
    def receive(address: FooAddress)(controller: BazController): String = controller.baz(address).toString
  }

  // val aConfig = new Config {
  //   type T = Boolean
  // }

  trait Device { self =>
    type Ctrl <: Controller
    val controller: Ctrl
    val address: Ctrl#Addr
    def receive(rx: Rx { type Ctrl >: self.Ctrl }): rx.T = rx.receive(address)(controller)
    def transmit(tx: Tx { type Ctrl >: self.Ctrl })(value: tx.T): Unit = tx.transmit(address, value)(controller)
  }

  val address1 = new FooAddress {}
  val address2 = new BarAddress {}

  val device1 = new Device {
    type Ctrl = FooController
    val address = new BazAddress {}
    val controller = new FooController {}
  }

  val device2 = new Device {
    type Ctrl = BarController
    val address = address2
    val controller = new BarController {}
  }

  val device3 = new Device {
    type Ctrl = BazController
    val address: FooAddress = new BazAddress {}
    val controller = new BazController {}
  }

  val data1 = device1.receive(stringFooRx)
  val data2 = device2.receive(stringBarRx)
  val data3a = device3.receive(stringBazRx)
  val data3b = device3.receive(stringFooRx)
  //aDevice.transmit(aConfig)(true)
}

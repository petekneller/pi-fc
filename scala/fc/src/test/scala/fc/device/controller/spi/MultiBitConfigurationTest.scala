package fc.device.controller.spi

import eu.timepit.refined.refineMV
import eu.timepit.refined.auto.autoRefineV
import eu.timepit.refined.numeric.Positive
import spire.syntax.literals._
import org.scalatest.{FlatSpec, Matchers}
import org.scalactic.TypeCheckedTripleEquals
import org.scalamock.scalatest.MockFactory
import fc.device.api._

class MultiBitConfigurationTest extends FlatSpec with Matchers with TypeCheckedTripleEquals with MockFactory {

  val device = SpiAddress(0, 0)
  implicit val mockController = stub[SpiRegisterController]
  val register = 0x35.toByte

  "receive" should "return only the specified bits" in {
    (mockController.receive _).when(*, *, refineMV[Positive](1)).returns(Right(Seq(0x03.toByte)))

    MultiBitConfiguration(register, 1, 2, TestEnum).read(device)  should === (Right(TestEnum.Three))
    MultiBitConfiguration(register, 2, 2, TestEnum).read(device)  should === (Right(TestEnum.One))
    MultiBitConfiguration(register, 3, 2, TestEnum).read(device)  should === (Right(TestEnum.Zero))
  }

  it should "return a FlagException if a values is found that does not correspond with one of the specified options" in {
    (mockController.receive _).when(*, *, refineMV[Positive](1)).returns(Right(Seq(0x02.toByte)))

    MultiBitConfiguration(register, 1, 2, TestEnum).read(device) should === (Left(FlagException(b"2", TestEnum.values)))
  }


  "transmit" should "set the correct bits" in {
    (mockController.receive _).when(*, *, refineMV[Positive](1))returns(Right(Seq(b"0")))
    (mockController.transmit _).when(*, *, *).returns(Right(()))

    MultiBitConfiguration(register, 1, 2, TestEnum).write(device, TestEnum.Three)
    (mockController.transmit _).verify(*, register, Seq(0x03.toByte))

    MultiBitConfiguration(register, 2, 2, TestEnum).write(device, TestEnum.Three)
    (mockController.transmit _).verify(*, register, Seq(0x06.toByte))

    MultiBitConfiguration(register, 3, 2, TestEnum).write(device, TestEnum.Three)
    (mockController.transmit _).verify(*, register, Seq(0x0C.toByte))
  }

  it should "not affect bits in the register outside of that defined" in {
    (mockController.receive _).when(*, *, refineMV[Positive](1))returns(Right(Seq(0xFF.toByte)))
    (mockController.transmit _).when(*, *, *).returns(Right(()))

    MultiBitConfiguration(register, 4, 3, TestEnum).write(device, TestEnum.Zero)
    (mockController.transmit _).verify(*, register, Seq(0xE3.toByte))
  }


  object TestEnum extends FlagEnumeration {
    type T = TestVal
    case class TestVal(value: Byte) extends Flag

    val Zero =  TestVal(b"0")
    val One =   TestVal(b"1")
    val Three = TestVal(b"3")

    def values = Set(Zero, One, Three)
  }
}

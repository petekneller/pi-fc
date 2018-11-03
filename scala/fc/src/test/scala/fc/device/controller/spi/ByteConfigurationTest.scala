package fc.device.controller.spi

import eu.timepit.refined.refineMV
import eu.timepit.refined.auto.autoRefineV
import eu.timepit.refined.numeric.Positive
import spire.syntax.literals._
import org.scalatest.{FlatSpec, Matchers}
import org.scalactic.TypeCheckedTripleEquals
import org.scalamock.scalatest.MockFactory
import fc.device.api._

class ByteConfigurationTest extends FlatSpec with Matchers with TypeCheckedTripleEquals with MockFactory {

  val device = SpiAddress(0, 0)
  implicit val mockController = stub[SpiController]
  val register = 0x35.toByte

  "ByteConfiguration.read" should "make a read request for that register" in {
    val configValue = 0x12.toByte
    val config = ByteConfiguration(register)

    (mockController.receive _).when(*, *, *).returns(Right(Seq(configValue)))
    config.read(device)
    (mockController.receive _).verify(*, register, refineMV[Positive](1))
  }

  it should "return the whole register value" in {
    val configValue = 0x12.toByte
    val config = ByteConfiguration(register)

    (mockController.receive _).when(*, *, *).returns(Right(Seq(configValue)))
    config.read(device) should === (Right(configValue))
  }

  "ByteConfiguration.write" should "set the whole register value" in {
    val config = ByteConfiguration(register)
    val configValue = 0x33.toByte

    config.write(device, configValue)
    (mockController.transmit _).verify(*, register, Seq(configValue))
  }

  "SingleBitFlag.read" should "return a boolean that reflects the value of the bit specified in the configuration arguments" in {
    val registerValue = 0x04.toByte
    val bit1Flag = SingleBitFlag(register, 1)
    val bit2Flag = SingleBitFlag(register, 2)

    (mockController.receive _).when(*, *, *).returns(Right(Seq(registerValue)))
    bit1Flag.read(device) should === (Right(false))
    bit2Flag.read(device) should === (Right(true))
  }

  "SingleBitFlag.write" should "not affect bits in the register outside of that defined for the configuration" in {
    val bit1Flag = SingleBitFlag(register, 1)

    (mockController.receive _).when(*, *, refineMV[Positive](1)).returns(Right(Seq(0x71.toByte)))
    (mockController.transmit _).when(*, *, *)returns(Right(Unit))
    bit1Flag.write(device, true)
    (mockController.transmit _).verify(*, register, Seq(0x73.toByte))
  }

  it should "be able to also unset bits" in {
    val bit1Flag = SingleBitFlag(register, 1)

    (mockController.receive _).when(*, *, refineMV[Positive](1)).returns(Right(Seq(0x73.toByte)))
    (mockController.transmit _).when(*, *, *)returns(Right(Unit))
    bit1Flag.write(device, false)
    (mockController.transmit _).verify(*, register, Seq(0x71.toByte))
  }

  "MultiBitFlag.receive" should "return only the specified bits" in {
    (mockController.receive _).when(*, *, refineMV[Positive](1)).returns(Right(Seq(0x03.toByte)))

    MultiBitFlag(register, 1, 2, TestEnum).read(device)  should === (Right(TestEnum.Three))
    MultiBitFlag(register, 2, 2, TestEnum).read(device)  should === (Right(TestEnum.One))
    MultiBitFlag(register, 3, 2, TestEnum).read(device)  should === (Right(TestEnum.Zero))
  }

  it should "return a FlagException if a values is found that does not correspond with one of the specified options" in {
    (mockController.receive _).when(*, *, refineMV[Positive](1)).returns(Right(Seq(0x02.toByte)))

    MultiBitFlag(register, 1, 2, TestEnum).read(device) should === (Left(FlagException(b"2", TestEnum.values)))
  }


  "MultiBitFlag.transmit" should "set the correct bits" in {
    (mockController.receive _).when(*, *, refineMV[Positive](1))returns(Right(Seq(b"0")))
    (mockController.transmit _).when(*, *, *).returns(Right(()))

    MultiBitFlag(register, 1, 2, TestEnum).write(device, TestEnum.Three)
    (mockController.transmit _).verify(*, register, Seq(0x03.toByte))

    MultiBitFlag(register, 2, 2, TestEnum).write(device, TestEnum.Three)
    (mockController.transmit _).verify(*, register, Seq(0x06.toByte))

    MultiBitFlag(register, 3, 2, TestEnum).write(device, TestEnum.Three)
    (mockController.transmit _).verify(*, register, Seq(0x0C.toByte))
  }

  it should "not affect bits in the register outside of that defined" in {
    (mockController.receive _).when(*, *, refineMV[Positive](1))returns(Right(Seq(0xFF.toByte)))
    (mockController.transmit _).when(*, *, *).returns(Right(()))

    MultiBitFlag(register, 4, 3, TestEnum).write(device, TestEnum.Zero)
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

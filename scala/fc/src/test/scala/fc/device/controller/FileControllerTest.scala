package fc.device.controller

import eu.timepit.refined.auto.autoRefineV
import eu.timepit.refined.refineMV
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive
import spire.syntax.literals._
import org.scalatest.{FlatSpec, Matchers}
import org.scalactic.TypeCheckedTripleEquals
import org.scalamock.scalatest.MockFactory
import ioctl.IOCtl.{O_RDONLY, O_WRONLY}
import ioctl.IOCtlImpl.size_t
import fc.device.api._

class FileControllerTest extends FlatSpec with Matchers with TypeCheckedTripleEquals with MockFactory {

  val mockFileApi = stub[FileApi]
  implicit val controller = new FileController(mockFileApi)
  val device = new Address { type Bus = File; def toFilename = "/address" }
  val register = "foo"
  val fd = 2

  "receive" should "open the underlying file correctly" in {
    (mockFileApi.read _).when(*, *, *).returns(new size_t)

    controller.receive(device, "register", 1) should === (Right(Seq.empty))
    (mockFileApi.open _).verify("/address/register", O_RDONLY)
  }

  it should "close the underlying file even if an error occurs during read" in {
    val result = b"1"
    val errorCause = new RuntimeException("")
    (mockFileApi.open _).when(*, *).returns(fd)
    (mockFileApi.read _).when(*, *, *).throws(errorCause)

    controller.receive(device, register, 1) should === (Left(TransferFailedException(errorCause)))
    (mockFileApi.close _).verify(fd).once()
  }

  it should "succeed if the underlying device returned less bytes than were specified" in {
    val two = b"2"
    val three = b"3"
    val desiredNumBytes = refineMV[Positive](3)
    (mockFileApi.open _).when(*, *).returns(fd)
    (mockFileApi.read _).when(*, *, *).onCall{ (_, rxBuffer, desiredNumBytes) =>
      rxBuffer.put(0, two).put(1, three)
      new size_t(2L)
    }

    controller.receive(device, register, desiredNumBytes) should === (Right(Seq(two, three)))
  }

  "transmit" should "open the underlying file correctly" in {
    (mockFileApi.write _).when(*, *, *).returns(new size_t(1L))

    controller.transmit(device, "register", Seq(b"1")) should === (Right(()))
    (mockFileApi.open _).verify("/address/register", O_WRONLY)
  }

  it should "close the underlying file even if an error occurs during write" in {
    val fd = 3
    val errorCause = new RuntimeException("")
    (mockFileApi.open _).when(*, *).returns(fd)
    (mockFileApi.write _).when(*, *, *).throws(errorCause)

    controller.transmit(device, register, Seq(b"1")) should === (Left(TransferFailedException(errorCause)))
    (mockFileApi.close _).verify(fd).once()
  }

  it should "return an error if less bytes were written than specified" in {
    val desiredNumBytes = 3
    val numBytesWritten = 2
    (mockFileApi.write _).when(*, *, *).returns(new size_t(numBytesWritten.toLong))

    controller.transmit(device, register, Seq(b"1", b"2", b"3")) should === (Left(IncompleteDataException(desiredNumBytes, numBytesWritten)))
  }

}

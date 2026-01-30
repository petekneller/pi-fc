package fc.device.controller.filesystem

import eu.timepit.refined.auto.autoRefineV
import eu.timepit.refined.refineMV
import eu.timepit.refined.numeric.Positive
import spire.syntax.literals._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalactic.TypeCheckedTripleEquals
import org.scalamock.scalatest.MockFactory
import ioctl.IOCtl.{O_RDONLY, O_WRONLY}
import ioctl.IOCtlImpl.size_t
import fc.device.api._

class FileSystemControllerTest extends AnyFlatSpec with Matchers with TypeCheckedTripleEquals with MockFactory {

  val mockApi = stub[FileApi]
  val controller = new FileSystemControllerImpl(mockApi)
  val device = new FileSystemAddress { def toFilename = "/address" }
  val register = "foo"
  val fd = 2

  "receive" should "open the underlying file correctly" in {
    (mockApi.read _).when(*, *, *).returns(new size_t)

    controller.receive(device, "register", 1) should === (Right(Seq.empty))
    (mockApi.open _).verify("/address/register", O_RDONLY)
  }

  it should "close the underlying file even if an error occurs during read" in {
    (mockApi.open _).when(*, *).returns(fd)
    val errorCause = new RuntimeException("")
    (mockApi.read _).when(*, *, *).throws(errorCause)

    controller.receive(device, register, 1) should === (Left(TransferFailedException(errorCause)))
    (mockApi.close _).verify(fd).once()
  }

  it should "succeed if the underlying device returned less bytes than were specified" in {
    val two = b"2"
    val three = b"3"
    val desiredNumBytes = refineMV[Positive](3)
    (mockApi.open _).when(*, *).returns(fd)
    (mockApi.read _).when(*, *, *).onCall{ (_, rxBuffer, desiredNumBytes) =>
      rxBuffer.put(0, two).put(1, three)
      new size_t(2L)
    }

    controller.receive(device, register, desiredNumBytes) should === (Right(Seq(two, three)))
  }

  "transmit" should "open the underlying file correctly" in {
    (mockApi.write _).when(*, *, *).returns(new size_t(1L))

    controller.transmit(device, "register", Seq(b"1")) should === (Right(()))
    (mockApi.open _).verify("/address/register", O_WRONLY)
  }

  it should "close the underlying file even if an error occurs during write" in {
    (mockApi.open _).when(*, *).returns(fd)
    val errorCause = new RuntimeException("")
    (mockApi.write _).when(*, *, *).throws(errorCause)

    controller.transmit(device, register, Seq(b"1")) should === (Left(TransferFailedException(errorCause)))
    (mockApi.close _).verify(fd).once()
  }

  it should "return an error if less bytes were written than specified" in {
    val desiredNumBytes = 3
    val numBytesWritten = 2
    (mockApi.write _).when(*, *, *).returns(new size_t(numBytesWritten.toLong))

    controller.transmit(device, register, Seq(b"1", b"2", b"3")) should === (Left(IncompleteDataException(desiredNumBytes, numBytesWritten)))
  }

}

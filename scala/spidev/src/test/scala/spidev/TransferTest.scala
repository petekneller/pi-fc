package spidev

import java.nio.ByteBuffer
import java.io.File
import org.scalatest.{FlatSpec, Matchers}
import org.scalactic.TypeCheckedTripleEquals
import ioctl.IOCtl.{open, close, O_RDONLY}
import Spidev._

class TransferTest extends FlatSpec with Matchers with TypeCheckedTripleEquals {

  "transfer" should "succeed in fetching the WHOAMI from the MPU9250 on the navio2" in {
    assert(new File("/dev/spidev0.1").exists)
    val fd = open("/dev/spidev0.1", O_RDONLY)

    val tx = ByteBuffer.allocateDirect(2)
    val rx = ByteBuffer.allocateDirect(2)

    val readFlag = 0x8.toByte
    val whoamiAddress = 0x76.toByte
    tx.put(0, (readFlag | whoamiAddress).toByte)
    val bytesTransferred = transfer(fd, tx, rx, 2, 100000)
    close(fd)

    bytesTransferred should === (2)
    rx.get(1) should === (113.toByte)
  }

  it should "fail if either buffer is not a 'direct buffer'" in {
    intercept[SpiTransferException] {
      transfer(0, ByteBuffer.allocate(1), ByteBuffer.allocateDirect(1), 1, 100000)
    }

    intercept[SpiTransferException] {
      transfer(0, ByteBuffer.allocateDirect(1), ByteBuffer.allocate(1), 1, 100000)
    }
  }

  it should "fail if the specified length of transfer exceeds the 'limit' of either buffer" in {
    intercept[SpiTransferException]{
      transfer(0, ByteBuffer.allocateDirect(1), ByteBuffer.allocateDirect(2), 2, 100000)
    }

    intercept[SpiTransferException]{
      transfer(0, ByteBuffer.allocateDirect(2), ByteBuffer.allocateDirect(1), 2, 100000)
    }
  }
}

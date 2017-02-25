package spidev

import java.nio.ByteBuffer
import java.io.File
import org.scalatest.{FlatSpec, Matchers}
import org.scalactic.TypeCheckedTripleEquals
import ioctl.IOCtl.{open, close, O_RDONLY}
import Spidev._

class TransferTest extends FlatSpec with Matchers with TypeCheckedTripleEquals {

  "SPI_MSGSIZE" should "return sizes in multiples of 32 bit" in {
    SPI_MSGSIZE(0) should === (0)
    SPI_MSGSIZE(1) should === (32)
    SPI_MSGSIZE(2) should === (64)
  }

  "SPI_IOC_MESSAGE" should "return values equivalent to the macros in spidev.h" in {
    hex(SPI_IOC_MESSAGE(1)) should === ("0x40206b00")
    hex(SPI_IOC_MESSAGE(2)) should === ("0x40406b00")
    hex(SPI_IOC_MESSAGE(3)) should === ("0x40606b00")

    def hex(i: Int): String = "0x%8x".format(i)
  }

  "transfer" should "succeed in fetching the WHOAMI from the MPU9250 on the navio2" in {
    assert(new File("/dev/spidev0.1").exists)
    val fd = open("/dev/spidev0.1", O_RDONLY)

    val tx = ByteBuffer.allocateDirect(2)
    val rx = ByteBuffer.allocateDirect(2)

    val readFlag = 0x80.toByte
    val whoamiAddress = 0x75.toByte
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

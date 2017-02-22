package spidev

import org.scalatest.{FlatSpec, Matchers}
import org.scalactic.TypeCheckedTripleEquals
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder.LITTLE_ENDIAN
import com.sun.jna.{Native, NativeLong}
import ioctl._
import ioctl.IOCtl._
import Spidev._

class IOCtlTest extends FlatSpec with Matchers with TypeCheckedTripleEquals {

  "ioctl" should "support get/set of SPI word size" in {
    assert(new File("/dev/spidev0.0").exists)

    val fd = open("/dev/spidev0.0", O_RDWR)
    val data1 = ByteBuffer.allocate(Native.getNativeSize(classOf[Byte]))
    data1.order(LITTLE_ENDIAN)
    ioctl(fd, new NativeLong(SPI_IOC_RD_BITS_PER_WORD.unsigned), data1)
    data1.get should === (8.toByte)

    val data2 = ByteBuffer.allocate(Native.getNativeSize(classOf[Byte]))
    data2.order(LITTLE_ENDIAN)
    data2.put(16.toByte)
    ioctl(fd, new NativeLong(SPI_IOC_WR_BITS_PER_WORD.unsigned), data2)

    val data3 = ByteBuffer.allocate(Native.getNativeSize(classOf[Byte]))
    data3.order(LITTLE_ENDIAN)
    ioctl(fd, new NativeLong(SPI_IOC_RD_BITS_PER_WORD.unsigned), data3)
    data3.get should === (16.toByte)

    ioctl(fd, new NativeLong(SPI_IOC_WR_BITS_PER_WORD.unsigned), data1)
  }

}

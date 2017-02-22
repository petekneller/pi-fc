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

  "ioctl" should "support get/set of SPI mode" in {
    assert(new File("/dev/spidev0.0").exists)

    val fd = open("/dev/spidev0.0", O_RDWR)
    val data1 = ByteBuffer.allocate(Native.getNativeSize(classOf[Byte]))
    data1.order(LITTLE_ENDIAN)
    ioctl(fd, new NativeLong(SPI_IOC_RD_MODE.unsigned), data1)

    val existingMode = data1.get
    existingMode should === (SPI_MODE_0)

    val data2 = ByteBuffer.allocate(Native.getNativeSize(classOf[Byte]))
    data2.order(LITTLE_ENDIAN)
    data2.put(SPI_MODE_2)
    ioctl(fd, new NativeLong(SPI_IOC_WR_MODE.unsigned), data2)

    val data3 = ByteBuffer.allocate(Native.getNativeSize(classOf[Byte]))
    data3.order(LITTLE_ENDIAN)
    ioctl(fd, new NativeLong(SPI_IOC_RD_MODE.unsigned), data3)
    data3.get should === (SPI_MODE_2)

    ioctl(fd, new NativeLong(SPI_IOC_WR_MODE.unsigned), data1)
  }

}

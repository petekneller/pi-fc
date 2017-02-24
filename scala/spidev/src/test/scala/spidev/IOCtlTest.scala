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

  "ioctl" should "support fetch of SPI mode (8 bit value)" in {
    fetchCommandValue(SPI_IOC_RD_MODE, classOf[Byte]).get should === (SPI_MODE_0)
  }

  it should "support fetch of SPI mode (32 bit value)" in {
    fetchCommandValue(SPI_IOC_RD_MODE32, classOf[Int]).asIntBuffer.get should === (0)
  }

  it should "support fetch of SPI bit justification" in {
    fetchCommandValue(SPI_IOC_RD_LSB_FIRST, classOf[Byte]).get should === (0.toByte)
  }

  it should "support fetch of SPI word size" in {
    fetchCommandValue(SPI_IOC_RD_BITS_PER_WORD, classOf[Byte]).get should === (8.toByte)
  }

  it should "support get/set of SPI clock speed" in {
    assert(new File(device0).exists)

    val fd = open(device0, O_RDWR)
    val data = ByteBuffer.allocate(Native.getNativeSize(classOf[Int]))
    data.order(LITTLE_ENDIAN)
    ioctl(fd, new NativeLong(SPI_IOC_RD_MAX_SPEED_HZ.unsigned), data)
    val originalSpeed = data.asIntBuffer.get
    originalSpeed should be > (0)

    data.clear()
    data.asIntBuffer.put(0, 250000)
    ioctl(fd, new NativeLong(SPI_IOC_WR_MAX_SPEED_HZ.unsigned), data)

    data.clear()
    data.asIntBuffer.put(0, 0)
    ioctl(fd, new NativeLong(SPI_IOC_RD_MAX_SPEED_HZ.unsigned), data)
    data.asIntBuffer.get should === (250000)

    data.clear()
    data.asIntBuffer.put(0, originalSpeed)
    ioctl(fd, new NativeLong(SPI_IOC_WR_MAX_SPEED_HZ.unsigned), data)
  }


  def fetchCommandValue[A](command: Int, argumentSize: Class[A]): ByteBuffer = {
    assert(new File(device0).exists)

    val fd = open(device0, O_RDONLY)
    try {
      val data = ByteBuffer.allocate(Native.getNativeSize(argumentSize))
      data.order(LITTLE_ENDIAN)
      ioctl(fd, new NativeLong(command.unsigned), data)
      data
    } finally {
      close(fd)
    }
  }

  val device0 = "/dev/spidev0.0"

}

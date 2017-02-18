package ioctl

import org.scalatest.{FlatSpec, Matchers}
import org.scalactic.TypeCheckedTripleEquals
import com.sun.jna.{NativeLong, Native}
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder.LITTLE_ENDIAN

class IOCtlTest extends FlatSpec with Matchers with TypeCheckedTripleEquals {

  "ioctl" should "succeed with FS_IOC_GETVERSION on a valid file" in {
    val tempfile = File.createTempFile("aaaa", "bbbb").getCanonicalPath
    val fd = IOCtl.open(tempfile, IOCtl.O_RDONLY)

    val FS_IOC_GETVERSION = 0x80087601L

    val bb = ByteBuffer.allocate(Native.getNativeSize(classOf[Int]))
    bb.order(LITTLE_ENDIAN)
    IOCtl.ioctl(fd, new NativeLong(FS_IOC_GETVERSION), bb) should === (0)

    val version = bb.asIntBuffer.get.unsigned
    version should be >(0L)
  }

}

package ioctl

import org.scalatest.{FlatSpec, Matchers}
import com.sun.jna.{LastErrorException, NativeLong}
import java.io.File

class IOCtlTest extends FlatSpec with Matchers {

  "ioctl" should "succeed with FS_IOC_GETVERSION on a valid file" in {
    val tempfile = File.createTempFile("aaaa", "bbbb").getCanonicalPath
    val fd = IOCtl.open(tempfile, IOCtl.O_RDONLY)
    val zero = 0.toByte
    val data = Array(zero, zero, zero, zero, zero, zero, zero, zero)
    val FS_IOC_GETVERSION = 0x80087601
    IOCtl.ioctl(fd, new NativeLong(FS_IOC_GETVERSION), data) should === (0)
    data should be(0)
  }

}

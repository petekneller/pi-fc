package ioctl

import org.scalatest.{FlatSpec, Matchers}
import org.scalactic.TypeCheckedTripleEquals
import com.sun.jna.LastErrorException
import java.io.File

class OpenCloseTest extends FlatSpec with Matchers with TypeCheckedTripleEquals {

  "open" should "throw error upon specifying a non-existent file" in {
    intercept[LastErrorException] {
      IOCtl.open("/tmp/doesnotexist", IOCtl.O_RDWR)
    }

    IOCtl.errno() should === (IOCtl.ENOENT)
  }

  "close" should "throw error upon specifying a non-open file" in {
    intercept[LastErrorException] {
      IOCtl.close(999)
    }

    IOCtl.errno() should === (IOCtl.EBADF)
  }

  it should "succeed when specifying an open file" in {
    val tempfile = File.createTempFile("aaaa", "bbbb").getCanonicalPath
    val fd = IOCtl.open(tempfile, IOCtl.O_RDWR)
    IOCtl.close(fd) should === (0)
    IOCtl.errno() should === (0)
  }
}

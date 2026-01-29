package ioctl

import org.scalatest.{FlatSpec, Matchers}
import org.scalactic.TypeCheckedTripleEquals
import com.sun.jna.{NativeLong, Platform}
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder.LITTLE_ENDIAN
import macros.IOR
import IOCtl._

class IOCtlTest extends FlatSpec with Matchers with TypeCheckedTripleEquals {

  "ioctl" should "succeed with FS_IOC_GETFLAGS on a valid file" in {
    val tempfile = File.createTempFile("aaaa", null, new File("target")).getCanonicalPath
    val fd = open(tempfile, O_RDONLY)

    // The standard/64bit version of this command is required on my dev laptop,
    // the 32bit version is req'd on the RPi
    val FS_IOC_GETFLAGS = IOR('f'.toByte, 1.toByte, 8) // 0x80086601 or 2148034049
    val FS_IOC32_GETFLAGS = IOR('f'.toByte, 1.toByte, 4) // 0x80046601 or 2147771905
    val op = if (Platform.is64Bit()) FS_IOC_GETFLAGS else FS_IOC32_GETFLAGS

    val bb = ByteBuffer.allocate(8)
    bb.order(LITTLE_ENDIAN)
    ioctl(fd, new NativeLong(op), bb) should === (0)
    close(fd)

    val version = bb.asLongBuffer.get
    version should be >(0L)
  }

}

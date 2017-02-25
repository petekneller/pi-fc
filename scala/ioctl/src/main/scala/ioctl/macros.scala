package ioctl

import com.sun.jna.Native
import syntax._

object macros {

  def IOR[A](`type`: Byte, nr: Byte, dataType: Class[A]): Int = IOR(`type`, nr, Native.getNativeSize(dataType))

  def IOR(`type`: Byte, nr: Byte, dataSize: Int): Int = IOC(IOC_READ, `type`, nr, dataSize)

  def IOW[A](`type`: Byte, nr: Byte, dataType: Class[A]): Int = IOW(`type`, nr, Native.getNativeSize(dataType))

  def IOW(`type`: Byte, nr: Byte, dataSize: Int): Int = IOC(IOC_WRITE, `type`, nr, dataSize)

  val IOC_NONE: Byte = 0
  val IOC_WRITE: Byte = 1
  val IOC_READ: Byte = 2

  def IOC[A](dir: Byte, `type`: Byte, nr: Byte, dataSize: Int): Int =
    (dir.unsigned << IOC_DIRSHIFT) |
    (IOC_SIZED(dataSize).unsigned << IOC_SIZESHIFT) |
    (`type`.unsigned << IOC_TYPESHIFT) |
    (nr.unsigned << IOC_NRSHIFT)

  // the masking of the 2 highest bits is necessary to prevent overlap with the 'direction' field
  def IOC_SIZED[A](dataSize: Int): Short = (dataSize & 0x3FFF).toShort

  val IOC_NRBITS = 8
  val IOC_TYPEBITS = 8
  val IOC_SIZEBITS = 14
  val IOC_DIRBITS = 2

  val IOC_NRSHIFT = 0
  val IOC_TYPESHIFT = IOC_NRSHIFT + IOC_NRBITS
  val IOC_SIZESHIFT = IOC_TYPESHIFT + IOC_TYPEBITS
  val IOC_DIRSHIFT = IOC_SIZESHIFT + IOC_SIZEBITS

}

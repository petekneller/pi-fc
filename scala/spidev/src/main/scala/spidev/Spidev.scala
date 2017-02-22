package spidev

import ioctl.macros._

object Spidev {

  /* User space versions of kernel symbols for SPI clocking modes,
   * matching <linux/spi/spi.h>
   */

  val SPI_CPHA: Byte =      0x01
  val SPI_CPOL: Byte =      0x02

  val SPI_MODE_0: Byte =    (0|0)
  val SPI_MODE_1: Byte =    (0|SPI_CPHA).toByte
  val SPI_MODE_2: Byte =    (SPI_CPOL|0).toByte
  val SPI_MODE_3: Byte =    (SPI_CPOL|SPI_CPHA).toByte

  val SPI_CS_HIGH: Byte =   0x04
  val SPI_LSB_FIRST: Byte = 0x08
  val SPI_3WIRE: Byte =     0x10
  val SPI_LOOP: Byte =      0x20
  val SPI_NO_CS: Byte =     0x40
  val SPI_READY: Byte =     0x80.toByte
  val SPI_TX_DUAL: Byte =   0x100.toByte
  val SPI_TX_QUAD: Byte =   0x200.toByte
  val SPI_RX_DUAL: Byte =   0x400.toByte
  val SPI_RX_QUAD: Byte =   0x800.toByte

  val SPI_IOC_MAGIC: Byte = 'k'

  /* Read / Write of SPI mode (SPI_MODE_0..SPI_MODE_3) (limited to 8 bits) */
  val SPI_IOC_RD_MODE = IOR(SPI_IOC_MAGIC, 1, classOf[Byte])
  val SPI_IOC_WR_MODE = IOW(SPI_IOC_MAGIC, 1, classOf[Byte])

  /* Read / Write SPI bit justification */
  val SPI_IOC_RD_LSB_FIRST = IOR(SPI_IOC_MAGIC, 2, classOf[Byte])
  val SPI_IOC_WR_LSB_FIRST = IOW(SPI_IOC_MAGIC, 2, classOf[Byte])

  /* Read / Write SPI device word length (1..N) */
  val SPI_IOC_RD_BITS_PER_WORD = IOR(SPI_IOC_MAGIC, 3, classOf[Byte])
  val SPI_IOC_WR_BITS_PER_WORD = IOW(SPI_IOC_MAGIC, 3, classOf[Byte])

  /* Read / Write SPI device default max speed hz */
  val SPI_IOC_RD_MAX_SPEED_HZ = IOR(SPI_IOC_MAGIC, 4, classOf[Int])
  val SPI_IOC_WR_MAX_SPEED_HZ = IOW(SPI_IOC_MAGIC, 4, classOf[Int])

  /* Read / Write of the SPI mode field */
  val SPI_IOC_RD_MODE32 = IOR(SPI_IOC_MAGIC, 5, classOf[Int])
  val SPI_IOC_WR_MODE32 = IOW(SPI_IOC_MAGIC, 5, classOf[Int])
}

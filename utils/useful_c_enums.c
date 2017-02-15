#include <stdio.h>
#include <fcntl.h>
#include <linux/fs.h>
#include <linux/spi/spidev.h>

int main(int argc, char* argv) {

  struct pair {
    char* name;
    int value;
  };

  struct pair enums[] = {
    // fcntl.h
    { "O_RDONLY", O_RDONLY },
    { "O_RDWR", O_RDWR },
    // linux/fs.h
    { "BLKGETSIZE64", BLKGETSIZE64 },
    // spi/spidev.h
    { "SPI_IOC_RD_MODE", SPI_IOC_RD_MODE },
    { "SPI_IOC_WR_MODE", SPI_IOC_WR_MODE },
    { "SPI_IOC_RD_MODE32", SPI_IOC_RD_MODE32 },
    { "SPI_IOC_WR_MODE32", SPI_IOC_WR_MODE32 },
    { "SPI_IOC_RD_LSB_FIRST", SPI_IOC_RD_LSB_FIRST },
    { "SPI_IOC_WR_LSB_FIRST", SPI_IOC_WR_LSB_FIRST },
    { "SPI_IOC_RD_BITS_PER_WORD", SPI_IOC_RD_BITS_PER_WORD },
    { "SPI_IOC_WR_BITS_PER_WORD", SPI_IOC_WR_BITS_PER_WORD },
    { "SPI_IOC_RD_MAX_SPEED_HZ", SPI_IOC_RD_MAX_SPEED_HZ },
    { "SPI_IOC_WR_MAX_SPEED_HZ", SPI_IOC_WR_MAX_SPEED_HZ },
    { "SPI_IOC_MESSAGE(1)", SPI_IOC_MESSAGE(1) },
    { "SPI_IOC_MESSAGE(2)", SPI_IOC_MESSAGE(2) },
    { "SPI_IOC_MESSAGE(3)", SPI_IOC_MESSAGE(3) }
  };

  int i;
  int len = sizeof(enums) / sizeof(struct pair);
  for(i = 0; i < len; i++)
  {
    int value = enums[i].value;
    printf("%s = 0x%x (%u) \n", enums[i].name, value, value);
  }

  return 0;
}

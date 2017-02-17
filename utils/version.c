#include <stdio.h>
#include <fcntl.h>
#include <sys/ioctl.h>
#include <linux/fs.h>

int main(int argc, char* argv)
{
  int fd = open("/tmp/aaaa6318914674647288465bbbb", O_RDONLY);
  int data = 0;
  ioctl(fd, FS_IOC_GETVERSION, &data);
  printf("%x \n", data);
  printf("%x %x %x %x \n", ((char*)&data)[0], ((char*)&data)[1], ((char*)&data)[2], ((char*)&data)[3]);
  return 0;
}

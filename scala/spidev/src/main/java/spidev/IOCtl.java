package spidev;

import com.sun.jna.NativeLong;

public class IOCtl {

    public native int ioctl(int fd, NativeLong request, SpiIocTransfer data);
}

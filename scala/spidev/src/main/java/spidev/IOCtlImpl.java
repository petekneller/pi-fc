package spidev;

import com.sun.jna.NativeLong;
import com.sun.jna.LastErrorException;

public class IOCtlImpl {

    public native int ioctl(int fd, NativeLong request, SpiIocTransfer data) throws LastErrorException;
}

package ioctl;

import com.sun.jna.*;
import java.nio.ByteBuffer;

public class IOCtl {
    public int O_RDONLY = 0x0;

    public int O_WRONLY = 0x1;

    public int O_RDWR = 0x2;

    public int O_APPEND = 0x2000;

    public native int open(String path, int flags) throws LastErrorException;

    public native int close(int fd) throws LastErrorException;

    public int errno() {
        return Native.getLastError();
    }

    // Errno codes

    public int ENOENT = 2;

    public int EBADF = 9;

    public int EACCES = 13;

    public native int ioctl(int fd, NativeLong request, ByteBuffer data) throws LastErrorException;
}

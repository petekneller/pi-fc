package ioctl;

import com.sun.jna.*;
import java.nio.ByteBuffer;

public class IOCtl {
    public int O_RDONLY = 0;

    public int O_RDWR = 2;

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

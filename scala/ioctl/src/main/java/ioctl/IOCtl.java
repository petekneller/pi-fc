package ioctl;

import com.sun.jna.*;

public class IOCtl {
    static {
        Native.register("c");
        Native.setPreserveLastError(true);
    }

    public static int O_RDWR = 2;

    public static native int open(String path, int flags) throws LastErrorException;

    public static native int close(int fd) throws LastErrorException;

    public static int errno() {
        return Native.getLastError();
    }

    // Errno codes

    public static int ENOENT = 2;

    public static int EBADF = 9;

    public static int EACCES = 13;
}

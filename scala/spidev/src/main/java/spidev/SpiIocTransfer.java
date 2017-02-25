package spidev;

import java.util.List;
import java.util.Arrays;
import java.nio.ByteBuffer;
import com.sun.jna.Structure;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

public class SpiIocTransfer extends Structure {
    public long tx_buf;
    public long rx_buf;

    public int len;
    public int speed_hz;

    public short delay_usecs;
    public byte bits_per_word;
    public byte cs_change;
    public byte tx_nbits;
    public byte rx_nbits;
    public short pad;

    @Override
    protected List getFieldOrder() {
        return Arrays.asList(
                             "tx_buf",
                             "rx_buf",
                             "len",
                             "speed_hz",
                             "delay_usecs",
                             "bits_per_word",
                             "cs_change",
                             "tx_nbits",
                             "rx_nbits",
                             "pad"
        );
    }

    public static class ByValue extends SpiIocTransfer implements Structure.ByValue {}
}

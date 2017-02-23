package spidev;

import java.util.List;
import java.util.Arrays;
import java.nio.ByteBuffer;
import com.sun.jna.Structure;
import com.sun.jna.NativeLong;

public class SpiIocTransfer extends Structure {
    ByteBuffer tx_buf;
    ByteBuffer rx_buf;

    int len;
    int speed_hz;

    short delay_usecs;
    byte bits_per_word;
    byte cs_change;
    byte tx_nbits;
    byte rx_nbits;
    short pad;

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
}

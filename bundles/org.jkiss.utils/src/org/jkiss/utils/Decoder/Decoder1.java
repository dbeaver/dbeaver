package org.jkiss.utils.Decoder;

import org.jkiss.code.Decoder;

public class Decoder1 implements Decoder {
    @Override
    public int decode(int[] decodedBytes, byte[] obuf, int wp) {
        obuf[wp] = (byte) (decodedBytes[0] << 2 & 0xfc | decodedBytes[1] >> 4 & 0x3);
        return 1;
    }
}

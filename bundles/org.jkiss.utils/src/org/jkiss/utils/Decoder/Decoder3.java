package org.jkiss.utils.Decoder;

import org.jkiss.code.Decoder;

public class Decoder3 implements Decoder {
    @Override
    public int decode(int[] decodedBytes, byte[] obuf, int wp) {
        obuf[wp++] = (byte) (decodedBytes[0] << 2 & 0xfc | decodedBytes[1] >> 4 & 0x3);
        obuf[wp++] = (byte) (decodedBytes[1] << 4 & 0xf0 | decodedBytes[2] >> 2 & 0xf);
        obuf[wp] = (byte) (decodedBytes[2] << 6 & 0xc0 | decodedBytes[3] & 0x3f);
        return 3;
    }
}

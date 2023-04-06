package org.jkiss.code;

public interface Decoder {
    int decode(int[] decodedBytes, byte[] obuf, int wp);
}

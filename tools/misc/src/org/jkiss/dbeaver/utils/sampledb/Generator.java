/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.utils.sampledb;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public class Generator {
    private static final Map<byte[], byte[]> cache = new IdentityHashMap<byte[], byte[]>();

    public static byte[] generate(byte[] src)
    {
        byte[] generated = cache.get(src);
        if (generated == null) {
            synchronized (cache) {
                generated = cache.get(src);
                if (generated == null) {
                    generated = doGenerate(src);
                    cache.put(src, generated);
                }
            }
        }
        return generated;
    }

    private static byte[] doGenerate(byte[] src)
    {
        return src;
    }

    public static void main(String[] args)
    {
        byte[] src1 = new byte[] {1, 2, 3};
        byte[] src2 = new byte[] {1, 2, 3};
        final byte[] res1 = generate(src1);
        final byte[] res2 = generate(src2);
        System.out.println(res1.equals(res2));
    }

}
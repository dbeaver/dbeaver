/*
 * Copyright (C) 2010-2014 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.utils.sampledb;

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
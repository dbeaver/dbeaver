/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.utils;

import org.jkiss.code.NotNull;
import org.jkiss.junit.DBeaverUnitTest;
import org.jkiss.utils.io.BOMInputStream;
import org.jkiss.utils.io.ByteOrderMark;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;

// http://www.ltg.ed.ac.uk/~richard/utf-8.cgi
public class BOMInputStreamTest extends DBeaverUnitTest {
    @Test
    public void testUtf8WithoutBom() throws IOException {
        final BOMInputStream is = input(ByteOrderMark.UTF_8, 0xF0, 0x9F, 0x94, 0xA5);
        final BufferedReader rd = reader(ByteOrderMark.UTF_8, is);
        Assert.assertFalse(is.hasBOM());
        Assert.assertEquals("\uD83D\uDD25", rd.readLine());
    }

    @Test
    public void testUtf8WithBom() throws IOException {
        final BOMInputStream is = input(ByteOrderMark.UTF_8, 0xEF, 0xBB, 0xBF, 0xF0, 0x9F, 0x94, 0xA5);
        final BufferedReader rd = reader(ByteOrderMark.UTF_8, is);
        Assert.assertTrue(is.hasBOM());
        Assert.assertEquals(ByteOrderMark.UTF_8, is.getBOM());
        Assert.assertEquals("\uD83D\uDD25", rd.readLine());
    }

    @NotNull
    private static BOMInputStream input(@NotNull ByteOrderMark bom, int... bytes) {
        final byte[] b = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] > 0xFF) {
                throw new IllegalArgumentException("Byte cannot fit");
            }
            b[i] = (byte) (bytes[i] & 0xff);
        }
        return new BOMInputStream(new ByteArrayInputStream(b), bom);
    }

    @NotNull
    private static BufferedReader reader(@NotNull ByteOrderMark bom, @NotNull InputStream is) throws IOException {
        return new BufferedReader(new InputStreamReader(is, bom.getCharsetName()));
    }
}

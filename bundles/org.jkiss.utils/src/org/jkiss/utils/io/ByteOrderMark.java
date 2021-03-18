/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.utils.io;

import org.jkiss.code.NotNull;

import java.nio.charset.Charset;
import java.util.Arrays;

public enum ByteOrderMark implements Comparable<ByteOrderMark> {
    /**
     * UTF-8 BOM
     */
    UTF_8("UTF-8", new int[]{0xEF, 0xBB, 0xBF}),

    /**
     * UTF-16BE BOM (Big-Endian)
     */
    UTF_16BE("UTF-16BE", new int[]{0xFE, 0xFF}),

    /**
     * UTF-16LE BOM (Little-Endian)
     */
    UTF_16LE("UTF-16LE", new int[]{0xFF, 0xFE}),

    /**
     * UTF-32BE BOM (Big-Endian)
     */
    UTF_32BE("UTF-32BE", new int[]{0x00, 0x00, 0xFE, 0xFF}),

    /**
     * UTF-32LE BOM (Little-Endian)
     */
    UTF_32LE("UTF-32LE", new int[]{0xFF, 0xFE, 0x00, 0x00});

    private final String charsetName;
    private final int[] bytes;

    ByteOrderMark(@NotNull String charsetName, @NotNull int[] bytes) {
        this.charsetName = charsetName;
        this.bytes = bytes;
    }

    @NotNull
    public String getCharsetName() {
        return charsetName;
    }

    @NotNull
    public int[] getBytes() {
        return Arrays.copyOf(bytes, bytes.length);
    }

    public int get(int position) {
        return bytes[position];
    }

    public int length() {
        return bytes.length;
    }

    @NotNull
    public static ByteOrderMark fromCharset(@NotNull Charset charset) {
        for (ByteOrderMark bom : values()) {
            if (charset.name().equals(bom.getCharsetName())) {
                return bom;
            }
        }
        throw new IllegalArgumentException("Can't find BOM for charset " + charset.name());
    }
}

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
import org.jkiss.code.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * This class wraps a stream that includes an
 * encoded {@link ByteOrderMark} as its first bytes.
 * <p>
 * This class detects these bytes and skips them and
 * return the subsequent byte as the first byte in the stream.
 * <p>
 * This class is highly inspired by the <code>BOMInputStream</code>
 * class from the Apache Commons library.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Byte_order_mark">Wikipedia - Byte Order Mark</a>
 */
public class BOMInputStream extends InputStream {
    private static final Comparator<ByteOrderMark> BOM_LENGTH_COMPARATOR = Comparator.comparing(ByteOrderMark::length).reversed();

    private final InputStream in;
    private final List<ByteOrderMark> boms;
    private ByteOrderMark bom;
    private int[] firstBytes;
    private int fbLength;
    private int fbIndex;
    private int markFbIndex;
    private boolean markedAtStart;

    public BOMInputStream(@NotNull InputStream delegate, @NotNull ByteOrderMark... boms) {
        if (boms.length == 0) {
            throw new IllegalArgumentException("No BOMs specified");
        }

        this.in = delegate;
        this.boms = Arrays.asList(boms);
        this.boms.sort(BOM_LENGTH_COMPARATOR);
    }

    public BOMInputStream(@NotNull InputStream delegate, @NotNull Charset charset) {
        this(delegate, ByteOrderMark.fromCharset(charset));
    }

    public BOMInputStream(@NotNull InputStream delegate) {
        this(delegate, ByteOrderMark.UTF_8);
    }

    @Override
    public int read() throws IOException {
        getBOM();
        return fbIndex < fbLength ? firstBytes[fbIndex++] : in.read();
    }

    @Override
    public synchronized void mark(int limit) {
        markFbIndex = fbIndex;
        markedAtStart = firstBytes == null;
        in.mark(limit);
    }

    @Override
    public synchronized void reset() throws IOException {
        fbIndex = markFbIndex;
        if (markedAtStart) {
            firstBytes = null;
        }
        in.reset();
    }

    @Nullable
    public ByteOrderMark getBOM() throws IOException {
        if (firstBytes == null) {
            fbLength = 0;
            firstBytes = new int[boms.get(0).length()];
            for (int i = 0; i < firstBytes.length; i++) {
                firstBytes[i] = in.read();
                fbLength++;
                if (firstBytes[i] < 0) {
                    break;
                }
            }
            bom = find();
            if (bom != null) {
                if (bom.length() < firstBytes.length) {
                    fbIndex = bom.length();
                } else {
                    fbLength = 0;
                }
            }
        }
        return bom;
    }

    public boolean hasBOM() throws IOException {
        return getBOM() != null;
    }

    public boolean hasBOM(@NotNull ByteOrderMark bom) throws IOException {
        if (!boms.contains(bom)) {
            throw new IllegalArgumentException("Stream is not configured to detect " + bom);
        }
        getBOM();
        return this.bom != null && this.bom.equals(bom);
    }

    @Nullable
    private ByteOrderMark find() {
        for (ByteOrderMark bom : boms) {
            if (matches(bom)) {
                return bom;
            }
        }
        return null;
    }

    private boolean matches(@NotNull ByteOrderMark bom) {
        for (int i = 0; i < bom.length(); i++) {
            if (bom.get(i) != firstBytes[i]) {
                return false;
            }
        }
        return true;
    }
}

/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.ai;

import org.jkiss.code.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;

public record AIImageDimensions(int width, int height) {
    @NotNull
    public static AIImageDimensions read(@NotNull byte[] bytes) throws IOException {
        if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
            && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return readWebp(bytes);
        }
        // read metadata only: decoded image buffers can be much larger than the file
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                try {
                    reader.setInput(input, true, true);
                    return dimensions(reader.getWidth(0), reader.getHeight(0));
                } finally {
                    reader.dispose();
                }
            }
        }
        throw new IOException("Cannot read image dimensions.");
    }

    @NotNull
    private static AIImageDimensions readWebp(@NotNull byte[] bytes) throws IOException {
        int offset = 12;
        while (offset <= bytes.length - 8) {
            long length = readLittleEndian(bytes, offset + 4, 4);
            int payload = offset + 8;
            if (length > bytes.length - payload) {
                break;
            }
            if (bytes[offset] == 'V' && bytes[offset + 1] == 'P' && bytes[offset + 2] == '8') {
                if (bytes[offset + 3] == 'X' && length >= 10) {
                    return dimensions(1 + (int) readLittleEndian(bytes, payload + 4, 3),
                        1 + (int) readLittleEndian(bytes, payload + 7, 3));
                }
                if (bytes[offset + 3] == 'L' && length >= 5 && bytes[payload] == 0x2f) {
                    long packed = readLittleEndian(bytes, payload + 1, 4);
                    return dimensions(1 + (int) (packed & 0x3fff), 1 + (int) ((packed >>> 14) & 0x3fff));
                }
                if (bytes[offset + 3] == ' ' && length >= 10 && bytes[payload + 3] == (byte) 0x9d
                    && bytes[payload + 4] == 1 && bytes[payload + 5] == 0x2a) {
                    return dimensions((int) readLittleEndian(bytes, payload + 6, 2) & 0x3fff,
                        (int) readLittleEndian(bytes, payload + 8, 2) & 0x3fff);
                }
            }
            long next = payload + length + (length & 1);
            if (next > bytes.length) {
                break;
            }
            offset = (int) next;
        }
        throw new IOException("Cannot read WebP image dimensions.");
    }

    private static long readLittleEndian(@NotNull byte[] bytes, int offset, int length) {
        long value = 0;
        for (int index = 0; index < length; index++) {
            value |= (long) (bytes[offset + index] & 0xff) << (index * 8);
        }
        return value;
    }

    @NotNull
    private static AIImageDimensions dimensions(int width, int height) throws IOException {
        if (width <= 0 || height <= 0) {
            throw new IOException("Invalid image dimensions.");
        }
        return new AIImageDimensions(width, height);
    }
}

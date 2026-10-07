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
package org.jkiss.dbeaver.ui.ai.chat.controls;

import org.eclipse.swt.graphics.ImageData;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.ai.AIImageDimensions;
import org.jkiss.dbeaver.ui.swt.ImageConverter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;

final class AIImageThumbnail {
    private static final long MAX_DECODED_PIXELS = 16_000_000;
    private static final int MAX_WEBP_FRAMES = 64;

    private AIImageThumbnail() {
    }

    @NotNull
    static ImageData read(@NotNull byte[] bytes, int size) throws IOException {
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (readers.hasNext()) {
                ImageReader reader = readers.next();
                try {
                    reader.setInput(input, true, true);
                    int width = reader.getWidth(0);
                    int height = reader.getHeight(0);
                    checkPixels((long) width * height);
                    var parameters = reader.getDefaultReadParam();
                    int sampling = Math.max(1, (int) Math.ceil((double) Math.max(width, height) / size));
                    parameters.setSourceSubsampling(sampling, sampling, 0, 0);
                    // read only the first GIF frame, without allocating every animation frame
                    return scale(ImageConverter.convertToImageData(reader.read(0, parameters)), size);
                } finally {
                    reader.dispose();
                }
            }
        }
        // native SWT decoders support WebP on platforms without an ImageIO reader
        AIImageDimensions dimensions = AIImageDimensions.read(bytes);
        checkWebpPixels(bytes, (long) dimensions.width() * dimensions.height());
        return scale(new ImageData(new ByteArrayInputStream(bytes)), size);
    }

    private static void checkWebpPixels(@NotNull byte[] bytes, long canvasPixels) throws IOException {
        checkPixels(canvasPixels);
        int offset = 12;
        int frames = 0;
        long pixels = 0;
        while (offset <= bytes.length - 8) {
            long length = Integer.toUnsignedLong(java.nio.ByteBuffer.wrap(bytes, offset + 4, 4)
                .order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt());
            if (length > bytes.length - offset - 8) {
                throw new IOException("Incomplete WebP chunk.");
            }
            if (bytes[offset] == 'A' && bytes[offset + 1] == 'N' && bytes[offset + 2] == 'M' && bytes[offset + 3] == 'F') {
                frames++;
                pixels += canvasPixels;
                checkPixels(pixels);
                if (frames > MAX_WEBP_FRAMES) {
                    throw new IOException("Too many WebP frames to create a thumbnail safely.");
                }
            }
            offset += (int) (8 + length + (length & 1));
        }
    }

    private static void checkPixels(long pixels) throws IOException {
        if (pixels <= 0 || pixels > MAX_DECODED_PIXELS) {
            throw new IOException("Image is too large to create a thumbnail safely.");
        }
    }

    @NotNull
    private static ImageData scale(@NotNull ImageData source, int size) {
        double scale = Math.min(1, (double) size / Math.max(source.width, source.height));
        return source.scaledTo(Math.max(1, (int) Math.round(source.width * scale)),
            Math.max(1, (int) Math.round(source.height * scale)));
    }
}

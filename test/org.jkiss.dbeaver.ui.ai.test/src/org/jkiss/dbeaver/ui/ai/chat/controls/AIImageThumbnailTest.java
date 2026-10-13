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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Base64;
import javax.imageio.ImageIO;

class AIImageThumbnailTest {
    private static final String LOSSY =
        "UklGRkQAAABXRUJQVlA4IDgAAAAwAgCdASogABAAAAAAJQA7AH4AfoB/AAIRgAD+/29W///yw/H9Sef/LEP///ienN4ip5X/8TIAAA==";
    private static final String LOSSLESS = "UklGRhwAAABXRUJQVlA4TA8AAAAvH8ADAAcQ/Y/+ByKi/wEA";
    private static final String TRANSPARENT = "UklGRhwAAABXRUJQVlA4TA8AAAAvH8ADEAcQ/Y8CBSKi/wEA";
    private static final String ANIMATED =
        "UklGRoQAAABXRUJQVlA4WAoAAAACAAAAHwAADwAAQU5JTQYAAAAAAAAAAABBTk1GKAAAAAAAAAAAAB8AAA8AAGQAAAJWUDhMDwAAAC8f"
            + "wAMABxD9j/4HIqL/AQBBTk1GKAAAAAAAAAAAAB8AAA8AAGQAAABWUDhMDwAAAC8fwAMAB9D/iP4HIqL/AQA=";

    @Test
    void passesWebpToBrowserWithoutNativeDecoding() throws IOException {
        for (String source : new String[]{LOSSY, LOSSLESS, TRANSPARENT, ANIMATED}) {
            byte[] bytes = decode(source);
            Assertions.assertTrue(AIImageThumbnail.isWebp(bytes));
            Assertions.assertEquals("data:image/webp;base64," + source, AIImageThumbnail.toDataUrl(bytes, 128));
            Assertions.assertEquals("data:image/webp;base64," + source, AIImageThumbnail.toDataUrl(bytes, 2048));
        }
    }

    @Test
    void stillCreatesPngThumbnailsForStandardFormats() throws IOException {
        BufferedImage source = new BufferedImage(32, 16, BufferedImage.TYPE_INT_RGB);
        for (String format : new String[]{"png", "jpeg", "gif"}) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Assertions.assertTrue(ImageIO.write(source, format, output));
            byte[] bytes = output.toByteArray();
            Assertions.assertFalse(AIImageThumbnail.isWebp(bytes));
            ImageData thumbnail = AIImageThumbnail.read(bytes, 8);
            Assertions.assertEquals(8, thumbnail.width);
            Assertions.assertEquals(4, thumbnail.height);
            String url = AIImageThumbnail.toDataUrl(bytes, 8);
            String prefix = "data:image/png;base64,";
            Assertions.assertTrue(url.startsWith(prefix));
            BufferedImage preview = ImageIO.read(new ByteArrayInputStream(decode(url.substring(prefix.length()))));
            Assertions.assertEquals(8, preview.getWidth());
            Assertions.assertEquals(4, preview.getHeight());
        }
    }

    @Test
    void rejectsOversizedWebpBeforePassingItToBrowser() {
        byte[] bytes = decode(LOSSLESS);
        // lossless WebP stores width and height minus one in two 14-bit fields
        ByteBuffer.wrap(bytes, 21, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(16383 | (16383 << 14));
        IOException exception = Assertions.assertThrows(IOException.class, () -> AIImageThumbnail.toDataUrl(bytes, 128));
        Assertions.assertTrue(exception.getMessage().contains("16 million pixels"));
    }

    @Test
    void rejectsExcessiveWebpAnimationFrames() throws IOException {
        byte[] animation = decode(ANIMATED);
        byte[] header = Arrays.copyOf(animation, 44);
        // a one-pixel canvas isolates the frame-count limit from the pixel limit
        Arrays.fill(header, 24, 30, (byte) 0);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(header);
        for (int frame = 0; frame < 65; frame++) {
            output.write(animation, 44, 48);
        }
        byte[] bytes = output.toByteArray();
        ByteBuffer.wrap(bytes, 4, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(bytes.length - 8);
        IOException exception = Assertions.assertThrows(IOException.class, () -> AIImageThumbnail.toDataUrl(bytes, 128));
        Assertions.assertTrue(exception.getMessage().contains("Too many WebP frames"));
    }

    @Test
    void rejectsIncompleteWebp() {
        byte[] bytes = Arrays.copyOf(decode(LOSSLESS), 26);
        Assertions.assertThrows(IOException.class, () -> AIImageThumbnail.toDataUrl(bytes, 128));
    }

    @Test
    void rejectsUnsupportedImages() {
        byte[] bytes = new byte[32];
        Assertions.assertFalse(AIImageThumbnail.isWebp(bytes));
        Assertions.assertFalse(AIImageThumbnail.isWebp(new byte[4]));
        Assertions.assertThrows(IOException.class, () -> AIImageThumbnail.toDataUrl(bytes, 128));
    }

    @NotNull
    private static byte[] decode(@NotNull String base64) {
        return Base64.getDecoder().decode(base64);
    }
}

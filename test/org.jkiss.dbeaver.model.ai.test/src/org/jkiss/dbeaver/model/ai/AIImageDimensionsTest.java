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

import org.jkiss.dbeaver.model.ai.engine.copilot.CopilotCompletionEngine;
import org.jkiss.dbeaver.model.ai.engine.copilot.CopilotProperties;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIEngine;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIProperties;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAiUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

class AIImageDimensionsTest {
    @Test
    void readsPngJpegAndGifWithoutDecodingPixelData() throws IOException {
        for (String format : new String[]{"png", "jpeg", "gif"}) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(new BufferedImage(127, 63, BufferedImage.TYPE_INT_RGB), format, output);
            Assertions.assertEquals(new AIImageDimensions(127, 63), AIImageDimensions.read(output.toByteArray()));
        }
    }

    @Test
    void readsAllThreeWebpDimensionHeaders() throws IOException {
        byte[] extended = webp("VP8X", new byte[]{0, 0, 0, 0, 126, 0, 0, 62, 0, 0});
        byte[] lossless = webp("VP8L", ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN)
            .put((byte) 0x2f).putInt(126 | (62 << 14)).array());
        byte[] lossy = webp("VP8 ", new byte[]{0, 0, 0, (byte) 0x9d, 1, 0x2a, 127, 0, 63, 0});
        for (byte[] bytes : new byte[][]{extended, lossless, lossy}) {
            Assertions.assertEquals(new AIImageDimensions(127, 63), AIImageDimensions.read(bytes));
        }
        Assertions.assertThrows(IOException.class, () -> AIImageDimensions.read(webp("VP8X", new byte[3])));
        Assertions.assertThrows(IOException.class, () -> AIImageDimensions.read(new byte[0]));
    }

    @Test
    void reservesTheLargerGpt4oMiniImageBudget() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", output);
        AIImageAttachment image = AIImageAttachment.fromBytes("small.png", output.toByteArray());
        OpenAIProperties openAIProperties = new OpenAIProperties();
        openAIProperties.setModel("gpt-4o-mini");
        Assertions.assertEquals(8500, new OpenAIEngine<>(openAIProperties).estimateImageTokens(image));
        CopilotProperties copilotProperties = new CopilotProperties();
        copilotProperties.setModel("gpt-4o-mini");
        Assertions.assertEquals(8500, new CopilotCompletionEngine<>(copilotProperties).estimateImageTokens(image));
        Assertions.assertEquals(8500, OpenAiUtils.estimateImageTokens("gpt-4o-mini-2024-07-18", image));
        Assertions.assertEquals(AIImageAttachment.DEFAULT_TOKEN_ESTIMATE, OpenAiUtils.estimateImageTokens("gpt-4o", image));
        AIImageAttachment invalid = AIImageAttachment.fromBytes("invalid.png", new byte[]{(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10});
        Assertions.assertEquals(48169, OpenAiUtils.estimateImageTokens("gpt-4o-mini", invalid));
    }

    private static byte[] webp(String type, byte[] payload) {
        return ByteBuffer.allocate(20 + payload.length + (payload.length & 1)).order(ByteOrder.LITTLE_ENDIAN)
            .put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(12 + payload.length)
            .put("WEBP".getBytes(StandardCharsets.US_ASCII)).put(type.getBytes(StandardCharsets.US_ASCII))
            .putInt(payload.length).put(payload).array();
    }
}

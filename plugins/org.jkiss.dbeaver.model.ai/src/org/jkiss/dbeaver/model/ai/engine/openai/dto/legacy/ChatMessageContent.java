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
package org.jkiss.dbeaver.model.ai.engine.openai.dto.legacy;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.ai.AIMessage;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIMessage;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIMessageContent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ChatMessageContent {
    private ChatMessageContent() {
    }

    @NotNull
    public static Object from(@NotNull AIMessage message) {
        if (message.getImages().isEmpty()) {
            return message.getContent();
        }
        List<Map<String, Object>> content = new ArrayList<>();
        if (!message.getContent().isEmpty()) {
            content.add(Map.of("type", "text", "text", message.getContent()));
        }
        message.getImages().forEach(image -> content.add(
            Map.of("type", "image_url", "image_url", Map.of("url", image.toDataUrl()))));
        return content;
    }

    @NotNull
    public static Object from(@NotNull OAIMessage message) {
        if (message.content.stream().noneMatch(content -> OAIMessageContent.TYPE_INPUT_IMAGE.equals(content.type))) {
            return message.getFullText();
        }
        return message.content.stream().map(content -> {
            if (OAIMessageContent.TYPE_INPUT_IMAGE.equals(content.type)) {
                return Map.of("type", "image_url", "image_url", Map.of("url", content.imageUrl));
            }
            return Map.of("type", "text", "text", content.text);
        }).toList();
    }
}

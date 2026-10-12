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
    private static final String TYPE = "type"; //$NON-NLS-1$
    private static final String TEXT = "text"; //$NON-NLS-1$
    private static final String IMAGE_URL = "image_url"; //$NON-NLS-1$
    private static final String URL = "url"; //$NON-NLS-1$

    private ChatMessageContent() {
    }

    @NotNull
    public static Object from(@NotNull AIMessage message) {
        if (message.getImages().isEmpty()) {
            return message.getContent();
        }
        List<Map<String, Object>> content = new ArrayList<>();
        if (!message.getContent().isEmpty()) {
            content.add(Map.of(TYPE, TEXT, TEXT, message.getContent()));
        }
        message.getImages().forEach(image -> content.add(
            Map.of(TYPE, IMAGE_URL, IMAGE_URL, Map.of(URL, image.toDataUrl()))));
        return content;
    }

    @NotNull
    public static Object from(@NotNull OAIMessage message) {
        if (message.content.stream().noneMatch(content -> OAIMessageContent.TYPE_INPUT_IMAGE.equals(content.type))) {
            return message.getFullText();
        }
        return message.content.stream().map(content -> {
            if (OAIMessageContent.TYPE_INPUT_IMAGE.equals(content.type)) {
                return Map.of(TYPE, IMAGE_URL, IMAGE_URL, Map.of(URL, content.imageUrl));
            }
            return Map.of(TYPE, TEXT, TEXT, content.text);
        }).toList();
    }
}

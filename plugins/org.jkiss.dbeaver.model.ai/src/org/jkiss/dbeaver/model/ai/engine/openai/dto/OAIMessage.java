/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.ai.engine.openai.dto;

import com.google.gson.annotations.SerializedName;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.ai.AIMessage;
import org.jkiss.dbeaver.model.ai.AIMessageType;

import java.util.List;
import java.util.stream.Collectors;

public class OAIMessage {
    public static final String TYPE_MESSAGE = "message";
    public static final String TYPE_FUNCTION_CALL = "function_call";
    public static final String TYPE_FUNCTION_REASONING = "reasoning";

    public String id;
    public String type;
    public String status;
    public String role;
    public String name;
    public String arguments;
    @SerializedName("call_id")
    public String callId;
    public List<OAIMessageContent> content;

    public OAIMessage() {
    }

    public OAIMessage(@NotNull AIMessage msg) {
        type = TYPE_MESSAGE;
        role = mapRole(msg.getRole());
        boolean input = switch (msg.getRole()) {
            case SYSTEM, USER -> true;
            default -> false;
        };
        content = List.of(new OAIMessageContent(input, msg.getContent()));
    }

    @NotNull
    public String getFullText() {
        if (content == null) {
            return "";
        }
        return content.stream().map(c -> c.text).collect(Collectors.joining());
    }

    @Nullable
    private static String mapRole(@NotNull AIMessageType role) {
        return switch (role) {
            case SYSTEM -> "system";
            case USER -> "user";
            case ASSISTANT, FUNCTION -> "assistant";
            default -> null;
        };
    }
}

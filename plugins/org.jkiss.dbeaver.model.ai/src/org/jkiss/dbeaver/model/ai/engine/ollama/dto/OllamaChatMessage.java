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
package org.jkiss.dbeaver.model.ai.engine.ollama.dto;

import com.google.gson.annotations.SerializedName;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.List;

public record OllamaChatMessage(
    @NotNull OllamaRole role,
    @Nullable String content,
    @SerializedName("tool_calls")
    @Nullable List<OllamaToolCall> toolCalls,
    @SerializedName("tool_name")
    @Nullable String toolName,
    @Nullable List<String> images
) {
    public OllamaChatMessage(
        @NotNull OllamaRole role,
        @Nullable String content,
        @Nullable List<OllamaToolCall> toolCalls,
        @Nullable String toolName
    ) {
        this(role, content, toolCalls, toolName, null);
    }

    public OllamaChatMessage(@NotNull OllamaRole role, @Nullable String content) {
        this(role, content, null, null);
    }
}

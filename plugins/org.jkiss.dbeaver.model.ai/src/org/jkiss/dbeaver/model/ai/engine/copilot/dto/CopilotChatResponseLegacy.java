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
package org.jkiss.dbeaver.model.ai.engine.copilot.dto;

import com.google.gson.annotations.SerializedName;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.ai.AIUsage;

import java.util.List;
import java.util.Map;

public record CopilotChatResponseLegacy(
    @Nullable
    String id,
    @NotNull
    List<Choice> choices,
    @Nullable
    CopilotUsage usage,
    @Nullable
    String model,
    @Nullable
    @SerializedName("service_tier")
    String serviceTier
) {

    public record Choice(
        @Nullable
        @SerializedName("finish_reason")
        String finishReason,
        int index,
        @Nullable
        @SerializedName("content_filter_results")
        Map<String, Object> contentFilterResults,
        Message message
    ) {
    }

    public record Message(
        @Nullable
        String content,
        @Nullable
        String padding,
        @Nullable
        String role,
        @Nullable
        @SerializedName("tool_calls")
        List<ToolCall> toolCalls
    ) {
    }

    public record ToolCall(
        int index,
        @Nullable
        String id,
        @Nullable
        String type,
        @Nullable
        Function function
    ) {
    }

    public record Function(
        @Nullable
        String name,
        @Nullable
        String arguments
    ) {
    }

    @Nullable
    public AIUsage getAIUsage() {
        if (usage == null) {
            return null;
        }

        return new AIUsage(
            usage.promptTokens(),
            usage.promptTokensDetails() != null ? usage.promptTokensDetails().cachedTokens() : 0,
            usage.completionTokens(),
            usage.reasoningTokens()
        );
    }
}

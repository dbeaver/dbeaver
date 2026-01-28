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

import com.google.gson.annotations.SerializedName;
import org.jkiss.code.Nullable;

public record OAICompletionUsage(
    @SerializedName("prompt_tokens")
    int promptTokens,

    @SerializedName("completion_tokens")
    int completionTokens,

    @SerializedName("total_tokens")
    int totalTokens,

    @Nullable
    @SerializedName("prompt_tokens_details")
    PromptTokensDetails promptTokensDetails,

    @Nullable
    @SerializedName("completion_tokens_details")
    CompletionTokensDetails completionTokensDetails
) {

    public record PromptTokensDetails(
        @SerializedName("cached_tokens")
        int cachedTokens,
        @SerializedName("audio_tokens")
        int audioTokens
    ) {
    }

    public record CompletionTokensDetails(
        @SerializedName("reasoning_tokens")
        int reasoningTokens,
        @SerializedName("audio_tokens")
        int audioTokens,
        @SerializedName("accepted_prediction_tokens")
        int acceptedPredictionTokens,
        @SerializedName("rejected_prediction_tokens")
        int rejectedPredictionTokens
    ) {

    }
}

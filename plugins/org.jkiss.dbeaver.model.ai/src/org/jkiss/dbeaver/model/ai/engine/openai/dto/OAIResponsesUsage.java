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
package org.jkiss.dbeaver.model.ai.engine.openai.dto;

import com.google.gson.annotations.SerializedName;
import org.jkiss.code.Nullable;

public record OAIResponsesUsage(
    @SerializedName("input_tokens")
    int inputTokens,

    @Nullable
    @SerializedName("input_tokens_details")
    InputTokenDetails inputTokensDetails,

    @SerializedName("output_tokens")
    int outputTokens,

    @Nullable
    @SerializedName("output_tokens_details")
    OutputTokenDetails outputTokensDetails
) {

    public record InputTokenDetails(
        @SerializedName("cached_tokens")
        int cachedTokens
    ) {
    }

    public record OutputTokenDetails(
        @SerializedName("reasoning_tokens")
        int reasoningTokens
    ) {
    }
}

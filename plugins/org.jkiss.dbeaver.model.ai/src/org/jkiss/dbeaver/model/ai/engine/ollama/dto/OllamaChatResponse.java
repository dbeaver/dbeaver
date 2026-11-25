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
import org.jkiss.code.Nullable;

import java.time.Instant;

public record OllamaChatResponse(
    @Nullable String model,
    @SerializedName("created_at")
    @Nullable Instant createdAt,
    @Nullable OllamaChatMessage message,
    boolean done,
    @SerializedName("total_duration")
    long totalDuration,
    @SerializedName("load_duration")
    long loadDuration,
    @SerializedName("prompt_eval_count")
    int promptEvalCount,
    @SerializedName("prompt_eval_duration")
    long promptEvalDuration,
    @SerializedName("eval_count")
    int evalCount,
    @SerializedName("eval_duration")
    long evalDuration
) {
}

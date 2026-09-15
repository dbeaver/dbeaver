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
package org.jkiss.dbeaver.model.ai.engine;

import com.google.gson.annotations.SerializedName;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.List;
import java.util.Set;

public record AIModelCatalogEntry(
    @Nullable Limits limit,
    @Nullable Boolean temperature,
    @Nullable Boolean reasoning,
    @SerializedName("tool_call") @Nullable Boolean toolCall,
    @SerializedName("structured_output") @Nullable Boolean structuredOutput,
    @Nullable Modalities modalities
) {
    @NotNull
    public AIModel enrich(@NotNull AIModel model) {
        AIModel result = new AIModel(
            model.name(),
            preferLimit(model.contextWindowSize(), limit == null ? null : limit.context()),
            Set.copyOf(model.features()),
            model.defaultTemperature(),
            preferLimit(model.inputTokenLimit(), limit == null ? null : limit.input()),
            preferLimit(model.outputTokenLimit(), limit == null ? null : limit.output()),
            model.maxTemperature()
        ).withFeature(AIModelFeature.TOOL_CALL, toolCall)
            .withFeature(AIModelFeature.REASONING, reasoning)
            .withFeature(AIModelFeature.STRUCTURED_OUTPUT, structuredOutput);
        if (temperature != null) {
            result = result.withFeature(AIModelFeature.TEMPERATURE_UNSUPPORTED, !temperature);
            if (temperature) {
                result = result.withFeature(AIModelFeature.ALWAYS_DEFAULT_TEMPERATURE, false);
            }
        }
        if (modalities != null && modalities.input() != null) {
            result = result.withFeature(AIModelFeature.VISION, modalities.input().contains("image"))
                .withFeature(AIModelFeature.PDF_INPUT, modalities.input().contains("pdf"));
        }
        return result;
    }

    @Nullable
    private static Integer preferLimit(@Nullable Integer nativeLimit, @Nullable Integer catalogLimit) {
        if (nativeLimit != null && nativeLimit > 0) {
            return nativeLimit;
        }
        return catalogLimit != null && catalogLimit > 0 ? catalogLimit : null;
    }

    public record Limits(@Nullable Integer context, @Nullable Integer input, @Nullable Integer output) {
    }

    public record Modalities(@Nullable List<String> input, @Nullable List<String> output) {
    }
}

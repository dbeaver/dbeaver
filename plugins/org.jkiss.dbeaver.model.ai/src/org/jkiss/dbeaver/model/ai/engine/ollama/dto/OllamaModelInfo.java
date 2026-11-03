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
import org.jkiss.dbeaver.model.ai.engine.AIModel;
import org.jkiss.dbeaver.model.ai.engine.AIModelFeature;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record OllamaModelInfo(
    @SerializedName("model_info") @Nullable Map<String, Object> modelInfo,
    @Nullable List<String> capabilities,
    @Nullable String parameters
) {
    private static final Pattern TEMPERATURE_PARAMETER = Pattern.compile("(?m)^temperature\\s+(\\S+)\\s*$");

    @NotNull
    public AIModel toAIModel(@NotNull String name) {
        Set<AIModelFeature> features = new HashSet<>();
        if (capabilities == null || capabilities.contains("completion")) {
            features.add(AIModelFeature.CHAT);
        }
        if (capabilities != null) {
            if (capabilities.contains("embedding")) {
                features.add(AIModelFeature.EMBEDDING);
            }
            if (capabilities.contains("tools")) {
                features.add(AIModelFeature.TOOL_CALL);
            }
            if (capabilities.contains("vision")) {
                features.add(AIModelFeature.VISION);
            }
            if (capabilities.contains("thinking")) {
                features.add(AIModelFeature.REASONING);
            }
        }
        return new AIModel(name, contextWindowSize(), Set.copyOf(features), defaultTemperature())
            .withFeature(AIModelFeature.VISION, capabilities == null ? null : capabilities.contains("vision"));
    }

    @Nullable
    private Integer contextWindowSize() {
        if (modelInfo != null) {
            Object architecture = modelInfo.get("general.architecture");
            Object contextLength = modelInfo.get(architecture + ".context_length");
            if (contextLength instanceof Number number && number.doubleValue() > 0 && number.doubleValue() <= Integer.MAX_VALUE) {
                return number.intValue();
            }
        }
        return null;
    }

    private double defaultTemperature() {
        if (parameters != null) {
            Matcher matcher = TEMPERATURE_PARAMETER.matcher(parameters);
            if (matcher.find()) {
                try {
                    double value = Double.parseDouble(matcher.group(1));
                    if (Double.isFinite(value) && value >= 0) {
                        return value;
                    }
                } catch (NumberFormatException ignored) {
                    // older servers may return non-numeric parameter values
                }
            }
        }
        return 0.0;
    }
}

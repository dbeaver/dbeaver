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
package org.jkiss.dbeaver.model.ai.engine.openai;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.ai.engine.AIModel;
import org.jkiss.dbeaver.model.ai.engine.AIModelFeature;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class OpenAIModels {
    private OpenAIModels() {
    }

    private static final Pattern CHAT_MODEL_PATTERN = Pattern.compile("^(gpt(-\\w+)?|o\\d+)(-mini|-pro|-turbo)?$");
    public static final String DEFAULT_MODEL = "gpt-4o";

    public static final Map<String, AIModel> KNOWN_MODELS = Stream.of(
        new AIModel("o4-mini", 200_000, Set.of(AIModelFeature.CHAT)),
        new AIModel("o3-pro", 200_000, Set.of(AIModelFeature.CHAT)),
        new AIModel("o3", 200_000, Set.of(AIModelFeature.CHAT)),
        new AIModel("o3-mini", 200_000, Set.of(AIModelFeature.CHAT)),
        new AIModel("o1-pro", 200_000, Set.of(AIModelFeature.CHAT)),
        new AIModel("o1", 200_000, Set.of(AIModelFeature.CHAT)),
        new AIModel("o1-mini", 128_000, Set.of(AIModelFeature.CHAT)),
        new AIModel("gpt-4.1", 1_048_576, Set.of(AIModelFeature.CHAT)),
        new AIModel("gpt-4o", 128_000, Set.of(AIModelFeature.CHAT)),
        new AIModel("gpt-4o-mini", 128_000, Set.of(AIModelFeature.CHAT)),
        new AIModel("gpt-4-turbo", 128_000, Set.of(AIModelFeature.CHAT)),
        new AIModel("gpt-3.5-turbo", 16_384, Set.of(AIModelFeature.CHAT)),
        new AIModel("gpt-4", 8_192, Set.of(AIModelFeature.CHAT)),

        new AIModel("gpt-4o-transcribe", 128_000, Set.of(AIModelFeature.SPEECH_TO_TEXT)),
        new AIModel("gpt-4o-mini-transcribe", 128_000, Set.of(AIModelFeature.SPEECH_TO_TEXT)),
        new AIModel("whisper-1", 30_000, Set.of(AIModelFeature.SPEECH_TO_TEXT))
    ).collect(Collectors.toMap(
        AIModel::name,
        Function.identity()
    ));

    public static final Set<String> DEPRECATED_MODELS = Set.of(
        "gpt-3.5-turbo-0301",
        "gpt-3.5-turbo-0613",
        "gpt-3.5-turbo-1106",
        "gpt-3.5-turbo-16k",
        "gpt-3.5-turbo-16k-0613",
        "gpt-3.5-turbo-16k-1106"
    );

    /**
     * Returns the replacement model name for the given model name.
     * If the model name is null or empty, returns the default model.
     * If the model name is known, returns it in lowercase.
     * If the model name is deprecated, returns the default model.
     *
     * @param modelName the model name to check
     * @return the replacement model name
     */
    public static String getEffectiveModelName(@Nullable String modelName) {
        if (modelName == null || modelName.isEmpty()) {
            return DEFAULT_MODEL;
        }
        String lowerCaseModelName = modelName.toLowerCase(Locale.ROOT);
        if (KNOWN_MODELS.containsKey(lowerCaseModelName)) {
            return lowerCaseModelName;
        }
        if (DEPRECATED_MODELS.contains(lowerCaseModelName)) {
            return DEFAULT_MODEL;
        }
        return lowerCaseModelName;
    }

    @Nullable
    public static Integer getContextWindowSize(@Nullable String modelName) {
        if (modelName == null) {
            return null;
        }

        AIModel knownModel = KNOWN_MODELS.get(modelName.toLowerCase(Locale.ROOT));
        if (knownModel != null) {
            return knownModel.contextWindowSize();
        }

        return null;
    }

    public static Set<AIModelFeature> getModelFeatures(@NotNull String modelName) {
        AIModel knownModel = KNOWN_MODELS.get(modelName.toLowerCase(Locale.ROOT));
        if (knownModel != null) {
            return knownModel.features();
        }

        // If the model is not known, return an empty set
        Set<AIModelFeature> features = new HashSet<>();

        if (CHAT_MODEL_PATTERN.matcher(modelName).matches()) {
            features.add(AIModelFeature.CHAT);
        }

        return Set.of();
    }
}

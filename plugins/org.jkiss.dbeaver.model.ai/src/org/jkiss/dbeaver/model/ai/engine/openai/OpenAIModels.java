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
package org.jkiss.dbeaver.model.ai.engine.openai;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.ai.engine.AIModel;
import org.jkiss.dbeaver.model.ai.engine.AIModelCatalog;
import org.jkiss.dbeaver.model.ai.engine.AIModelCatalogEntry;
import org.jkiss.dbeaver.model.ai.engine.AIModelFeature;
import org.jkiss.dbeaver.model.ai.engine.openai.dto.OAIModel;
import org.jkiss.utils.CommonUtils;

import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OpenAIModels {
    public static final String CATALOG_PROVIDER_ID = "openai";
    private static final Pattern EMBEDDING_MODEL_PATTERN = Pattern.compile("text-embedding-.*");
    private static final Pattern SNAPSHOT_MODEL_PATTERN = Pattern.compile("^(.+)-\\d{4}-\\d{2}-\\d{2}$");

    private OpenAIModels() {
    }

    /**
     * Returns the effective model name for the given model name.
     * If the model name is null or empty, returns null.
     * Model IDs are preserved as supplied by the provider.
     *
     * @param modelName the model name to check
     * @return the effective model name
     */
    @Nullable
    public static String getEffectiveModelName(@Nullable String modelName) {
        if (CommonUtils.isEmpty(modelName)) {
            return null;
        }
        return modelName;
    }

    @NotNull
    public static Optional<AIModel> getModelByName(@Nullable String modelName) {
        String name = getEffectiveModelName(modelName);
        if (name == null) {
            return Optional.empty();
        }
        AIModelCatalogEntry entry = findCatalogEntry(AIModelCatalog.getInstance().getCachedModels(CATALOG_PROVIDER_ID), name);
        return Optional.ofNullable(entry).map(metadata -> metadata.enrich(new AIModel(name, null, detectModelFeatures(name))));
    }

    @Nullable
    public static AIModelCatalogEntry findCatalogEntry(
        @NotNull Map<String, AIModelCatalogEntry> catalog,
        @Nullable String modelName
    ) {
        if (CommonUtils.isEmpty(modelName)) {
            return null;
        }
        AIModelCatalogEntry entry = catalog.get(modelName);
        if (entry != null) {
            return entry;
        }
        Matcher snapshot = SNAPSHOT_MODEL_PATTERN.matcher(modelName);
        return snapshot.matches() ? catalog.get(snapshot.group(1)) : null;
    }

    public static boolean isOpenAIEndpoint(@Nullable String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return true;
        }
        try {
            return "api.openai.com".equalsIgnoreCase(URI.create(baseUrl).getHost());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @NotNull
    public static AIModel fromApiModel(@NotNull OAIModel model) {
        return fromApiModel(model, null);
    }

    @NotNull
    public static AIModel fromApiModel(@NotNull OAIModel model, @Nullable AIModelCatalogEntry catalogEntry) {
        Integer contextSize = model.contextLength();
        AIModel result = new AIModel(
            model.id(), contextSize != null && contextSize > 0 ? contextSize : null, detectModelFeatures(model.id())
        );
        return catalogEntry == null ? result : catalogEntry.enrich(result);
    }

    @NotNull
    public static Set<AIModelFeature> detectModelFeatures(@NotNull String modelName) {
        Set<AIModelFeature> features = new HashSet<>();

        if (isChatModel(modelName)) {
            features.add(AIModelFeature.CHAT);
            features.add(AIModelFeature.STREAMING);
        }

        if (EMBEDDING_MODEL_PATTERN.matcher(modelName).matches()) {
            features.add(AIModelFeature.EMBEDDING);
        }

        if (modelName.startsWith("whisper-") || modelName.contains("-transcribe")) {
            features.add(AIModelFeature.SPEECH_TO_TEXT);
        }

        return features;
    }

    private static final List<String> CHAT_EXCLUDED_KEYWORDS = List.of(
        "search",
        "research",
        "moderation",
        "realtime",
        "audio",
        "transcribe",
        "image"
    );

    private static boolean isChatModel(@NotNull String modelName) {
        if (!(modelName.startsWith("gpt-") || modelName.startsWith("o"))) {
            return false;
        }
        for (String keyword : CHAT_EXCLUDED_KEYWORDS) {
            if (modelName.contains(keyword)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isTemperatureEditable(@NotNull AIModel model) {
        return !model.features().contains(AIModelFeature.ALWAYS_DEFAULT_TEMPERATURE)
            && !model.features().contains(AIModelFeature.TEMPERATURE_UNSUPPORTED);
    }
}

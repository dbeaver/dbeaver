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

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.ai.registry.AISettingsManager;
import org.jkiss.utils.SecurityUtils;

import java.util.Set;

public final class AIModelListUtils {
    // request tuning and OAuth token renewal do not change the available models
    private static final Set<String> IGNORED_PROPERTIES = Set.of(
        "model", "contextWindowSize", "contextSize", "temperature", "loggingEnabled", "timeout",
        "accessToken", "refreshToken", "expiresAt"
    );
    private static final Gson CONFIGURATION_GSON = AISettingsManager.READ_PROPS_GSON.newBuilder()
        .addSerializationExclusionStrategy(new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(@NotNull FieldAttributes field) {
                return AIEngineProperties.class.isAssignableFrom(field.getDeclaringClass())
                    && IGNORED_PROPERTIES.contains(field.getName());
            }

            @Override
            public boolean shouldSkipClass(@NotNull Class<?> type) {
                return false;
            }
        }).create();

    private AIModelListUtils() {
    }

    @NotNull
    public static String getConfigurationFingerprint(@NotNull AIEngineProperties properties) {
        // credentials affect the model list, but must not be retained in cache keys as plain text
        return SecurityUtils.sha256Hex(properties.getClass().getName() + '\n' + CONFIGURATION_GSON.toJson(properties));
    }

    public static boolean isChatModel(@NotNull AIModel model) {
        return model.features().isEmpty() || model.features().contains(AIModelFeature.CHAT);
    }
}

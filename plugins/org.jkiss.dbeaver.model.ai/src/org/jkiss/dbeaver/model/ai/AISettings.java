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
package org.jkiss.dbeaver.model.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;
import com.google.gson.ToNumberPolicy;
import com.google.gson.reflect.TypeToken;
import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.engine.AIEngineProperties;
import org.jkiss.dbeaver.model.ai.registry.AIEngineDescriptor;
import org.jkiss.dbeaver.model.ai.registry.AIEngineRegistry;
import org.jkiss.dbeaver.model.ai.registry.AISettingsManager;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * AI global settings.
 * Keeps global parameters and configuration of all AI engines
 */
public class AISettings implements IAdaptable {
    private boolean aiDisabled;
    private String activeEngine;
    private final Map<String, AIEngineProperties> engineConfigurations = new LinkedHashMap<>();
    private final Set<String> resolvedSecrets = new HashSet<>();

    private static final Gson GSON = new GsonBuilder()
        .setStrictness(Strictness.LENIENT)
        .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
        .create();

    public static AIEngineProperties updatePropertiesFromMap(AIEngineProperties configuration, Map<String, Object> properties) {
        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();

        Map<String, Object> propMap = GSON.fromJson(GSON.toJson(configuration), type);
        Map<String, Object> mergedMap = new LinkedHashMap<>(propMap);
        mergedMap.putAll(properties);

        return GSON.fromJson(GSON.toJsonTree(mergedMap), configuration.getClass());
    }

    public boolean isAiDisabled() {
        return aiDisabled;
    }

    public void setAiDisabled(boolean aiDisabled) {
        this.aiDisabled = aiDisabled;
    }

    public String activeEngine() {
        return activeEngine;
    }

    public void setActiveEngine(String activeEngine) {
        AIEngineDescriptor engineDescriptor = AIEngineRegistry.getInstance().getEngineDescriptor(activeEngine);
        if (engineDescriptor != null) {
            // Replacement?
            activeEngine = engineDescriptor.getId();
        }
        this.activeEngine = activeEngine;
    }

    public boolean hasConfiguration(String engineId) {
        return engineConfigurations.containsKey(engineId);
    }

    @NotNull
    public synchronized <T extends AIEngineProperties> T getEngineConfiguration(@NotNull String engineId) throws DBException {
        AIEngineDescriptor engineDescriptor = AIEngineRegistry.getInstance().getEngineDescriptor(engineId);
        if (engineDescriptor == null) {
            throw new DBException("AI engine " + engineId + " not found");
        }

        AIEngineProperties aiEngineSettings = engineConfigurations.get(engineId);
        if (aiEngineSettings == null) {
            aiEngineSettings = engineDescriptor.createPropertiesInstance();
        }

        if (aiEngineSettings != null) {
            if (!AISettingsManager.saveSecretsAsPlainText()) {
                if (!resolvedSecrets.contains(engineId)) {
                    aiEngineSettings.resolveSecrets();
                    resolvedSecrets.add(engineId);
                }
            }
        }

        return (T) aiEngineSettings;
    }

    public Map<String, AIEngineProperties> getEngineConfigurations() {
        return engineConfigurations;
    }

    public void setEngineConfiguration(
        @NotNull String engineId,
        @NotNull AIEngineProperties engineConfiguration
    ) {
        engineConfigurations.put(engineId, engineConfiguration);
    }

    public void setEngineConfigurations(
        @NotNull Map<String, AIEngineProperties> engineConfigurations
    ) {
        this.engineConfigurations.putAll(engineConfigurations);
    }

    public void saveSecrets() throws DBException {
        for (Map.Entry<String, AIEngineProperties> entry : engineConfigurations.entrySet()) {
            String engineId = entry.getKey();
            AIEngineProperties engineConfiguration = entry.getValue();

            if (resolvedSecrets.contains(engineId)) {
                engineConfiguration.saveSecrets();
            }
        }
    }

    @Override
    public <T> T getAdapter(@NotNull Class<T> adapter) {
        return null;
    }
}

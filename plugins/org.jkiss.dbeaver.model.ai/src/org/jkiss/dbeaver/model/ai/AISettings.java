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
import com.google.gson.JsonElement;
import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPAdaptable;
import org.jkiss.dbeaver.model.ai.engine.AIEngineProperties;
import org.jkiss.dbeaver.model.ai.registry.AIEngineDescriptor;
import org.jkiss.dbeaver.model.ai.registry.AIEngineRegistry;
import org.jkiss.dbeaver.model.ai.registry.AISettingsWriter;

import java.util.*;

/**
 * AI global settings.
 * Keeps global parameters and configuration of all AI engines
 */
public class AISettings implements DBPAdaptable {
    private final Gson readGson;
    private final Gson writeGson;
    private boolean aiDisabled;
    private String activeEngine;
    private final Map<String, AIEngineProperties> engineConfigurations = new LinkedHashMap<>();
    private final Map<String, JsonElement> rawEngineConfigurations = new LinkedHashMap<>();

    private final Map<String, Object> properties = new LinkedHashMap<>();
    private final Set<String> resolvedSecrets = new HashSet<>();
    private final Set<String> enabledFunctionCategories = new LinkedHashSet<>();
    private final Set<String> enabledFunctions = new LinkedHashSet<>();

    public AISettings(@NotNull Gson readGson, @NotNull Gson writeGson) {
        this.readGson = readGson;
        this.writeGson = writeGson;
    }

    public Map<String, Object> getAllProperties() {
        return properties;
    }

    public <T> T getProperty(@NotNull String name, @Nullable T defaultValue) {
        return (T) properties.getOrDefault(name, defaultValue);
    }

    public void setProperty(@NotNull String name, @Nullable Object value) {
        if (value == null) {
            properties.remove(name);
        } else {
            properties.put(name, value);
        }
    }

    @NotNull
    public Set<String> getEnabledFunctions() {
        return new HashSet<>(enabledFunctions);
    }

    public void setEnabledFunctions(@Nullable Set<String> functions) {
        this.enabledFunctions.clear();
        if (functions != null) {
            this.enabledFunctions.addAll(functions);
        }
    }

    public boolean isFunctionEnabled(@NotNull String functionId) {
        return enabledFunctions.contains(functionId);
    }

    public void enableFunction(@NotNull String functionId) {
        enabledFunctions.add(functionId);
    }

    public void disableFunction(@NotNull String functionId) {
        enabledFunctions.remove(functionId);
    }

    @NotNull
    public Set<String> getEnabledFunctionCategories() {
        return new HashSet<>(enabledFunctionCategories);
    }

    public void setEnabledFunctionCategories(@Nullable Set<String> categories) {
        this.enabledFunctionCategories.clear();
        if (categories != null) {
            this.enabledFunctionCategories.addAll(categories);
        }
    }

    public boolean isFunctionCategoryEnabled(String category) {
        return enabledFunctionCategories.contains(category);
    }

    public void enableFunctionCategory(@NotNull String category) {
        enabledFunctionCategories.add(category);
    }

    public void disableFunctionCategory(@NotNull String category) {
        enabledFunctionCategories.remove(category);
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
        return engineConfigurations.containsKey(engineId) || rawEngineConfigurations.containsKey(engineId);
    }

    @NotNull
    public synchronized <T extends AIEngineProperties> T getEngineConfiguration(@NotNull String engineId) throws DBException {
        AIEngineDescriptor engineDescriptor = AIEngineRegistry.getInstance().getEngineDescriptor(engineId);
        if (engineDescriptor == null) {
            throw new DBException("AI engine " + engineId + " not found");
        }

        AIEngineProperties aiEngineSettings = engineConfigurations.computeIfAbsent(
            engineId, k -> {
                JsonElement jsonObject = rawEngineConfigurations.remove(engineId);
                if (jsonObject != null) {
                    return readGson.fromJson(jsonObject, engineDescriptor.getPropertiesType());
                }
                return null;
            }
        );

        if (aiEngineSettings == null) {
            aiEngineSettings = engineDescriptor.createPropertiesInstance();
        }

        if (aiEngineSettings != null) {
            if (!AISettingsWriter.saveSecretsAsPlainText()) {
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

    public void setRawEngineConfigurations(
        @NotNull Map<String, JsonElement> rawEngineConfigurations
    ) {
        this.rawEngineConfigurations.putAll(rawEngineConfigurations);
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

    @NotNull
    public Map<String, JsonElement> getEngineConfigurationsRaw() {
        Map<String, JsonElement> rawConfigs = new LinkedHashMap<>();
        for (Map.Entry<String, AIEngineProperties> entry : engineConfigurations.entrySet()) {
            String engineId = entry.getKey();
            AIEngineProperties engineConfiguration = entry.getValue();
            JsonElement jsonElement = writeGson.toJsonTree(engineConfiguration);
            rawConfigs.put(engineId, jsonElement);
        }

        for (Map.Entry<String, JsonElement> stringJsonElementEntry : rawEngineConfigurations.entrySet()) {
            String engineId = stringJsonElementEntry.getKey();
            if (!rawConfigs.containsKey(engineId)) {
                rawConfigs.put(engineId, stringJsonElementEntry.getValue());
            }
        }

        return rawConfigs;
    }
}

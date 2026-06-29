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
package org.jkiss.dbeaver.model.ai;

import com.google.gson.annotations.JsonAdapter;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPAdaptable;
import org.jkiss.dbeaver.model.ai.engine.AIEngineProperties;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIConstants;
import org.jkiss.dbeaver.model.ai.registry.AIEngineDescriptor;
import org.jkiss.dbeaver.model.ai.registry.AIEngineRegistry;
import org.jkiss.dbeaver.model.ai.registry.AISettingsManager;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.*;

/**
 * AI global settings.
 * Keeps global parameters and configuration of all AI engines
 */
public class AISettings implements DBPAdaptable {
    protected static final Log log = Log.getLog(AISettings.class);
    private boolean aiDisabled;

    private final Map<String, AIConfigurationProfile> configurations = new LinkedHashMap<>();
    private String defaultConfiguration;

    private String activeEngine;
    @JsonAdapter(AISettingsManager.EngineConfigAdapter.class)
    private final Map<String, AIEngineProperties> engineConfigurations = new LinkedHashMap<>();
    private final Map<String, Object> properties = new LinkedHashMap<>();
    private final Map<String, String> customInstructions = new LinkedHashMap<>();

    private transient final Set<String> resolvedSecrets = new HashSet<>();

    public AISettings() {
        try {
            aiDisabled = DBWorkbench.isDistributed() && !AISettingsManager.isConfigExists();
        } catch (DBException e) {
            log.error("Error checking AI configuration", e);
            aiDisabled = true;
        }
    }

    @NotNull
    public AIConfigurationProfile[] getConfigurations() {
        return configurations.values().toArray(new AIConfigurationProfile[0]);
    }

    @NotNull
    public AIConfigurationProfile getDefaultConfiguration() throws DBException {
        AIConfigurationProfile profile = getDefaultConfigurationOrNull();
        if (profile == null) {
            throw new DBException("AI engine is not configured");
        }
        return profile;
    }

    @Nullable
    public AIConfigurationProfile getDefaultConfigurationOrNull() {
        return configurations.get(defaultConfiguration);
    }

    public void setDefaultConfiguration(@Nullable AIConfigurationProfile profile) {
        defaultConfiguration = profile == null ? null : profile.getProfileId();
        activeEngine = profile == null ? OpenAIConstants.OPENAI_ENGINE : profile.getEngineId();
    }

    public AIConfigurationProfile createConfiguration(
        @NotNull String id,
        @NotNull AIEngineDescriptor engine
    ) throws DBException {
        if (id.isBlank()) {
            throw new DBException("Empty aI configuration ID");
        }
        if (configurations.containsKey(id)) {
            throw new DBException("AI configuration '" + id + "' already exists");
        }
        AIConfigurationProfile profile = new AIConfigurationProfile();
        profile.setProfileId(id);
        profile.setEngineId(engine.getId());
        configurations.put(id, profile);

        return profile;
    }

    public void removeConfiguration(@NotNull AIConfigurationProfile profile) {
        configurations.remove(profile.getProfileId());
        if (defaultConfiguration.equals(profile.getProfileId())) {
            defaultConfiguration = configurations.isEmpty() ? null : configurations.keySet().iterator().next();
        }
    }

    /**
     * Global AI settings used by different modules
     */
    @NotNull
    public Map<String, Object> getAllProperties() {
        return properties;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T getProperty(@NotNull String name) {
        return (T) properties.get(name);
    }

    @NotNull
    public <T> T getProperty(@NotNull String name, @NotNull T defaultValue) {
        return Objects.requireNonNullElse(getProperty(name), defaultValue);
    }

    public void setProperty(@NotNull String name, @Nullable Object value) {
        if (value == null) {
            properties.remove(name);
        } else {
            properties.put(name, value);
        }
    }

    @NotNull
    public Map<String, String> getCustomInstructions() {
        return Map.copyOf(customInstructions);
    }

    @Nullable
    public String getCustomInstructions(@NotNull String promptGeneratorId) {
        return customInstructions.get(promptGeneratorId);
    }

    public void setCustomInstructions(@NotNull Map<String, String> instructions) {
        customInstructions.clear();
        customInstructions.putAll(instructions);
    }

    public boolean isAiDisabled() {
        return aiDisabled;
    }

    // Disables AI integration. Saves configuration.
    public void setAiDisabled(boolean aiDisabled) {
        this.aiDisabled = aiDisabled;
        AISettingsManager.getInstance().saveSettings();
    }

    @NotNull
    public String activeEngine() {
        return activeEngine;
    }

    public void setActiveEngine(@NotNull String activeEngine) {
        AIEngineDescriptor engineDescriptor = AIEngineRegistry.getInstance().getEngineDescriptor(activeEngine);
        if (engineDescriptor != null) {
            // Replacement?
            activeEngine = engineDescriptor.getId();
        }
        this.activeEngine = activeEngine;
    }

    public boolean hasConfiguration(@NotNull String engineId) {
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

        if (!AISettingsManager.saveSecretsAsPlainText()) {
            if (!resolvedSecrets.contains(engineId)) {
                aiEngineSettings.resolveSecrets();
                resolvedSecrets.add(engineId);
            }
        }

        return (T) aiEngineSettings;
    }

    @NotNull
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

    // Patches configuration to support legacy configuration format
    public void migrateLegacySettings() {
        // Def engine
        if (activeEngine == null || !hasConfiguration(activeEngine)) {
            activeEngine = OpenAIConstants.OPENAI_ENGINE;
        }

        if (configurations.isEmpty() && !engineConfigurations.isEmpty()) {
            // Profiles from engine settings
            for (Map.Entry<String, AIEngineProperties> ep : engineConfigurations.entrySet()) {
                String engineId = ep.getKey();
                AIEngineDescriptor engineDescriptor = AIEngineRegistry.getInstance().getEngineDescriptor(engineId);
                if (engineDescriptor != null) {
                    AIConfigurationProfile cp = new AIConfigurationProfile();
                    cp.setProfileId(engineId);
                    cp.setProfileName(engineDescriptor.getLabel());
                    cp.setEngineId(engineId);
                    cp.setConfiguration(ep.getValue());
                    configurations.put(ep.getKey(), cp);
                }
            }
            defaultConfiguration = activeEngine;
        } else if (!configurations.isEmpty()) {
            // Set profile IDs
            for (Map.Entry<String, AIConfigurationProfile> ep : configurations.entrySet()) {
                ep.getValue().setProfileId(ep.getKey());
            }

            if (engineConfigurations.isEmpty()) {
                // Copy to engine configs for backward compatibility
                for (Map.Entry<String, AIConfigurationProfile> ep : configurations.entrySet()) {
                    try {
                        engineConfigurations.put(ep.getValue().getEngineId(), ep.getValue().getConfiguration());
                    } catch (DBException e) {
                        log.error(e);
                    }
                }
            }
        }
    }

}

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
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIProperties;
import org.jkiss.dbeaver.model.ai.registry.AIEngineDescriptor;
import org.jkiss.dbeaver.model.ai.registry.AISettingsManager;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * AI global settings.
 * Keeps global parameters and configuration of all AI engines
 */
public class AISettings implements DBPAdaptable {
    protected static final Log log = Log.getLog(AISettings.class);
    private boolean aiDisabled;

    private final Map<String, AIConfigurationProfile> configurations = new LinkedHashMap<>();
    private String defaultConfiguration;
    private transient String userDefaultConfiguration;

    private String activeEngine;
    // Deprecated. Use AIConfigurationProfile instead
    @Deprecated(forRemoval = true)
    @JsonAdapter(AISettingsManager.EngineConfigAdapter.class)
    private final Map<String, AILegacyEngineConfiguration> engineConfigurations = new LinkedHashMap<>();
    private final Map<String, Object> properties = new LinkedHashMap<>();
    private final Map<String, String> customInstructions = new LinkedHashMap<>();

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
        profile.getEngineDescriptor();
        return profile;
    }

    @Nullable
    public AIConfigurationProfile getConfigurationOrNull(@NotNull String profileId) {
        return configurations.get(profileId);
    }

    @Nullable
    public AIConfigurationProfile getConfigurationByNameOrNull(@NotNull String profileName) {
        for (AIConfigurationProfile profile : configurations.values()) {
            if (Objects.equals(profile.getProfileName(), profileName)) {
                return profile;
            }
        }
        return null;
    }

    @NotNull
    public AIConfigurationProfile getConfiguration(@NotNull String profileId) throws DBException {
        AIConfigurationProfile profile = getConfigurationOrNull(profileId);
        if (profile == null) {
            throw new DBException("AI configuration '" + profileId + "' not found");
        }
        return profile;
    }

    @Nullable
    public AIConfigurationProfile getDefaultConfigurationOrNull() {
        AIConfigurationProfile userProfile = configurations.get(userDefaultConfiguration);
        if (userProfile != null) {
            return userProfile;
        }
        if (CommonUtils.isEmpty(defaultConfiguration)) {
            if (!configurations.isEmpty()) {
                defaultConfiguration = configurations.values().iterator().next().getProfileId();
            }
        }
        AIConfigurationProfile profile = configurations.get(defaultConfiguration);
        if (profile == null) {
            if (configurations.isEmpty()) {
                return null;
            }
            profile = configurations.values().iterator().next();
            defaultConfiguration = profile.getProfileId();
        }
        return profile;
    }

    public void setUserDefaultConfiguration(@Nullable AIConfigurationProfile profile) {
        userDefaultConfiguration = profile == null ? null : profile.getProfileId();
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
        profile.setConfiguration(engine.createPropertiesInstance());
        profile.resolveSecrets();
        configurations.put(id, profile);

        return profile;
    }

    @NotNull
    public String generateProfileId(@NotNull AIEngineDescriptor engine) {
        String baseId = engine.getId();
        String id = baseId;
        for (int i = 1; getConfigurationOrNull(id) != null; i++) {
            id = baseId + "_" + i;
        }
        return id;
    }

    @NotNull
    public String generateProfileName(@NotNull String baseName) {
        String name = baseName;
        for (int i = 1; getConfigurationByNameOrNull(name) != null; i++) {
            name = baseName + " (" + i + ")";
        }
        return name;
    }

    @NotNull
    public synchronized AIConfigurationProfile copyConfiguration(@NotNull AIConfigurationProfile source) throws DBException {
        return copyConfiguration(
            source,
            generateProfileId(source.getEngineDescriptor()),
            generateProfileName(source.getProfileName())
        );
    }

    @NotNull
    public synchronized AIConfigurationProfile copyConfiguration(
        @NotNull AIConfigurationProfile source,
        @NotNull String id
    ) throws DBException {
        return copyConfiguration(source, id, generateProfileName(source.getProfileName()));
    }

    @NotNull
    public synchronized AIConfigurationProfile copyConfiguration(
        @NotNull AIConfigurationProfile source,
        @NotNull String id,
        @NotNull String name
    ) throws DBException {
        AIConfigurationProfile copy = createConfiguration(id, source.getEngineDescriptor());
        copy.setProfileName(name);
        copy.setGlobal(source.isGlobal());
        AIEngineProperties sourceConfiguration = source.getConfiguration();
        copy.setConfiguration(AISettingsManager.READ_PROPS_GSON.fromJson(
            AISettingsManager.READ_PROPS_GSON.toJson(sourceConfiguration),
            sourceConfiguration.getClass()
        ));
        if (!AISettingsManager.saveSecretsAsPlainText()) {
            copy.saveSecrets();
        }
        copy.resolveSecrets();
        return copy;
    }

    public void removeConfiguration(@NotNull AIConfigurationProfile profile) {
        configurations.remove(profile.getProfileId());
        if (CommonUtils.equalObjects(defaultConfiguration, profile.getProfileId())) {
            defaultConfiguration = configurations.isEmpty() ? null : configurations.keySet().iterator().next();
        }
        // Remove in legacy config too
        engineConfigurations.remove(profile.getEngineId());
        if (!profile.isAvailable()) {
            return;
        }
        try {
            profile.getConfiguration().deleteSecrets(profile);
        } catch (DBException e) {
            log.error(e);
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
        AISettingsManager.getInstance().saveSettings(this);
    }

    @Override
    public <T> T getAdapter(@NotNull Class<T> adapter) {
        return null;
    }

    // Patches configuration to support legacy configuration format
    public void finishSettingsLoading() {
        boolean migrateLegacyConfigurations = configurations.isEmpty() && !engineConfigurations.isEmpty();
        if (migrateLegacyConfigurations) {
            // Profiles from engine settings
            for (Map.Entry<String, AILegacyEngineConfiguration> entry : engineConfigurations.entrySet()) {
                String engineId = entry.getKey();
                AIConfigurationProfile profile = AISettingsManager.EngineConfigAdapter.toProfile(engineId, entry.getValue());
                if (profile.isAvailable()) {
                    try {
                        if (profile.getConfiguration() instanceof OpenAIProperties properties
                            && CommonUtils.isEmpty(properties.getModel())) {
                            properties.setModel(OpenAIConstants.LEGACY_DEFAULT_MODEL);
                        }
                    } catch (DBException e) {
                        log.error(e);
                    }
                }
                configurations.put(engineId, profile);
            }
        }

        // keep the saved engine even when this product does not provide it
        if (activeEngine == null) {
            activeEngine = OpenAIConstants.OPENAI_ENGINE;
        }

        if (migrateLegacyConfigurations) {
            defaultConfiguration = activeEngine;
        } else if (!configurations.isEmpty()) {
            // Set profile IDs
            for (Map.Entry<String, AIConfigurationProfile> ep : configurations.entrySet()) {
                ep.getValue().setProfileId(ep.getKey());
            }
        }
    }

    public void prepareSettingsSaving() throws DBException {
        for (AIConfigurationProfile profile : configurations.values()) {
            if (!profile.isAvailable()) {
                continue;
            }
            String engineId = profile.getEngineId();
            if (!engineConfigurations.containsKey(engineId)
                || profile.isMigrated() && profile.getProfileId().equals(engineId)) {
                engineConfigurations.put(engineId, new AILegacyEngineConfiguration(profile.getConfiguration()));
            }
        }
        // legacy settings are shared, so a per-user default must not choose their representative
        AIConfigurationProfile sharedDefault = configurations.get(defaultConfiguration);
        if (sharedDefault != null && sharedDefault.isAvailable()) {
            engineConfigurations.put(sharedDefault.getEngineId(), new AILegacyEngineConfiguration(sharedDefault.getConfiguration()));
        }
    }

    public void resolveSecrets() {
        for (AIConfigurationProfile profile : this.getConfigurations()) {
            if (!profile.isAvailable()) {
                continue;
            }
            try {
                profile.getConfiguration().resolveSecrets(profile);
            } catch (DBException e) {
                log.error("Error resolving profile '" + profile.getProfileId() + "' secrets", e);
            }
        }
    }


}

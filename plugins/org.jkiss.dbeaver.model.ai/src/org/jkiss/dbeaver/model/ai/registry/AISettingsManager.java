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
package org.jkiss.dbeaver.model.ai.registry;

import com.google.gson.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.WorkspaceConfigEventManager;
import org.jkiss.dbeaver.model.ai.AISettings;
import org.jkiss.dbeaver.model.ai.engine.AIEngineProperties;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIConstants;
import org.jkiss.dbeaver.model.app.DBPApplication;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.PropertySerializationUtils;
import org.jkiss.utils.CommonUtils;

import java.io.StringReader;
import java.util.*;

public class AISettingsManager {
    private static final Log log = Log.getLog(AISettingsManager.class);

    public static final String AI_CONFIGURATION_FILE_NAME = "ai-configuration.json";

    private static final String AI_DISABLED_KEY = "aiDisabled";
    private static final String ACTIVE_ENGINE_KEY = "activeEngine";
    private static final String PROPERTIES_KEY = "properties";
    private static final String ENGINE_CONFIGURATIONS_KEY = "engineConfigurations";
    private static final String FUNCTIONS_ENABLED_KEY = "functionsEnabled";
    private static final String ENABLED_FUNCTION_CATEGORIES_KEY = "enabledFunctionCategories";
    private static final String ENABLED_FUNCTIONS_KEY = "enabledFunctions";
    private static final String CUSTOM_INSTRUCTIONS_KEY = "customInstructions";
    public static final String ENGINE_PROPERTIES = "properties";

    private static AISettingsManager instance = null;

    public static final Gson READ_PROPS_GSON = createPropertiesLoadGson();
    public static final Gson SAVE_PROPS_GSON = createPropertiesSaveGson();

    private final Set<AISettingsEventListener> settingsChangedListeners = Collections.synchronizedSet(new HashSet<>());

    private AISettingsManager() {
        WorkspaceConfigEventManager.addConfigChangedListener(
            AI_CONFIGURATION_FILE_NAME, o -> {
                // reset current context for settings to be lazily reloaded when needed
                this.getSettingsHolder().reset();
                this.raiseChangedEvent(this); // consider detailed event info
            });
    }

    public static synchronized AISettingsManager getInstance() {
        if (instance == null) {
            instance = new AISettingsManager();
        }
        return instance;
    }

    public void addChangedListener(AISettingsEventListener listener) {
        this.settingsChangedListeners.add(listener);
    }

    public void removeChangedListener(AISettingsEventListener listener) {
        this.settingsChangedListeners.remove(listener);
    }

    private void raiseChangedEvent(AISettingsManager registry) {
        for (AISettingsEventListener listener : this.settingsChangedListeners.toArray(AISettingsEventListener[]::new)) {
            listener.onSettingsUpdate(registry);
        }
    }

    private AISettingsHolder getSettingsHolder() {
        return AISettingsLocalHolder.INSTANCE;
    }

    @NotNull
    public AISettings getSettings() {
        return this.getSettingsHolder().getSettings();
    }

    @NotNull
    private static AISettings loadSettingsFromConfig() {
        Map<String, Object> configMap = null;
        try {
            String content = loadConfig();
            if (!CommonUtils.isEmpty(content)) {
                configMap = READ_PROPS_GSON.fromJson(new StringReader(content), JSONUtils.MAP_TYPE_TOKEN);
            }
        } catch (Exception e) {
            log.error("Error loading AI settings, falling back to defaults.", e);
        }
        if (configMap == null) {
            configMap = new LinkedHashMap<>();
        }

        AISettings settings = new AISettings();

        {
            Map<String, AIEngineProperties> engineConfigurationMap = new LinkedHashMap<>();

            if (!configMap.isEmpty()) {
                settings.setAiDisabled(JSONUtils.getBoolean(configMap, AI_DISABLED_KEY));
                settings.setActiveEngine(JSONUtils.getString(configMap, ACTIVE_ENGINE_KEY));
                JSONUtils.getObject(configMap, PROPERTIES_KEY).forEach(settings::setProperty);

                List<String> enabledCategories = JSONUtils.getStringList(configMap, ENABLED_FUNCTION_CATEGORIES_KEY);
                if (!enabledCategories.isEmpty()) {
                    settings.setEnabledFunctionCategories(new HashSet<>(enabledCategories));
                }
                settings.setFunctionsEnabled(JSONUtils.getBoolean(configMap, FUNCTIONS_ENABLED_KEY, true));
                List<String> enabledFunctions = JSONUtils.getStringList(configMap, ENABLED_FUNCTIONS_KEY);
                if (!enabledFunctions.isEmpty()) {
                    settings.setEnabledFunctions(new HashSet<>(enabledFunctions));
                }

                @SuppressWarnings("unchecked")
                Map<String, String> customInstructions = (Map<String, String>) configMap.get(CUSTOM_INSTRUCTIONS_KEY);
                if (!CommonUtils.isEmpty(customInstructions)) {
                    settings.setCustomInstructions(customInstructions);
                }

                Map<String, Object> ecRoot = JSONUtils.getObject(configMap, ENGINE_CONFIGURATIONS_KEY);

                for (Map.Entry<String, Object> entry : ecRoot.entrySet()) {
                    String engineId = entry.getKey();
                    AIEngineDescriptor engineDescriptor = AIEngineRegistry.getInstance().getEngineDescriptor(engineId);
                    if (engineDescriptor == null) {
                        log.error("AI engine '" + engineId + "' not found. Ignore config");
                        continue;
                    }
                    if (entry.getValue() instanceof Map map) {
                        try {
                            Map<String, Object> properties = JSONUtils.getObject(map, ENGINE_PROPERTIES);
                            JsonElement engineConfigTree = READ_PROPS_GSON.toJsonTree(properties, Map.class);
                            AIEngineProperties engineSettings = READ_PROPS_GSON.fromJson(
                                engineConfigTree, engineDescriptor.getPropertiesType());

                            engineConfigurationMap.put(engineDescriptor.getId(), engineSettings);
                        } catch (JsonSyntaxException e) {
                            log.error("Error parsing '" + engineId + "' properties", e);
                        }
                    }
                }
            }

            if (settings.getEnabledFunctionCategories().isEmpty()) {
                settings.setEnabledFunctionCategories(
                    AIFunctionRegistry.getInstance().getDefaultEnabledCategoryIds()
                );
            }

            settings.setEngineConfigurations(engineConfigurationMap);
        }
        if (settings.activeEngine() == null || !settings.hasConfiguration(settings.activeEngine())) {
            settings.setActiveEngine(OpenAIConstants.OPENAI_ENGINE);
        }

        // Fill missing settings
        Map<String, AIEngineProperties> configurations = settings.getEngineConfigurations();
        for (AIEngineDescriptor aed : AIEngineRegistry.getInstance().getCompletionEngines()) {
            if (!configurations.containsKey(aed.getId())) {
                try {
                    configurations.put(aed.getId(), aed.createPropertiesInstance());
                } catch (DBException e) {
                    log.error(e);
                }
            }
        }

        return settings;
    }

    public void saveSettings(@NotNull AISettings settings) {
        try {
            if (!DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_CONFIGURATION_MANAGER)) {
                log.warn("The user has no permission to save AI configuration");
                return;
            }

            JsonObject json = new JsonObject();
            json.addProperty(AI_DISABLED_KEY, settings.isAiDisabled());
            json.addProperty(ACTIVE_ENGINE_KEY, settings.activeEngine());

            JsonObject propertiesObject = new JsonObject();
            for (Map.Entry<String, Object> property : settings.getAllProperties().entrySet()) {
                JsonElement propValue = SAVE_PROPS_GSON.toJsonTree(property.getValue());
                propertiesObject.add(property.getKey(), propValue);
            }
            json.add(PROPERTIES_KEY, propertiesObject);

            json.add(FUNCTIONS_ENABLED_KEY, new JsonPrimitive(settings.isFunctionsEnabled()));
            Set<String> enabledCategories = settings.getEnabledFunctionCategories();
            if (!enabledCategories.isEmpty()) {
                JsonArray categoriesArray = new JsonArray();
                for (String category : enabledCategories) {
                    categoriesArray.add(category);
                }
                json.add(ENABLED_FUNCTION_CATEGORIES_KEY, categoriesArray);
            }

            Set<String> enabledFunctions = settings.getEnabledFunctions();
            if (!enabledFunctions.isEmpty()) {
                JsonArray functionsArray = new JsonArray();
                for (String function : enabledFunctions) {
                    functionsArray.add(function);
                }
                json.add(ENABLED_FUNCTIONS_KEY, functionsArray);
            }

            Map<String, String> customInstructions = settings.getCustomInstructions();
            if (!customInstructions.isEmpty()) {
                JsonObject object = new JsonObject();

                for (Map.Entry<String, String> entry : customInstructions.entrySet()) {
                    object.addProperty(entry.getKey(), entry.getValue());
                }
                json.add(CUSTOM_INSTRUCTIONS_KEY, object);
            }

            JsonObject engineConfigurations = new JsonObject();
            for (Map.Entry<String, AIEngineProperties> configuration : settings.getEngineConfigurations().entrySet()) {
                JsonElement savedProps = SAVE_PROPS_GSON.toJsonTree(configuration.getValue());
                if (savedProps instanceof JsonObject jo && !jo.isEmpty()) {
                    JsonObject props = new JsonObject();
                    props.add(ENGINE_PROPERTIES, savedProps);
                    engineConfigurations.add(configuration.getKey(), props);
                }
            }
            json.add(ENGINE_CONFIGURATIONS_KEY, engineConfigurations);

            String content = SAVE_PROPS_GSON.toJson(json);

            DBWorkbench.getPlatform().getConfigurationController().saveConfigurationFile(AI_CONFIGURATION_FILE_NAME, content);

            if (!saveSecretsAsPlainText()) {
                settings.saveSecrets();
            }

            this.getSettingsHolder().setSettings(settings);
        } catch (Exception e) {
            log.error("Error saving AI configuration", e);
        }
        raiseChangedEvent(this);
    }


    @Nullable
    private static String loadConfig() throws DBException {
        return DBWorkbench.getPlatform()
            .getConfigurationController()
            .loadConfigurationFile(AI_CONFIGURATION_FILE_NAME);
    }

    public static boolean isConfigExists() throws DBException {
        String content = loadConfig();
        return CommonUtils.isNotEmpty(content);
    }

    public static boolean saveSecretsAsPlainText() {
        DBPApplication application = DBWorkbench.getPlatform().getApplication();
        return application.isMultiuser() || application.isDistributed();
    }


    @NotNull
    private static Gson createPropertiesLoadGson() {
        return new GsonBuilder()
            .setStrictness(Strictness.LENIENT)
            .create();
    }

    @NotNull
    private static Gson createPropertiesSaveGson() {
        if (saveSecretsAsPlainText()) {
            return createPropertiesLoadGson();
        } else {
            return PropertySerializationUtils.baseNonSecurePropertiesGsonBuilder().create();
        }
    }

    private interface AISettingsHolder {
        AISettings getSettings();

        void setSettings(AISettings mruSettings);

        void reset();
    }

    private static class AISettingsLocalHolder implements AISettingsHolder {
        public static final AISettingsHolder INSTANCE = new AISettingsLocalHolder();

        private AISettings settings = null;

        @Override
        public synchronized AISettings getSettings() {
            if (settings == null) {
                AISettings loaded = loadSettingsFromConfig();
                // This check prevents redundant reloading of settings by the same thread.
                // Reason: loadSettingsFromConfig() may initiate loading of other bundles,
                // which could lead subsequently to calls back into this method to
                // modify the settings during initialization, leading to multiple
                // loads and potential inconsistencies without this safeguard.

                if (settings == null) {
                    settings = loaded;
                }
            }

            return settings;
        }

        @Override
        public synchronized void setSettings(AISettings mruSettings) {
            this.settings = mruSettings;
        }

        @Override
        public synchronized void reset() {
            this.settings = null;
        }
    }
}

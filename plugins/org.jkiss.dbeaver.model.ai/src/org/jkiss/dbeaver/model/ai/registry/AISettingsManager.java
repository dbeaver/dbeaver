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
import org.jkiss.dbeaver.model.auth.SMSessionPersistent;
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
    private static final String ENGINE_CONFIGURATIONS_KEY = "engineConfigurations";
    private static final String ENABLED_FUNCTION_CATEGORIES_KEY = "enabledFunctionCategories";
    private static final String ENABLED_FUNCTIONS_KEY = "enabledFunctions";
    public static final String ENGINE_PROPERTIES = "properties";

    private static AISettingsManager instance = null;

    private static final Gson readPropsGson = createPropertiesLoadGson();
    private static final Gson savePropsGson = createPropertiesSaveGson();

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
        if (DBWorkbench.getPlatform().getWorkspace().getWorkspaceSession() instanceof SMSessionPersistent session) {
            return AISettingsSessionHolder.getForSession(session);
        } else {
            return AISettingsLocalHolder.INSTANCE;
        }
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
                configMap = readPropsGson.fromJson(new StringReader(content), JSONUtils.MAP_TYPE_TOKEN);
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

                List<String> enabledCategories = JSONUtils.getStringList(configMap, ENABLED_FUNCTION_CATEGORIES_KEY);
                if (!enabledCategories.isEmpty()) {
                    settings.setEnabledFunctionCategories(new HashSet<>(enabledCategories));
                }

                List<String> enabledFunctions = JSONUtils.getStringList(configMap, ENABLED_FUNCTIONS_KEY);
                if (!enabledFunctions.isEmpty()) {
                    settings.setEnabledFunctions(new HashSet<>(enabledFunctions));
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
                            JsonElement engineConfigTree = readPropsGson.toJsonTree(properties, Map.class);
                            AIEngineProperties engineSettings = readPropsGson.fromJson(
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


            JsonObject engineConfigurations = new JsonObject();
            for (Map.Entry<String, AIEngineProperties> configuration : settings.getEngineConfigurations().entrySet()) {
                JsonElement savedProps = savePropsGson.toJsonTree(configuration.getValue());
                if (savedProps instanceof JsonObject jo && !jo.isEmpty()) {
                    JsonObject props = new JsonObject();
                    props.add(ENGINE_PROPERTIES, savedProps);
                    engineConfigurations.add(configuration.getKey(), props);
                }
            }
            json.add(ENGINE_CONFIGURATIONS_KEY, engineConfigurations);

            String content = savePropsGson.toJson(json);

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

    private static class AISettingsSessionHolder implements AISettingsHolder {
        private static final Map<SMSessionPersistent, AISettingsSessionHolder> holderBySession
            = Collections.synchronizedMap(new WeakHashMap<>());

        private final SMSessionPersistent session;

        private volatile AISettings mruSettings = null;
        private volatile boolean settingsReadInProgress = false;

        private AISettingsSessionHolder(SMSessionPersistent session) {
            this.session = session;
        }

        public static AISettingsHolder getForSession(SMSessionPersistent session) {
            return holderBySession.computeIfAbsent(session, AISettingsSessionHolder::new);
        }

        public static void resetAll() {
            holderBySession.clear();
        }

        @Override
        public synchronized AISettings getSettings() {
            AISettings mruSettings = this.mruSettings;
            AISettings sharedSettings = this.session.getAttribute(AISettings.class.getName());
            if (mruSettings == null || !mruSettings.equals(sharedSettings)) {
                if (settingsReadInProgress) {
                    // FIXME: it is a hack. Settings loading may cause infinite recursion because
                    // conf loading shows UI which may re-ask settings
                    // The fix is to disable UI during config read? But this lead to UI freeze..
                    return new AISettings();
                }
                settingsReadInProgress = true;
                try {
                    // if current context is not initialized or was invalidated, then reload settings for this session
                    this.setSettings(mruSettings = loadSettingsFromConfig());
                } finally {
                    settingsReadInProgress = false;
                }
            }
            return mruSettings;
        }

        @Override
        public synchronized void setSettings(AISettings mruSettings) {
            this.mruSettings = mruSettings;
            this.session.setAttribute(AISettings.class.getName(), mruSettings);
        }

        @Override
        public synchronized void reset() {
            // session contexts are not differentiated for now, so simply invalidate all of them
            resetAll();
        }
    }

    private static class AISettingsLocalHolder implements AISettingsHolder {
        public static final AISettingsHolder INSTANCE = new AISettingsLocalHolder();

        private AISettings settings = null;

        @Override
        public synchronized AISettings getSettings() {
            AISettings settings = this.settings;
            if (settings == null) {
                // if current context is not initialized or was invalidated, then reload settings
                this.settings = settings = loadSettingsFromConfig();
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

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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.WorkspaceConfigEventManager;
import org.jkiss.dbeaver.model.ai.AISettings;
import org.jkiss.dbeaver.model.ai.engine.AIEngineProperties;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIConstants;
import org.jkiss.dbeaver.model.app.DBPApplication;
import org.jkiss.dbeaver.model.auth.SMSessionPersistent;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.PropertySerializationUtils;
import org.jkiss.utils.CommonUtils;

import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.*;

public class AISettingsManager {
    private static final Log log = Log.getLog(AISettingsManager.class);

    public static final String AI_CONFIGURATION_FILE_NAME = "ai-configuration.json";

    private static final String AI_DISABLED_KEY = "aiDisabled";
    private static final String ACTIVE_ENGINE_KEY= "activeEngine";
    private static final String ENGINE_CONFIGURATIONS_KEY = "engineConfigurations";

    private static AISettingsManager instance = null;

    private static final Gson readPropsGson = createPropertiesLoadGson();
    private static final Gson savePropsGson = createPropertiesSaveGson();

    private final Set<AISettingsEventListener> settingsChangedListeners = Collections.synchronizedSet(new HashSet<>());

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
        AISettings settings = null;
        try {
            String content = loadConfig();
            if (!CommonUtils.isEmpty(content)) {
                settings = readPropsGson.fromJson(new StringReader(content), AISettings.class);
            }
        } catch (Exception e) {
            log.error("Error loading AI settings, falling back to defaults.", e);
        }
        if (settings == null) {
            settings = new AISettings();
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

    public void saveSettings(AISettings settings) {
        try {
            if (!DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_CONFIGURATION_MANAGER)) {
                log.warn("The user has no permission to save AI configuration");
                return;
            }

            if (!saveSecretsAsPlainText()) {
                settings.saveSecrets();
            }
            String content = savePropsGson.toJson(settings);

            DBWorkbench.getPlatform().getConfigurationController().saveConfigurationFile(AI_CONFIGURATION_FILE_NAME, content);
            this.getSettingsHolder().setSettings(settings);
        } catch (Exception e) {
            log.error("Error saving AI configuration", e);
        }
        raiseChangedEvent(this);
    }

    private static String loadConfig() throws DBException {
        return DBWorkbench.getPlatform()
            .getConfigurationController()
            .loadConfigurationFile(AI_CONFIGURATION_FILE_NAME);
    }

    public static boolean isConfigExists() throws DBException {
        String content = loadConfig();
        return CommonUtils.isNotEmpty(content);
    }

    private static class AIConfigurationSerDe
        implements JsonSerializer<AISettings>, JsonDeserializer<AISettings> {

        @Override
        public AISettings deserialize(
            JsonElement json,
            Type typeOfT,
            JsonDeserializationContext context
        ) throws JsonParseException {
            AISettings aiSettings = new AISettings();
            Map<String, AIEngineProperties> engineConfigurationMap = new LinkedHashMap<>();

            if (json != null && json.isJsonObject()) {
                JsonObject root = json.getAsJsonObject();

                JsonElement aiDisabledEl = root.get(AI_DISABLED_KEY);
                aiSettings.setAiDisabled(
                    aiDisabledEl != null
                        && aiDisabledEl.isJsonPrimitive()
                        && aiDisabledEl.getAsJsonPrimitive().isBoolean()
                        && aiDisabledEl.getAsBoolean()
                );

                JsonElement activeEngineEl = root.get(ACTIVE_ENGINE_KEY);
                aiSettings.setActiveEngine(
                    activeEngineEl != null && !activeEngineEl.isJsonNull()
                        ? activeEngineEl.getAsString()
                        : null
                );

                JsonObject ecRoot = root.has(ENGINE_CONFIGURATIONS_KEY)
                    && root.get(ENGINE_CONFIGURATIONS_KEY).isJsonObject()
                    ? root.getAsJsonObject(ENGINE_CONFIGURATIONS_KEY)
                    : new JsonObject();

                //Map<String, String> replacements = AIEngineSettingsRegistry.getInstance().getReplacements();
                for (Map.Entry<String, JsonElement> entry : ecRoot.entrySet()) {
                    String engineId = entry.getKey();
//                    String engineIdReplaced = replacements.get(engineId);
//                    String fallbackEngineId = Optional.ofNullable(
//                            entry.getValue()
//                                .getAsJsonObject()
//                                .get(AIEngineSettings.FALLBACK_ENGINE_ID)
//                        )
//                        .map(JsonElement::getAsString)
//                        .orElse(null);

                    AIEngineDescriptor engineDescriptor = AIEngineRegistry.getInstance().getEngineDescriptor(engineId);
                    try {
                        AIEngineProperties engineSettings = readPropsGson.fromJson(
                            entry.getValue(), engineDescriptor.getPropertiesType());

                        engineConfigurationMap.put(engineId, engineSettings);
                    } catch (JsonSyntaxException e) {
                        log.error("Error parsing '" + engineId + "' properties", e);
                    }
                }
            }

            aiSettings.setEngineConfigurations(engineConfigurationMap);

            return aiSettings;
        }

        @Override
        public JsonElement serialize(AISettings src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            json.addProperty(AI_DISABLED_KEY, src.isAiDisabled());
            json.addProperty(ACTIVE_ENGINE_KEY, src.activeEngine());

            JsonObject engineConfigurations = new JsonObject();
            for (Map.Entry<String, AIEngineProperties> configuration : src.getEngineConfigurations().entrySet()) {
                JsonElement savedProps = savePropsGson.toJsonTree(configuration.getValue());
                engineConfigurations.add(configuration.getKey(), savedProps);
            }
            json.add(ENGINE_CONFIGURATIONS_KEY, engineConfigurations);

            return json;
        }
    }

    public static boolean saveSecretsAsPlainText() {
        DBPApplication application = DBWorkbench.getPlatform().getApplication();
        return application.isMultiuser() || application.isDistributed();
    }


    @NotNull
    private static Gson createPropertiesLoadGson() {
        return new GsonBuilder()
            .setStrictness(Strictness.LENIENT)
            .registerTypeAdapter(AISettings.class, new AIConfigurationSerDe())
            .create();
    }

    private static Gson createPropertiesSaveGson() {
        if (saveSecretsAsPlainText()) {
            return createPropertiesLoadGson();
        } else {
            return PropertySerializationUtils.baseNonSecurePropertiesGsonBuilder()
                .registerTypeAdapter(AISettings.class, new AIConfigurationSerDe())
                .create();
        }
    }
}

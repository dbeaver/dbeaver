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

import com.google.gson.*;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.WorkspaceConfigEventManager;
import org.jkiss.dbeaver.model.ai.openai.OpenAIConstants;
import org.jkiss.dbeaver.model.app.DBPApplication;
import org.jkiss.dbeaver.model.auth.SMSessionPersistent;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.PropertySerializationUtils;
import org.jkiss.utils.CommonUtils;

import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

public class AISettingsRegistry {
    private static final Log log = Log.getLog(AISettingsRegistry.class);
    private static final String AI_DISABLED_KEY = "aiDisabled";
    private static final String ACTIVE_ENGINE_KEY= "activeEngine";
    private static final String ENGINE_CONFIGURATIONS_KEY = "engineConfigurations";

    public static final String AI_CONFIGURATION_JSON = "ai-configuration.json";


    private static AISettingsRegistry instance = null;

    private static final Gson readPropsGson = new GsonBuilder()
        .setStrictness(Strictness.LENIENT)
        .registerTypeAdapter(AISettings.class, new AIConfigurationSerDe())
        .create();
    private static final Gson savePropsGson = savePropsGson();

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
            return holderBySession.computeIfAbsent(session, s -> new AISettingsSessionHolder(s));
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


    private AISettingsRegistry() {
        WorkspaceConfigEventManager.addConfigChangedListener(AI_CONFIGURATION_JSON, o -> {
            // reset current context for settings to be lazily reloaded when needed
            this.getSettingsHolder().reset();
            this.raiseChangedEvent(this); // consider detailed event info
        });
    }

    public static synchronized AISettingsRegistry getInstance() {
        if (instance == null) {
            instance = new AISettingsRegistry();
        }
        return instance;
    }

    public void addChangedListener(AISettingsEventListener listener) {
        this.settingsChangedListeners.add(listener);
    }

    public void removeChangedListener(AISettingsEventListener listener) {
        this.settingsChangedListeners.remove(listener);
    }

    private void raiseChangedEvent(AISettingsRegistry registry) {
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
        AISettings settings;
        try {
            String content = loadConfig();
            if (CommonUtils.isEmpty(content)) {
                settings = prepareDefaultSettings();
            } else {
                settings = readPropsGson.fromJson(new StringReader(content), AISettings.class);
            }
        } catch (Exception e) {
            log.error("Error loading AI settings, falling back to defaults.", e);
            settings = prepareDefaultSettings();
        }

        if (settings.activeEngine() == null) {
            settings.setActiveEngine(OpenAIConstants.OPENAI_ENGINE);
        }

        return settings;
    }

    private static AISettings prepareDefaultSettings() {
        AISettings settings = new AISettings();
        if (DBWorkbench.getPlatform().getPreferenceStore().getString(AICompletionConstants.AI_DISABLED) != null) {
            settings.setAiDisabled(DBWorkbench.getPlatform().getPreferenceStore().getBoolean(AICompletionConstants.AI_DISABLED));
        } else {
            // Enable AI by default
            settings.setAiDisabled(false);
        }

        Map<String, AIEngineSettings<?>> stringMap = getSerDes().stream()
            .collect(Collectors.toMap(
                AIEngineSettingsSerDe::getId,
                serDe -> serDe.deserialize(null, readPropsGson)
            ));

        settings.setEngineConfigurations(stringMap);

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

            DBWorkbench.getPlatform().getConfigurationController().saveConfigurationFile(AI_CONFIGURATION_JSON, content);
            this.getSettingsHolder().setSettings(settings);
        } catch (Exception e) {
            log.error("Error saving AI configuration", e);
        }
        raiseChangedEvent(this);
    }

    private static String loadConfig() throws DBException {
        return DBWorkbench.getPlatform()
            .getConfigurationController()
            .loadConfigurationFile(AI_CONFIGURATION_JSON);
    }

    public static boolean isConfigExists() throws DBException {
        String content = loadConfig();
        return CommonUtils.isNotEmpty(content);
    }

    private static class AIConfigurationSerDe
        implements JsonSerializer<AISettings>, JsonDeserializer<AISettings> {
        private final List<AIEngineSettingsSerDe<?>> engineSerDe = getSerDes();

        @Override
        public AISettings deserialize(
            JsonElement json,
            Type typeOfT,
            JsonDeserializationContext context
        ) throws JsonParseException {
            if (json == null || !json.isJsonObject()) {
                return prepareDefaultSettings();
            }

            JsonObject root = json.getAsJsonObject();
            AISettings aiSettings = new AISettings();

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

            Map<String, AIEngineSettings<?>> engineConfigurationMap = engineSerDe.stream()
                .collect(Collectors.toMap(
                    AIEngineSettingsSerDe::getId,
                        serDe -> serDe.deserialize(ecRoot.getAsJsonObject(serDe.getId()), readPropsGson)
                    )
                );
            aiSettings.setEngineConfigurations(engineConfigurationMap);

            return aiSettings;
        }

        @Override
        public JsonElement serialize(AISettings src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject json = new JsonObject();
            json.addProperty(AI_DISABLED_KEY, src.isAiDisabled());
            json.addProperty(ACTIVE_ENGINE_KEY, src.activeEngine());

            JsonObject engineConfigurations = new JsonObject();
            for (AIEngineSettingsSerDe<?> serDe : engineSerDe) {
                try {
                    engineConfigurations.add(serDe.getId(), serDe.serialize(src.getEngineConfiguration(serDe.getId()), savePropsGson()));
                } catch (DBException e) {
                    throw new JsonParseException("Error serializing AI engine settings: " + serDe.getId(), e);
                }
            }
            json.add(ENGINE_CONFIGURATIONS_KEY, engineConfigurations);

            return json;
        }
    }

    private static List<AIEngineSettingsSerDe<?>> getSerDes() {
        List<AIEngineSettingsSerDe<?>> result = new ArrayList<>();
        for (IConfigurationElement iConfigurationElement : Platform.getExtensionRegistry()
            .getConfigurationElementsFor(AIEngineConfigurationSerDeDescriptor.EXTENSION_ID)) {
            AIEngineConfigurationSerDeDescriptor descriptor = new AIEngineConfigurationSerDeDescriptor(iConfigurationElement);
            try {
                result.add(descriptor.createInstance());
            } catch (DBException e) {
                throw new IllegalStateException(e);
            }
        }
        return result;
    }

    private static boolean saveSecretsAsPlainText() {
        DBPApplication application = DBWorkbench.getPlatform().getApplication();
        return application.isMultiuser() || application.isDistributed();
    }

    private static Gson savePropsGson() {
        if (saveSecretsAsPlainText()) {
            return new GsonBuilder()
                .setStrictness(Strictness.LENIENT)
                .registerTypeAdapter(AISettings.class, new AIConfigurationSerDe())
                .create();
        } else {
            return PropertySerializationUtils.baseNonSecurePropertiesGsonBuilder()
                .registerTypeAdapter(AISettings.class, new AIConfigurationSerDe())
                .create();
        }
    }
}

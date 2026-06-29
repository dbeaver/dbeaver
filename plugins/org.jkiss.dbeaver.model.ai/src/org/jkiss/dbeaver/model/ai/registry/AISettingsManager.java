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
import org.jkiss.dbeaver.model.ai.engine.AICredentialsProvider;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIConstants;
import org.jkiss.dbeaver.model.app.DBPApplication;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.PropertySerializationUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class AISettingsManager {
    private static final Log log = Log.getLog(AISettingsManager.class);

    public static final String AI_CONFIGURATION_FILE_NAME = "ai-configuration.json";

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

    @NotNull
    private AISettingsHolder getSettingsHolder() {
        return AISettingsLocalHolder.INSTANCE;
    }

    @NotNull
    public AISettings getSettings() {
        return this.getSettingsHolder().getSettings();
    }

    @NotNull
    private static AISettings loadSettingsFromConfig() {
        try {
            String content = loadConfig();
            AISettings settings;
            if (!CommonUtils.isEmpty(content)) {
                settings = READ_PROPS_GSON.fromJson(content, AISettings.class);

                String activeEngine = settings.activeEngine();
                if (activeEngine == null || !settings.hasConfiguration(activeEngine)) {
                    settings.setActiveEngine(OpenAIConstants.OPENAI_ENGINE);
                }
            } else {
                settings = new AISettings();
            }
            return settings;
        } catch (Exception e) {
            log.error("Error loading AI settings, falling back to defaults.", e);
            return new AISettings();
        }
    }

    /**
     * Modify settings with given consumer and save them.
     *
     * @param consumer consumer to modify settings
     */
    public void modifySettings(@NotNull Consumer<AISettings> consumer) {
        AISettings settings = this.getSettings();
        consumer.accept(settings);
        this.saveSettings(settings);
    }

    public void saveSettings(@NotNull AISettings settings) {
        try {
            if (!DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_CONFIGURATION_MANAGER)) {
                log.warn("The user has no permission to save AI configuration");
                return;
            }

            String content = SAVE_PROPS_GSON.toJson(settings);

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
            .registerTypeAdapter(AICredentialsProvider.class, new DBAAuthProviderAdapter())
            .create();
    }

    @NotNull
    private static Gson createPropertiesSaveGson() {
        if (saveSecretsAsPlainText()) {
            return createPropertiesLoadGson();
        } else {
            return PropertySerializationUtils.baseNonSecurePropertiesGsonBuilder()
                .registerTypeAdapter(AICredentialsProvider.class, new DBAAuthProviderAdapter()).create();
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

    static class DBAAuthProviderAdapter implements JsonDeserializer<AICredentialsProvider<?>>, JsonSerializer<AICredentialsProvider<?>> {

        @Override
        public AICredentialsProvider<?> deserialize(
            @NotNull JsonElement json,
            @NotNull Type typeOfT,
            @NotNull JsonDeserializationContext context
        ) {
            JsonObject obj = json.getAsJsonObject();
            String type = obj.get("type").getAsString(); //NON-NLS-1
            DBACredentialsProviderDescriptor authProviderByID = AICredentialsProviderRegistry.getInstance()
                .getCredentialsProviderByID(type);
            if (authProviderByID == null) {
                log.error("Auth provider '" + type + "' not found");
                return null;
            }
            Class<?> providerClass = authProviderByID.getProviderClass();
            return context.deserialize(obj.getAsJsonObject("data"), providerClass); //NON-NLS-1
        }


        @Override
        public JsonElement serialize(
            @NotNull AICredentialsProvider src,
            @NotNull Type typeOfSrc,
            @NotNull JsonSerializationContext context
        ) {

            JsonObject obj = new JsonObject();
            obj.add("data", context.serialize(src, src.getClass())); //NON-NLS-1
            obj.addProperty("type", src.getProviderId()); //NON-NLS-1
            return obj;
        }
    }
}

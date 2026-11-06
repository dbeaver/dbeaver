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
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.WorkspaceConfigEventManager;
import org.jkiss.dbeaver.model.ai.AIConfigurationProfile;
import org.jkiss.dbeaver.model.ai.AIConstants;
import org.jkiss.dbeaver.model.ai.AILegacyEngineConfiguration;
import org.jkiss.dbeaver.model.ai.AISettings;
import org.jkiss.dbeaver.model.ai.engine.AICredentialsProvider;
import org.jkiss.dbeaver.model.ai.engine.AIEngineProperties;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIProperties;
import org.jkiss.dbeaver.model.ai.internal.AIMessages;
import org.jkiss.dbeaver.model.app.DBPApplication;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.PropertySerializationUtils;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
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
                this.notifyProfilesChanged();
                this.raiseChangedEvent(this); // consider detailed event info
            });
    }

    public static synchronized AISettingsManager getInstance() {
        if (instance == null) {
            instance = new AISettingsManager();
        }
        return instance;
    }

    @NotNull
    public static synchronized AISettings getStaticSettings() {
        return getInstance().getSettings();
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

    public void notifyProfilesChanged() {
        for (AISettingsEventListener listener : this.settingsChangedListeners.toArray(AISettingsEventListener[]::new)) {
            listener.onProfilesUpdate(this);
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

                settings.finishSettingsLoading();
                applyUserDefaultConfiguration(settings);
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

    public void setDefaultConfiguration(@Nullable AIConfigurationProfile profile) {
        getSettings().setUserDefaultConfiguration(profile);
        saveDefaultConfiguration(profile);
        raiseChangedEvent(this);
    }

    private static void applyUserDefaultConfiguration(@NotNull AISettings settings) {
        String profileId = DBWorkbench.getPlatform().getPreferenceStore().getString(AIConstants.AI_DEFAULT_CONFIGURATION);
        AIConfigurationProfile profile = settings.getConfigurationOrNull(profileId);
        settings.setUserDefaultConfiguration(profile);
        if (profile == null && !CommonUtils.isEmpty(profileId)) {
            saveDefaultConfiguration(null);
        }
    }

    private static void saveDefaultConfiguration(@Nullable AIConfigurationProfile profile) {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        if (profile == null) {
            store.setToDefault(AIConstants.AI_DEFAULT_CONFIGURATION);
        } else {
            store.setValue(AIConstants.AI_DEFAULT_CONFIGURATION, profile.getProfileId());
        }
        try {
            store.save();
        } catch (IOException e) {
            log.error("Error saving default AI configuration", e);
            DBWorkbench.getPlatformUI().showError(AIMessages.ai_settings_save_error_title, null, e);
        }
    }

    public void saveSettings() {
        saveSettings(getSettings());
    }

    /**
     * Saves the given settings and makes them current
     */
    public void saveSettings(@NotNull AISettings settings) {
        try {
            if (!DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_CONFIGURATION_MANAGER)) {
                log.warn("The user has no permission to save AI configuration");
                return;
            }

            if (!saveSecretsAsPlainText()) {
                AIConfigurationProfile defaultProfile = settings.getDefaultConfigurationOrNull();
                for (AIConfigurationProfile profile : settings.getConfigurations()) {
                    if (!profile.isAvailable()) {
                        continue;
                    }
                    if (profile == defaultProfile) {
                        profile.saveSecrets();
                    } else if (profile.getConfiguration() instanceof OpenAIProperties properties) {
                        properties.saveCustomHeaderSecrets(profile);
                    }
                }
            }

            settings.prepareSettingsSaving();
            String content = SAVE_PROPS_GSON.toJson(settings);

            DBWorkbench.getPlatform().getConfigurationController().saveConfigurationFile(AI_CONFIGURATION_FILE_NAME, content);

            applyUserDefaultConfiguration(settings);
            this.getSettingsHolder().setSettings(settings);
        } catch (Exception e) {
            log.error("Error saving AI configuration", e);
            // the settings were entered by the user, so a failed save must not stay in the log only
            DBWorkbench.getPlatformUI().showError(AIMessages.ai_settings_save_error_title, null, e);
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

    private static void writePreservedJson(@NotNull JsonWriter out, @NotNull JsonObject json) throws IOException {
        boolean serializeNulls = out.getSerializeNulls();
        out.setSerializeNulls(true);
        try {
            SAVE_PROPS_GSON.getAdapter(JsonElement.class).write(out, json);
        } finally {
            out.setSerializeNulls(serializeNulls);
        }
    }

    public static class EngineConfigAdapter extends TypeAdapter<Map<String, AILegacyEngineConfiguration>> {
        @Override
        public void write(@NotNull JsonWriter out, @NotNull Map<String, AILegacyEngineConfiguration> value) throws IOException {
            out.beginObject();
            for (Map.Entry<String, AILegacyEngineConfiguration> entry : value.entrySet()) {
                out.name(entry.getKey());
                AILegacyEngineConfiguration configuration = entry.getValue();
                AIEngineProperties properties = configuration.getProperties();
                if (properties != null) {
                    out.beginObject();
                    out.name("properties");
                    SAVE_PROPS_GSON.toJson(properties, properties.getClass(), out);
                    out.endObject();
                } else {
                    writePreservedJson(out, Objects.requireNonNull(configuration.getUnavailableConfiguration()));
                }
            }
            out.endObject();
        }

        @NotNull
        @Override
        public Map<String, AILegacyEngineConfiguration> read(@NotNull JsonReader in) throws IOException {
            Map<String, AILegacyEngineConfiguration> result = new LinkedHashMap<>();
            in.beginObject();
            while (in.hasNext()) {
                String engineId = in.nextName();
                JsonObject json = JsonParser.parseReader(in).getAsJsonObject();
                AIEngineDescriptor descriptor = AIEngineRegistry.getInstance().getEngineDescriptor(engineId);
                if (descriptor == null) {
                    result.put(engineId, AILegacyEngineConfiguration.unavailable(json));
                } else {
                    AIEngineProperties properties = READ_PROPS_GSON.fromJson(json.get("properties"), descriptor.getPropertiesType());
                    result.put(engineId, new AILegacyEngineConfiguration(properties));
                }
            }
            in.endObject();
            return result;
        }

        @NotNull
        public static AIConfigurationProfile toProfile(@NotNull String engineId, @NotNull AILegacyEngineConfiguration configuration) {
            AIConfigurationProfile profile = new AIConfigurationProfile();
            profile.setProfileId(engineId);
            profile.setProfileName(engineId);
            profile.setEngineId(engineId);
            profile.setMigrated(true);
            AIEngineProperties properties = configuration.getProperties();
            if (properties != null) {
                profile.setConfiguration(properties);
            } else {
                JsonObject json = new JsonObject();
                json.addProperty("name", engineId);
                json.addProperty("engine", engineId);
                json.addProperty("migrated", true);
                json.add("configuration", Objects.requireNonNull(configuration.getUnavailableConfiguration()).get("properties"));
                profile.setUnavailableProfile(json);
            }
            return profile;
        }
    }

    public static class ConfigProfileAdapter extends TypeAdapter<AIConfigurationProfile> {
        @Override
        public void write(@NotNull JsonWriter out, @NotNull AIConfigurationProfile value) throws IOException {
            JsonObject unavailable = value.getUnavailableProfile();
            if (unavailable != null) {
                writePreservedJson(out, unavailable);
                return;
            }
            out.beginObject();
            out.name("name").value(value.getProfileName());
            out.name("engine").value(value.getEngineId());
            if (value.isMigrated()) {
                out.name("migrated").value(true);
            }
            out.name("configuration");
            try {
                AIEngineProperties configuration = value.getConfiguration();
                SAVE_PROPS_GSON.toJson(configuration, configuration.getClass(), out);
            } catch (DBException e) {
                throw new IOException("Error saving AI profile '" + value.getProfileId() + "'", e);
            }
            out.endObject();
        }

        @NotNull
        @Override
        public AIConfigurationProfile read(@NotNull JsonReader in) throws IOException {
            JsonObject json = JsonParser.parseReader(in).getAsJsonObject();
            AIConfigurationProfile result = new AIConfigurationProfile();
            result.setEngineId(json.get("engine").getAsString());
            result.setProfileName(json.has("name") && !json.get("name").isJsonNull()
                ? json.get("name").getAsString() : result.getEngineId());
            if (json.has("migrated")) {
                result.setMigrated(json.get("migrated").getAsBoolean());
            }
            AIEngineDescriptor engineDescriptor = AIEngineRegistry.getInstance().getEngineDescriptor(result.getEngineId());
            if (engineDescriptor == null) {
                result.setUnavailableProfile(json);
            } else if (json.has("configuration") && !json.get("configuration").isJsonNull()) {
                result.setConfiguration(READ_PROPS_GSON.fromJson(json.get("configuration"), engineDescriptor.getPropertiesType()));
            }
            return result;
        }
    }

}

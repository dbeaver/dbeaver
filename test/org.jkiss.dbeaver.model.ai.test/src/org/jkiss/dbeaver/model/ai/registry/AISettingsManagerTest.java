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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.WorkspaceConfigEventManager;
import org.jkiss.dbeaver.model.ai.AIConfigurationProfile;
import org.jkiss.dbeaver.model.ai.AIConstants;
import org.jkiss.dbeaver.model.ai.AISettings;
import org.jkiss.dbeaver.model.ai.engine.AIModel;
import org.jkiss.dbeaver.model.ai.engine.AIModelFeature;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIConstants;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIProperties;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.DBPPlatformUI;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AISettingsManagerTest extends DBeaverUnitTest {

    @Test
    public void savesUserDefaultWithoutChangingSharedConfiguration() throws Exception {
        DBPPlatform platform = Mockito.mock(DBPPlatform.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(platform.getApplication().isMultiuser()).thenReturn(true);
        Mockito.when(platform.getConfigurationController().loadConfigurationFile(AISettingsManager.AI_CONFIGURATION_FILE_NAME))
            .thenReturn("""
                {"defaultConfiguration":"shared","activeEngine":"openai","configurations":{
                    "shared":{"name":"Shared","engine":"openai","configuration":{}},
                    "personal":{"name":"Personal","engine":"openai","configuration":{}}
                }}
                """);
        AIEngineRegistry registry = Mockito.mock(AIEngineRegistry.class);
        AIEngineDescriptor engine = Mockito.mock(AIEngineDescriptor.class);
        Mockito.when(registry.getEngineDescriptor(OpenAIConstants.OPENAI_ENGINE)).thenReturn(engine);
        Mockito.doReturn(OpenAIProperties.class).when(engine).getPropertiesType();
        DBPPreferenceStore store = platform.getPreferenceStore();
        Map<String, String> preferences = new HashMap<>();
        Mockito.doAnswer(invocation -> {
            preferences.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(store).setValue(ArgumentMatchers.anyString(), ArgumentMatchers.anyString());
        Mockito.doAnswer(invocation -> {
            preferences.remove(invocation.getArgument(0));
            return null;
        }).when(store).setToDefault(ArgumentMatchers.anyString());
        Mockito.when(store.getString(ArgumentMatchers.anyString()))
            .thenAnswer(invocation -> preferences.getOrDefault(invocation.getArgument(0), ""));

        try (MockedStatic<DBWorkbench> workbench = Mockito.mockStatic(
            DBWorkbench.class, Mockito.withSettings().mockMaker("mock-maker-inline"));
            MockedStatic<AIEngineRegistry> registries = Mockito.mockStatic(
                AIEngineRegistry.class, Mockito.withSettings().mockMaker("mock-maker-inline"))) {
            workbench.when(DBWorkbench::getPlatform).thenReturn(platform);
            registries.when(AIEngineRegistry::getInstance).thenReturn(registry);
            AISettingsManager manager = AISettingsManager.getInstance();
            WorkspaceConfigEventManager.fireConfigChangedEvent(AISettingsManager.AI_CONFIGURATION_FILE_NAME);
            AISettings settings = manager.getSettings();
            Mockito.when(platform.getWorkspace().hasRealmPermission(RMConstants.PERMISSION_CONFIGURATION_MANAGER)).thenReturn(true);
            manager.saveSettings();
            Assertions.assertTrue(preferences.isEmpty());
            Mockito.verify(store, Mockito.never()).save();
            ArgumentCaptor<String> initialContent = ArgumentCaptor.forClass(String.class);
            Mockito.verify(platform.getConfigurationController()).saveConfigurationFile(
                ArgumentMatchers.eq(AISettingsManager.AI_CONFIGURATION_FILE_NAME), initialContent.capture());
            String sharedConfiguration = initialContent.getValue();
            manager.setDefaultConfiguration(settings.getConfiguration("shared"));
            Assertions.assertEquals("shared", preferences.get(AIConstants.AI_DEFAULT_CONFIGURATION));
            var changedSharedConfiguration = JsonParser.parseString(sharedConfiguration).getAsJsonObject();
            changedSharedConfiguration.addProperty("defaultConfiguration", "personal");
            Mockito.when(platform.getConfigurationController().loadConfigurationFile(AISettingsManager.AI_CONFIGURATION_FILE_NAME))
                .thenReturn(changedSharedConfiguration.toString());
            WorkspaceConfigEventManager.fireConfigChangedEvent(AISettingsManager.AI_CONFIGURATION_FILE_NAME);
            Assertions.assertEquals("shared", manager.getSettings().getDefaultConfiguration().getProfileId());

            Mockito.when(platform.getConfigurationController().loadConfigurationFile(AISettingsManager.AI_CONFIGURATION_FILE_NAME))
                .thenReturn(sharedConfiguration);
            preferences.clear();
            WorkspaceConfigEventManager.fireConfigChangedEvent(AISettingsManager.AI_CONFIGURATION_FILE_NAME);
            settings = manager.getSettings();
            Mockito.clearInvocations(store, platform.getConfigurationController());
            Mockito.when(platform.getWorkspace().hasRealmPermission(RMConstants.PERMISSION_CONFIGURATION_MANAGER)).thenReturn(false);

            AIConfigurationProfile profile = settings.getConfiguration("personal");
            manager.setDefaultConfiguration(profile);

            Assertions.assertFalse(platform.getWorkspace().hasRealmPermission(RMConstants.PERMISSION_CONFIGURATION_MANAGER));
            Assertions.assertSame(profile, settings.getDefaultConfigurationOrNull());
            Assertions.assertEquals("personal", preferences.get(AIConstants.AI_DEFAULT_CONFIGURATION));
            Mockito.verify(store).save();
            Mockito.verify(platform.getConfigurationController(), Mockito.never())
                .saveConfigurationFile(ArgumentMatchers.anyString(), ArgumentMatchers.anyString());

            Mockito.when(platform.getWorkspace().hasRealmPermission(RMConstants.PERMISSION_CONFIGURATION_MANAGER)).thenReturn(true);
            manager.saveSettings();
            ArgumentCaptor<String> content = ArgumentCaptor.forClass(String.class);
            Mockito.verify(platform.getConfigurationController())
                .saveConfigurationFile(ArgumentMatchers.eq(AISettingsManager.AI_CONFIGURATION_FILE_NAME), content.capture());
            var saved = JsonParser.parseString(content.getValue()).getAsJsonObject();
            Assertions.assertEquals("shared", saved.get("defaultConfiguration").getAsString());
            Assertions.assertEquals("openai", saved.get("activeEngine").getAsString());
            Assertions.assertFalse(saved.has("userDefaultConfiguration"));

            WorkspaceConfigEventManager.fireConfigChangedEvent(AISettingsManager.AI_CONFIGURATION_FILE_NAME);
            AISettings reloaded = manager.getSettings();
            Assertions.assertNotSame(settings, reloaded);
            Assertions.assertEquals("personal", reloaded.getDefaultConfiguration().getProfileId());
            reloaded.removeConfiguration(reloaded.getConfiguration("personal"));
            Assertions.assertEquals("shared", reloaded.getDefaultConfiguration().getProfileId());

            manager.saveSettings();
            Assertions.assertFalse(preferences.containsKey(AIConstants.AI_DEFAULT_CONFIGURATION));
            Mockito.when(platform.getConfigurationController().loadConfigurationFile(AISettingsManager.AI_CONFIGURATION_FILE_NAME))
                .thenReturn(sharedConfiguration);
            WorkspaceConfigEventManager.fireConfigChangedEvent(AISettingsManager.AI_CONFIGURATION_FILE_NAME);
            Assertions.assertEquals("shared", manager.getSettings().getDefaultConfiguration().getProfileId());

            manager.setDefaultConfiguration(manager.getSettings().getConfiguration("personal"));
            var withoutPersonal = JsonParser.parseString(sharedConfiguration).getAsJsonObject();
            withoutPersonal.getAsJsonObject("configurations").remove("personal");
            Mockito.when(platform.getConfigurationController().loadConfigurationFile(AISettingsManager.AI_CONFIGURATION_FILE_NAME))
                .thenReturn(withoutPersonal.toString());
            WorkspaceConfigEventManager.fireConfigChangedEvent(AISettingsManager.AI_CONFIGURATION_FILE_NAME);
            Assertions.assertEquals("shared", manager.getSettings().getDefaultConfiguration().getProfileId());
            Assertions.assertFalse(preferences.containsKey(AIConstants.AI_DEFAULT_CONFIGURATION));

            var replacement = JsonParser.parseString(sharedConfiguration).getAsJsonObject();
            replacement.getAsJsonObject("configurations").getAsJsonObject("personal").addProperty("name", "Replacement");
            Mockito.when(platform.getConfigurationController().loadConfigurationFile(AISettingsManager.AI_CONFIGURATION_FILE_NAME))
                .thenReturn(replacement.toString());
            WorkspaceConfigEventManager.fireConfigChangedEvent(AISettingsManager.AI_CONFIGURATION_FILE_NAME);
            Assertions.assertEquals("shared", manager.getSettings().getDefaultConfiguration().getProfileId());
            Assertions.assertEquals("Replacement", manager.getSettings().getConfiguration("personal").getProfileName());
        } finally {
            WorkspaceConfigEventManager.fireConfigChangedEvent(AISettingsManager.AI_CONFIGURATION_FILE_NAME);
        }
    }

    @Test
    public void headerSecretsShouldBeSavedForAllProfiles() throws Exception {
        AISettingsManager manager = AISettingsManager.getInstance();
        AISettings previousSettings = manager.getSettings();
        AISettings settings = new AISettings();
        AIEngineDescriptor engine = AIEngineRegistry.getInstance().getEngineDescriptor(OpenAIConstants.OPENAI_ENGINE);
        Assertions.assertNotNull(engine);
        AIConfigurationProfile work = settings.createConfiguration("test-work", engine);
        ((OpenAIProperties) work.getConfiguration()).setCustomHeaders(Map.of("X-Api-Key", "work-secret"));
        AIConfigurationProfile personal = settings.createConfiguration("test-personal", engine);
        ((OpenAIProperties) personal.getConfiguration()).setCustomHeaders(Map.of("Authorization", "personal-secret"));
        settings.setDefaultConfiguration(work);

        Map<String, String> secrets = new HashMap<>();
        secrets.put("gpt.token_test-personal", "existing-personal-token");
        DBSSecretController controller = Mockito.mock(DBSSecretController.class);
        Mockito.when(controller.getPrivateSecretValue(ArgumentMatchers.anyString()))
            .thenAnswer(invocation -> secrets.get(invocation.getArgument(0)));
        Mockito.doAnswer(invocation -> {
            secrets.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(controller).setPrivateSecretValue(ArgumentMatchers.anyString(), ArgumentMatchers.nullable(String.class));
        DBPPlatform platform = Mockito.mock(DBPPlatform.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(platform.getWorkspace().hasRealmPermission(RMConstants.PERMISSION_CONFIGURATION_MANAGER)).thenReturn(true);

        try (MockedStatic<DBWorkbench> workbench = Mockito.mockStatic(
            DBWorkbench.class, Mockito.withSettings().mockMaker("mock-maker-inline"));
            MockedStatic<DBSSecretController> controllers = Mockito.mockStatic(
                DBSSecretController.class, Mockito.withSettings().mockMaker("mock-maker-inline"))) {
            workbench.when(DBWorkbench::getPlatform).thenReturn(platform);
            controllers.when(DBSSecretController::getGlobalSecretControllerOrNull).thenReturn(controller);
            try {
                manager.saveSettings(settings);
                Assertions.assertEquals("existing-personal-token", secrets.get("gpt.token_test-personal"));
                ArgumentCaptor<String> savedJson = ArgumentCaptor.forClass(String.class);
                Mockito.verify(platform.getConfigurationController()).saveConfigurationFile(
                    ArgumentMatchers.eq(AISettingsManager.AI_CONFIGURATION_FILE_NAME), savedJson.capture());
                Assertions.assertFalse(savedJson.getValue().contains("secret"));
                Assertions.assertFalse(savedJson.getValue().contains("openai.headers"));
                AISettings restored = AISettingsManager.READ_PROPS_GSON.fromJson(savedJson.getValue(), AISettings.class);
                restored.finishSettingsLoading();
                restored.resolveSecrets();
                Assertions.assertEquals(Map.of("X-Api-Key", "work-secret"),
                    ((OpenAIProperties) restored.getConfiguration("test-work").getConfiguration()).getCustomHeaders());
                Assertions.assertEquals(Map.of("Authorization", "personal-secret"),
                    ((OpenAIProperties) restored.getConfiguration("test-personal").getConfiguration()).getCustomHeaders());
            } finally {
                manager.saveSettings(previousSettings);
            }
        }
    }

    @Test
    public void modelSelectionIsSavedPerProfile() throws Exception {
        AISettings settings = new AISettings();
        AIEngineDescriptor engine = AIEngineRegistry.getInstance().getEngineDescriptor(OpenAIConstants.OPENAI_ENGINE);
        Assertions.assertNotNull(engine);
        AIConfigurationProfile work = settings.createConfiguration("test-work", engine);
        work.setProfileName("Work");
        work.getConfiguration().selectModel(new AIModel("gpt-4.1", 1_048_576, Set.of(AIModelFeature.CHAT)));
        AIConfigurationProfile personal = settings.createConfiguration("test-personal", engine);
        personal.setProfileName("Personal");
        personal.getConfiguration().selectModel(new AIModel("gpt-4.1-mini", 1_048_576, Set.of(AIModelFeature.CHAT)));
        settings.setDefaultConfiguration(work);

        personal.getConfiguration().selectModel(new AIModel("gpt-4o", 128_000, Set.of(AIModelFeature.CHAT)));
        Assertions.assertTrue(settings.getProperty(AIConstants.AI_CHAT_SHOW_PROFILE_AND_MODEL, true));
        settings.setProperty(AIConstants.AI_CHAT_SHOW_PROFILE_AND_MODEL, false);
        AISettings restored = AISettingsManager.READ_PROPS_GSON.fromJson(
            AISettingsManager.SAVE_PROPS_GSON.toJson(settings), AISettings.class);
        restored.finishSettingsLoading();

        Assertions.assertEquals("gpt-4.1", restored.getConfiguration("test-work").getConfiguration().getModel());
        Assertions.assertEquals("gpt-4o", restored.getConfiguration("test-personal").getConfiguration().getModel());
        Assertions.assertEquals(1_048_576, restored.getConfiguration("test-work").getConfiguration().getContextWindowSize());
        Assertions.assertEquals(128_000, restored.getConfiguration("test-personal").getConfiguration().getContextWindowSize());
        Assertions.assertEquals("test-work", restored.getDefaultConfiguration().getProfileId());
        Assertions.assertFalse(restored.getProperty(AIConstants.AI_CHAT_SHOW_PROFILE_AND_MODEL, true));
    }

    @Test
    public void migratesLegacyDefaultEngineAndOpenAIModel() throws Exception {
        String config = """
            {
              "activeEngine": "copilot",
              "engineConfigurations": {
                "openai": {
                  "properties": {
                    "gpt.token": "openai-token"
                  }
                },
                "copilot": {
                  "properties": {
                    "copilot.access.token": "copilot-token",
                    "gpt.model": "gpt-4o"
                  }
                }
              }
            }
            """;

        AISettings settings = AISettingsManager.READ_PROPS_GSON.fromJson(config, AISettings.class);
        settings.finishSettingsLoading();

        Assertions.assertEquals("copilot", settings.getDefaultConfiguration().getProfileId());
        Assertions.assertEquals("copilot", settings.getDefaultConfiguration().getEngineId());
        Assertions.assertEquals(
            OpenAIConstants.LEGACY_DEFAULT_MODEL,
            settings.getConfiguration(OpenAIConstants.OPENAI_ENGINE).getConfiguration().getModel()
        );
        Assertions.assertEquals(
            OpenAIConstants.LEGACY_DEFAULT_MODEL,
            JsonParser.parseString(AISettingsManager.SAVE_PROPS_GSON.toJson(settings))
                .getAsJsonObject()
                .getAsJsonObject("configurations")
                .getAsJsonObject(OpenAIConstants.OPENAI_ENGINE)
                .getAsJsonObject("configuration")
                .get(OpenAIConstants.GPT_MODEL)
                .getAsString()
        );
    }

    @Test
    public void preservesUnknownLegacyEngineConfiguration() throws Exception {
        String config = """
            {
              "unsupported": {
                "properties": {
                  "applicableAuthTypes": [
                    {
                      "title": "Unsupported"
                    }
                  ]
                }
              }
            }
            """;

        try (JsonReader reader = new JsonReader(new StringReader(config))) {
            Assertions.assertEquals(JsonParser.parseString(config).getAsJsonObject().get("unsupported"),
                new AISettingsManager.EngineConfigAdapter().read(reader).get("unsupported").getUnavailableConfiguration());
            Assertions.assertEquals(JsonToken.END_DOCUMENT, reader.peek());
        }
    }

    @Test
    public void readsSupportedLegacyConfigurationAsEngineProperties() throws Exception {
        try (JsonReader reader = new JsonReader(new StringReader("""
            {"openai": {"properties": {"gpt.model": "gpt-4o", "gpt.model.temperature": 0.4}}}
            """))) {
            var configuration = new AISettingsManager.EngineConfigAdapter().read(reader).get("openai");
            OpenAIProperties properties = Assertions.assertInstanceOf(OpenAIProperties.class, configuration.getProperties());
            Assertions.assertEquals("gpt-4o", properties.getModel());
            Assertions.assertEquals(0.4, properties.getTemperature());
            Assertions.assertNull(configuration.getUnavailableConfiguration());
        }
    }

    @Test
    public void serializesNonGlobalProfileFlag() {
        String config = """
            {
              "name": "User OpenAI",
              "engine": "openai",
              "configuration": {
                "global": false
              }
            }
            """;

        AIConfigurationProfile profile = AISettingsManager.READ_PROPS_GSON.fromJson(
            config,
            AIConfigurationProfile.class
        );

        Assertions.assertFalse(profile.isGlobal());
        String serialized = AISettingsManager.SAVE_PROPS_GSON.toJson(profile);
        Assertions.assertFalse(JsonParser.parseString(serialized)
            .getAsJsonObject()
            .getAsJsonObject("configuration")
            .get("global")
            .getAsBoolean());
    }

    @Test
    public void synchronizesGlobalProfileFlag() {
        AIConfigurationProfile profile = new AIConfigurationProfile();
        OpenAIProperties properties = new OpenAIProperties();
        properties.setGlobal(false);

        profile.setConfiguration(properties);
        Assertions.assertFalse(profile.isGlobal());

        properties.setGlobal(true);
        Assertions.assertTrue(profile.isGlobal());

        profile.setGlobal(false);
        Assertions.assertFalse(properties.isGlobal());
    }

    @Test
    public void generatesUniqueIdentityForProfileCopy() throws Exception {
        AISettings settings = new AISettings();
        AIEngineDescriptor engine = AIEngineRegistry.getInstance().getEngineDescriptor(OpenAIConstants.OPENAI_ENGINE);
        Assertions.assertNotNull(engine);

        AIConfigurationProfile source = settings.createConfiguration("source", engine);
        source.setProfileName("Profile");
        AIConfigurationProfile existing = settings.createConfiguration(OpenAIConstants.OPENAI_ENGINE, engine);
        existing.setProfileName("Profile (1)");

        AIConfigurationProfile copy = settings.copyConfiguration(source);

        Assertions.assertEquals(OpenAIConstants.OPENAI_ENGINE + "_1", copy.getProfileId());
        Assertions.assertEquals("Profile (2)", copy.getProfileName());

        AIConfigurationProfile copyWithProvidedId = settings.copyConfiguration(source, "provided-copy-id");
        Assertions.assertEquals("provided-copy-id", copyWithProvidedId.getProfileId());
        Assertions.assertEquals("Profile (3)", copyWithProvidedId.getProfileName());
    }

    @Test
    public void preservesUnavailableProfilesAcrossRepeatedSaves() throws Exception {
        JsonObject source = JsonParser.parseString("""
            {
              "defaultConfiguration": "unavailable",
              "configurations": {
                "unavailable": {
                  "configuration": {"nested": {"list": [1, null, {"unknown": true}]}, "global": false},
                  "name": "Unavailable profile",
                  "engine": "missing-test-engine",
                  "futureField": {"nullable": null, "number": 9007199254740993}
                },
                "available": {"engine": "openai", "name": "Available profile", "configuration": {"gpt.model": "gpt-4o"}}
              }
            }
            """).getAsJsonObject();
        JsonObject serialized = source;
        for (int iteration = 0; iteration < 3; iteration++) {
            AISettings settings = AISettingsManager.READ_PROPS_GSON.fromJson(serialized, AISettings.class);
            settings.finishSettingsLoading();
            Assertions.assertFalse(settings.getConfiguration("unavailable").isAvailable());
            Assertions.assertFalse(settings.getConfiguration("unavailable").isGlobal());
            Assertions.assertThrows(DBException.class, settings::getDefaultConfiguration);
            Assertions.assertThrows(DBException.class, () -> settings.getConfiguration("unavailable").getConfiguration());
            settings.resolveSecrets();
            settings.setProperty("test.chat.setting", iteration);
            settings.prepareSettingsSaving();
            serialized = AISettingsManager.SAVE_PROPS_GSON.toJsonTree(settings).getAsJsonObject();
            Assertions.assertEquals(source.getAsJsonObject("configurations").get("unavailable"),
                serialized.getAsJsonObject("configurations").get("unavailable"));
            Assertions.assertEquals("unavailable", serialized.get("defaultConfiguration").getAsString());
            Assertions.assertEquals(iteration, serialized.getAsJsonObject("properties").get("test.chat.setting").getAsInt());
        }
    }

    @Test
    public void preservesUnavailableLegacySettingsDuringMigration() throws Exception {
        JsonObject source = JsonParser.parseString("""
            {
              "activeEngine": "missing-test-engine",
              "engineConfigurations": {
                "missing-test-engine": {
                  "properties": {"provider.setting": ["value", null], "gpt.model.temperature": 0.4},
                  "futureWrapper": {"keep": true}
                },
                "openai": {"properties": {"gpt.model": "gpt-4o"}}
              }
            }
            """).getAsJsonObject();
        AISettings settings = AISettingsManager.READ_PROPS_GSON.fromJson(source, AISettings.class);
        settings.finishSettingsLoading();
        Assertions.assertEquals("missing-test-engine", settings.getDefaultConfigurationOrNull().getProfileId());
        settings.setDefaultConfiguration(settings.getConfiguration("openai"));
        settings.prepareSettingsSaving();
        JsonObject serialized = AISettingsManager.SAVE_PROPS_GSON.toJsonTree(settings).getAsJsonObject();
        Assertions.assertEquals(source.getAsJsonObject("engineConfigurations").get("missing-test-engine"),
            serialized.getAsJsonObject("engineConfigurations").get("missing-test-engine"));
        Assertions.assertEquals(source.getAsJsonObject("engineConfigurations").getAsJsonObject("missing-test-engine").get("properties"),
            serialized.getAsJsonObject("configurations").getAsJsonObject("missing-test-engine").get("configuration"));
    }

    @Test
    public void preservesLegacyMappingWhenSavingMultipleProfiles() throws Exception {
        AISettings settings = AISettingsManager.READ_PROPS_GSON.fromJson("""
            {
              "defaultConfiguration": "work",
              "activeEngine": "openai",
              "configurations": {
                "work": {"name":"Work", "engine":"openai", "configuration":{"gpt.model":"gpt-4o"}},
                "personal": {"name":"Personal", "engine":"openai", "configuration":{"gpt.model":"gpt-4.1"}}
              },
              "engineConfigurations": {"openai":{"properties":{"gpt.model":"gpt-4o"}}}
            }
            """, AISettings.class);
        for (int iteration = 0; iteration < 3; iteration++) {
            settings.finishSettingsLoading();
            settings.setUserDefaultConfiguration(settings.getConfiguration("personal"));
            settings.setProperty("test.chat.setting", iteration);
            settings.prepareSettingsSaving();
            JsonObject saved = AISettingsManager.SAVE_PROPS_GSON.toJsonTree(settings).getAsJsonObject();
            Assertions.assertEquals("gpt-4o", saved.getAsJsonObject("engineConfigurations")
                .getAsJsonObject("openai").getAsJsonObject("properties").get("gpt.model").getAsString());
            JsonObject legacy = saved.deepCopy();
            legacy.remove("configurations");
            legacy.remove("defaultConfiguration");
            AISettings restoredLegacy = AISettingsManager.READ_PROPS_GSON.fromJson(legacy, AISettings.class);
            restoredLegacy.finishSettingsLoading();
            Assertions.assertEquals("gpt-4o", restoredLegacy.getDefaultConfiguration().getConfiguration().getModel());
            settings = AISettingsManager.READ_PROPS_GSON.fromJson(saved, AISettings.class);
        }
    }

    @Test
    public void usesSharedDefaultForNewLegacyEntry() throws Exception {
        AISettings settings = AISettingsManager.READ_PROPS_GSON.fromJson("""
            {
              "defaultConfiguration": "personal",
              "configurations": {
                "personal": {"name":"Personal", "engine":"openai", "configuration":{"gpt.model":"gpt-4.1"}},
                "work": {"name":"Work", "engine":"openai", "configuration":{"gpt.model":"gpt-4o"}}
              }
            }
            """, AISettings.class);
        settings.finishSettingsLoading();
        settings.setUserDefaultConfiguration(settings.getConfiguration("work"));
        settings.prepareSettingsSaving();
        JsonObject saved = AISettingsManager.SAVE_PROPS_GSON.toJsonTree(settings).getAsJsonObject();
        Assertions.assertEquals("gpt-4.1", saved.getAsJsonObject("engineConfigurations")
            .getAsJsonObject("openai").getAsJsonObject("properties").get("gpt.model").getAsString());
    }

    @Test
    public void preservesLegacyMappingForNonDefaultEngine() throws Exception {
        AISettings settings = AISettingsManager.READ_PROPS_GSON.fromJson("""
            {
              "defaultConfiguration": "local",
              "configurations": {
                "local": {"name":"Local", "engine":"ollama", "configuration":{}},
                "work": {"name":"Work", "engine":"openai", "configuration":{"gpt.model":"gpt-4o"}},
                "personal": {"name":"Personal", "engine":"openai", "configuration":{"gpt.model":"gpt-4.1"}}
              },
              "engineConfigurations": {"openai":{"properties":{"gpt.model":"gpt-4o"}}}
            }
            """, AISettings.class);
        settings.finishSettingsLoading();
        settings.prepareSettingsSaving();
        JsonObject saved = AISettingsManager.SAVE_PROPS_GSON.toJsonTree(settings).getAsJsonObject();
        Assertions.assertEquals("gpt-4o", saved.getAsJsonObject("engineConfigurations")
            .getAsJsonObject("openai").getAsJsonObject("properties").get("gpt.model").getAsString());
    }

    @Test
    public void synchronizesMigratedLegacyProfileAfterReload() throws Exception {
        AISettings settings = AISettingsManager.READ_PROPS_GSON.fromJson("""
            {
              "activeEngine": "ollama",
              "engineConfigurations": {
                "ollama": {"properties":{"ollama.model":"llama3"}},
                "openai": {"properties":{"gpt.model":"gpt-4o"}}
              }
            }
            """, AISettings.class);
        settings.finishSettingsLoading();
        settings.prepareSettingsSaving();
        settings = AISettingsManager.READ_PROPS_GSON.fromJson(AISettingsManager.SAVE_PROPS_GSON.toJson(settings), AISettings.class);
        settings.finishSettingsLoading();
        settings.getConfiguration("openai").getConfiguration().setModel("gpt-4.1");
        settings.prepareSettingsSaving();
        JsonObject saved = AISettingsManager.SAVE_PROPS_GSON.toJsonTree(settings).getAsJsonObject();
        Assertions.assertEquals("gpt-4.1", saved.getAsJsonObject("engineConfigurations")
            .getAsJsonObject("openai").getAsJsonObject("properties").get("gpt.model").getAsString());
    }

    @Test
    public void restoresPreservedProfileWhenProviderBecomesAvailable() throws Exception {
        String source = """
            {"engine": "missing-test-engine", "name": "Restored profile", "configuration": {"gpt.model": "gpt-4o"}}
            """;
        AIConfigurationProfile unavailable = AISettingsManager.READ_PROPS_GSON.fromJson(source, AIConfigurationProfile.class);
        String preserved = AISettingsManager.SAVE_PROPS_GSON.toJson(unavailable);
        AIEngineRegistry registry = Mockito.mock(AIEngineRegistry.class);
        AIEngineDescriptor descriptor = Mockito.mock(AIEngineDescriptor.class);
        Mockito.when(registry.getEngineDescriptor("missing-test-engine")).thenReturn(descriptor);
        Mockito.doReturn(OpenAIProperties.class).when(descriptor).getPropertiesType();
        try (MockedStatic<AIEngineRegistry> registries = Mockito.mockStatic(
            AIEngineRegistry.class, Mockito.withSettings().mockMaker("mock-maker-inline"))) {
            registries.when(AIEngineRegistry::getInstance).thenReturn(registry);
            AIConfigurationProfile restored = AISettingsManager.READ_PROPS_GSON.fromJson(preserved, AIConfigurationProfile.class);
            Assertions.assertTrue(restored.isAvailable());
            Assertions.assertEquals("gpt-4o", restored.getConfiguration().getModel());
        }
    }

    @Test
    public void savesAvailableSettingsWithUnavailableProfiles() throws Exception {
        AISettingsManager manager = AISettingsManager.getInstance();
        AISettings previousSettings = manager.getSettings();
        AISettings settings = AISettingsManager.READ_PROPS_GSON.fromJson("""
            {
              "defaultConfiguration": "available",
              "configurations": {
                "unavailable": {"engine": "missing-test-engine", "name": "Unavailable", "configuration": {"keep": "value"}},
                "available": {"engine": "openai", "name": "Available", "configuration": {"gpt.model": "gpt-4o"}}
              }
            }
            """, AISettings.class);
        settings.finishSettingsLoading();
        ((OpenAIProperties) settings.getConfiguration("available").getConfiguration()).setTemperature(0.7);
        settings.setProperty(AIConstants.AI_CHAT_SHOW_PROFILE_AND_MODEL, false);
        DBPPlatform platform = Mockito.mock(DBPPlatform.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(platform.getWorkspace().hasRealmPermission(RMConstants.PERMISSION_CONFIGURATION_MANAGER)).thenReturn(true);
        try (MockedStatic<DBWorkbench> workbench = Mockito.mockStatic(
            DBWorkbench.class, Mockito.withSettings().mockMaker("mock-maker-inline"))) {
            workbench.when(DBWorkbench::getPlatform).thenReturn(platform);
            try {
                manager.saveSettings(settings);
                ArgumentCaptor<String> savedJson = ArgumentCaptor.forClass(String.class);
                Mockito.verify(platform.getConfigurationController()).saveConfigurationFile(
                    ArgumentMatchers.eq(AISettingsManager.AI_CONFIGURATION_FILE_NAME), savedJson.capture());
                AISettings restored = AISettingsManager.READ_PROPS_GSON.fromJson(savedJson.getValue(), AISettings.class);
                restored.finishSettingsLoading();
                Assertions.assertEquals(0.7, restored.getConfiguration("available").getConfiguration().getTemperature());
                Assertions.assertFalse(restored.getProperty(AIConstants.AI_CHAT_SHOW_PROFILE_AND_MODEL, true));
                Assertions.assertEquals("value", JsonParser.parseString(savedJson.getValue()).getAsJsonObject()
                    .getAsJsonObject("configurations").getAsJsonObject("unavailable")
                    .getAsJsonObject("configuration").get("keep").getAsString());
            } finally {
                manager.saveSettings(previousSettings);
            }
        }
    }

    @Test
    public void removesUnavailableProfileWithoutResolvingItsProvider() throws Exception {
        AISettings settings = AISettingsManager.READ_PROPS_GSON.fromJson("""
            {"activeEngine": "missing-test-engine", "engineConfigurations": {
              "missing-test-engine": {"properties": {"keep": true}}
            }}
            """, AISettings.class);
        settings.finishSettingsLoading();
        settings.removeConfiguration(settings.getConfiguration("missing-test-engine"));
        JsonObject serialized = AISettingsManager.SAVE_PROPS_GSON.toJsonTree(settings).getAsJsonObject();
        Assertions.assertTrue(serialized.getAsJsonObject("configurations").isEmpty());
        Assertions.assertTrue(serialized.getAsJsonObject("engineConfigurations").isEmpty());
    }

    @Test
    public void reportsFailuresForAvailableProfilesWithoutWritingPartialSettings() throws Exception {
        AISettingsManager manager = AISettingsManager.getInstance();
        AISettings settings = Mockito.mock(AISettings.class);
        AIConfigurationProfile profile = Mockito.mock(AIConfigurationProfile.class);
        Mockito.when(profile.isAvailable()).thenReturn(true);
        Mockito.when(profile.getConfiguration()).thenThrow(new DBException("Test credential failure"));
        Mockito.when(settings.getConfigurations()).thenReturn(new AIConfigurationProfile[] {profile});
        DBPPlatform platform = Mockito.mock(DBPPlatform.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(platform.getWorkspace().hasRealmPermission(RMConstants.PERMISSION_CONFIGURATION_MANAGER)).thenReturn(true);
        DBPPlatformUI platformUi = Mockito.mock(DBPPlatformUI.class);
        try (MockedStatic<DBWorkbench> workbench = Mockito.mockStatic(
            DBWorkbench.class, Mockito.withSettings().mockMaker("mock-maker-inline"))) {
            workbench.when(DBWorkbench::getPlatform).thenReturn(platform);
            workbench.when(DBWorkbench::getPlatformUI).thenReturn(platformUi);
            manager.saveSettings(settings);
            Mockito.verify(platform.getConfigurationController(), Mockito.never()).saveConfigurationFile(
                ArgumentMatchers.anyString(), ArgumentMatchers.anyString());
            Mockito.verify(platformUi).showError(ArgumentMatchers.anyString(), ArgumentMatchers.isNull(),
                ArgumentMatchers.any(DBException.class));
        }
    }

}

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

import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import org.jkiss.dbeaver.model.ai.AIConfigurationProfile;
import org.jkiss.dbeaver.model.ai.AIConstants;
import org.jkiss.dbeaver.model.ai.AISettings;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIConstants;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIModels;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIProperties;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.dbeaver.runtime.DBWorkbench;
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

public class AISettingsManagerTest extends DBeaverUnitTest {

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
        work.getConfiguration().selectModel(OpenAIModels.getModelByName("gpt-4.1").orElseThrow());
        AIConfigurationProfile personal = settings.createConfiguration("test-personal", engine);
        personal.setProfileName("Personal");
        personal.getConfiguration().selectModel(OpenAIModels.getModelByName("gpt-4.1-mini").orElseThrow());
        settings.setDefaultConfiguration(work);

        personal.getConfiguration().selectModel(OpenAIModels.getModelByName("gpt-4o").orElseThrow());
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
    public void skipsUnknownLegacyEngineConfiguration() throws Exception {
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
            Assertions.assertTrue(new AISettingsManager.EngineConfigAdapter().read(reader).isEmpty());
            Assertions.assertEquals(JsonToken.END_DOCUMENT, reader.peek());
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
}

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

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import org.jkiss.dbeaver.model.ai.AIConfigurationProfile;
import org.jkiss.dbeaver.model.ai.AIConstants;
import org.jkiss.dbeaver.model.ai.AISettings;
import org.jkiss.dbeaver.model.ai.engine.AIModel;
import org.jkiss.dbeaver.model.ai.engine.AIModelFeature;
import org.jkiss.dbeaver.model.ai.engine.AIModelListUtils;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIConstants;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIModels;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIProperties;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.Set;

public class AISettingsManagerTest extends DBeaverUnitTest {

    @Test
    public void modelListFingerprintTracksConnectionChanges() {
        OpenAIProperties properties = new OpenAIProperties();
        properties.setBaseUrl("https://first.example.test/v1");
        properties.setToken("first-test-token");
        String first = AIModelListUtils.getConfigurationFingerprint(properties);

        properties.setBaseUrl("https://second.example.test/v1");
        String second = AIModelListUtils.getConfigurationFingerprint(properties);
        Assertions.assertNotEquals(first, second);
        properties.setToken("second-test-token");
        String third = AIModelListUtils.getConfigurationFingerprint(properties);
        Assertions.assertNotEquals(second, third);
        properties.setAuthentication(OpenAIProperties.AUTHENTICATION_CHATGPT_ACCOUNT);
        Assertions.assertNotEquals(third, AIModelListUtils.getConfigurationFingerprint(properties));

        OpenAIProperties firstAccount = AISettingsManager.READ_PROPS_GSON.fromJson(
            "{\"openai.account.accountId\":\"first\"}", OpenAIProperties.class);
        OpenAIProperties secondAccount = AISettingsManager.READ_PROPS_GSON.fromJson(
            "{\"openai.account.accountId\":\"second\"}", OpenAIProperties.class);
        Assertions.assertNotEquals(
            AIModelListUtils.getConfigurationFingerprint(firstAccount), AIModelListUtils.getConfigurationFingerprint(secondAccount));
    }

    @Test
    public void modelListFingerprintIgnoresRequestSettings() {
        OpenAIProperties properties = new OpenAIProperties();
        properties.setBaseUrl("https://models.example.test/v1");
        properties.setToken("test-token");
        final String initial = AIModelListUtils.getConfigurationFingerprint(properties);

        properties.selectModel(OpenAIModels.getModelByName("gpt-4o").orElseThrow());
        properties.setContextWindowSize(65_536);
        properties.setTemperature(0.5);
        properties.setLoggingEnabled(true);
        properties.setTimeout(60);

        Assertions.assertEquals(initial, AIModelListUtils.getConfigurationFingerprint(properties));
        OpenAIProperties restored = AISettingsManager.READ_PROPS_GSON.fromJson(
            AISettingsManager.READ_PROPS_GSON.toJson(properties), OpenAIProperties.class);
        Assertions.assertEquals(initial, AIModelListUtils.getConfigurationFingerprint(restored));
    }

    @Test
    public void modelListFingerprintIgnoresAccountTokenRenewal() {
        OpenAIProperties original = AISettingsManager.READ_PROPS_GSON.fromJson("""
            {
              "openai.authentication": "chatgptAccount",
              "openai.account.accountId": "test-account",
              "openai.account.accessToken": "original-test-access-token",
              "openai.account.refreshToken": "original-test-refresh-token",
              "openai.account.expiresAt": 1
            }
            """, OpenAIProperties.class);
        OpenAIProperties renewed = AISettingsManager.READ_PROPS_GSON.fromJson("""
            {
              "openai.authentication": "chatgptAccount",
              "openai.account.accountId": "test-account",
              "openai.account.accessToken": "renewed-test-access-token",
              "openai.account.refreshToken": "renewed-test-refresh-token",
              "openai.account.expiresAt": 2
            }
            """, OpenAIProperties.class);

        Assertions.assertEquals(
            AIModelListUtils.getConfigurationFingerprint(original), AIModelListUtils.getConfigurationFingerprint(renewed));
    }

    @Test
    public void unknownModelCapabilitiesDoNotExcludeChatModels() {
        Assertions.assertTrue(AIModelListUtils.isChatModel(new AIModel("llama3", null, Set.of())));
        Assertions.assertTrue(AIModelListUtils.isChatModel(new AIModel("mistral", null, Set.of())));
        Assertions.assertTrue(AIModelListUtils.isChatModel(new AIModel("chat", null, Set.of(AIModelFeature.CHAT))));
        Assertions.assertFalse(AIModelListUtils.isChatModel(new AIModel("embedding", null, Set.of(AIModelFeature.EMBEDDING))));
        Assertions.assertFalse(AIModelListUtils.isChatModel(new AIModel("transcription", null, Set.of(AIModelFeature.SPEECH_TO_TEXT))));
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
}

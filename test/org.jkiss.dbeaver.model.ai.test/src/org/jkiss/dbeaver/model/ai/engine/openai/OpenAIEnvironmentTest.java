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
package org.jkiss.dbeaver.model.ai.engine.openai;

import com.google.gson.Gson;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.AIConfigurationProfile;
import org.jkiss.dbeaver.model.ai.engine.BaseAIEngineProperties;
import org.jkiss.dbeaver.model.ai.engine.copilot.CopilotCompletionEngine;
import org.jkiss.dbeaver.model.ai.engine.copilot.CopilotProperties;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.Map;

class OpenAIEnvironmentTest {
    @Test
    void environmentIsOptInAndDoesNotReplaceStoredValues() {
        OpenAIProperties properties = withEnvironment(new OpenAIProperties(), Map.of(
            OpenAIProperties.ENV_API_KEY, "environment-key",
            OpenAIProperties.ENV_BASE_URL, "https://gateway.example/v1/",
            OpenAIProperties.ENV_MODEL, "environment-model"
        ));
        properties.setToken("saved-key");
        properties.setBaseUrl("https://saved.example/v1/");
        properties.setModel("saved-model");

        Assertions.assertEquals("saved-key", properties.getEffectiveToken());
        Assertions.assertEquals("saved-model", properties.getModelDisplayName());
        properties.setUseEnvVariables(true);
        Assertions.assertEquals("environment-key", properties.getEffectiveToken());
        Assertions.assertEquals("https://gateway.example/v1/", properties.getEffectiveBaseUrl());
        Assertions.assertEquals("environment-model", properties.getModelDisplayName());
        Assertions.assertFalse(properties.isModelSelectionSupported());
        Assertions.assertEquals("saved-key", properties.getToken());
        Assertions.assertEquals("saved-model", properties.getModel());

        String json = new Gson().toJson(properties, OpenAIProperties.class);
        Assertions.assertFalse(json.contains("environment-key"));
        Assertions.assertFalse(json.contains("gateway.example"));
        Assertions.assertFalse(json.contains("environment-model"));
        OpenAIProperties restored = new Gson().fromJson(json, OpenAIProperties.class);
        Assertions.assertTrue(restored.isUseEnvVariables());
        Assertions.assertEquals("saved-key", restored.getToken());
        properties.setUseEnvVariables(false);
        Assertions.assertTrue(properties.isModelSelectionSupported());
        Assertions.assertEquals("saved-model", properties.getModelDisplayName());
    }

    @Test
    void missingAndBlankEnvironmentValuesUseSavedSettings() {
        OpenAIProperties properties = withEnvironment(new OpenAIProperties(), Map.of(
            OpenAIProperties.ENV_API_KEY, "  ", OpenAIProperties.ENV_MODEL, ""
        ));
        properties.setUseEnvVariables(true);
        properties.setToken("saved-key");
        properties.setModel("saved-model");
        properties.setBaseUrl("https://saved.example/v1/");
        Assertions.assertEquals("saved-key", properties.getEffectiveToken());
        Assertions.assertEquals("saved-model", properties.getEffectiveModel());
        Assertions.assertEquals("https://saved.example/v1/", properties.getEffectiveBaseUrl());
        Assertions.assertTrue(properties.isModelSelectionSupported());
    }

    @Test
    void environmentCredentialsReachTheHttpClient() throws Exception {
        OpenAIProperties properties = withEnvironment(new OpenAIProperties(), Map.of(
            OpenAIProperties.ENV_API_KEY, "environment-key", OpenAIProperties.ENV_BASE_URL, "https://gateway.example/v1/"
        ));
        properties.setUseEnvVariables(true);
        Assertions.assertTrue(properties.isValidConfiguration());
        try (OpenAIClientResponses client = new OpenAIEngine<>(properties).createClient()) {
            HttpRequest request = client.applyFilters(HttpRequest.newBuilder(URI.create(properties.getEffectiveBaseUrl())).build());
            Assertions.assertEquals("Bearer environment-key", request.headers().firstValue("Authorization").orElseThrow());
        }
    }

    @Test
    void accountAuthenticationDoesNotUseApiKeyEnvironment() {
        OpenAIProperties properties = withEnvironment(new OpenAIProperties(), Map.of(
            OpenAIProperties.ENV_API_KEY, "environment-key", OpenAIProperties.ENV_BASE_URL, "https://gateway.example/v1/"
        ));
        properties.setUseEnvVariables(true);
        properties.setAuthentication(OpenAIProperties.AUTHENTICATION_CHATGPT_ACCOUNT);
        properties.setToken("saved-key");
        Assertions.assertEquals("saved-key", properties.getEffectiveToken());
        Assertions.assertEquals(OpenAIClientResponses.OPENAI_ENDPOINT, properties.getEffectiveBaseUrl());
    }

    @Test
    void savingSecretsDoesNotPersistTheEnvironmentKey() throws DBException {
        OpenAIProperties properties = withEnvironment(new OpenAIProperties(), Map.of(OpenAIProperties.ENV_API_KEY, "environment-key"));
        properties.setUseEnvVariables(true);
        properties.setToken("saved-key");
        AIConfigurationProfile profile = new AIConfigurationProfile();
        profile.setProfileId("environment-test");
        DBSSecretController controller = Mockito.mock(DBSSecretController.class);
        try (MockedStatic<DBSSecretController> controllers = Mockito.mockStatic(
            DBSSecretController.class, Mockito.withSettings().mockMaker("mock-maker-inline"))) {
            controllers.when(DBSSecretController::getGlobalSecretControllerOrNull).thenReturn(controller);
            properties.saveSecrets(profile);
            Mockito.verify(controller).setPrivateSecretValue("gpt.token_environment-test", "saved-key");
            Mockito.verify(controller, Mockito.never()).setPrivateSecretValue(Mockito.anyString(), Mockito.eq("environment-key"));
        }
    }

    @Test
    void oldAnthropicFlagStillDeserializes() {
        OpenAIProperties restored = new Gson().fromJson("{\"anthropic.useEnvVariables\":true}", OpenAIProperties.class);
        Assertions.assertTrue(restored.isUseEnvVariables());
    }

    @Test
    void copilotTokenAliasesHaveDocumentedPriority() {
        CopilotProperties properties = withEnvironment(new CopilotProperties(), Map.of(
            CopilotProperties.ENV_TOKEN, "copilot-token", CopilotProperties.ENV_GH_TOKEN, "gh-token",
            CopilotProperties.ENV_GITHUB_TOKEN, "github-token", CopilotProperties.ENV_MODEL, "environment-model",
            CopilotProperties.ENV_AUTH_URL, "https://github.example/"
        ));
        properties.setUseEnvVariables(true);
        properties.setModel("saved-model");
        Assertions.assertEquals("copilot-token", properties.getEffectiveToken());
        Assertions.assertEquals("environment-model", properties.getModelDisplayName());
        Assertions.assertEquals("https://github.example/", properties.getEffectiveBaseAuthUrl());
        CopilotProperties fallback = withEnvironment(new CopilotProperties(), Map.of(
            CopilotProperties.ENV_TOKEN, " ", CopilotProperties.ENV_GH_TOKEN, "gh-token",
            CopilotProperties.ENV_GITHUB_TOKEN, "github-token"
        ));
        fallback.setUseEnvVariables(true);
        Assertions.assertEquals("gh-token", fallback.getEffectiveToken());
    }

    @Test
    void openAiContextOverrideSurvivesSettingsSaveAndEnvironmentOptOut() throws DBException {
        OpenAIProperties properties = withEnvironment(new OpenAIProperties(), Map.of(OpenAIProperties.ENV_MODEL, "gpt-4o"));
        properties.setModel("gpt-4");
        properties.setContextWindowSize(4096);
        properties.setUseEnvVariables(true);
        try (OpenAIEngine<OpenAIProperties> engine = new OpenAIEngine<>(properties)) {
            Assertions.assertEquals(128000, engine.getContextWindowSize(new VoidProgressMonitor()));
        }
        properties.setContextWindowSize(properties.getContextWindowSize());
        OpenAIProperties saved = new Gson().fromJson(new Gson().toJson(properties, OpenAIProperties.class), OpenAIProperties.class);
        saved.setUseEnvVariables(false);
        Assertions.assertEquals(4096, saved.getContextWindowSize());
        Assertions.assertEquals("gpt-4", saved.getModel());
    }

    @Test
    void copilotContextOverrideSurvivesSettingsSaveAndEnvironmentOptOut() throws DBException {
        CopilotProperties properties = withEnvironment(new CopilotProperties(), Map.of(CopilotProperties.ENV_MODEL, "gpt-4o"));
        properties.setModel("gpt-4");
        properties.setContextWindowSize(4096);
        properties.setUseEnvVariables(true);
        try (CopilotCompletionEngine<CopilotProperties> engine = new CopilotCompletionEngine<>(properties)) {
            Assertions.assertEquals(128000, engine.getContextWindowSize(new VoidProgressMonitor()));
        }
        properties.setContextWindowSize(properties.getContextWindowSize());
        CopilotProperties saved = new Gson().fromJson(new Gson().toJson(properties, CopilotProperties.class), CopilotProperties.class);
        saved.setUseEnvVariables(false);
        Assertions.assertEquals(4096, saved.getContextWindowSize());
        Assertions.assertEquals("gpt-4", saved.getModel());
    }

    @Test
    void unknownEnvironmentModelsRetainTheConfiguredContextLimit() {
        OpenAIProperties openAiProperties = withEnvironment(new OpenAIProperties(), Map.of(OpenAIProperties.ENV_MODEL, "custom-model"));
        openAiProperties.setUseEnvVariables(true);
        openAiProperties.setContextWindowSize(4096);
        Assertions.assertEquals(4096, openAiProperties.getEffectiveContextWindowSize());
        CopilotProperties copilotProperties = withEnvironment(new CopilotProperties(), Map.of(CopilotProperties.ENV_MODEL, "custom-model"));
        copilotProperties.setUseEnvVariables(true);
        copilotProperties.setContextWindowSize(4096);
        Assertions.assertEquals(4096, copilotProperties.getEffectiveContextWindowSize());
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private static <PROPERTIES extends BaseAIEngineProperties> PROPERTIES withEnvironment(
        @NotNull PROPERTIES properties,
        @NotNull Map<String, String> environment
    ) {
        return (PROPERTIES) Mockito.mock(properties.getClass(), Mockito.withSettings().spiedInstance(properties)
            .defaultAnswer(invocation -> "getEnvironmentVariable".equals(invocation.getMethod().getName())
                ? environment.get(invocation.getArgument(0)) : invocation.callRealMethod()));
    }
}

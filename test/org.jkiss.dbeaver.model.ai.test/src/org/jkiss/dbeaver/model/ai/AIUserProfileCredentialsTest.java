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
package org.jkiss.dbeaver.model.ai;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.ai.engine.openai.AIAccountAuthenticator;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIProperties;
import org.jkiss.dbeaver.model.ai.registry.AIEngineDescriptor;
import org.jkiss.dbeaver.model.ai.registry.AIEngineRegistry;
import org.jkiss.dbeaver.model.ai.registry.AISettingsManager;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.dbeaver.model.secret.DBSSecretObject;
import org.jkiss.dbeaver.model.secret.DBSSecretValue;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

class AIUserProfileCredentialsTest {
    private final Map<String, String> secrets = new HashMap<>();
    private final DBSSecretController controller = Mockito.mock(DBSSecretController.class);
    private MockedStatic<DBWorkbench> workbench;
    private MockedStatic<AIEngineRegistry> engines;
    private MockedStatic<DBSSecretController> controllers;

    @BeforeEach
    void setUp() throws DBException {
        DBPPlatform platform = Mockito.mock(DBPPlatform.class, Mockito.RETURNS_DEEP_STUBS);
        workbench = Mockito.mockStatic(DBWorkbench.class, Mockito.withSettings().mockMaker("mock-maker-inline"));
        workbench.when(DBWorkbench::getPlatform).thenReturn(platform);
        workbench.when(DBWorkbench::isDistributed).thenCallRealMethod();
        Mockito.when(platform.getApplication().isDistributed()).thenReturn(true);

        AIEngineRegistry registry = Mockito.mock(AIEngineRegistry.class);
        AIEngineDescriptor descriptor = Mockito.mock(AIEngineDescriptor.class);
        Mockito.when(registry.getEngineDescriptor("openai")).thenReturn(descriptor);
        Mockito.doReturn(OpenAIProperties.class).when(descriptor).getPropertiesType();
        engines = Mockito.mockStatic(AIEngineRegistry.class, Mockito.withSettings().mockMaker("mock-maker-inline"));
        engines.when(AIEngineRegistry::getInstance).thenReturn(registry);

        controllers = Mockito.mockStatic(DBSSecretController.class, Mockito.withSettings().mockMaker("mock-maker-inline"));
        controllers.when(DBSSecretController::getGlobalSecretController).thenReturn(controller);
        Mockito.when(controller.getPrivateSecretValue(ArgumentMatchers.anyString()))
            .thenAnswer(invocation -> secrets.get(invocation.<String>getArgument(0)));
        Mockito.doAnswer(invocation -> {
            DBSSecretValue value = invocation.getArgument(1);
            if (value.getValue() == null) {
                secrets.remove(value.getId());
            } else {
                secrets.put(value.getId(), value.getValue());
            }
            return null;
        }).when(controller).setPrivateSecretValue(ArgumentMatchers.any(DBSSecretObject.class), ArgumentMatchers.any(DBSSecretValue.class));
    }

    @AfterEach
    void tearDown() {
        controllers.close();
        engines.close();
        workbench.close();
    }

    @Test
    void usesCloudBeaverSecretIds() throws Exception {
        AIConfigurationProfile profile = loadSettings().getConfiguration("openai");
        ((OpenAIProperties) profile.getConfiguration()).setToken("user-token");

        AIUserProfileCredentials.save(profile);

        Assertions.assertEquals("user-token", secrets.get("ai.profile.openai.token"));
        Assertions.assertEquals("false", secrets.get("ai.profile.openai.accountAuthentication"));
        Mockito.verify(controller).flushChanges();
    }

    @Test
    void resolvesStoredCredentialsOnce() throws Exception {
        secrets.put("ai.profile.openai.token", "user-token");
        AIConfigurationProfile profile = loadSettings().getConfiguration("openai");

        profile.resolveSecrets();
        secrets.put("ai.profile.openai.token", "changed-token");
        profile.resolveSecrets();

        OpenAIProperties properties = (OpenAIProperties) profile.getConfiguration();
        Assertions.assertEquals("user-token", properties.getToken());
        Assertions.assertEquals("gpt-4", properties.getModel());
    }

    @Test
    void missingSecretsClearCredentials() throws Exception {
        AIConfigurationProfile profile = loadSettings().getConfiguration("openai");
        OpenAIProperties properties = (OpenAIProperties) profile.getConfiguration();
        properties.setToken("previous-token");
        properties.setAccessToken("previous-access-token");

        AIUserProfileCredentials.load(profile);

        Assertions.assertEquals("", properties.getToken());
        Assertions.assertNull(properties.getAccessToken());
    }

    @Test
    void sharedConfigurationDoesNotContainPersonalCredentials() throws Exception {
        AISettings settings = loadSettings();
        OpenAIProperties properties = (OpenAIProperties) settings.getConfiguration("openai").getConfiguration();
        properties.setToken("user-token");
        properties.setCustomHeaders(Map.of("X-Admin-Header", "admin-header-value"));
        properties.setAccountTokens(new AIAccountAuthenticator.Tokens("user-access", "user-refresh", 3600, "id", "user@example.com"));

        settings.prepareSettingsSaving();
        String json = AISettingsManager.getSavePropertiesGson().toJson(settings);

        Assertions.assertFalse(json.contains("user-token"));
        Assertions.assertFalse(json.contains("user-access"));
        Assertions.assertFalse(json.contains("user-refresh"));
        Assertions.assertFalse(json.contains("user@example.com"));
        Assertions.assertTrue(json.contains("gpt-4"));
        Assertions.assertTrue(json.contains("admin-header-value"));
        Mockito.when(DBWorkbench.getPlatform().getApplication().isDistributed()).thenReturn(false);
        String standaloneJson = AISettingsManager.getSavePropertiesGson().toJson(settings);
        Assertions.assertFalse(standaloneJson.contains("admin-header-value"));
        Assertions.assertTrue(standaloneJson.contains("gpt-4"));
        OpenAIProperties copy = (OpenAIProperties) AIUserProfileCredentials.copyWithoutCredentials(properties);
        Assertions.assertEquals(properties.getCustomHeaders(), copy.getCustomHeaders());
        Assertions.assertEquals("user-token", properties.getToken());
    }

    @Test
    void userAuthenticationChoiceDoesNotChangeSharedProfile() throws Exception {
        secrets.put("ai.profile.openai.accountAuthentication", "true");
        AISettings settings = loadSettings();
        AIConfigurationProfile profile = settings.getConfiguration("openai");

        AIUserProfileCredentials.load(profile);

        Assertions.assertTrue(((OpenAIProperties) profile.getConfiguration()).isAccountAuthentication());
        String json = AISettingsManager.getSavePropertiesGson().toJson(settings);
        Assertions.assertFalse(json.contains(OpenAIProperties.AUTHENTICATION_CHATGPT_ACCOUNT));
    }

    @Test
    void refreshUsesTokenRotatedByAnotherClient() throws Exception {
        secrets.put("ai.profile.openai.refreshToken", "desktop-refresh");
        AIConfigurationProfile profile = loadSettings().getConfiguration("openai");
        AIUserProfileCredentials.load(profile);
        secrets.put("ai.profile.openai.refreshToken", "web-refresh");
        secrets.put("ai.profile.openai.token", "web-token");
        AIAccountAuthenticator authenticator = Mockito.mock(AIAccountAuthenticator.class);
        Mockito.when(authenticator.refresh("web-refresh"))
            .thenReturn(new AIAccountAuthenticator.Tokens("new-access", "new-refresh", 3600, null, null));

        String accessToken = ((OpenAIProperties) profile.getConfiguration()).getValidAccessToken(authenticator);

        Assertions.assertEquals("new-access", accessToken);
        Assertions.assertEquals("new-refresh", secrets.get("ai.profile.openai.refreshToken"));
        Assertions.assertEquals("web-token", secrets.get("ai.profile.openai.token"));
    }

    @Test
    void refreshFailsAfterSignOutInAnotherClient() throws Exception {
        secrets.put("ai.profile.openai.refreshToken", "desktop-refresh");
        AIConfigurationProfile profile = loadSettings().getConfiguration("openai");
        AIUserProfileCredentials.load(profile);
        secrets.remove("ai.profile.openai.refreshToken");
        AIAccountAuthenticator authenticator = Mockito.mock(AIAccountAuthenticator.class);
        OpenAIProperties properties = (OpenAIProperties) profile.getConfiguration();

        Assertions.assertThrows(DBException.class, () -> properties.getValidAccessToken(authenticator));

        Assertions.assertNull(secrets.get("ai.profile.openai.refreshToken"));
        Assertions.assertFalse(properties.isAccountConnected());
        Mockito.verifyNoInteractions(authenticator);
    }

    @Test
    void signOutRemovesStoredAccountTokens() throws Exception {
        secrets.put("ai.profile.openai.refreshToken", "refresh");
        AIConfigurationProfile profile = loadSettings().getConfiguration("openai");
        AIUserProfileCredentials.load(profile);
        OpenAIProperties properties = (OpenAIProperties) profile.getConfiguration();

        properties.clearAccountTokens();
        properties.saveAccountTokens();

        Assertions.assertNull(secrets.get("ai.profile.openai.refreshToken"));
    }

    @Test
    void deleteRemovesCredentialsOfAllUsers() throws Exception {
        AIUserProfileCredentials.delete(loadSettings().getConfiguration("openai"));

        Mockito.verify(controller).deleteObjectSecrets(ArgumentMatchers.argThat(object ->
            object.getSecretObjectType().equals("aiProfile") && object.getSecretObjectId().equals("openai")));
    }

    private static AISettings loadSettings() throws DBException {
        AISettings settings = AISettingsManager.READ_PROPS_GSON.fromJson("""
            {"configurations":{"openai":{"name":"OpenAI","engine":"openai",
              "configuration":{"global":false,"gpt.model":"gpt-4"}}}}
            """, AISettings.class);
        settings.finishSettingsLoading();
        return settings;
    }
}

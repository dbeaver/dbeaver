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
import org.jkiss.dbeaver.model.WorkspaceConfigEventManager;
import org.jkiss.dbeaver.model.ai.engine.AIEngineProperties;
import org.jkiss.dbeaver.model.ai.engine.openai.AIAccountAuthenticator;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIProperties;
import org.jkiss.dbeaver.model.ai.registry.AIEngineDescriptor;
import org.jkiss.dbeaver.model.ai.registry.AIEngineRegistry;
import org.jkiss.dbeaver.model.ai.registry.AISettingsManager;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.auth.SMSession;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.dbeaver.model.secret.DBSSecretObject;
import org.jkiss.dbeaver.model.secret.DBSSecretValue;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

class AIUserProfileCredentialsTest extends DBeaverUnitTest {
    @Test
    void loadsWebCompatibleSecretForSelectedProfile() throws Exception {
        OpenAIProperties properties = new OpenAIProperties();
        AIConfigurationProfile profile = profile("work", properties);
        DBSSecretController controller = Mockito.mock(DBSSecretController.class);
        Mockito.when(controller.getPrivateSecretValue("ai.profile.work.token")).thenReturn("personal-token");

        Assertions.assertEquals(Map.of("token", "personal-token"), AIUserProfileCredentials.load(profile, controller));
        Mockito.verify(controller).getPrivateSecretValue("ai.profile.work.token");
        Mockito.verifyNoMoreInteractions(controller);
    }

    @Test
    void missingSecretClearsPreviouslyResolvedTokenAndPreservesSettings() throws Exception {
        OpenAIProperties properties = new OpenAIProperties();
        properties.setToken("previous-user-token");
        properties.setModel("admin-model");
        properties.setBaseUrl("https://admin.example/v1");
        properties.setTimeout(45);

        AIUserProfileCredentials.apply(properties, Map.of());

        Assertions.assertEquals("", properties.getToken());
        Assertions.assertEquals("admin-model", properties.getModel());
        Assertions.assertEquals("https://admin.example/v1", properties.getBaseUrl());
        Assertions.assertEquals(45, properties.getTimeout());
    }

    @Test
    void savesPersonalTokenWithWebCompatibleObjectMetadata() throws Exception {
        DBSSecretController controller = Mockito.mock(DBSSecretController.class);
        AIUserProfileCredentials.save(profile("work", new OpenAIProperties()), Map.of("token", "personal-token"), controller);
        ArgumentCaptor<DBSSecretObject> secretObject = ArgumentCaptor.forClass(DBSSecretObject.class);
        ArgumentCaptor<DBSSecretValue> secretValue = ArgumentCaptor.forClass(DBSSecretValue.class);
        Mockito.verify(controller).setPrivateSecretValue(secretObject.capture(), secretValue.capture());
        Assertions.assertEquals("aiProfile", secretObject.getValue().getSecretObjectType());
        Assertions.assertEquals("work", secretObject.getValue().getSecretObjectId());
        Assertions.assertEquals("", secretObject.getValue().getProjectId());
        Assertions.assertEquals("ai.profile.work.token", secretValue.getValue().getId());
        Assertions.assertEquals("personal-token", secretValue.getValue().getValue());
        Mockito.verify(controller).flushChanges();
    }

    @Test
    void clearingTokenDeletesSecret() throws Exception {
        DBSSecretController controller = Mockito.mock(DBSSecretController.class);
        AIUserProfileCredentials.save(profile("work", new OpenAIProperties()), Map.of("token", ""), controller);
        ArgumentCaptor<DBSSecretValue> value = ArgumentCaptor.forClass(DBSSecretValue.class);
        Mockito.verify(controller).setPrivateSecretValue(ArgumentMatchers.any(DBSSecretObject.class), value.capture());
        Assertions.assertNull(value.getValue().getValue());
    }

    @Test
    void rejectsChangesToAdministratorSettingsBeforeWritingSecrets() throws Exception {
        DBSSecretController controller = Mockito.mock(DBSSecretController.class);
        AIConfigurationProfile profile = profile("work", new OpenAIProperties());
        Assertions.assertThrows(DBException.class,
            () -> AIUserProfileCredentials.save(profile, Map.of("token", "secret", "model", "other-model"), controller));
        Mockito.verifyNoInteractions(controller);
    }

    @Test
    void sharedProfilesDoNotAcceptPersonalCredentials() throws Exception {
        AIConfigurationProfile profile = profile("shared", new OpenAIProperties());
        Mockito.when(profile.isGlobal()).thenReturn(true);
        DBSSecretController controller = Mockito.mock(DBSSecretController.class);
        Assertions.assertThrows(DBException.class, () -> AIUserProfileCredentials.load(profile, controller));
        Assertions.assertThrows(DBException.class, () -> AIUserProfileCredentials.save(profile, Map.of("token", "secret"), controller));
        Mockito.verifyNoInteractions(controller);
    }

    @Test
    void personalCredentialsAreOnlyResolvedInDistributedDesktop() {
        AIConfigurationProfile profile = Mockito.mock(AIConfigurationProfile.class);
        DBPPlatform platform = Mockito.mock(DBPPlatform.class, Mockito.RETURNS_DEEP_STUBS);
        try (MockedStatic<DBWorkbench> workbench = Mockito.mockStatic(
            DBWorkbench.class, Mockito.withSettings().mockMaker("mock-maker-inline"))) {
            workbench.when(DBWorkbench::getPlatform).thenReturn(platform);
            workbench.when(DBWorkbench::isDistributed).thenReturn(false);
            Assertions.assertFalse(AIUserProfileCredentials.isUserProfile(profile));
            workbench.when(DBWorkbench::isDistributed).thenReturn(true);
            Assertions.assertTrue(AIUserProfileCredentials.isUserProfile(profile));
            Mockito.when(platform.getApplication().isHeadlessMode()).thenReturn(true);
            Assertions.assertFalse(AIUserProfileCredentials.isUserProfile(profile));
        }
    }

    @Test
    void personalTokensAreExcludedFromSharedProfilesAndLegacyConfigurations() throws Exception {
        DBPPlatform platform = Mockito.mock(DBPPlatform.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(platform.getApplication().isDistributed()).thenReturn(true);
        AIEngineRegistry registry = Mockito.mock(AIEngineRegistry.class);
        AIEngineDescriptor descriptor = Mockito.mock(AIEngineDescriptor.class);
        Mockito.doReturn(OpenAIProperties.class).when(descriptor).getPropertiesType();
        Mockito.when(registry.getEngineDescriptor("openai")).thenReturn(descriptor);
        try (MockedStatic<DBWorkbench> workbench = Mockito.mockStatic(
                 DBWorkbench.class, Mockito.withSettings().mockMaker("mock-maker-inline"));
             MockedStatic<AIEngineRegistry> engines = Mockito.mockStatic(
                 AIEngineRegistry.class, Mockito.withSettings().mockMaker("mock-maker-inline"))) {
            workbench.when(DBWorkbench::getPlatform).thenReturn(platform);
            workbench.when(DBWorkbench::isDistributed).thenReturn(true);
            engines.when(AIEngineRegistry::getInstance).thenReturn(registry);
            AISettings settings = AISettingsManager.READ_PROPS_GSON.fromJson("""
                {"configurations":{"work":{"name":"Work","engine":"openai",
                  "configuration":{"global":false,"gpt.token":"personal-token","gpt.model":"admin-model"}}}}
                """, AISettings.class);
            settings.finishSettingsLoading();
            OpenAIProperties properties = (OpenAIProperties) settings.getConfiguration("work").getConfiguration();
            properties.setCustomHeaders(Map.of("X-Admin-Header", "admin-header-value"));
            properties.setAccessToken("personal-access-token");
            properties.setRefreshToken("personal-refresh-token");
            OpenAIProperties sanitized = (OpenAIProperties) AIUserProfileCredentials.copyWithoutCredentials(properties);
            Assertions.assertEquals("", sanitized.getToken());
            Assertions.assertNull(sanitized.getAccessToken());
            Assertions.assertNull(sanitized.getRefreshToken());
            Assertions.assertEquals(properties.getCustomHeaders(), sanitized.getCustomHeaders());
            String serialized = AISettingsManager.SAVE_PROPS_GSON.toJson(settings);
            Assertions.assertFalse(serialized.contains("personal-token"));
            Assertions.assertFalse(serialized.contains("personal-access-token"));
            Assertions.assertFalse(serialized.contains("personal-refresh-token"));
            Assertions.assertTrue(serialized.contains("admin-model"));
            Assertions.assertEquals("personal-token", properties.getToken());
            Assertions.assertEquals("personal-access-token", properties.getAccessToken());
            Assertions.assertEquals("personal-refresh-token", properties.getRefreshToken());
        }
    }

    @Test
    void savesEditedCredentialsAfterTheyHaveBeenResolved() throws Exception {
        OpenAIProperties properties = new OpenAIProperties();
        AIConfigurationProfile profile = userProfile(properties);
        DBSSecretController controller = Mockito.mock(DBSSecretController.class);
        Mockito.when(controller.getPrivateSecretValue("ai.profile.work.token")).thenReturn("old-token");
        try (MockedStatic<DBSSecretController> controllers = Mockito.mockStatic(
            DBSSecretController.class, Mockito.withSettings().mockMaker("mock-maker-inline"))) {
            controllers.when(DBSSecretController::getGlobalSecretController).thenReturn(controller);
            AIUserProfileCredentials.resolve(profile);
            properties.setToken("edited-token");

            AIUserProfileCredentials.saveChanges(profile);
            AIUserProfileCredentials.saveChanges(profile);

            ArgumentCaptor<DBSSecretValue> value = ArgumentCaptor.forClass(DBSSecretValue.class);
            Mockito.verify(controller).setPrivateSecretValue(ArgumentMatchers.any(DBSSecretObject.class), value.capture());
            Assertions.assertEquals("edited-token", value.getValue().getValue());
            Assertions.assertEquals(Map.of("token", "edited-token"), profile.getResolvedUserCredentials());
            Mockito.verify(controller).flushChanges();
        }
    }

    @Test
    void doesNotOverwritePrivateCredentialsBeforeResolvingThem() throws Exception {
        AIConfigurationProfile profile = userProfile(new OpenAIProperties());
        DBSSecretController controller = Mockito.mock(DBSSecretController.class);
        try (MockedStatic<DBSSecretController> controllers = Mockito.mockStatic(
            DBSSecretController.class, Mockito.withSettings().mockMaker("mock-maker-inline"))) {
            controllers.when(DBSSecretController::getGlobalSecretController).thenReturn(controller);
            AIUserProfileCredentials.saveChanges(profile);
            Mockito.verifyNoInteractions(controller);
        }
    }

    @Test
    void clearingAnEditedCredentialDeletesThePrivateValue() throws Exception {
        OpenAIProperties properties = new OpenAIProperties();
        AIConfigurationProfile profile = userProfile(properties);
        DBSSecretController controller = Mockito.mock(DBSSecretController.class);
        Mockito.when(controller.getPrivateSecretValue("ai.profile.work.token")).thenReturn("old-token");
        try (MockedStatic<DBSSecretController> controllers = Mockito.mockStatic(
            DBSSecretController.class, Mockito.withSettings().mockMaker("mock-maker-inline"))) {
            controllers.when(DBSSecretController::getGlobalSecretController).thenReturn(controller);
            AIUserProfileCredentials.resolve(profile);
            properties.setToken("");
            AIUserProfileCredentials.saveChanges(profile);
            ArgumentCaptor<DBSSecretValue> value = ArgumentCaptor.forClass(DBSSecretValue.class);
            Mockito.verify(controller).setPrivateSecretValue(ArgumentMatchers.any(DBSSecretObject.class), value.capture());
            Assertions.assertNull(value.getValue().getValue());
        }
    }

    @Test
    void failedSaveKeepsCredentialChangesPending() throws Exception {
        OpenAIProperties properties = new OpenAIProperties();
        AIConfigurationProfile profile = userProfile(properties);
        DBSSecretController controller = Mockito.mock(DBSSecretController.class);
        Mockito.when(controller.getPrivateSecretValue("ai.profile.work.token")).thenReturn("old-token");
        Mockito.doThrow(new DBException("Storage unavailable")).when(controller).setPrivateSecretValue(
            ArgumentMatchers.any(DBSSecretObject.class), ArgumentMatchers.any(DBSSecretValue.class));
        try (MockedStatic<DBSSecretController> controllers = Mockito.mockStatic(
            DBSSecretController.class, Mockito.withSettings().mockMaker("mock-maker-inline"))) {
            controllers.when(DBSSecretController::getGlobalSecretController).thenReturn(controller);
            AIUserProfileCredentials.resolve(profile);
            properties.setToken("edited-token");
            Assertions.assertThrows(DBException.class, () -> AIUserProfileCredentials.saveChanges(profile));
            Assertions.assertEquals(Map.of("token", "old-token"), profile.getResolvedUserCredentials());
        }
    }

    @Test
    void administratorSavePersistsEditedUserTokenPrivately() throws Exception {
        DBPPlatform platform = Mockito.mock(DBPPlatform.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(platform.getApplication().isDistributed()).thenReturn(true);
        Mockito.when(platform.getWorkspace().hasRealmPermission(RMConstants.PERMISSION_CONFIGURATION_MANAGER)).thenReturn(true);
        Mockito.when(platform.getConfigurationController().loadConfigurationFile(AISettingsManager.AI_CONFIGURATION_FILE_NAME))
            .thenReturn("""
                {"configurations":{"work":{"name":"Work","engine":"openai","configuration":{"global":false}}}}
                """);
        AIEngineRegistry registry = Mockito.mock(AIEngineRegistry.class);
        AIEngineDescriptor descriptor = Mockito.mock(AIEngineDescriptor.class);
        Mockito.doReturn(OpenAIProperties.class).when(descriptor).getPropertiesType();
        Mockito.when(registry.getEngineDescriptor("openai")).thenReturn(descriptor);
        DBSSecretController controller = Mockito.mock(DBSSecretController.class);
        Mockito.when(controller.getPrivateSecretValue("ai.profile.work.token")).thenReturn("previous-token");
        try (MockedStatic<DBWorkbench> workbench = Mockito.mockStatic(
                 DBWorkbench.class, Mockito.withSettings().mockMaker("mock-maker-inline"));
             MockedStatic<AIEngineRegistry> engines = Mockito.mockStatic(
                 AIEngineRegistry.class, Mockito.withSettings().mockMaker("mock-maker-inline"));
             MockedStatic<DBSSecretController> controllers = Mockito.mockStatic(
                 DBSSecretController.class, Mockito.withSettings().mockMaker("mock-maker-inline"))) {
            workbench.when(DBWorkbench::getPlatform).thenReturn(platform);
            workbench.when(DBWorkbench::isDistributed).thenReturn(true);
            engines.when(AIEngineRegistry::getInstance).thenReturn(registry);
            controllers.when(DBSSecretController::getGlobalSecretController).thenReturn(controller);
            AISettingsManager manager = AISettingsManager.getInstance();
            WorkspaceConfigEventManager.fireConfigChangedEvent(AISettingsManager.AI_CONFIGURATION_FILE_NAME);
            try {
                AISettings settings = manager.getSettings();
                settings.resolveSecrets();
                ((OpenAIProperties) settings.getConfiguration("work").getConfiguration()).setToken("edited-token");

                manager.saveSettings(settings);

                ArgumentCaptor<DBSSecretValue> token = ArgumentCaptor.forClass(DBSSecretValue.class);
                Mockito.verify(controller).setPrivateSecretValue(ArgumentMatchers.any(DBSSecretObject.class), token.capture());
                Assertions.assertEquals("edited-token", token.getValue().getValue());
                ArgumentCaptor<String> content = ArgumentCaptor.forClass(String.class);
                Mockito.verify(platform.getConfigurationController()).saveConfigurationFile(
                    ArgumentMatchers.eq(AISettingsManager.AI_CONFIGURATION_FILE_NAME), content.capture());
                Assertions.assertFalse(content.getValue().contains("edited-token"));
            } finally {
                WorkspaceConfigEventManager.fireConfigChangedEvent(AISettingsManager.AI_CONFIGURATION_FILE_NAME);
            }
        }
    }

    @Test
    void resolvesAccountCredentialsFromTheWebProfileStore() throws Exception {
        OpenAIProperties properties = new OpenAIProperties();
        properties.setAccessToken("stale-other-account");
        properties.setModel("administrator-model");
        properties.setBaseUrl("https://administrator.example/v1");
        AIConfigurationProfile profile = userProfile(properties);
        Map<String, String> secrets = new HashMap<>(Map.of(
            "ai.profile.work.accountAuthentication", "true",
            "ai.profile.work.accessToken", "web-access",
            "ai.profile.work.refreshToken", "web-refresh",
            "ai.profile.work.accountEmail", "user@example.com",
            "ai.profile.work.accountId", "web-account",
            "ai.profile.work.expiresAt", Long.toString(System.currentTimeMillis() + 600_000)
        ));
        DBSSecretController controller = privateStore(secrets);
        try (MockedStatic<DBSSecretController> controllers = Mockito.mockStatic(
            DBSSecretController.class, Mockito.withSettings().mockMaker("mock-maker-inline"))) {
            controllers.when(DBSSecretController::getGlobalSecretController).thenReturn(controller);
            AIUserProfileCredentials.resolve(profile);
            Assertions.assertTrue(properties.isAccountAuthentication());
            Assertions.assertEquals("web-access", properties.getAccessToken());
            Assertions.assertEquals("web-refresh", properties.getRefreshToken());
            Assertions.assertEquals("user@example.com", properties.getAccountEmail());
            Assertions.assertEquals("web-account", properties.getAccountId());
            Assertions.assertEquals("administrator-model", properties.getModel());
            Assertions.assertEquals("https://administrator.example/v1", properties.getBaseUrl());
            secrets.remove("ai.profile.work.refreshToken");
            Assertions.assertThrows(DBException.class,
                () -> properties.getValidAccessToken(Mockito.mock(AIAccountAuthenticator.class)));
        }
    }

    @Test
    void savesAndClearsAccountTokensOnlyInThePrivateProfileStore() throws Exception {
        OpenAIProperties properties = new OpenAIProperties();
        AIConfigurationProfile profile = userProfile(properties);
        Map<String, String> secrets = new HashMap<>();
        DBSSecretController controller = privateStore(secrets);
        try (MockedStatic<DBSSecretController> controllers = Mockito.mockStatic(
            DBSSecretController.class, Mockito.withSettings().mockMaker("mock-maker-inline"))) {
            controllers.when(DBSSecretController::getGlobalSecretController).thenReturn(controller);
            AIUserProfileCredentials.resolve(profile);
            properties.setAccountAuthentication(true);
            properties.setAccountTokens(new AIAccountAuthenticator.Tokens(
                "desktop-access", "desktop-refresh", 600, "personal-account", "user@example.com"));
            properties.saveAccountTokens();
            Assertions.assertEquals("desktop-access", secrets.get("ai.profile.work.accessToken"));
            Assertions.assertEquals("desktop-refresh", secrets.get("ai.profile.work.refreshToken"));
            Assertions.assertEquals("true", secrets.get("ai.profile.work.accountAuthentication"));
            Assertions.assertTrue(secrets.keySet().stream().allMatch(key -> key.startsWith("ai.profile.work.")));
            properties.clearAccountTokens();
            properties.saveAccountTokens();
            Assertions.assertNull(secrets.get("ai.profile.work.accessToken"));
            Assertions.assertNull(secrets.get("ai.profile.work.refreshToken"));
            AIUserProfileCredentials.resolve(profile);
            Assertions.assertFalse(properties.isAccountConnected());
        }
    }

    @Test
    void switchingToApiTokenKeepsAccountTokensAndDoesNotChangeSharedSettings() throws Exception {
        OpenAIProperties properties = new OpenAIProperties();
        AIConfigurationProfile profile = userProfile(properties);
        Map<String, String> secrets = new HashMap<>(Map.of(
            "ai.profile.work.accountAuthentication", "true",
            "ai.profile.work.accessToken", "account-access",
            "ai.profile.work.refreshToken", "account-refresh"
        ));
        DBSSecretController controller = privateStore(secrets);
        try (MockedStatic<DBSSecretController> controllers = Mockito.mockStatic(
            DBSSecretController.class, Mockito.withSettings().mockMaker("mock-maker-inline"))) {
            controllers.when(DBSSecretController::getGlobalSecretController).thenReturn(controller);
            AIUserProfileCredentials.resolve(profile);
            properties.setAccountAuthentication(false);
            properties.setToken("api-token");
            AIUserProfileCredentials.saveChanges(profile);
            Assertions.assertEquals("false", secrets.get("ai.profile.work.accountAuthentication"));
            Assertions.assertEquals("api-token", secrets.get("ai.profile.work.token"));
            Assertions.assertEquals("account-refresh", secrets.get("ai.profile.work.refreshToken"));
            AIUserProfileCredentials.resolve(profile);
            Assertions.assertFalse(properties.isAccountAuthentication());
            Assertions.assertEquals("api-token", properties.getToken());
        }
    }

    @Test
    void accountCredentialsNeverFallBackToAnotherProfile() throws Exception {
        OpenAIProperties properties = new OpenAIProperties();
        AIConfigurationProfile profile = userProfile(properties);
        Map<String, String> secrets = new HashMap<>(Map.of(
            "ai.profile.other.accessToken", "another-user-access",
            "ai.profile.other.refreshToken", "another-user-refresh"
        ));
        DBSSecretController controller = privateStore(secrets);
        try (MockedStatic<DBSSecretController> controllers = Mockito.mockStatic(
            DBSSecretController.class, Mockito.withSettings().mockMaker("mock-maker-inline"))) {
            controllers.when(DBSSecretController::getGlobalSecretController).thenReturn(controller);
            AIUserProfileCredentials.resolve(profile);
            Assertions.assertNull(properties.getAccessToken());
            Assertions.assertNull(properties.getRefreshToken());
            Assertions.assertFalse(properties.isAccountConnected());
        }
    }

    @Test
    void accountAuthorizationIsAvailableInTeamDesktopButNotHeadlessOrMultiuser() {
        DBPPlatform platform = Mockito.mock(DBPPlatform.class, Mockito.RETURNS_DEEP_STUBS);
        try (MockedStatic<DBWorkbench> workbench = Mockito.mockStatic(
            DBWorkbench.class, Mockito.withSettings().mockMaker("mock-maker-inline"))) {
            workbench.when(DBWorkbench::getPlatform).thenReturn(platform);
            workbench.when(DBWorkbench::isDistributed).thenReturn(true);
            Assertions.assertTrue(AIAccountAuthenticator.isSupported());
            Mockito.when(platform.getApplication().isHeadlessMode()).thenReturn(true);
            Assertions.assertFalse(AIAccountAuthenticator.isSupported());
            Mockito.when(platform.getApplication().isHeadlessMode()).thenReturn(false);
            Mockito.when(platform.getApplication().isMultiuser()).thenReturn(true);
            Assertions.assertFalse(AIAccountAuthenticator.isSupported());
        }
    }

    @Test
    void completedAccountAuthorizationCannotWriteAfterTheTeamSessionChanges() throws Exception {
        OpenAIProperties properties = new OpenAIProperties();
        AIConfigurationProfile profile = userProfile(properties);
        DBPPlatform platform = Mockito.mock(DBPPlatform.class, Mockito.RETURNS_DEEP_STUBS);
        SMSession session = Mockito.mock(SMSession.class);
        Mockito.when(platform.getWorkspace().getWorkspaceSession()).thenReturn(session);
        DBSSecretController controller = privateStore(new HashMap<>());
        try (MockedStatic<DBWorkbench> workbench = Mockito.mockStatic(
                 DBWorkbench.class, Mockito.withSettings().mockMaker("mock-maker-inline"));
             MockedStatic<DBSSecretController> controllers = Mockito.mockStatic(
                 DBSSecretController.class, Mockito.withSettings().mockMaker("mock-maker-inline"))) {
            workbench.when(DBWorkbench::getPlatform).thenReturn(platform);
            workbench.when(DBWorkbench::isPlatformStarted).thenReturn(true);
            controllers.when(DBSSecretController::getGlobalSecretController).thenReturn(controller);
            AIUserProfileCredentials.resolve(profile);
            properties.setAccountTokens(new AIAccountAuthenticator.Tokens("access", "refresh", 600, "account", "user@example.com"));
            Mockito.when(platform.getWorkspace().getWorkspaceSession()).thenReturn(Mockito.mock(SMSession.class));
            Assertions.assertThrows(DBException.class, properties::saveAccountTokens);
            Mockito.verify(controller, Mockito.never()).setPrivateSecretValue(
                ArgumentMatchers.any(DBSSecretObject.class), ArgumentMatchers.any(DBSSecretValue.class));
        }
    }

    @Test
    void accountCredentialWritesStopIfTheTeamSessionChangesMidSave() throws Exception {
        OpenAIProperties properties = new OpenAIProperties();
        AIConfigurationProfile profile = userProfile(properties);
        DBPPlatform platform = Mockito.mock(DBPPlatform.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(platform.getWorkspace().getWorkspaceSession()).thenReturn(Mockito.mock(SMSession.class));
        DBSSecretController controller = privateStore(new HashMap<>());
        Mockito.doAnswer(invocation -> {
            Mockito.when(platform.getWorkspace().getWorkspaceSession()).thenReturn(Mockito.mock(SMSession.class));
            return null;
        }).when(controller).setPrivateSecretValue(ArgumentMatchers.any(DBSSecretObject.class), ArgumentMatchers.any(DBSSecretValue.class));
        try (MockedStatic<DBWorkbench> workbench = Mockito.mockStatic(
                 DBWorkbench.class, Mockito.withSettings().mockMaker("mock-maker-inline"));
             MockedStatic<DBSSecretController> controllers = Mockito.mockStatic(
                 DBSSecretController.class, Mockito.withSettings().mockMaker("mock-maker-inline"))) {
            workbench.when(DBWorkbench::getPlatform).thenReturn(platform);
            workbench.when(DBWorkbench::isPlatformStarted).thenReturn(true);
            controllers.when(DBSSecretController::getGlobalSecretController).thenReturn(controller);
            AIUserProfileCredentials.resolve(profile);
            properties.setAccountTokens(new AIAccountAuthenticator.Tokens("access", "refresh", 600, "account", "user@example.com"));
            Assertions.assertThrows(DBException.class, properties::saveAccountTokens);
            Mockito.verify(controller).setPrivateSecretValue(
                ArgumentMatchers.any(DBSSecretObject.class), ArgumentMatchers.any(DBSSecretValue.class));
            Mockito.verify(controller, Mockito.never()).flushChanges();
        }
    }

    @Test
    void tokenRefreshDoesNotOverwriteCredentialsRotatedByAnotherClient() throws Exception {
        OpenAIProperties properties = new OpenAIProperties();
        AIConfigurationProfile profile = userProfile(properties);
        Map<String, String> secrets = new HashMap<>(Map.of(
            "ai.profile.work.accountAuthentication", "true",
            "ai.profile.work.accessToken", "expired-access",
            "ai.profile.work.refreshToken", "old-refresh"
        ));
        DBSSecretController controller = privateStore(secrets);
        AIAccountAuthenticator authenticator = Mockito.mock(AIAccountAuthenticator.class);
        Mockito.when(authenticator.refresh("old-refresh")).thenAnswer(invocation -> {
            secrets.put("ai.profile.work.refreshToken", "web-rotated-refresh");
            return new AIAccountAuthenticator.Tokens("desktop-access", "desktop-refresh", 600, "account", "user@example.com");
        });
        try (MockedStatic<DBSSecretController> controllers = Mockito.mockStatic(
            DBSSecretController.class, Mockito.withSettings().mockMaker("mock-maker-inline"))) {
            controllers.when(DBSSecretController::getGlobalSecretController).thenReturn(controller);
            AIUserProfileCredentials.resolve(profile);
            Assertions.assertThrows(DBException.class, () -> properties.getValidAccessToken(authenticator));
            Assertions.assertEquals("web-rotated-refresh", secrets.get("ai.profile.work.refreshToken"));
            Mockito.verify(controller, Mockito.never()).setPrivateSecretValue(
                ArgumentMatchers.any(DBSSecretObject.class), ArgumentMatchers.any(DBSSecretValue.class));
        }
    }

    private DBSSecretController privateStore(Map<String, String> secrets) throws DBException {
        DBSSecretController controller = Mockito.mock(DBSSecretController.class);
        Mockito.when(controller.getPrivateSecretValue(ArgumentMatchers.anyString()))
            .thenAnswer(invocation -> secrets.get(invocation.getArgument(0)));
        Mockito.doAnswer(invocation -> {
            DBSSecretObject object = invocation.getArgument(0);
            DBSSecretValue value = invocation.getArgument(1);
            Assertions.assertEquals("aiProfile", object.getSecretObjectType());
            Assertions.assertEquals("work", object.getSecretObjectId());
            if (value.getValue() == null) {
                secrets.remove(value.getId());
            } else {
                secrets.put(value.getId(), value.getValue());
            }
            return null;
        }).when(controller).setPrivateSecretValue(ArgumentMatchers.any(DBSSecretObject.class), ArgumentMatchers.any(DBSSecretValue.class));
        return controller;
    }

    private AIConfigurationProfile userProfile(AIEngineProperties properties) throws DBException {
        AIConfigurationProfile profile = Mockito.spy(new AIConfigurationProfile());
        profile.setProfileId("work");
        profile.setProfileName("Work");
        profile.setGlobal(false);
        Mockito.doReturn(properties).when(profile).getConfiguration();
        return profile;
    }

    private AIConfigurationProfile profile(String profileId, AIEngineProperties properties) throws DBException {
        AIConfigurationProfile profile = Mockito.mock(AIConfigurationProfile.class);
        Mockito.when(profile.getProfileId()).thenReturn(profileId);
        Mockito.when(profile.getProfileName()).thenReturn(profileId);
        Mockito.when(profile.getConfiguration()).thenReturn(properties);
        return profile;
    }
}

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
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIProperties;
import org.jkiss.dbeaver.model.ai.registry.AIEngineDescriptor;
import org.jkiss.dbeaver.model.ai.registry.AIEngineRegistry;
import org.jkiss.dbeaver.model.ai.registry.AISettingsManager;
import org.jkiss.dbeaver.model.app.DBPPlatform;
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

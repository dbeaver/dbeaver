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

import com.google.gson.JsonParser;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBConfigurationController;
import org.jkiss.dbeaver.model.WorkspaceConfigEventManager;
import org.jkiss.dbeaver.model.ai.engine.openai.AIAccountAuthenticator;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class AIUserProfileLifecycleTest extends DBeaverUnitTest {
    @Test
    void personalAccountMethodDoesNotReplaceSharedMethod() throws Exception {
        try (Fixture fixture = new Fixture()) {
            AISettings settings = fixture.settings(false);
            AIConfigurationProfile profile = settings.getConfiguration("openai");
            fixture.secrets.put("ai.profile.openai.accountAuthentication", "true");
            profile.resolveSecrets();
            OpenAIProperties properties = (OpenAIProperties) profile.getConfiguration();
            Assertions.assertTrue(properties.isAccountAuthentication());
            properties.setAuthentication(OpenAIProperties.AUTHENTICATION_API_TOKEN);
            AIUserProfileCredentials.saveChanges(profile);
            properties.setAuthentication(OpenAIProperties.AUTHENTICATION_CHATGPT_ACCOUNT);
            AIUserProfileCredentials.saveChanges(profile);

            settings.prepareSettingsSaving();
            String serialized = AISettingsManager.SAVE_PROPS_GSON.toJson(settings);
            var json = JsonParser.parseString(serialized).getAsJsonObject();
            Assertions.assertEquals(OpenAIProperties.AUTHENTICATION_API_TOKEN,
                json.getAsJsonObject("configurations").getAsJsonObject("openai")
                    .getAsJsonObject("configuration").get("openai.authentication").getAsString());
            Assertions.assertEquals(OpenAIProperties.AUTHENTICATION_API_TOKEN,
                json.getAsJsonObject("engineConfigurations").getAsJsonObject("openai")
                    .getAsJsonObject("properties").get("openai.authentication").getAsString());
            Assertions.assertTrue(properties.isAccountAuthentication());
            Assertions.assertEquals("true", fixture.secrets.get("ai.profile.openai.accountAuthentication"));

            fixture.secrets.clear();
            fixture.secrets.put("ai.profile.openai.token", "other-user-token");
            AISettings otherSettings = AISettingsManager.READ_PROPS_GSON.fromJson(serialized, AISettings.class);
            otherSettings.finishSettingsLoading();
            AIConfigurationProfile otherProfile = otherSettings.getConfiguration("openai");
            otherProfile.resolveSecrets();
            OpenAIProperties otherProperties = (OpenAIProperties) otherProfile.getConfiguration();
            Assertions.assertFalse(otherProperties.isAccountAuthentication());
            Assertions.assertEquals("other-user-token", otherProperties.getToken());
            Assertions.assertTrue(otherProperties.isValidConfiguration());
        }
    }

    @Test
    void personalApiMethodPreservesExistingSharedAccountDefault() throws Exception {
        try (Fixture fixture = new Fixture()) {
            AISettings settings = fixture.settings(false);
            AIConfigurationProfile profile = settings.getConfiguration("openai");
            OpenAIProperties properties = (OpenAIProperties) profile.getConfiguration();
            properties.setAccountAuthentication(true);
            fixture.secrets.put("ai.profile.openai.accountAuthentication", "false");
            profile.resolveSecrets();
            Assertions.assertFalse(properties.isAccountAuthentication());

            OpenAIProperties copy = (OpenAIProperties) AIUserProfileCredentials.copyWithoutCredentials(properties);
            Assertions.assertTrue(copy.isAccountAuthentication());
            Assertions.assertFalse(properties.isAccountAuthentication());
        }
    }

    @Test
    void missingPrivateMethodDoesNotReusePreviousUsersMethod() throws Exception {
        try (Fixture fixture = new Fixture()) {
            AIConfigurationProfile profile = fixture.settings(false).getConfiguration("openai");
            fixture.secrets.put("ai.profile.openai.accountAuthentication", "true");
            profile.resolveSecrets();
            Assertions.assertTrue(((OpenAIProperties) profile.getConfiguration()).isAccountAuthentication());
            fixture.secrets.clear();
            profile.resolveSecrets();
            Assertions.assertFalse(((OpenAIProperties) profile.getConfiguration()).isAccountAuthentication());
        }
    }

    @Test
    void deletingProfileRemovesAllUsersCredentialsBeforeReusingItsId() throws Exception {
        try (Fixture fixture = new Fixture()) {
            AISettings settings = fixture.settings(false);
            AIConfigurationProfile profile = settings.getConfiguration("openai");
            fixture.secrets.put("ai.profile.openai.token", "user-token");
            fixture.secrets.put("ai.profile.other.token", "unrelated-token");
            fixture.otherSecrets.put("ai.profile.openai.refreshToken", "other-user-refresh");
            profile.resolveSecrets();

            AISettingsManager.getInstance().removeConfiguration(settings, profile);

            Assertions.assertEquals(Map.of("ai.profile.other.token", "unrelated-token"), fixture.secrets);
            Assertions.assertTrue(fixture.otherSecrets.isEmpty());
            Mockito.verify(fixture.controller).flushChanges();
            Mockito.verify(fixture.controller, Mockito.never()).setPrivateSecretValue(
                ArgumentMatchers.anyString(), ArgumentMatchers.nullable(String.class));
            String newId = settings.generateProfileId(fixture.descriptor);
            Assertions.assertEquals("openai", newId);
            AIConfigurationProfile recreated = settings.createConfiguration(newId, fixture.descriptor);
            recreated.setGlobal(false);
            recreated.resolveSecrets();
            OpenAIProperties properties = (OpenAIProperties) recreated.getConfiguration();
            Assertions.assertEquals("", properties.getToken());
            Assertions.assertNull(properties.getRefreshToken());
        }
    }

    @Test
    void deletionCleansCredentialsEvenIfTheProfileIsSharedOrUnavailable() throws Exception {
        for (boolean shared : List.of(false, true)) {
            try (Fixture fixture = new Fixture()) {
                AISettings settings = fixture.settings(shared);
                fixture.secrets.put("ai.profile.openai.token", "old-personal-token");
                if (!shared) {
                    Mockito.when(fixture.registry.getEngineDescriptor("openai")).thenReturn(null);
                }

                AISettingsManager.getInstance().removeConfiguration(settings, settings.getConfiguration("openai"));

                Assertions.assertTrue(fixture.secrets.isEmpty());
            }
        }
    }

    @Test
    void failedConfigurationSavePreservesEveryUsersCredentialsAndRestoresProfile() throws Exception {
        try (Fixture fixture = new Fixture()) {
            AISettings settings = fixture.settings(false);
            AIConfigurationProfile profile = settings.getConfiguration("openai");
            settings.setDefaultConfiguration(profile);
            settings.setUserDefaultConfiguration(profile);
            fixture.secrets.put("ai.profile.openai.token", "user-token");
            fixture.otherSecrets.put("ai.profile.openai.refreshToken", "other-user-token");
            profile.resolveSecrets();
            Mockito.doThrow(new DBException("Storage unavailable")).when(fixture.configurationController)
                .saveConfigurationFile(ArgumentMatchers.anyString(), ArgumentMatchers.anyString());

            Assertions.assertThrows(DBException.class, () -> AISettingsManager.getInstance().removeConfiguration(settings, profile));

            Assertions.assertSame(profile, settings.getConfiguration("openai"));
            Assertions.assertSame(profile, settings.getDefaultConfiguration());
            Assertions.assertTrue(settings.getPendingProfileSecretDeletions().isEmpty());
            Assertions.assertEquals("user-token", fixture.secrets.get("ai.profile.openai.token"));
            Assertions.assertEquals("other-user-token", fixture.otherSecrets.get("ai.profile.openai.refreshToken"));
            Mockito.verify(fixture.controller, Mockito.never()).deleteObjectSecrets(ArgumentMatchers.any(DBSSecretObject.class));
            Mockito.verify(fixture.controller, Mockito.never()).flushChanges();
            authorize((OpenAIProperties) profile.getConfiguration(), "current-refresh");
            Assertions.assertEquals("current-refresh", fixture.secrets.get("ai.profile.openai.refreshToken"));
        }
    }

    @Test
    void failedCleanupReservesProfileIdAcrossReloadAndCanBeRetried() throws Exception {
        try (Fixture fixture = new Fixture()) {
            AISettings settings = fixture.settings(false);
            fixture.secrets.put("ai.profile.openai.token", "user-token");
            AIConfigurationProfile profile = settings.getConfiguration("openai");
            Mockito.doThrow(new DBException("Cleanup unavailable")).when(fixture.controller)
                .deleteObjectSecrets(ArgumentMatchers.any(DBSSecretObject.class));

            Assertions.assertThrows(DBException.class, () -> AISettingsManager.getInstance().removeConfiguration(settings, profile));

            AISettings reloaded = fixture.reload();
            Assertions.assertNull(reloaded.getConfigurationOrNull("openai"));
            Assertions.assertEquals(List.of("openai"), reloaded.getPendingProfileSecretDeletions());
            Assertions.assertEquals("openai_1", reloaded.generateProfileId(fixture.descriptor));
            Assertions.assertThrows(DBException.class, () -> reloaded.createConfiguration("openai", fixture.descriptor));
            Assertions.assertEquals("user-token", fixture.secrets.get("ai.profile.openai.token"));

            fixture.allowCleanup();
            AISettingsManager.getInstance().saveSettingsOrThrow(reloaded);
            Assertions.assertTrue(fixture.secrets.isEmpty());
            Assertions.assertTrue(fixture.reload().getPendingProfileSecretDeletions().isEmpty());
            Assertions.assertNotNull(reloaded.createConfiguration("openai", fixture.descriptor));
        }
    }

    @Test
    void failedReservationRemovalCanBeRetriedAfterReload() throws Exception {
        try (Fixture fixture = new Fixture()) {
            AISettings settings = fixture.settings(false);
            fixture.secrets.put("ai.profile.openai.token", "user-token");
            AIConfigurationProfile profile = settings.getConfiguration("openai");
            Mockito.doAnswer(invocation -> {
                String content = invocation.getArgument(1);
                if (fixture.secrets.isEmpty()) {
                    throw new DBException("Final write unavailable");
                }
                fixture.persistedConfiguration = content;
                return null;
            }).when(fixture.configurationController).saveConfigurationFile(ArgumentMatchers.anyString(), ArgumentMatchers.anyString());

            Assertions.assertThrows(DBException.class, () -> AISettingsManager.getInstance().removeConfiguration(settings, profile));
            Assertions.assertTrue(fixture.secrets.isEmpty());
            Assertions.assertEquals(List.of("openai"), settings.getPendingProfileSecretDeletions());
            Assertions.assertThrows(DBException.class, () -> settings.createConfiguration("openai", fixture.descriptor));
            AISettings reloaded = fixture.reload();
            Assertions.assertEquals(List.of("openai"), reloaded.getPendingProfileSecretDeletions());
            fixture.allowConfigurationWrites();
            AISettingsManager.getInstance().saveSettingsOrThrow(reloaded);
            Assertions.assertTrue(fixture.reload().getPendingProfileSecretDeletions().isEmpty());
        }
    }

    @Test
    void remoteUpdateStopsPendingCredentialCleanup() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.persistedConfiguration = """
                {"pendingProfileSecretDeletions":["first","second"]}
                """;
            AISettings settings = fixture.reload();
            Mockito.doAnswer(invocation -> {
                WorkspaceConfigEventManager.fireConfigChangedEvent(AISettingsManager.AI_CONFIGURATION_FILE_NAME);
                return null;
            }).when(fixture.controller).deleteObjectSecrets(ArgumentMatchers.any(DBSSecretObject.class));

            Assertions.assertThrows(DBException.class, () -> AISettingsManager.getInstance().saveSettingsOrThrow(settings));

            var removed = ArgumentCaptor.forClass(DBSSecretObject.class);
            Mockito.verify(fixture.controller).deleteObjectSecrets(removed.capture());
            Assertions.assertEquals("first", removed.getValue().getSecretObjectId());
            Assertions.assertEquals(List.of("first", "second"), fixture.reload().getPendingProfileSecretDeletions());
        }
    }

    @Test
    void unsupportedCleanupRejectsDeletionWithoutChangingProfileOrCredentials() throws Exception {
        try (Fixture fixture = new Fixture()) {
            AISettings settings = fixture.settings(false);
            AIConfigurationProfile profile = settings.getConfiguration("openai");
            fixture.secrets.put("ai.profile.openai.token", "user-token");
            Mockito.when(fixture.controller.supportsObjectSecretDeletion()).thenReturn(false);

            Assertions.assertThrows(DBException.class, () -> AISettingsManager.getInstance().removeConfiguration(settings, profile));

            Assertions.assertSame(profile, settings.getConfiguration("openai"));
            Assertions.assertEquals("user-token", fixture.secrets.get("ai.profile.openai.token"));
            Assertions.assertTrue(settings.getPendingProfileSecretDeletions().isEmpty());
            Mockito.verify(fixture.configurationController, Mockito.never())
                .saveConfigurationFile(ArgumentMatchers.anyString(), ArgumentMatchers.anyString());
        }
    }

    @Test
    void standaloneDeletionDoesNotDeleteTeamProfileSecrets() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.workbench.when(DBWorkbench::isDistributed).thenReturn(false);
            AISettings settings = fixture.settings(false);
            AIConfigurationProfile profile = settings.getConfiguration("openai");
            Mockito.verify(fixture.controller, Mockito.never()).deleteObjectSecrets(ArgumentMatchers.any(DBSSecretObject.class));
        }
    }

    @Test
    void removedProfileAndItsEditorCannotWriteBeforeOrAfterIdReuse() throws Exception {
        try (Fixture fixture = new Fixture()) {
            AISettings settings = fixture.settings(false);
            AIConfigurationProfile profile = settings.getConfiguration("openai");
            profile.resolveSecrets();
            AIConfigurationProfile personal = credentialsCopy(profile);
            OpenAIProperties target = (OpenAIProperties) profile.getConfiguration();
            OpenAIProperties editorTarget = (OpenAIProperties) personal.getConfiguration();

            AISettingsManager.getInstance().removeConfiguration(settings, profile);

            Assertions.assertThrows(DBException.class, () -> authorize(target, "late-refresh"));
            Assertions.assertThrows(DBException.class, () -> authorize(editorTarget, "late-editor"));
            Assertions.assertThrows(DBException.class, () -> AIUserProfileCredentials.save(personal, Map.of("token", "late-token")));
            Assertions.assertTrue(fixture.secrets.isEmpty());
            String id = settings.generateProfileId(fixture.descriptor);
            Assertions.assertEquals("openai", id);

            AIConfigurationProfile replacement = settings.createConfiguration(id, fixture.descriptor);
            replacement.setGlobal(false);
            replacement.resolveSecrets();
            authorize((OpenAIProperties) replacement.getConfiguration(), "replacement-refresh");

            Assertions.assertThrows(DBException.class, () -> authorize(target, "late-refresh"));
            Assertions.assertThrows(DBException.class, () -> authorize(editorTarget, "late-editor"));
            Assertions.assertEquals("replacement-refresh", fixture.secrets.get("ai.profile.openai.refreshToken"));
        }
    }

    @Test
    void configurationReplacementRejectsPreviousAuthorization() throws Exception {
        try (Fixture fixture = new Fixture()) {
            AIConfigurationProfile profile = fixture.settings(false).getConfiguration("openai");
            profile.resolveSecrets();
            OpenAIProperties previous = (OpenAIProperties) profile.getConfiguration();
            OpenAIProperties replacement = new OpenAIProperties();
            replacement.setGlobal(false);
            profile.setConfiguration(replacement);
            profile.resolveSecrets();
            authorize(replacement, "replacement-refresh");

            Assertions.assertThrows(DBException.class, () -> authorize(previous, "late-refresh"));

            Assertions.assertEquals("replacement-refresh", fixture.secrets.get("ai.profile.openai.refreshToken"));
        }
    }

    @Test
    void savingCurrentSettingsKeepsCredentialEditorsUsable() throws Exception {
        try (Fixture fixture = new Fixture()) {
            AISettings settings = fixture.settings(false);
            AISettingsManager manager = AISettingsManager.getInstance();
            manager.saveSettingsOrThrow(settings);
            AIConfigurationProfile personal = credentialsCopy(settings.getConfiguration("openai"));
            manager.saveSettingsOrThrow(settings);

            authorize((OpenAIProperties) personal.getConfiguration(), "current-refresh");

            Assertions.assertSame(settings, manager.getSettings());
            Assertions.assertEquals("current-refresh", fixture.secrets.get("ai.profile.openai.refreshToken"));
        }
    }

    @Test
    void staleSettingsCannotRestoreDeletedProfilesOrAuthorization() throws Exception {
        for (boolean reloadBeforeSaving : List.of(false, true)) {
            try (Fixture fixture = new Fixture()) {
                AISettings settings = fixture.settings(false);
                AISettingsManager manager = AISettingsManager.getInstance();
                manager.saveSettingsOrThrow(settings);
                AIConfigurationProfile profile = settings.getConfiguration("openai");
                profile.resolveSecrets();
                AIConfigurationProfile personal = credentialsCopy(profile);
                fixture.persistedConfiguration = "{\"configurations\":{},\"engineConfigurations\":{}}";
                String remoteConfiguration = fixture.persistedConfiguration;
                WorkspaceConfigEventManager.fireConfigChangedEvent(AISettingsManager.AI_CONFIGURATION_FILE_NAME);
                if (reloadBeforeSaving) {
                    Assertions.assertNull(manager.getSettings().getConfigurationOrNull("openai"));
                }
                Mockito.clearInvocations(fixture.controller, fixture.configurationController);

                Assertions.assertThrows(DBException.class, settings::validateCurrentSettings);
                Assertions.assertThrows(DBException.class, () -> manager.saveSettingsOrThrow(settings));

                Mockito.verifyNoInteractions(fixture.controller, fixture.configurationController);
                Assertions.assertEquals(remoteConfiguration, fixture.persistedConfiguration);
                Assertions.assertNull(manager.getSettings().getConfigurationOrNull("openai"));
                Assertions.assertThrows(DBException.class, () -> authorize((OpenAIProperties) profile.getConfiguration(), "late-original"));
                Assertions.assertThrows(DBException.class, () -> authorize((OpenAIProperties) personal.getConfiguration(), "late-copy"));
                Assertions.assertTrue(fixture.secrets.isEmpty());
            }
        }
    }

    @Test
    void staleSettingsCannotSaveEditedPrivateCredentials() throws Exception {
        try (Fixture fixture = new Fixture()) {
            AISettings settings = fixture.settings(false);
            AISettingsManager manager = AISettingsManager.getInstance();
            manager.saveSettingsOrThrow(settings);
            AIConfigurationProfile profile = settings.getConfiguration("openai");
            profile.resolveSecrets();
            ((OpenAIProperties) profile.getConfiguration()).setToken("pending-token");
            WorkspaceConfigEventManager.fireConfigChangedEvent(AISettingsManager.AI_CONFIGURATION_FILE_NAME);
            Mockito.clearInvocations(fixture.controller, fixture.configurationController);

            Assertions.assertThrows(DBException.class, () -> manager.saveSettingsOrThrow(settings));

            Mockito.verifyNoInteractions(fixture.controller, fixture.configurationController);
            Assertions.assertTrue(fixture.secrets.isEmpty());
        }
    }

    @Test
    void remoteNotificationDuringSaveCannotReactivateOldAuthorization() throws Exception {
        try (Fixture fixture = new Fixture()) {
            AISettings settings = fixture.settings(false);
            AISettingsManager manager = AISettingsManager.getInstance();
            manager.saveSettingsOrThrow(settings);
            AIConfigurationProfile profile = settings.getConfiguration("openai");
            profile.resolveSecrets();
            Mockito.doAnswer(invocation -> {
                fixture.persistedConfiguration = "{\"configurations\":{},\"engineConfigurations\":{}}";
                WorkspaceConfigEventManager.fireConfigChangedEvent(AISettingsManager.AI_CONFIGURATION_FILE_NAME);
                return null;
            }).when(fixture.configurationController)
                .saveConfigurationFile(ArgumentMatchers.anyString(), ArgumentMatchers.anyString());

            Assertions.assertThrows(DBException.class, () -> manager.saveSettingsOrThrow(settings));

            Assertions.assertNull(manager.getSettings().getConfigurationOrNull("openai"));
            Assertions.assertThrows(DBException.class, () -> authorize((OpenAIProperties) profile.getConfiguration(), "late-refresh"));
            Assertions.assertTrue(fixture.secrets.isEmpty());
        }
    }

    @Test
    void standaloneSaveDoesNotRequireTeamSnapshotValidation() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.workbench.when(DBWorkbench::isDistributed).thenReturn(false);
            AISettings settings = fixture.settings(false);
            settings.markStale();

            AISettingsManager.getInstance().saveSettingsOrThrow(settings);

            Assertions.assertNotNull(fixture.reload().getConfigurationOrNull("openai"));
        }
    }

    private AIConfigurationProfile credentialsCopy(AIConfigurationProfile profile) throws DBException {
        AIConfigurationProfile copy = new AIConfigurationProfile();
        copy.setProfileId(profile.getProfileId());
        copy.setProfileName(profile.getProfileName());
        copy.setEngineId(profile.getEngineId());
        copy.setConfiguration(AISettingsManager.READ_PROPS_GSON.fromJson(
            AISettingsManager.READ_PROPS_GSON.toJson(profile.getConfiguration()), OpenAIProperties.class));
        copy.useUserCredentialScope(profile);
        copy.resolveSecrets();
        return copy;
    }

    private void authorize(OpenAIProperties properties, String refreshToken) throws DBException {
        properties.setAccountAuthentication(true);
        properties.setAccountTokens(new AIAccountAuthenticator.Tokens("access", refreshToken, 600, "account", "user@example.com"));
        properties.saveAccountTokens();
    }

    static final class Fixture implements AutoCloseable {
        final Map<String, String> secrets = new HashMap<>();
        final Map<String, String> otherSecrets = new HashMap<>();
        final DBPPlatform platform = Mockito.mock(DBPPlatform.class, Mockito.RETURNS_DEEP_STUBS);
        final DBConfigurationController configurationController = platform.getConfigurationController();
        String persistedConfiguration;
        final DBSSecretController controller = Mockito.mock(DBSSecretController.class);
        final AIEngineRegistry registry = Mockito.mock(AIEngineRegistry.class);
        final AIEngineDescriptor descriptor = Mockito.mock(AIEngineDescriptor.class);
        final MockedStatic<DBWorkbench> workbench = Mockito.mockStatic(
            DBWorkbench.class, Mockito.withSettings().mockMaker("mock-maker-inline"));
        final MockedStatic<AIEngineRegistry> engines = Mockito.mockStatic(
            AIEngineRegistry.class, Mockito.withSettings().mockMaker("mock-maker-inline"));
        final MockedStatic<DBSSecretController> controllers = Mockito.mockStatic(
            DBSSecretController.class, Mockito.withSettings().mockMaker("mock-maker-inline"));

        Fixture() throws DBException {
            Mockito.when(platform.getApplication().isDistributed()).thenReturn(true);
            Mockito.when(platform.getWorkspace().hasRealmPermission(RMConstants.PERMISSION_CONFIGURATION_MANAGER)).thenReturn(true);
            workbench.when(DBWorkbench::getPlatform).thenReturn(platform);
            workbench.when(DBWorkbench::isDistributed).thenReturn(true);
            workbench.when(DBWorkbench::isPlatformStarted).thenReturn(true);
            engines.when(AIEngineRegistry::getInstance).thenReturn(registry);
            controllers.when(DBSSecretController::getGlobalSecretController).thenReturn(controller);
            controllers.when(DBSSecretController::getGlobalSecretControllerOrNull).thenReturn(controller);
            Mockito.when(controller.supportsObjectSecretDeletion()).thenReturn(true);
            Mockito.when(configurationController.loadConfigurationFile(ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> persistedConfiguration);
            allowConfigurationWrites();
            Mockito.when(registry.getEngineDescriptor("openai")).thenReturn(descriptor);
            Mockito.when(descriptor.getId()).thenReturn("openai");
            Mockito.doReturn(OpenAIProperties.class).when(descriptor).getPropertiesType();
            Mockito.when(descriptor.createPropertiesInstance()).thenAnswer(invocation -> new OpenAIProperties());
            Mockito.when(controller.getPrivateSecretValue(ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> secrets.get(invocation.getArgument(0)));
            Mockito.doAnswer(invocation -> {
                DBSSecretValue value = invocation.getArgument(1);
                if (value.getValue() == null) {
                    secrets.remove(value.getId());
                } else {
                    secrets.put(value.getId(), value.getValue());
                }
                return null;
            }).when(controller).setPrivateSecretValue(
                ArgumentMatchers.any(DBSSecretObject.class), ArgumentMatchers.any(DBSSecretValue.class));
            allowCleanup();
        }

        void allowConfigurationWrites() throws DBException {
            Mockito.doAnswer(invocation -> {
                persistedConfiguration = invocation.getArgument(1);
                return null;
            }).when(configurationController).saveConfigurationFile(ArgumentMatchers.anyString(), ArgumentMatchers.anyString());
        }

        void allowCleanup() throws DBException {
            Mockito.doAnswer(invocation -> {
                DBSSecretObject object = invocation.getArgument(0);
                Assertions.assertEquals("", object.getProjectId());
                Assertions.assertEquals("aiProfile", object.getSecretObjectType());
                Assertions.assertEquals("openai", object.getSecretObjectId());
                AISettings saved = reload();
                Assertions.assertNull(saved.getConfigurationOrNull("openai"));
                Assertions.assertTrue(saved.getPendingProfileSecretDeletions().contains("openai"));
                for (Map<String, String> store : List.of(secrets, otherSecrets)) {
                    store.keySet().removeIf(key -> key.startsWith("ai.profile.openai."));
                }
                return null;
            }).when(controller).deleteObjectSecrets(ArgumentMatchers.any(DBSSecretObject.class));
        }

        AISettings settings(boolean global) throws DBException {
            AISettings settings = AISettingsManager.READ_PROPS_GSON.fromJson("""
                {"configurations":{"openai":{"name":"OpenAI","engine":"openai",
                  "configuration":{"gpt.model":"gpt-4"}}}}
                """, AISettings.class);
            settings.finishSettingsLoading();
            settings.getConfiguration("openai").setGlobal(global);
            persistedConfiguration = AISettingsManager.SAVE_PROPS_GSON.toJson(settings);
            return settings;
        }

        AISettings reload() {
            AISettings settings = AISettingsManager.READ_PROPS_GSON.fromJson(persistedConfiguration, AISettings.class);
            settings.finishSettingsLoading();
            return settings;
        }

        @Override
        public void close() {
            controllers.close();
            engines.close();
            workbench.close();
        }
    }
}

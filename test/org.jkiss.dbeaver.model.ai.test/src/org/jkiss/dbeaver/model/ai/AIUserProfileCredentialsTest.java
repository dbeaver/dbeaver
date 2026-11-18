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
import org.jkiss.dbeaver.model.ai.AIUserProfileLifecycleTest.Fixture;
import org.jkiss.dbeaver.model.ai.engine.openai.AIAccountAuthenticator;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIAccountRequestFilter;
import org.jkiss.dbeaver.model.ai.engine.openai.OpenAIProperties;
import org.jkiss.dbeaver.model.ai.registry.AISettingsManager;
import org.jkiss.dbeaver.model.auth.SMSession;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.dbeaver.model.secret.DBSSecretObject;
import org.jkiss.dbeaver.model.secret.DBSSecretValue;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.Map;

class AIUserProfileCredentialsTest extends DBeaverUnitTest {
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
    void rejectsChangesToAdministratorSettingsBeforeWritingSecrets() throws Exception {
        try (Fixture fixture = new Fixture()) {
            AIConfigurationProfile profile = fixture.settings(false).getConfiguration("openai");
            Assertions.assertThrows(DBException.class,
                () -> AIUserProfileCredentials.save(profile, Map.of("token", "secret", "model", "other-model")));
            Mockito.verifyNoInteractions(fixture.controller);
        }
    }

    @Test
    void sharedProfilesDoNotAcceptPersonalCredentials() throws Exception {
        try (Fixture fixture = new Fixture()) {
            AIConfigurationProfile profile = fixture.settings(true).getConfiguration("openai");
            Assertions.assertThrows(DBException.class, () -> AIUserProfileCredentials.load(profile, fixture.controller));
            Assertions.assertThrows(DBException.class, () -> AIUserProfileCredentials.save(profile, Map.of("token", "secret")));
            Mockito.verifyNoInteractions(fixture.controller);
        }
    }

    @Test
    void personalTokensAreExcludedFromSharedProfilesAndLegacyConfigurations() throws Exception {
        try (Fixture fixture = new Fixture()) {
            AISettings settings = AISettingsManager.READ_PROPS_GSON.fromJson("""
                {"configurations":{"work":{"name":"Work","engine":"openai",
                  "configuration":{"global":false,"gpt.token":"personal-token","gpt.model":"admin-model"}},
                  "unavailable":{"name":"Unavailable","engine":"missing-provider",
                  "configuration":{"global":false,"preserved":null,"option":"unchanged"}}}}
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
            settings.prepareSettingsSaving();
            String serialized = AISettingsManager.SAVE_PROPS_GSON.toJson(settings);
            var json = JsonParser.parseString(serialized).getAsJsonObject();
            Assertions.assertTrue(json.getAsJsonObject("engineConfigurations").has("openai"));
            var unavailable = json.getAsJsonObject("configurations").getAsJsonObject("unavailable");
            Assertions.assertEquals("missing-provider", unavailable.get("engine").getAsString());
            Assertions.assertEquals("unchanged", unavailable.getAsJsonObject("configuration").get("option").getAsString());
            Assertions.assertTrue(unavailable.getAsJsonObject("configuration").get("preserved").isJsonNull());
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
        try (Fixture fixture = new Fixture()) {
            AIConfigurationProfile profile = fixture.settings(false).getConfiguration("openai");
            OpenAIProperties properties = (OpenAIProperties) profile.getConfiguration();
            fixture.secrets.put("ai.profile.openai.token", "old-token");
            profile.resolveSecrets();
            Assertions.assertEquals("old-token", properties.getToken());
            properties.setToken("edited-token");

            AIUserProfileCredentials.saveChanges(profile);
            AIUserProfileCredentials.saveChanges(profile);

            ArgumentCaptor<DBSSecretObject> object = ArgumentCaptor.forClass(DBSSecretObject.class);
            ArgumentCaptor<DBSSecretValue> value = ArgumentCaptor.forClass(DBSSecretValue.class);
            Mockito.verify(fixture.controller).setPrivateSecretValue(object.capture(), value.capture());
            Assertions.assertEquals("aiProfile", object.getValue().getSecretObjectType());
            Assertions.assertEquals("openai", object.getValue().getSecretObjectId());
            Assertions.assertEquals("", object.getValue().getProjectId());
            Assertions.assertEquals("ai.profile.openai.token", value.getValue().getId());
            Assertions.assertEquals("edited-token", value.getValue().getValue());
            Assertions.assertEquals(Map.of("token", "edited-token"), profile.getResolvedUserCredentials());
            Mockito.verify(fixture.controller).flushChanges();
        }
    }

    @Test
    void doesNotOverwritePrivateCredentialsBeforeResolvingThem() throws Exception {
        try (Fixture fixture = new Fixture()) {
            AIConfigurationProfile profile = fixture.settings(false).getConfiguration("openai");
            AIUserProfileCredentials.saveChanges(profile);
            Mockito.verifyNoInteractions(fixture.controller);
        }
    }

    @Test
    void clearingAnEditedCredentialDeletesThePrivateValue() throws Exception {
        try (Fixture fixture = new Fixture()) {
            AIConfigurationProfile profile = fixture.settings(false).getConfiguration("openai");
            OpenAIProperties properties = (OpenAIProperties) profile.getConfiguration();
            fixture.secrets.put("ai.profile.openai.token", "old-token");
            profile.resolveSecrets();
            properties.setToken("");
            AIUserProfileCredentials.saveChanges(profile);
            ArgumentCaptor<DBSSecretValue> value = ArgumentCaptor.forClass(DBSSecretValue.class);
            Mockito.verify(fixture.controller).setPrivateSecretValue(ArgumentMatchers.any(DBSSecretObject.class), value.capture());
            Assertions.assertNull(value.getValue().getValue());
        }
    }

    @Test
    void failedSaveKeepsCredentialChangesPending() throws Exception {
        try (Fixture fixture = new Fixture()) {
            AIConfigurationProfile profile = fixture.settings(false).getConfiguration("openai");
            OpenAIProperties properties = (OpenAIProperties) profile.getConfiguration();
            Mockito.doThrow(new DBException("Storage unavailable")).when(fixture.controller).setPrivateSecretValue(
                ArgumentMatchers.any(DBSSecretObject.class), ArgumentMatchers.any(DBSSecretValue.class));
            fixture.secrets.put("ai.profile.openai.token", "old-token");
            profile.resolveSecrets();
            properties.setToken("edited-token");
            Assertions.assertThrows(DBException.class, () -> AIUserProfileCredentials.saveChanges(profile));
            Assertions.assertEquals(Map.of("token", "old-token"), profile.getResolvedUserCredentials());
        }
    }

    @Test
    void administratorSavePersistsEditedUserTokenPrivately() throws Exception {
        try (Fixture fixture = new Fixture()) {
            AISettings settings = fixture.settings(false);
            var profile = settings.getConfiguration("openai");
            fixture.secrets.put("ai.profile.openai.token", "previous-token");
            profile.resolveSecrets();
            ((OpenAIProperties) profile.getConfiguration()).setToken("edited-token");

            AISettingsManager.getInstance().saveSettingsOrThrow(settings);

            Assertions.assertEquals("edited-token", fixture.secrets.get("ai.profile.openai.token"));
            Assertions.assertFalse(fixture.persistedConfiguration.contains("edited-token"));
        }
    }

    @Test
    void resolvesAccountCredentialsFromTheWebProfileStore() throws Exception {
        try (Fixture fixture = new Fixture()) {
            AIConfigurationProfile profile = fixture.settings(false).getConfiguration("openai");
            OpenAIProperties properties = (OpenAIProperties) profile.getConfiguration();
            properties.setAccessToken("stale-other-account");
            properties.setModel("administrator-model");
            properties.setBaseUrl("https://administrator.example/v1");
            fixture.secrets.putAll(Map.of(
                "ai.profile.openai.accountAuthentication", "true",
                "ai.profile.openai.accessToken", "web-access",
                "ai.profile.openai.refreshToken", "web-refresh",
                "ai.profile.openai.accountEmail", "user@example.com",
                "ai.profile.openai.accountId", "web-account",
                "ai.profile.openai.expiresAt", Long.toString(System.currentTimeMillis() + 600_000)
            ));
            profile.resolveSecrets();
            Assertions.assertTrue(properties.isAccountAuthentication());
            Assertions.assertEquals("web-access", properties.getAccessToken());
            Assertions.assertEquals("web-refresh", properties.getRefreshToken());
            Assertions.assertEquals("user@example.com", properties.getAccountEmail());
            Assertions.assertEquals("web-account", properties.getAccountId());
            Assertions.assertEquals("administrator-model", properties.getModel());
            Assertions.assertEquals("https://administrator.example/v1", properties.getBaseUrl());
            fixture.secrets.remove("ai.profile.openai.refreshToken");
            Assertions.assertThrows(DBException.class,
                () -> properties.getValidAccessToken(Mockito.mock(AIAccountAuthenticator.class)));
        }
    }

    @Test
    void savesAndClearsAccountTokensOnlyInThePrivateProfileStore() throws Exception {
        try (Fixture fixture = new Fixture()) {
            AIConfigurationProfile profile = fixture.settings(false).getConfiguration("openai");
            OpenAIProperties properties = (OpenAIProperties) profile.getConfiguration();
            profile.resolveSecrets();
            properties.setAccountAuthentication(true);
            properties.setAccountTokens(new AIAccountAuthenticator.Tokens(
                "desktop-access", "desktop-refresh", 600, "personal-account", "user@example.com"));
            properties.saveAccountTokens();
            Assertions.assertEquals("desktop-access", fixture.secrets.get("ai.profile.openai.accessToken"));
            Assertions.assertEquals("desktop-refresh", fixture.secrets.get("ai.profile.openai.refreshToken"));
            Assertions.assertEquals("true", fixture.secrets.get("ai.profile.openai.accountAuthentication"));
            Assertions.assertTrue(fixture.secrets.keySet().stream().allMatch(key -> key.startsWith("ai.profile.openai.")));
            properties.clearAccountTokens();
            properties.saveAccountTokens();
            Assertions.assertNull(fixture.secrets.get("ai.profile.openai.accessToken"));
            Assertions.assertNull(fixture.secrets.get("ai.profile.openai.refreshToken"));
            Assertions.assertNull(fixture.secrets.get("ai.profile.openai.accountId"));
            Assertions.assertNull(fixture.secrets.get("ai.profile.openai.accountEmail"));
            Assertions.assertEquals("0", fixture.secrets.get("ai.profile.openai.expiresAt"));
            profile.resolveSecrets();
            Assertions.assertFalse(properties.isAccountConnected());
        }
    }

    @Test
    void switchingToApiTokenKeepsAccountTokensAndDoesNotChangeSharedSettings() throws Exception {
        try (Fixture fixture = new Fixture()) {
            AIConfigurationProfile profile = fixture.settings(false).getConfiguration("openai");
            OpenAIProperties properties = (OpenAIProperties) profile.getConfiguration();
            fixture.secrets.putAll(Map.of(
                "ai.profile.openai.accountAuthentication", "true",
                "ai.profile.openai.accessToken", "account-access",
                "ai.profile.openai.refreshToken", "account-refresh"
            ));
            profile.resolveSecrets();
            properties.setAccountAuthentication(false);
            properties.setToken("api-token");
            AIUserProfileCredentials.saveChanges(profile);
            Assertions.assertEquals("false", fixture.secrets.get("ai.profile.openai.accountAuthentication"));
            Assertions.assertEquals("api-token", fixture.secrets.get("ai.profile.openai.token"));
            Assertions.assertEquals("account-refresh", fixture.secrets.get("ai.profile.openai.refreshToken"));
            profile.resolveSecrets();
            Assertions.assertFalse(properties.isAccountAuthentication());
            Assertions.assertEquals("api-token", properties.getToken());
        }
    }

    @Test
    void accountCredentialsNeverFallBackToAnotherProfile() throws Exception {
        try (Fixture fixture = new Fixture()) {
            AIConfigurationProfile profile = fixture.settings(false).getConfiguration("openai");
            OpenAIProperties properties = (OpenAIProperties) profile.getConfiguration();
            fixture.secrets.putAll(Map.of(
                "ai.profile.other.accessToken", "another-user-access",
                "ai.profile.other.refreshToken", "another-user-refresh"
            ));
            profile.resolveSecrets();
            Assertions.assertNull(properties.getAccessToken());
            Assertions.assertNull(properties.getRefreshToken());
            Assertions.assertFalse(properties.isAccountConnected());
        }
    }

    @Test
    void completedAccountAuthorizationCannotWriteAfterTheTeamSessionChanges() throws Exception {
        try (Fixture fixture = new Fixture()) {
            AIConfigurationProfile profile = fixture.settings(false).getConfiguration("openai");
            OpenAIProperties properties = (OpenAIProperties) profile.getConfiguration();
            profile.resolveSecrets();
            properties.setAccountTokens(new AIAccountAuthenticator.Tokens("access", "refresh", 600, "account", "user@example.com"));
            SMSession nextSession = Mockito.mock(SMSession.class);
            Mockito.when(fixture.platform.getWorkspace().getWorkspaceSession()).thenReturn(nextSession);
            Assertions.assertThrows(DBException.class, properties::saveAccountTokens);
            Mockito.verify(fixture.controller, Mockito.never()).setPrivateSecretValue(
                ArgumentMatchers.any(DBSSecretObject.class), ArgumentMatchers.any(DBSSecretValue.class));
        }
    }

    @Test
    void accountCredentialWritesStopIfTheTeamSessionChangesMidSave() throws Exception {
        try (Fixture fixture = new Fixture()) {
            AIConfigurationProfile profile = fixture.settings(false).getConfiguration("openai");
            OpenAIProperties properties = (OpenAIProperties) profile.getConfiguration();
            SMSession nextSession = Mockito.mock(SMSession.class);
            Mockito.doAnswer(invocation -> {
                Mockito.when(fixture.platform.getWorkspace().getWorkspaceSession()).thenReturn(nextSession);
                return null;
            }).when(fixture.controller).setPrivateSecretValue(
                ArgumentMatchers.any(DBSSecretObject.class), ArgumentMatchers.any(DBSSecretValue.class));
            profile.resolveSecrets();
            properties.setAccountTokens(new AIAccountAuthenticator.Tokens("access", "refresh", 600, "account", "user@example.com"));
            Assertions.assertThrows(DBException.class, properties::saveAccountTokens);
            Mockito.verify(fixture.controller).setPrivateSecretValue(
                ArgumentMatchers.any(DBSSecretObject.class), ArgumentMatchers.any(DBSSecretValue.class));
            Mockito.verify(fixture.controller, Mockito.never()).flushChanges();
        }
    }

    @Test
    void tokenRefreshDoesNotOverwriteCredentialsRotatedByAnotherClient() throws Exception {
        try (Fixture fixture = new Fixture()) {
            AIConfigurationProfile profile = fixture.settings(false).getConfiguration("openai");
            OpenAIProperties properties = (OpenAIProperties) profile.getConfiguration();
            fixture.secrets.putAll(Map.of(
                "ai.profile.openai.accountAuthentication", "true",
                "ai.profile.openai.accessToken", "expired-access",
                "ai.profile.openai.refreshToken", "old-refresh"
            ));
            AIAccountAuthenticator authenticator = Mockito.mock(AIAccountAuthenticator.class);
            Mockito.when(authenticator.refresh("old-refresh")).thenAnswer(invocation -> {
                fixture.secrets.put("ai.profile.openai.refreshToken", "web-rotated-refresh");
                return new AIAccountAuthenticator.Tokens("desktop-access", "desktop-refresh", 600, "account", "user@example.com");
            });
            profile.resolveSecrets();
            Assertions.assertThrows(DBException.class, () -> properties.getValidAccessToken(authenticator));
            Assertions.assertEquals("web-rotated-refresh", fixture.secrets.get("ai.profile.openai.refreshToken"));
            Mockito.verify(fixture.controller, Mockito.never()).setPrivateSecretValue(
                ArgumentMatchers.any(DBSSecretObject.class), ArgumentMatchers.any(DBSSecretValue.class));
        }
    }

    @Test
    void tokenRefreshPreservesAuthenticationMethodChangedByAnotherClient() throws Exception {
        for (boolean rotateRefreshToken : new boolean[]{false, true}) {
            try (Fixture fixture = new Fixture()) {
                AIConfigurationProfile profile = fixture.settings(false).getConfiguration("openai");
                OpenAIProperties properties = (OpenAIProperties) profile.getConfiguration();
                fixture.secrets.putAll(Map.of(
                    "ai.profile.openai.accountAuthentication", "true",
                    "ai.profile.openai.refreshToken", "old-refresh"
                ));
                AIAccountAuthenticator authenticator = Mockito.mock(AIAccountAuthenticator.class);
                Mockito.when(authenticator.refresh("old-refresh")).thenAnswer(invocation -> {
                    fixture.secrets.put("ai.profile.openai.accountAuthentication", "false");
                    return new AIAccountAuthenticator.Tokens("new-access", rotateRefreshToken ? "new-refresh" : "old-refresh",
                        600, "account", "user@example.com");
                });
                profile.resolveSecrets();

                Assertions.assertEquals("new-access", properties.getValidAccessToken(authenticator));

                Assertions.assertEquals("false", fixture.secrets.get("ai.profile.openai.accountAuthentication"));
                Assertions.assertEquals("new-access", fixture.secrets.get("ai.profile.openai.accessToken"));
                Assertions.assertTrue(Long.parseLong(fixture.secrets.get("ai.profile.openai.expiresAt")) > System.currentTimeMillis());
                profile.resolveSecrets();
                Assertions.assertFalse(properties.isAccountAuthentication());
            }
        }
    }

    @Test
    void tokenRefreshCannotWriteAfterSessionChanges() throws Exception {
        try (Fixture fixture = new Fixture()) {
            AIConfigurationProfile profile = fixture.settings(false).getConfiguration("openai");
            OpenAIProperties properties = (OpenAIProperties) profile.getConfiguration();
            fixture.secrets.put("ai.profile.openai.refreshToken", "old-refresh");
            AIAccountAuthenticator authenticator = Mockito.mock(AIAccountAuthenticator.class);
            SMSession nextSession = Mockito.mock(SMSession.class);
            Mockito.when(authenticator.refresh("old-refresh")).thenAnswer(invocation -> {
                Mockito.when(fixture.platform.getWorkspace().getWorkspaceSession()).thenReturn(nextSession);
                return new AIAccountAuthenticator.Tokens("new-access", "new-refresh", 600, null, null);
            });
            profile.resolveSecrets();

            Assertions.assertThrows(DBException.class, () -> properties.getValidAccessToken(authenticator));

            Mockito.verify(fixture.controller, Mockito.never()).setPrivateSecretValue(
                ArgumentMatchers.any(DBSSecretObject.class), ArgumentMatchers.any(DBSSecretValue.class));
        }
    }

    @Test
    void refreshPreservesMissingIdentityClaimsAfterReload() throws Exception {
        for (var tokens : new AIAccountAuthenticator.Tokens[]{
            new AIAccountAuthenticator.Tokens("new-access", "new-refresh", 600, null, null),
            new AIAccountAuthenticator.Tokens("new-access", "new-refresh", 600, "new-account", null),
            new AIAccountAuthenticator.Tokens("new-access", "new-refresh", 600, null, "new@example.com"),
            new AIAccountAuthenticator.Tokens("new-access", "new-refresh", 600, "new-account", "new@example.com")
        }) {
            try (Fixture fixture = new Fixture()) {
                AIConfigurationProfile profile = fixture.settings(false).getConfiguration("openai");
                OpenAIProperties properties = (OpenAIProperties) profile.getConfiguration();
                fixture.secrets.putAll(Map.of(
                    "ai.profile.openai.accountAuthentication", "true",
                    "ai.profile.openai.refreshToken", "old-refresh",
                    "ai.profile.openai.accountId", "old-account",
                    "ai.profile.openai.accountEmail", "old@example.com"
                ));
                AIAccountAuthenticator authenticator = Mockito.mock(AIAccountAuthenticator.class);
                Mockito.when(authenticator.refresh("old-refresh")).thenReturn(tokens);
                profile.resolveSecrets();

                Assertions.assertEquals("new-access", properties.getValidAccessToken(authenticator));
                profile.resolveSecrets();

                String expectedId = tokens.accountId() == null ? "old-account" : tokens.accountId();
                String expectedEmail = tokens.email() == null ? "old@example.com" : tokens.email();
                Assertions.assertEquals(expectedId, properties.getAccountId());
                Assertions.assertEquals(expectedEmail, properties.getAccountEmail());
                HttpRequest request = new OpenAIAccountRequestFilter(properties).filter(
                    HttpRequest.newBuilder(URI.create("http://localhost/no-request")).GET().build(), false);
                Assertions.assertEquals(expectedId, request.headers().firstValue("ChatGPT-Account-Id").orElseThrow());
            }
        }
    }

    @Test
    void personalProfilesAndAccountAuthorizationRespectProductMode() throws Exception {
        try (Fixture fixture = new Fixture()) {
            AIConfigurationProfile profile = fixture.settings(false).getConfiguration("openai");
            Assertions.assertTrue(AIUserProfileCredentials.isUserProfile(profile));
            Assertions.assertTrue(AIAccountAuthenticator.isSupported());

            Mockito.when(fixture.platform.getApplication().isHeadlessMode()).thenReturn(true);
            Assertions.assertFalse(AIUserProfileCredentials.isUserProfile(profile));
            Assertions.assertFalse(AIAccountAuthenticator.isSupported());

            Mockito.when(fixture.platform.getApplication().isHeadlessMode()).thenReturn(false);
            Mockito.when(fixture.platform.getApplication().isMultiuser()).thenReturn(true);
            Assertions.assertFalse(AIAccountAuthenticator.isSupported());

            fixture.workbench.when(DBWorkbench::isDistributed).thenReturn(false);
            Assertions.assertFalse(AIUserProfileCredentials.isUserProfile(profile));
        }
    }

}

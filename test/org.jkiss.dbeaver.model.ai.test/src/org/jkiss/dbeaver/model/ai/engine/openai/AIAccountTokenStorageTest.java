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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.ai.AIConfigurationProfile;
import org.jkiss.dbeaver.model.ai.utils.AIUtils;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.impl.app.SecurePreferencesSecretController;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.dbeaver.model.security.DBSecurityProviders;
import org.jkiss.dbeaver.model.security.DBSecurityUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;

class AIAccountTokenStorageTest extends DBeaverUnitTest {
    @Test
    void subscriptionTokensUseNativeProviderAndKeepLegacyReadable() throws Exception {
        AIConfigurationProfile profile = new AIConfigurationProfile();
        profile.setProfileId("work");
        final Map<String, String> nativeSecrets = new HashMap<>();
        Map<String, String> legacySecrets = new HashMap<>();
        legacySecrets.put("openai.account.refreshToken_work", "old-refresh");

        DBPPlatform platform = Mockito.mock(DBPPlatform.class, Mockito.RETURNS_DEEP_STUBS);
        DBSSecretController legacyController = Mockito.mock(DBSSecretController.class);
        SecurePreferencesSecretController nativeController = Mockito.mock(SecurePreferencesSecretController.class);
        Mockito.when(legacyController.getPrivateSecretValue(ArgumentMatchers.anyString()))
            .thenAnswer(invocation -> legacySecrets.get(invocation.getArgument(0)));
        Mockito.doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            String value = invocation.getArgument(1);
            if (value == null) {
                legacySecrets.remove(key);
            } else {
                legacySecrets.put(key, value);
            }
            return null;
        }).when(legacyController).setPrivateSecretValue(
            ArgumentMatchers.anyString(), ArgumentMatchers.nullable(String.class));
        Mockito.when(nativeController.getPrivateSecretValue(ArgumentMatchers.anyString()))
            .thenAnswer(invocation -> nativeSecrets.get(invocation.getArgument(0)));
        Mockito.doAnswer(invocation -> {
            String key = invocation.getArgument(0);
            String value = invocation.getArgument(1);
            if (value == null) {
                nativeSecrets.remove(key);
            } else {
                nativeSecrets.put(key, value);
            }
            return null;
        }).when(nativeController).setPrivateSecretValue(
            ArgumentMatchers.anyString(), ArgumentMatchers.nullable(String.class));
        Mockito.when(nativeController.isPrivateSecretEncrypted(ArgumentMatchers.anyString())).thenReturn(true);

        try (MockedStatic<DBWorkbench> workbench = Mockito.mockStatic(
                 DBWorkbench.class, Mockito.withSettings().mockMaker("mock-maker-inline"));
             MockedStatic<DBSSecretController> controllers = Mockito.mockStatic(
                 DBSSecretController.class, Mockito.withSettings().mockMaker("mock-maker-inline"));
             MockedStatic<DBSecurityUtils> providers = Mockito.mockStatic(
                 DBSecurityUtils.class, Mockito.withSettings().mockMaker("mock-maker-inline"));
             MockedStatic<SecurePreferencesSecretController> securePreferences = Mockito.mockStatic(
                 SecurePreferencesSecretController.class, Mockito.withSettings().mockMaker("mock-maker-inline"))) {
            workbench.when(DBWorkbench::getPlatform).thenReturn(platform);
            workbench.when(DBWorkbench::isPlatformStarted).thenReturn(true);
            controllers.when(DBSSecretController::getGlobalSecretControllerOrNull).thenReturn(legacyController);
            providers.when(DBSecurityUtils::findNativeOSProvider).thenReturn(DBSecurityProviders.MACOS_KEYCHAIN);
            securePreferences.when(() -> SecurePreferencesSecretController.create(
                SecurePreferencesSecretController.SECRET_PREFS_ROOT, DBSecurityProviders.MACOS_KEYCHAIN))
                .thenReturn(nativeController);

            OpenAIProperties openAI = new OpenAIProperties();
            openAI.resolveSecrets(profile);
            Assertions.assertEquals("old-refresh", openAI.getRefreshToken());
            Assertions.assertFalse(nativeSecrets.containsKey("integrated.openai.account.refreshToken_work"));

            legacySecrets.put("integrated.openai.account.refreshToken_work", "new-global-refresh");
            OpenAIProperties fromGlobal = new OpenAIProperties();
            fromGlobal.resolveSecrets(profile);
            Assertions.assertEquals("new-global-refresh", fromGlobal.getRefreshToken());

            openAI.setAccessToken("new-access");
            openAI.setRefreshToken("new-refresh");
            openAI.saveSecrets(profile);
            Assertions.assertEquals("new-access", nativeSecrets.get("integrated.openai.account.accessToken_work"));
            Assertions.assertEquals("new-refresh", nativeSecrets.get("integrated.openai.account.refreshToken_work"));
            Assertions.assertEquals("old-refresh", legacySecrets.get("openai.account.refreshToken_work"));
            OpenAIProperties fromNative = new OpenAIProperties();
            fromNative.resolveSecrets(profile);
            Assertions.assertEquals("new-refresh", fromNative.getRefreshToken());
            nativeSecrets.put("integrated.openai.account.refreshToken_work", "");
            Assertions.assertEquals("", AIUtils.getAccountTokenOrDefault(profile, "openai.account.refreshToken", null));
            nativeSecrets.put("integrated.openai.account.refreshToken_work", "new-refresh");

            OpenAIProperties grok = new OpenAIProperties() {
                @NotNull
                @Override
                protected String getAccessTokenSecretId() {
                    return "grok.account.accessToken";
                }

                @NotNull
                @Override
                protected String getRefreshTokenSecretId() {
                    return "grok.account.refreshToken";
                }
            };
            legacySecrets.put("grok.account.refreshToken_work", "old-grok-refresh");
            grok.resolveSecrets(profile);
            Assertions.assertEquals("old-grok-refresh", grok.getRefreshToken());
            grok.setAccessToken("grok-access");
            grok.setRefreshToken("grok-refresh");
            grok.saveSecrets(profile);
            Assertions.assertEquals("grok-access", nativeSecrets.get("integrated.grok.account.accessToken_work"));
            Assertions.assertEquals("grok-refresh", nativeSecrets.get("integrated.grok.account.refreshToken_work"));
            Assertions.assertEquals("old-grok-refresh", legacySecrets.get("grok.account.refreshToken_work"));

            openAI.deleteSecrets(profile);
            Assertions.assertFalse(nativeSecrets.containsKey("integrated.openai.account.accessToken_work"));
            Assertions.assertFalse(nativeSecrets.containsKey("integrated.openai.account.refreshToken_work"));
            Assertions.assertFalse(legacySecrets.containsKey("openai.account.refreshToken_work"));
            Assertions.assertFalse(legacySecrets.containsKey("integrated.openai.account.refreshToken_work"));

            providers.when(DBSecurityUtils::findNativeOSProvider).thenReturn(null);
            AIUtils.setAccountToken(profile, "openai.account.refreshToken", "global-refresh");
            Assertions.assertEquals("global-refresh", legacySecrets.get("integrated.openai.account.refreshToken_work"));
            Assertions.assertFalse(legacySecrets.containsKey("openai.account.refreshToken_work"));
            Assertions.assertEquals("global-refresh", AIUtils.getAccountTokenOrDefault(profile, "openai.account.refreshToken", null));
        }
    }
}

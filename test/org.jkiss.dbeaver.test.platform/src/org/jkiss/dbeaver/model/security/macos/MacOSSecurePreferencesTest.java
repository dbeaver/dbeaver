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
package org.jkiss.dbeaver.model.security.macos;

import org.eclipse.equinox.internal.security.storage.PasswordProviderSelector;
import org.eclipse.equinox.internal.security.storage.friends.InternalExchangeUtils;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.provider.IProviderHints;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.security.DBSecurityProviders;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

class MacOSSecurePreferencesTest {
    @Test
    @EnabledOnOs(OS.MAC)
    void providerIsRegistered() throws Exception {
        Assertions.assertEquals(DBSecurityProviders.MACOS_KEYCHAIN.toLowerCase(Locale.ROOT),
            PasswordProviderSelector.getInstance().findStorageModule(DBSecurityProviders.MACOS_KEYCHAIN).getID());
        var provider = InternalExchangeUtils.passwordProvidersFind().stream()
            .filter(item -> item.getId().equals(DBSecurityProviders.MACOS_KEYCHAIN.toLowerCase(Locale.ROOT)))
            .findFirst().orElseThrow();
        Assertions.assertEquals("macOS Keychain", provider.getName());
        Assertions.assertTrue(provider.getDescription().contains("macOS Keychain"));
    }

    // Real Keychain access may require interactive authorization; never run it in headless CI by default.
    @Test
    @EnabledOnOs(OS.MAC)
    @EnabledIfEnvironmentVariable(named = "DBEAVER_TEST_MACOS_KEYCHAIN", matches = "true")
    void newStorageUsesKeychain(@NotNull @TempDir Path directory) throws Exception {
        ISecurePreferences preferences = SecurePreferencesFactory.open(
            directory.resolve("secure_storage").toUri().toURL(),
            Map.of(IProviderHints.REQUIRED_MODULE_ID, DBSecurityProviders.MACOS_KEYCHAIN)
        );
        ISecurePreferences node = preferences.node("dbeaver-test");
        node.put("test", "secret", true);
        node.flush();

        Assertions.assertTrue(node.isEncrypted("test"));
        Assertions.assertEquals("secret", node.get("test", null));

        ISecurePreferences anotherFile = SecurePreferencesFactory.open(
            directory.resolve("other_secure_storage").toUri().toURL(),
            Map.of(IProviderHints.REQUIRED_MODULE_ID, DBSecurityProviders.MACOS_KEYCHAIN)
        );
        ISecurePreferences anotherNode = anotherFile.node("dbeaver-test");
        anotherNode.put("test", "another secret", true);
        anotherNode.flush();
        Assertions.assertTrue(anotherNode.isEncrypted("test"));
        Assertions.assertEquals("another secret", anotherNode.get("test", null));
    }
}

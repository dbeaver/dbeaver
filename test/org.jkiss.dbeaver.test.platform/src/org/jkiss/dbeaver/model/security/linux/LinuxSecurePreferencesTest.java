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
package org.jkiss.dbeaver.model.security.linux;

import org.eclipse.equinox.internal.security.storage.PasswordProviderSelector;
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
import java.util.Map;

class LinuxSecurePreferencesTest {
    // Requires a real, unlocked (or unlockable) session keyring. Never run in headless CI by default.
    @Test
    @EnabledOnOs(OS.LINUX)
    @EnabledIfEnvironmentVariable(named = "DBEAVER_TEST_LINUX_KEYRING", matches = "true")
    void differentStorageFilesUseSameKeyringPassword(@NotNull @TempDir Path directory) throws Exception {
        Assertions.assertNotNull(PasswordProviderSelector.getInstance()
            .findStorageModule(DBSecurityProviders.LINUX_KEYRING));
        ISecurePreferences first = SecurePreferencesFactory.open(
            directory.resolve("first_secure_storage").toUri().toURL(),
            Map.of(IProviderHints.REQUIRED_MODULE_ID, DBSecurityProviders.LINUX_KEYRING)
        );
        first.node("dbeaver-test").put("test", "first secret", true);
        first.flush();
        ISecurePreferences second = SecurePreferencesFactory.open(
            directory.resolve("second_secure_storage").toUri().toURL(),
            Map.of(IProviderHints.REQUIRED_MODULE_ID, DBSecurityProviders.LINUX_KEYRING)
        );
        second.node("dbeaver-test").put("test", "second secret", true);
        second.flush();

        Assertions.assertTrue(first.node("dbeaver-test").isEncrypted("test"));
        Assertions.assertTrue(second.node("dbeaver-test").isEncrypted("test"));
        Assertions.assertEquals("first secret", first.node("dbeaver-test").get("test", null));
        Assertions.assertEquals("second secret", second.node("dbeaver-test").get("test", null));
    }
}

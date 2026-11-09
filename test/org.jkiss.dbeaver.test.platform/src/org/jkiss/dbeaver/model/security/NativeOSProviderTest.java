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
package org.jkiss.dbeaver.model.security;

import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.internal.security.storage.PasswordProviderSelector;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.util.Locale;

class NativeOSProviderTest {
    private static final String WINDOWS_FRAGMENT = "org.eclipse.equinox.security.win32";

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void windowsProviderIsAvailable() throws Exception {
        Assumptions.assumeTrue(Platform.getBundle(WINDOWS_FRAGMENT) != null);
        String providerId = DBSecurityProviders.WINDOWS_INTEGRATION.toLowerCase(Locale.ROOT);
        Assertions.assertEquals(providerId, DBSecurityUtils.findNativeOSProvider());
        Assertions.assertEquals(providerId, PasswordProviderSelector.getInstance()
            .findStorageModule(DBSecurityProviders.WINDOWS_INTEGRATION).getID());
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void linuxKeyringProviderIsRegistered() {
        Assertions.assertNotNull(Platform.getExtensionRegistry().getExtension(DBSecurityProviders.LINUX_KEYRING));
        String providerId = DBSecurityUtils.findNativeOSProvider();
        if (providerId != null) {
            Assertions.assertEquals(DBSecurityProviders.LINUX_KEYRING.toLowerCase(Locale.ROOT),
                providerId);
        }
    }
}

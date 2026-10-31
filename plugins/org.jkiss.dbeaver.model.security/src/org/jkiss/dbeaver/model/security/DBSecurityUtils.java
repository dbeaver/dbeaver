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
import org.eclipse.equinox.security.storage.StorageException;
import org.jkiss.code.Nullable;

public final class DBSecurityUtils {
    private DBSecurityUtils() {
    }

    /** Returns the available OS-backed Equinox provider, without changing the default provider. */
    @Nullable
    public static String findNativeOSProvider() {
        if (Platform.getExtensionRegistry() == null) {
            return null;
        }
        String providerId = switch (Platform.getOS()) {
            case Platform.OS_MACOSX -> DBSecurityProviders.MACOS_KEYCHAIN;
            case Platform.OS_WIN32 -> DBSecurityProviders.WINDOWS_INTEGRATION;
            case Platform.OS_LINUX -> DBSecurityProviders.LINUX_INTEGRATION;
            default -> null;
        };
        if (providerId == null) {
            return null;
        }
        try {
            // The extension can be installed but unusable (notably a missing Linux keyring).
            return PasswordProviderSelector.getInstance().findStorageModule(providerId).getID();
        } catch (StorageException e) {
            return null;
        }
    }
}

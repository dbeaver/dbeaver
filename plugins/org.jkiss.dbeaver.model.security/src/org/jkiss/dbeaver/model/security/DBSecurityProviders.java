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

/** Equinox secure-storage provider identifiers used by DBeaver. */
public final class DBSecurityProviders {
    public static final String MACOS_KEYCHAIN = "org.jkiss.dbeaver.model.security.MacOSSecureStorageKeychain";
    public static final String LEGACY_MACOS_PROVIDER_EXTENSION_ID = ".osxkeystoreintegration";
    public static final String WINDOWS_INTEGRATION = "org.eclipse.equinox.security.win32.WindowsPasswordProvider64bit";
    public static final String LINUX_INTEGRATION = "org.eclipse.equinox.security.linux.LinuxKeystoreIntegrationJNA";

    private DBSecurityProviders() {
    }
}

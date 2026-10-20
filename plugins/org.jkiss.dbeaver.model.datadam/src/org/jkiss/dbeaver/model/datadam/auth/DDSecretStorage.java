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
package org.jkiss.dbeaver.model.datadam.auth;

import org.eclipse.core.runtime.ContributorFactoryOSGi;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.equinox.security.storage.provider.IProviderHints;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;

import java.io.IOException;
import java.util.Map;

/**
 * Stores DataDam credentials independently of the application's default secret controller.
 */
public class DDSecretStorage {
    private static final String NODE = "datadam";
    private static final String SECURE_STORAGE_EXTENSION = "org.eclipse.equinox.security.secureStorage";
    private static final DDSecretStorage INSTANCE = new DDSecretStorage();
    private final Map<String, String> options;

    private DDSecretStorage() {
        String nativeProvider = findNativeProvider();
        options = nativeProvider == null
            ? Map.of() : Map.of(IProviderHints.REQUIRED_MODULE_ID, nativeProvider);
    }

    @NotNull
    public static DDSecretStorage getInstance() {
        return INSTANCE;
    }

    @Nullable
    public String read(@NotNull String id) throws DBException {
        try {
            return open().get(id, null);
        } catch (IOException | StorageException e) {
            throw new DBException("Error reading DataDam secret: " + id, e);
        }
    }

    public void write(@NotNull String id, @NotNull String value) throws DBException {
        try {
            ISecurePreferences node = open();
            node.put(id, value, true);
            node.flush();
        } catch (IOException | StorageException e) {
            throw new DBException("Error saving DataDam secret: " + id, e);
        }
    }

    public void delete(@NotNull String id) throws DBException {
        try {
            ISecurePreferences node = open();
            node.remove(id);
            node.flush();
        } catch (IOException e) {
            throw new DBException("Error deleting DataDam secret: " + id, e);
        }
    }

    @NotNull
    private ISecurePreferences open() throws IOException {
        return SecurePreferencesFactory.open(null, options).node(NODE);
    }

    @Nullable
    private static String findNativeProvider() {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        if (registry == null) {
            return null;
        }
        String bundleId = "org.eclipse.equinox.security." + Platform.getOS();
        for (IConfigurationElement provider : registry.getConfigurationElementsFor(SECURE_STORAGE_EXTENSION)) {
            var bundle = ContributorFactoryOSGi.resolve(provider.getContributor());
            if (bundle != null && bundleId.equals(bundle.getSymbolicName())) {
                return provider.getDeclaringExtension().getUniqueIdentifier();
            }
        }
        return null;
    }
}

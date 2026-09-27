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
package org.jkiss.dbeaver.model.secret;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.auth.SMCredentialsProvider;
import org.jkiss.dbeaver.model.auth.SMSessionContext;

import java.util.LinkedHashMap;
import java.util.Map;

public class SecretControllerRegistry {
    public static final String EXTENSION_ID = "com.dbeaver.secretController"; //$NON-NLS-1$

    @Nullable
    private static SecretControllerRegistry instance;

    @NotNull
    private final Map<String, SecretControllerDescriptor> secretControllers = new LinkedHashMap<>();

    @NotNull
    public static synchronized SecretControllerRegistry getInstance() {
        if (instance == null) {
            instance = new SecretControllerRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private SecretControllerRegistry(@NotNull IExtensionRegistry registry) {
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            if ("controller".equals(ext.getName())) {
                SecretControllerDescriptor descriptor = new SecretControllerDescriptor(ext);
                secretControllers.put(descriptor.getId(), descriptor);
            }
        }
    }

    @NotNull
    public DBSSecretController getAuthorizedSecretController(
        @NotNull String id,
        @Nullable SMCredentialsProvider credentialsProvider,
        @Nullable SMSessionContext smSessionContext
    ) throws DBException {
        SecretControllerDescriptor descriptor = secretControllers.get(id);
        if (descriptor == null) {
            throw new DBException("Secret controller '" + id + "' not found");
        }
        DBSSecretControllerAuthorized controller = descriptor.createInstance(DBSSecretControllerAuthorized.class);
        controller.authorize(credentialsProvider, smSessionContext);
        return controller;
    }
}

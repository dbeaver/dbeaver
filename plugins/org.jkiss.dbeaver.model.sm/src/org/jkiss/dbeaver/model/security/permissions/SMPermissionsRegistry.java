/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.security.permissions;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class SMPermissionsRegistry {
    private static final String EXTENSION_ID = "org.jkiss.dbeaver.sm.permission";
    private static final String PERMISSION_TAG = "permission";

    private static SMPermissionsRegistry instance = null;

    private final Map<String, SMPermissionDescriptor> permissions = new LinkedHashMap<>();

    private SMPermissionsRegistry() {
    }

    public synchronized static SMPermissionsRegistry getInstance() {
        if (instance == null) {
            instance = new SMPermissionsRegistry();
            instance.loadExtensions(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private void loadExtensions(IExtensionRegistry registry) {
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(EXTENSION_ID);
        for (IConfigurationElement element : extElements) {
            if (PERMISSION_TAG.equals(element.getName())) {
                var descriptor = new SMPermissionDescriptor(element);
                permissions.put(descriptor.getId(), descriptor);
            }
        }
    }

    @Nullable
    public SMPermissionDescriptor getPermissionDescriptor(@NotNull String id) {
        return permissions.get(id);
    }

    public Collection<SMPermissionDescriptor> getPermissions() {
        return permissions.values();
    }

    public Map<String, SMPermissionDescriptor> getPermissionsMap() {
        return Map.copyOf(permissions);
    }
}

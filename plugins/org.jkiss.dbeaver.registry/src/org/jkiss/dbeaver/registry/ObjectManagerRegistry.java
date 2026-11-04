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

package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.edit.DBEObjectManager;
import org.jkiss.dbeaver.model.edit.DBERegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ObjectManagerRegistry
 */
public class ObjectManagerRegistry implements DBERegistry {

    private static final String TAG_MANAGER = "manager"; //NON-NLS-1

    private static ObjectManagerRegistry instance = null;

    public synchronized static ObjectManagerRegistry getInstance() {
        if (instance == null) {
            instance = new ObjectManagerRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private final List<ObjectManagerDescriptor> entityManagers = new ArrayList<>();
    private final Map<Class<?>, ObjectManagerDescriptor> entityManagerMap = new HashMap<>();
    private final Map<Class<?>, Boolean> nullEntityManagerMap = new HashMap<>();

    public ObjectManagerRegistry(@NotNull IExtensionRegistry registry) {
        // Load datasource providers from external plugins
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(ObjectManagerDescriptor.EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            if (TAG_MANAGER.equals(ext.getName())) {
                ObjectManagerDescriptor descriptor = new ObjectManagerDescriptor(ext);
                entityManagers.add(descriptor);
            }
        }
    }

    public void dispose() {
        for (ObjectManagerDescriptor descriptor : entityManagers) {
            descriptor.dispose();
        }
        entityManagers.clear();
        entityManagerMap.clear();
        nullEntityManagerMap.clear();
    }

    private ObjectManagerDescriptor getEntityManager(Class<?> objectType) {
        ObjectManagerDescriptor manager = entityManagerMap.get(objectType);
        if (manager != null) {
            return manager;
        }
        if (nullEntityManagerMap.containsKey(objectType)) {
            return null;
        }
        // Match the class itself before looking at its parents. Different bundles may
        // define classes with the same name (e.g. the current and legacy MongoDB models).
        for (ObjectManagerDescriptor descriptor : entityManagers) {
            if (objectType.getName().equals(descriptor.getObjectType().getImplName())
                && descriptor.getObjectType().getObjectClass() == objectType) {
                entityManagerMap.put(objectType, descriptor);
                return descriptor;
            }
        }
        // Find a manager registered for a superclass or interface.
        for (ObjectManagerDescriptor descriptor : entityManagers) {
            if (!objectType.getName().equals(descriptor.getObjectType().getImplName())
                && descriptor.appliesToType(objectType)) {
                entityManagerMap.put(objectType, descriptor);
                return descriptor;
            }
        }
        // TODO: need to re-validate. Maybe cache will break some lazy loaded bundles?
        nullEntityManagerMap.put(objectType, Boolean.TRUE);
        return null;
    }

    @Nullable
    public DBEObjectManager<?> getObjectManager(@NotNull Class<?> aClass) {
        ObjectManagerDescriptor entityManager = getEntityManager(aClass);
        return entityManager == null ? null : entityManager.getManager();
    }

    @Nullable
    public <T> T getObjectManager(@NotNull Class<?> objectClass, @NotNull Class<T> managerType) {
        final DBEObjectManager<?> objectManager = getObjectManager(objectClass);
        if (objectManager != null && managerType.isAssignableFrom(objectManager.getClass())) {
            return managerType.cast(objectManager);
        } else {
            return null;
        }
    }

}

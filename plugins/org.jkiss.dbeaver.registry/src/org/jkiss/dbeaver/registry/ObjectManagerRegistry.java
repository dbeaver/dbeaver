/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

    private List<ObjectManagerDescriptor> entityManagers = new ArrayList<ObjectManagerDescriptor>();
    private Map<String, ObjectManagerDescriptor> entityManagerMap = new HashMap<>();
    private Map<String, Boolean> nullEntityManagerMap = new HashMap<>();

    public ObjectManagerRegistry(IExtensionRegistry registry) {
        // Load datasource providers from external plugins
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(ObjectManagerDescriptor.EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            if (TAG_MANAGER.equals(ext.getName())) {
                ObjectManagerDescriptor descriptor = new ObjectManagerDescriptor(ext);
                entityManagers.add(descriptor);
            }
        }
        for (ObjectManagerDescriptor em : entityManagers) {
            entityManagerMap.put(em.getObjectType().getImplName(), em);
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

    private ObjectManagerDescriptor getEntityManager(Class objectType) {
        String targetTypeName = objectType.getName();

        // 1. Try exact match
        ObjectManagerDescriptor manager = entityManagerMap.get(targetTypeName);
        if (manager != null) {
            return manager;
        }
        if (nullEntityManagerMap.containsKey(targetTypeName)) {
            return null;
        }
        // 2. Find first applicable
        for (ObjectManagerDescriptor descriptor : entityManagers) {
            if (descriptor.appliesToType(objectType)) {
                entityManagerMap.put(targetTypeName, descriptor);
                return descriptor;
            }
        }
        // TODO: need to re-validate. Maybe cache will break some lazy loaded bundles?
        nullEntityManagerMap.put(targetTypeName, Boolean.TRUE);
        return null;
    }

    public DBEObjectManager<?> getObjectManager(Class<?> aClass) {
        ObjectManagerDescriptor entityManager = getEntityManager(aClass);
        return entityManager == null ? null : entityManager.getManager();
    }

    public <T> T getObjectManager(Class<?> objectClass, Class<T> managerType) {
        final DBEObjectManager<?> objectManager = getObjectManager(objectClass);
        if (objectManager != null && managerType.isAssignableFrom(objectManager.getClass())) {
            return managerType.cast(objectManager);
        } else {
            return null;
        }
    }

}

/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPResourceTypeDescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * Resource type registry
 */
public class ResourceTypeRegistry {

    private static final Log log = Log.getLog(ResourceTypeRegistry.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.resourceType"; //NON-NLS-1

    private final List<ResourceTypeDescriptor> resourceTypeDescriptors = new ArrayList<>();

    private static ResourceTypeRegistry instance = null;

    public synchronized static ResourceTypeRegistry getInstance() {
        if (instance == null) {
            instance = new ResourceTypeRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private ResourceTypeRegistry(IExtensionRegistry registry) {
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(ResourceTypeDescriptor.EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            resourceTypeDescriptors.add(new ResourceTypeDescriptor(ext));
        }
    }

    public List<ResourceTypeDescriptor> getResourceTypes() {
        return resourceTypeDescriptors;
    }

    public DBPResourceTypeDescriptor getResourceType(String typeId) {
        for (ResourceTypeDescriptor td : resourceTypeDescriptors) {
            if (td.getId().equals(typeId)) {
                return td;
            }
        }
        return null;
    }

    public ResourceTypeDescriptor getResourceTypeByRootPath(DBPProject project, String path) {
        for (ResourceTypeDescriptor rhd : resourceTypeDescriptors) {
            String defaultRoot = rhd.getDefaultRoot(project);
            if (defaultRoot != null && defaultRoot.equals(path)) {
                return rhd;
            }
        }
        return null;
    }

}
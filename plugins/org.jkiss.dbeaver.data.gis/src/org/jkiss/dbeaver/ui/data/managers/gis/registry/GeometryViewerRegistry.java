/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.data.managers.gis.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeometryViewerRegistry {

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.data.gis.geometryViewer"; //$NON-NLS-1$

    public synchronized static GeometryViewerRegistry getInstance() {
        if (instance == null) {
            instance = new GeometryViewerRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private static GeometryViewerRegistry instance = null;

    private final Map<String, org.jkiss.dbeaver.ui.data.managers.gis.registry.GeometryViewerDescriptor> viewers = new HashMap<>();

    private GeometryViewerRegistry(IExtensionRegistry registry) {
        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            org.jkiss.dbeaver.ui.data.managers.gis.registry.GeometryViewerDescriptor type = new org.jkiss.dbeaver.ui.data.managers.gis.registry.GeometryViewerDescriptor(ext);
            viewers.put(type.getId(), type);
        }
    }

    public List<org.jkiss.dbeaver.ui.data.managers.gis.registry.GeometryViewerDescriptor> getViewers() {
        return new ArrayList<>(viewers.values());
    }

    public org.jkiss.dbeaver.ui.data.managers.gis.registry.GeometryViewerDescriptor getViewer(String id) {
        return viewers.get(id);
    }


}

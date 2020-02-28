/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.gis.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.ui.gis.GeometryViewerConstants;
import org.jkiss.dbeaver.ui.gis.internal.GISViewerActivator;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeometryViewerRegistry {

    private static final Log log = Log.getLog(GeometryViewerRegistry.class);

    public synchronized static GeometryViewerRegistry getInstance() {
        if (instance == null) {
            instance = new GeometryViewerRegistry(Platform.getExtensionRegistry());
        }
        return instance;
    }

    private static GeometryViewerRegistry instance = null;

    private final Map<String, GeometryViewerDescriptor> viewers = new HashMap<>();
    private final List<LeafletTilesDescriptor> leafletTiles = new ArrayList<>();
    private LeafletTilesDescriptor defaultLeafletTiles = null;

    private GeometryViewerRegistry(IExtensionRegistry registry) {
        {
            IConfigurationElement[] extElements = registry.getConfigurationElementsFor(GeometryViewerDescriptor.EXTENSION_ID);
            for (IConfigurationElement ext : extElements) {
                GeometryViewerDescriptor type = new GeometryViewerDescriptor(ext);
                viewers.put(type.getId(), type);
            }
        }

        {
            IConfigurationElement[] extElements = registry.getConfigurationElementsFor(LeafletTilesDescriptor.EXTENSION_ID);
            for (IConfigurationElement ext : extElements) {
                LeafletTilesDescriptor type = new LeafletTilesDescriptor(ext);
                leafletTiles.add(type);
            }
            String defTilesId = GISViewerActivator.getDefault().getPreferences().getString(GeometryViewerConstants.PREF_DEFAULT_LEAFLET_TILES);
            if (!CommonUtils.isEmpty(defTilesId)) {
                defaultLeafletTiles = DBUtils.findObject(leafletTiles, defTilesId);
            }
            if (defaultLeafletTiles == null) {
                defaultLeafletTiles = leafletTiles.isEmpty() ? null : leafletTiles.get(0);
            }
        }
    }

    public List<GeometryViewerDescriptor> getViewers() {
        return new ArrayList<>(viewers.values());
    }

    public GeometryViewerDescriptor getViewer(String id) {
        return viewers.get(id);
    }

    public List<LeafletTilesDescriptor> getLeafletTiles() {
        return leafletTiles;
    }

    public LeafletTilesDescriptor getDefaultLeafletTiles() {
        return defaultLeafletTiles;
    }

    public void setDefaultLeafletTiles(LeafletTilesDescriptor defaultLeafletTiles) {
        this.defaultLeafletTiles = defaultLeafletTiles;
        GISViewerActivator.getDefault().getPreferences().setValue(
            GeometryViewerConstants.PREF_DEFAULT_LEAFLET_TILES,
            this.defaultLeafletTiles == null ? "" : this.defaultLeafletTiles.getId());
        try {
            GISViewerActivator.getDefault().getPreferences().save();
        } catch (IOException e) {
            log.error(e);
        }
    }
}

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
package org.jkiss.dbeaver.ui.gis.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.gis.GeometryViewerConstants;
import org.jkiss.dbeaver.ui.gis.internal.GISViewerActivator;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.SAXListener;
import org.jkiss.utils.xml.SAXReader;
import org.jkiss.utils.xml.XMLBuilder;
import org.jkiss.utils.xml.XMLException;
import org.xml.sax.Attributes;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GeometryViewerRegistry {
    private static final Log log = Log.getLog(GeometryViewerRegistry.class);
    private static final GeometryViewerRegistry INSTANCE = new GeometryViewerRegistry(Platform.getExtensionRegistry());

    private static final String KEY_ROOT = "config";
    private static final String KEY_NON_VISIBLE_PREDEFINED_TILES = "notVisiblePredefinedTiles";
    private static final String KEY_USER_DEFINED_TILES = "userDefinedTiles";
    private static final String KEY_ID = "id";
    private static final String KEY_LABEL = "label";
    private static final String KEY_LAYERS_DEF = "layersDefinition";
    private static final String KEY_IS_VISIBLE = "isVisible";

    private final Map<String, GeometryViewerDescriptor> viewers = new HashMap<>();
    private final List<LeafletTilesDescriptor> predefinedTiles = new ArrayList<>();
    private final List<LeafletTilesDescriptor> userDefinedTiles = new ArrayList<>();
    private final Object tilesLock = new Object();

    @Nullable
    private LeafletTilesDescriptor defaultLeafletTiles;

    public static GeometryViewerRegistry getInstance() {
        return INSTANCE;
    }

    private GeometryViewerRegistry(@NotNull IExtensionRegistry registry) {
        Collection<String> notVisiblePredefinedTilesIds = new HashSet<>();
        populateFromConfig(notVisiblePredefinedTilesIds, userDefinedTiles);

        IConfigurationElement[] extElements = registry.getConfigurationElementsFor(GeometryViewerDescriptor.EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            GeometryViewerDescriptor type = new GeometryViewerDescriptor(ext);
            viewers.put(type.getId(), type);
        }

        extElements = registry.getConfigurationElementsFor(LeafletTilesDescriptor.EXTENSION_ID);
        for (IConfigurationElement ext : extElements) {
            LeafletTilesDescriptor descriptor = LeafletTilesDescriptor.createPredefined(ext);
            if (notVisiblePredefinedTilesIds.contains(descriptor.getId())) {
                descriptor = descriptor.withFlippedVisibility();
            }
            predefinedTiles.add(descriptor);
        }

        String defTilesId = GISViewerActivator.getDefault().getPreferences().getString(GeometryViewerConstants.PREF_DEFAULT_LEAFLET_TILES);
        if (!CommonUtils.isEmpty(defTilesId)) {
            defaultLeafletTiles = Stream.concat(predefinedTiles.stream(), userDefinedTiles.stream())
                    .filter(tile -> tile.getId().equals(defTilesId))
                    .findAny()
                    .orElse(null);
        }
        if (defaultLeafletTiles == null) {
            autoAssignDefaultLeafletTiles();
        }
    }

    private void autoAssignDefaultLeafletTiles() {
        Optional<LeafletTilesDescriptor> opt = Stream.concat(predefinedTiles.stream(), userDefinedTiles.stream())
                .filter(LeafletTilesDescriptor::isVisible)
                .findFirst();
        setDefaultLeafletTilesNonSynchronized(opt.orElse(null));
    }

    private static void populateFromConfig(@NotNull Collection<String> notVisiblePredefinedTilesIds, @NotNull Collection<LeafletTilesDescriptor> userDefinedTiles) {
        File cfg = getConfigFile();
        if (!cfg.exists()) {
            return;
        }
        try (InputStream in = new FileInputStream(cfg)) {
            SAXReader saxReader = new SAXReader(in);
            saxReader.parse(new SAXListener.BaseListener() {
                private final StringBuilder buffer = new StringBuilder();
                private String lastId;
                private String lastLabel;
                private String lastDefinition;
                private String lastVisibility;

                @Override
                public void saxStartElement(SAXReader reader, String namespaceURI, String localName, Attributes attributes) {
                    buffer.setLength(0);
                    switch (localName) {
                        case KEY_NON_VISIBLE_PREDEFINED_TILES:
                            lastId = attributes.getValue(KEY_ID);
                            break;
                        case KEY_USER_DEFINED_TILES:
                            lastId = attributes.getValue(KEY_ID);
                            lastLabel = attributes.getValue(KEY_LABEL);
                            lastDefinition = attributes.getValue(KEY_LAYERS_DEF);
                            lastVisibility = attributes.getValue(KEY_IS_VISIBLE);
                            break;
                        default:
                            // ignore
                            break;
                    }
                }

                @Override
                public void saxEndElement(SAXReader reader, String namespaceURI, String localName) {
                    switch (localName) {
                        case KEY_NON_VISIBLE_PREDEFINED_TILES:
                            if (CommonUtils.isNotEmpty(lastId)) {
                                notVisiblePredefinedTilesIds.add(lastId.trim());
                            }
                            break;
                        case KEY_USER_DEFINED_TILES:
                            final String layersDefinitionText = getDefinitionText();
                            if (CommonUtils.isEmpty(lastId) || CommonUtils.isEmpty(lastLabel) || CommonUtils.isEmpty(layersDefinitionText)) {
                                log.debug("Malformed user-defined tiles descriptor, skipping");
                                return;
                            }
                            userDefinedTiles.add(LeafletTilesDescriptor.createUserDefined(
                                lastLabel.trim(),
                                layersDefinitionText,
                                CommonUtils.getBoolean(lastVisibility, true)
                            ));
                            break;
                        default:
                            // ignore
                            break;
                    }
                }

                @Override
                public void saxText(SAXReader reader, String data) {
                    buffer.append(data);
                }

                @Nullable
                private String getDefinitionText() {
                    if (lastDefinition != null) {
                        // Backward compatibility
                        return lastDefinition;
                    }
                    if (buffer.length() > 0) {
                        // Read from CDATA
                        return buffer.toString();
                    }
                    return null;
                }
            });
        } catch (XMLException | IOException e) {
            log.error("Error reading " + GeometryViewerRegistry.class.getName() + " configuration", e);
        }
    }

    @NotNull
    private static File getConfigFile() {
        return DBWorkbench.getPlatform().getConfigurationFile("geometry_registry_config.xml");
    }

    //viewers are read only, so it's ok to not synchronize access
    public List<GeometryViewerDescriptor> getSupportedViewers(@NotNull DBPDataSource dataSource) {
        return viewers.values().stream().filter(v -> v.supportedBy(dataSource)).collect(Collectors.toList());
    }

    @Nullable
    public GeometryViewerDescriptor getViewer(@Nullable String id) {
        return viewers.get(id);
    }

    @NotNull
    public List<LeafletTilesDescriptor> getPredefinedLeafletTiles() {
        synchronized (tilesLock) {
            return Collections.unmodifiableList(predefinedTiles);
        }
    }

    @NotNull
    public List<LeafletTilesDescriptor> getUserDefinedLeafletTiles() {
        synchronized (tilesLock) {
            return Collections.unmodifiableList(userDefinedTiles);
        }
    }

    @Nullable
    public LeafletTilesDescriptor getDefaultLeafletTiles() {
        synchronized (tilesLock) {
            return defaultLeafletTiles;
        }
    }

    public void setDefaultLeafletTiles(@Nullable LeafletTilesDescriptor defaultLeafletTiles) {
        synchronized (tilesLock) {
            setDefaultLeafletTilesNonSynchronized(defaultLeafletTiles);
        }
    }

    private void setDefaultLeafletTilesNonSynchronized(@Nullable LeafletTilesDescriptor defaultLeafletTiles) {
        try {
            this.defaultLeafletTiles = defaultLeafletTiles;
            String preference = defaultLeafletTiles == null ? "" : defaultLeafletTiles.getId();
            GISViewerActivator.getDefault().getPreferences().setValue(GeometryViewerConstants.PREF_DEFAULT_LEAFLET_TILES, preference);
            GISViewerActivator.getDefault().getPreferences().save();
        } catch (IOException e) {
            log.error(e);
        }
    }

    public void updateTiles(@NotNull Collection<LeafletTilesDescriptor> predefinedDescriptors, @NotNull Collection<LeafletTilesDescriptor> userDefinedDescriptors) {
        synchronized (tilesLock) {
            predefinedTiles.clear();
            predefinedTiles.addAll(predefinedDescriptors);
            userDefinedTiles.clear();
            userDefinedTiles.addAll(userDefinedDescriptors);
            if (defaultLeafletTiles == null || (!predefinedTiles.contains(defaultLeafletTiles) && !userDefinedTiles.contains(defaultLeafletTiles))) {
                autoAssignDefaultLeafletTiles();
            }
            flushConfig();
        }
    }

    private void flushConfig() {
        try (OutputStream out = new FileOutputStream(getConfigFile())) {
            XMLBuilder xmlBuilder = new XMLBuilder(out, GeneralUtils.UTF8_ENCODING);
            xmlBuilder.setButify(true);
            try (XMLBuilder.Element ignored = xmlBuilder.startElement(KEY_ROOT)) {
                try (XMLBuilder.Element ignored1 = xmlBuilder.startElement("userDefinedTilesDefinitions")) {
                    for (LeafletTilesDescriptor descriptor : userDefinedTiles) {
                        try (XMLBuilder.Element ignored2 = xmlBuilder.startElement(KEY_USER_DEFINED_TILES)) {
                            xmlBuilder.addAttribute(KEY_ID, descriptor.getId());
                            xmlBuilder.addAttribute(KEY_LABEL, descriptor.getLabel());
                            xmlBuilder.addAttribute(KEY_IS_VISIBLE, descriptor.isVisible());
                            xmlBuilder.addTextData(descriptor.getLayersDefinition());
                        }
                    }
                }
                try (XMLBuilder.Element ignored1 = xmlBuilder.startElement("notVisiblePredefinedTilesList")) {
                    for (LeafletTilesDescriptor descriptor : predefinedTiles) {
                        if (descriptor.isVisible()) {
                            continue;
                        }
                        try (XMLBuilder.Element ignored2 = xmlBuilder.startElement(KEY_NON_VISIBLE_PREDEFINED_TILES)) {
                            xmlBuilder.addAttribute(KEY_ID, descriptor.getId());
                        }
                    }
                }
            }
            xmlBuilder.flush();
        } catch (IOException e) {
            log.error("Error saving" + GeometryViewerRegistry.class.getName() + " configuration");
        }
    }
}

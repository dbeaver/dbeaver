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
package org.jkiss.dbeaver.ui.dashboard.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.dashboard.DashboardConstants;
import org.jkiss.dbeaver.model.dashboard.registry.DashboardItemConfiguration;
import org.jkiss.dbeaver.ui.dashboard.internal.UIDashboardActivator;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.XMLBuilder;
import org.jkiss.utils.xml.XMLException;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * DashboardViewConfiguration
 */
public class DashboardConfiguration {

    public static final String REF_PREFIX = "ref#";

    public enum Parameter {
        project,
        datasource,
        id
    };

    private static final Log log = Log.getLog(DashboardConfiguration.class);

    @NotNull
    private final DBPProject project;
    private final DBPDataSourceContainer dataSourceContainer;
    private final String dashboardId;
    private final List<DashboardItemViewSettings> items = new ArrayList<>();

    private boolean openConnectionOnActivate;
    private boolean useSeparateConnection;

    public DashboardConfiguration(@NotNull DBPProject project, @Nullable DBPDataSourceContainer dataSourceContainer, @Nullable String dashboardId, String viewId) {
        this.project = project;
        this.dataSourceContainer = dataSourceContainer;
        this.dashboardId = dashboardId;
        loadSettings();
    }

    @NotNull
    public DBPProject getProject() {
        return project;
    }

    public DBPDataSourceContainer getDataSourceContainer() {
        return dataSourceContainer;
    }

    public List<DashboardItemViewSettings> getDashboardItemConfigs() {
        return items;
    }

    public boolean isOpenConnectionOnActivate() {
        return openConnectionOnActivate;
    }

    public void setOpenConnectionOnActivate(boolean openConnectionOnActivate) {
        this.openConnectionOnActivate = openConnectionOnActivate;
    }

    public boolean isUseSeparateConnection() {
        return useSeparateConnection;
    }

    public void setUseSeparateConnection(boolean useSeparateConnection) {
        this.useSeparateConnection = useSeparateConnection;
    }

    public DashboardItemViewSettings getItemConfig(String itemId) {
        for (DashboardItemViewSettings item : items) {
            if (item.getItemId().equals(itemId)) {
                return item;
            }
        }
        return null;
    }

    public DashboardItemViewSettings readDashboardItemConfiguration(DashboardItemConfiguration item) {
        DashboardItemViewSettings itemConfig = getItemConfig(item.getId());
        if (itemConfig != null) {
            return itemConfig;
        }
        try {
            DashboardItemViewSettings itemViewConfiguration = new DashboardItemViewSettings(this, item, items.size());
            items.add(itemViewConfiguration);
            return itemViewConfiguration;
        } catch (DBException e) {
            log.error(e);
            return null;
        }
    }

    public boolean readDashboardItemConfiguration(DashboardItemViewSettings item) {
        return items.remove(item);
    }

    public void removeItem(String itemID) {
        int decValue = 0;
        for (int i = 0; i < items.size(); ) {
            DashboardItemViewSettings item = items.get(i);
            if (item.getItemId().equals(itemID)) {
                items.remove(i);
                decValue++;
            } else {
                item.setIndex(item.getIndex() - decValue);
                i++;
            }
        }
    }

    public void updateItemConfig(DashboardItemViewSettings config) {
        DashboardItemViewSettings curConfig = getItemConfig(config.getItemId());
        if (curConfig == null) {
            items.add(config);
        } else {
            curConfig.copyFrom(config);
        }
    }

    public void clearItems() {
        this.items.clear();
    }

    private void loadSettings() {
        Document dbDocument = null;

        try {
            String dbSerialized = dataSourceContainer.getExtension(DashboardConstants.DS_PROP_DASHBOARDS);
            if (!CommonUtils.isEmpty(dbSerialized)) {
                dbDocument = XMLUtils.parseDocument(new StringReader(dbSerialized));
            } else {
                // Backward compatibility - read from file
                Path configFile = getConfigFile(false);
                if (Files.exists(configFile)) {
                    dbDocument = XMLUtils.parseDocument(configFile.toFile());
                }
            }
        } catch (XMLException e) {
            log.error("Error parsing dashboards", e);
        }

        if (dbDocument != null) {
            try {
                Element rootElement = dbDocument.getDocumentElement();
                for (Element viewElement : XMLUtils.getChildElementList(rootElement, "view")) {
                    openConnectionOnActivate = CommonUtils.getBoolean(viewElement.getAttribute("openConnectionOnActivate"), openConnectionOnActivate);
                    useSeparateConnection = CommonUtils.getBoolean(viewElement.getAttribute("useSeparateConnection"), useSeparateConnection);
                }
                for (Element dbElement : XMLUtils.getChildElementList(rootElement, "dashboard")) {
                    String dashboardId = dbElement.getAttribute("id");
                    DashboardItemViewSettings itemConfig = new DashboardItemViewSettings(this, dashboardId, dbElement);
                    items.add(itemConfig);
                }
            } catch (Exception e) {
                log.error("Error loading dashboard view configuration", e);
            }

            items.sort(Comparator.comparingInt(DashboardItemViewSettings::getIndex));
        }
    }

    public void saveSettings() {
        StringWriter buffer = new StringWriter();
        try {
            XMLBuilder xml = new XMLBuilder(buffer, GeneralUtils.UTF8_ENCODING, false);
            xml.setButify(false);
            serializeConfig(xml);
            xml.flush();
        } catch (Exception e) {
            log.error("Error saving dashboard view configuration", e);
        }

        dataSourceContainer.setExtension(DashboardConstants.DS_PROP_DASHBOARDS, buffer.toString());
        dataSourceContainer.persistConfiguration();
    }

    private void serializeConfig(XMLBuilder xml) throws IOException {
        try (var ignored = xml.startElement("dashboards")) {
            if (!CommonUtils.isEmpty(dashboardId)) {
                xml.addAttribute("id", dashboardId);
            }
            try (var ignored2 = xml.startElement("view")) {
                if (openConnectionOnActivate) {
                    xml.addAttribute("openConnectionOnActivate", openConnectionOnActivate);
                }
                if (useSeparateConnection) {
                    xml.addAttribute("useSeparateConnection", useSeparateConnection);
                }
            }
            for (DashboardItemViewSettings itemConfig : items) {
                try (var ignored3 = xml.startElement("dashboard")) {
                    itemConfig.serialize(xml);
                }
            }
        }
    }

    private Path getConfigFile(boolean forceCreate) {
        Path pluginFolder = UIDashboardActivator.getDefault().getStateLocation().toPath();
        Path viewConfigFolder = pluginFolder.resolve("views");
        if (!Files.exists(viewConfigFolder)) {
            if (forceCreate) {
                try {
                    Files.createDirectories(viewConfigFolder);
                } catch (IOException e) {
                    log.error("Can't create view config folder " + viewConfigFolder, e);
                }
            }
        }
        return viewConfigFolder.resolve("view-" + project.getName() +
            (dataSourceContainer == null ? "" : "_" + dataSourceContainer.getId().replace("/", "_")) + ".xml");
    }

    public static String getViewId(DBPProject project, DBPDataSourceContainer dataSourceContainer, String id) {
        StringBuilder viewId = new StringBuilder();
        viewId.append(REF_PREFIX).append(Parameter.project.name()).append("=").append(project.getName());
        if (dataSourceContainer != null) {
            viewId.append(",").append(Parameter.datasource.name()).append("=").append(dataSourceContainer.getId());
        }
        if (id != null) {
            viewId.append(",").append(Parameter.id.name()).append("=").append(id);
        }
        return viewId.toString();
    }

}

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
import org.jkiss.dbeaver.model.dashboard.registry.DashboardItemDescriptor;
import org.jkiss.dbeaver.ui.dashboard.internal.UIDashboardActivator;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.XMLBuilder;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.OutputStream;
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

    private final String viewId;

    @NotNull
    private final DBPProject project;
    private final DBPDataSourceContainer dataSourceContainer;
    private final String dashboardId;
    private final List<DashboardItemConfiguration> items = new ArrayList<>();

    private boolean openConnectionOnActivate;
    private boolean useSeparateConnection;

    public DashboardConfiguration(@NotNull DBPProject project, @Nullable DBPDataSourceContainer dataSourceContainer, @Nullable String dashboardId, String viewId) {
        this.project = project;
        this.dataSourceContainer = dataSourceContainer;
        this.dashboardId = dashboardId;
        this.viewId = viewId;
        loadSettings();
    }

    @NotNull
    public DBPProject getProject() {
        return project;
    }

    public DBPDataSourceContainer getDataSourceContainer() {
        return dataSourceContainer;
    }

    public List<DashboardItemConfiguration> getDashboardItemConfigs() {
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

    public DashboardItemConfiguration getItemConfig(String itemId) {
        for (DashboardItemConfiguration item : items) {
            if (item.getItemId().equals(itemId)) {
                return item;
            }
        }
        return null;
    }

    public DashboardItemConfiguration readDashboardItemConfiguration(DashboardItemDescriptor item) {
        DashboardItemConfiguration itemConfig = getItemConfig(item.getId());
        if (itemConfig != null) {
            return itemConfig;
        }
        try {
            DashboardItemConfiguration itemViewConfiguration = new DashboardItemConfiguration(this, item, items.size());
            items.add(itemViewConfiguration);
            return itemViewConfiguration;
        } catch (DBException e) {
            log.error(e);
            return null;
        }
    }

    public boolean readDashboardItemConfiguration(DashboardItemConfiguration item) {
        return items.remove(item);
    }

    public void removeItem(String itemID) {
        int decValue = 0;
        for (int i = 0; i < items.size(); ) {
            DashboardItemConfiguration item = items.get(i);
            if (item.getItemId().equals(itemID)) {
                items.remove(i);
                decValue++;
            } else {
                item.setIndex(item.getIndex() - decValue);
                i++;
            }
        }
    }

    public void updateItemConfig(DashboardItemConfiguration config) {
        DashboardItemConfiguration curConfig = getItemConfig(config.getItemId());
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
        Path configFile = getConfigFile(false);
        if (!Files.exists(configFile)) {
            return;
        }

        try {
            Document document = XMLUtils.parseDocument(configFile.toFile());
            for (Element viewElement : XMLUtils.getChildElementList(document.getDocumentElement(), "view")) {
                openConnectionOnActivate = CommonUtils.getBoolean(viewElement.getAttribute("openConnectionOnActivate"), openConnectionOnActivate);
                useSeparateConnection = CommonUtils.getBoolean(viewElement.getAttribute("useSeparateConnection"), useSeparateConnection);
            }
            for (Element dbElement : XMLUtils.getChildElementList(document.getDocumentElement(), "dashboard")) {
                String dashboardId = dbElement.getAttribute("id");
                DashboardItemConfiguration itemConfig = new DashboardItemConfiguration(this, dashboardId, dbElement);
                items.add(itemConfig);
            }
        } catch (Exception e) {
            log.error("Error loading dashboard view configuration", e);
        }

        items.sort(Comparator.comparingInt(DashboardItemConfiguration::getIndex));
    }

    public void saveSettings() {
        Path configFile = getConfigFile(true);

        if (items.isEmpty()) {
            if (Files.exists(configFile)) {
                try {
                    Files.delete(configFile);
                } catch (IOException e) {
                    log.debug("Can't delete view configuration " + configFile, e);
                }
            }
            return;
        }

        try (OutputStream out = Files.newOutputStream(configFile)) {
            XMLBuilder xml = new XMLBuilder(out, GeneralUtils.UTF8_ENCODING);
            xml.setButify(true);
            try (var ignored = xml.startElement("dashboards")) {
                try (var ignored2 = xml.startElement("view")) {
                    xml.addAttribute("openConnectionOnActivate", openConnectionOnActivate);
                    xml.addAttribute("useSeparateConnection", useSeparateConnection);
                }
                for (DashboardItemConfiguration itemConfig : items) {
                    try (var ignored3 = xml.startElement("dashboard")) {
                        itemConfig.serialize(xml);
                    }
                }
            }
            xml.flush();
        } catch (Exception e) {
            log.error("Error saving dashboard view configuration", e);
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

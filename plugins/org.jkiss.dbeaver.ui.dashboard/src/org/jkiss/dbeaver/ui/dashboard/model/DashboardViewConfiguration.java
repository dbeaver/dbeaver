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
package org.jkiss.dbeaver.ui.dashboard.model;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.ui.dashboard.internal.UIDashboardActivator;
import org.jkiss.dbeaver.ui.dashboard.registry.DashboardDescriptor;
import org.jkiss.dbeaver.ui.dashboard.registry.DashboardRegistry;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.xml.XMLBuilder;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * DashboardViewConfiguration
 */
public class DashboardViewConfiguration {

    private static final Log log = Log.getLog(DashboardViewConfiguration.class);

    private String viewId;

    private final DBPDataSourceContainer dataSourceContainer;
    private List<DashboardItemViewConfiguration> items = new ArrayList<>();

    private boolean openConnectionOnActivate;
    private boolean useSeparateConnection;

    public DashboardViewConfiguration(DBPDataSourceContainer dataSourceContainer, String viewId) {
        this.dataSourceContainer = dataSourceContainer;
        this.viewId = viewId;
        loadSettings();
    }

    public String getViewId() {
        return viewId;
    }

    public DBPDataSourceContainer getDataSourceContainer() {
        return dataSourceContainer;
    }

    public List<DashboardItemViewConfiguration> getDashboardItemConfigs() {
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

    public DashboardItemViewConfiguration getDashboardConfig(String dashboardId) {
        for (DashboardItemViewConfiguration item : items) {
            if (item.getDashboardDescriptor().getId().equals(dashboardId)) {
                return item;
            }
        }
        return null;
    }

    public DashboardItemViewConfiguration readDashboardConfiguration(DashboardDescriptor dashboard) {
        DashboardItemViewConfiguration dashboardConfig = getDashboardConfig(dashboard.getId());
        if (dashboardConfig != null) {
            return dashboardConfig;
        }
        DashboardItemViewConfiguration itemViewConfiguration = new DashboardItemViewConfiguration(dashboard, items.size());
        items.add(itemViewConfiguration);
        return itemViewConfiguration;
    }

    public void removeDashboard(String dashboardId) {
        int decValue = 0;
        for (int i = 0; i < items.size(); ) {
            DashboardItemViewConfiguration item = items.get(i);
            if (item.getDashboardDescriptor().getId().equals(dashboardId)) {
                items.remove(i);
                decValue++;
            } else {
                item.setIndex(item.getIndex() - decValue);
                i++;
            }
        }
    }

    public void updateDashboardConfig(DashboardItemViewConfiguration config) {
        DashboardItemViewConfiguration curConfig = getDashboardConfig(config.getDashboardDescriptor().getId());
        if (curConfig == null) {
            items.add(config);
        } else {
            curConfig.copyFrom(config);
        }
    }

    public void clearDashboards() {
        this.items.clear();
    }

    private void loadSettings() {
        File configFile = getConfigFile();
        if (!configFile.exists()) {
            return;
        }

        try {
            Document document = XMLUtils.parseDocument(configFile);
            for (Element dbElement : XMLUtils.getChildElementList(document.getDocumentElement(), "dashboard")) {
                String dashboardId = dbElement.getAttribute("id");
                DashboardDescriptor dashboard = DashboardRegistry.getInstance().getDashboard(dashboardId);
                if (dashboard != null) {
                    DashboardItemViewConfiguration itemConfig = new DashboardItemViewConfiguration(dashboard, dbElement);
                    items.add(itemConfig);
                } else {
                    log.warn("Dashboard '" + dashboardId + "' not found");
                }
            }
        } catch (Exception e) {
            log.error("Error loading dashboard view configuration", e);
        }

        items.sort(Comparator.comparingInt(DashboardItemViewConfiguration::getIndex));
    }

    public void saveSettings() {
        File configFile = getConfigFile();

        if (items.isEmpty()) {
            if (configFile.exists() && !configFile.delete()) {
                log.debug("Can't delete view configuration " + configFile.getAbsolutePath());
            }
            return;
        }

        try (OutputStream out = new FileOutputStream(configFile)){
            XMLBuilder xml = new XMLBuilder(out, GeneralUtils.UTF8_ENCODING);
            xml.setButify(true);
            xml.startElement("dashboards");
            for (DashboardItemViewConfiguration itemConfig : items) {
                xml.startElement("dashboard");
                itemConfig.serialize(xml);
                xml.endElement();
            }
            xml.endElement();
            xml.flush();
        } catch (Exception e) {
            log.error("Error saving dashboard view configuration", e);
        }
    }

    private File getConfigFile() {
        File pluginFolder = UIDashboardActivator.getDefault().getStateLocation().toFile();
        File viewConfigFolder = new File(pluginFolder, "views");
        if (!viewConfigFolder.exists()) {
            if (!viewConfigFolder.mkdirs()) {
                log.error("Can't create view config folder " + viewConfigFolder.getAbsolutePath());
            }
        }
        return new File(viewConfigFolder, "view-" + viewId + ".xml");
    }

}

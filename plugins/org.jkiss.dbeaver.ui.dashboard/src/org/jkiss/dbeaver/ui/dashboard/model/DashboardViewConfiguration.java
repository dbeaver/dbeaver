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
import org.jkiss.dbeaver.ui.dashboard.internal.UIDashboardActivator;
import org.jkiss.dbeaver.ui.dashboard.registry.DashboardDescriptor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.xml.XMLBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * DashboardViewConfiguration
 */
public class DashboardViewConfiguration {

    private static final Log log = Log.getLog(DashboardViewConfiguration.class);

    private String viewId;

    private List<DashboardItemViewConfiguration> items = new ArrayList<>();

    public DashboardViewConfiguration(String viewId) {
        this.viewId = viewId;
        loadSettings();
    }

    public DashboardItemViewConfiguration getDashboardConfig(String dashboardId) {
        for (DashboardItemViewConfiguration item : items) {
            if (item.getDashboardDescriptor().getId().equals(dashboardId)) {
                return item;
            }
        }
        return null;
    }

    private void loadSettings() {
        File configFile = getConfigFile();
        if (!configFile.exists()) {
            return;
        }
    }

    public void saveSettings() {
        File configFile = getConfigFile();

        try (OutputStream out = new FileOutputStream(configFile)){
            XMLBuilder xml = new XMLBuilder(out, GeneralUtils.UTF8_ENCODING);
            xml.startElement("dashboards");
            for (DashboardItemViewConfiguration itemConfig : items) {
                xml.startElement("dashboard");
                //itemConfig.serialize(xml);
                xml.endElement();
            }
            xml.endElement();
            out.flush();
        } catch (Exception e) {
            log.error("Error saving dashboard view configuration", e);
        }
    }

    private File getConfigFile() {
        File pluginFolder = UIDashboardActivator.getDefault().getStateLocation().toFile();
        return new File(pluginFolder, "view-" + viewId + ".xml");
    }

    public void readDashboardConfiguration(DashboardDescriptor dashboard) {
        DashboardItemViewConfiguration dashboardConfig = getDashboardConfig(dashboard.getId());
        if (dashboardConfig != null) {
            return;
        }
        items.add(new DashboardItemViewConfiguration(dashboard));
    }

    public void removeDashboard(String dashboardId) {
        items.removeIf(
            dashboardItemViewConfiguration -> dashboardItemViewConfiguration.getDashboardDescriptor().getId().equals(dashboardId));
    }

    public void clearDashboards() {
        this.items.clear();
    }

    public void updateDashboardConfig(DashboardItemViewConfiguration config) {
        DashboardItemViewConfiguration curConfig = getDashboardConfig(config.getDashboardDescriptor().getId());
        if (curConfig == null) {
            items.add(config);
        } else {
            curConfig.copyFrom(config);
        }
    }
}

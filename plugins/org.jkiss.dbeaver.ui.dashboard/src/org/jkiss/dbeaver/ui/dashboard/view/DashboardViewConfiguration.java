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
package org.jkiss.dbeaver.ui.dashboard.view;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.dashboard.internal.UIDashboardActivator;
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

    public class DashboardItemViewConfig {
        private String dashboardId;

        private String description;
        private float widthRatio;
        private long updatePeriod;
        private int maxItems;
        private long maxAge;

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public float getWidthRatio() {
            return widthRatio;
        }

        public void setWidthRatio(float widthRatio) {
            this.widthRatio = widthRatio;
        }

        public long getUpdatePeriod() {
            return updatePeriod;
        }

        public void setUpdatePeriod(long updatePeriod) {
            this.updatePeriod = updatePeriod;
        }

        public int getMaxItems() {
            return maxItems;
        }

        public void setMaxItems(int maxItems) {
            this.maxItems = maxItems;
        }

        public long getMaxAge() {
            return maxAge;
        }

        public void setMaxAge(long maxAge) {
            this.maxAge = maxAge;
        }
    }

    List<DashboardItemViewConfig> items = new ArrayList<>();

    public DashboardViewConfiguration(String viewId) {
        this.viewId = viewId;
        loadSettings();
    }

    private void loadSettings() {
        File configFile = getConfigFile();
        if (!configFile.exists()) {
            return;
        }
    }

    private void saveSettings() {
        File configFile = getConfigFile();

        try (OutputStream out = new FileOutputStream(configFile)){
            XMLBuilder xml = new XMLBuilder(out, GeneralUtils.UTF8_ENCODING);
            xml.startElement("dashboards");
            for (DashboardItemViewConfig itemConfig : items) {
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

}

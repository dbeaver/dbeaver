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

import org.jkiss.dbeaver.ui.dashboard.registry.DashboardDescriptor;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.XMLBuilder;

import java.io.IOException;

public class DashboardItemViewConfiguration {
    private DashboardDescriptor dashboardDescriptor;

    private float widthRatio;
    private long updatePeriod;
    private int maxItems;
    private long maxAge;
    private String description;

    public DashboardDescriptor getDashboardDescriptor() {
        return dashboardDescriptor;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    DashboardItemViewConfiguration(DashboardDescriptor dashboardDescriptor) {
        this.dashboardDescriptor = dashboardDescriptor;
        this.widthRatio = dashboardDescriptor.getWidthRatio();
        this.updatePeriod = dashboardDescriptor.getUpdatePeriod();
        this.maxItems = dashboardDescriptor.getMaxItems();
        this.maxAge = dashboardDescriptor.getMaxAge();
        this.description = dashboardDescriptor.getDescription();
    }

    public DashboardItemViewConfiguration(DashboardItemViewConfiguration source) {
        copyFrom(source);
    }

    void copyFrom(DashboardItemViewConfiguration source) {
        this.dashboardDescriptor = source.dashboardDescriptor;
        this.widthRatio = source.widthRatio;
        this.updatePeriod = source.updatePeriod;
        this.maxItems = source.maxItems;
        this.maxAge = source.maxAge;
        this.description = source.description;
    }

    void serialize(XMLBuilder xml) throws IOException {
        xml.addAttribute("dashboard", dashboardDescriptor.getId());
        xml.addAttribute("widthRatio", widthRatio);
        xml.addAttribute("updatePeriod", updatePeriod);
        xml.addAttribute("maxItems", maxItems);
        xml.addAttribute("maxAge", maxAge);
        if (!CommonUtils.isEmpty(description)) {
            xml.addAttribute("description", description);
        }
    }
}

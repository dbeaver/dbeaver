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
import org.jkiss.dbeaver.ui.dashboard.registry.DashboardRegistry;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.XMLBuilder;
import org.w3c.dom.Element;

import java.io.IOException;

public class DashboardItemViewConfiguration {
    private DashboardDescriptor dashboardDescriptor;

    private DashboardViewType viewType;
    private int index;
    private float widthRatio;
    private long updatePeriod;
    private int maxItems;
    private long maxAge;
    private boolean legendVisible;
    private boolean domainTicksVisible;
    private boolean rangeTicksVisible;
    private String description;

    public DashboardDescriptor getDashboardDescriptor() {
        return dashboardDescriptor;
    }

    public DashboardViewType getViewType() {
        return viewType;
    }

    public void setViewType(DashboardViewType viewType) {
        this.viewType = viewType;
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

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public boolean isLegendVisible() {
        return legendVisible;
    }

    public void setLegendVisible(boolean legendVisible) {
        this.legendVisible = legendVisible;
    }

    public boolean isDomainTicksVisible() {
        return domainTicksVisible;
    }

    public void setDomainTicksVisible(boolean domainTicksVisible) {
        this.domainTicksVisible = domainTicksVisible;
    }

    public boolean isRangeTicksVisible() {
        return rangeTicksVisible;
    }

    public void setRangeTicksVisible(boolean rangeTicksVisible) {
        this.rangeTicksVisible = rangeTicksVisible;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    DashboardItemViewConfiguration(DashboardDescriptor dashboardDescriptor, int index) {
        this.dashboardDescriptor = dashboardDescriptor;
        this.viewType = dashboardDescriptor.getDefaultViewType();
        this.index = index;
        this.widthRatio = dashboardDescriptor.getWidthRatio();
        this.updatePeriod = dashboardDescriptor.getUpdatePeriod();
        this.maxItems = dashboardDescriptor.getMaxItems();
        this.maxAge = dashboardDescriptor.getMaxAge();

        this.legendVisible = true;
        this.domainTicksVisible = true;
        this.rangeTicksVisible = true;

        this.description = dashboardDescriptor.getDescription();
    }

    public DashboardItemViewConfiguration(DashboardItemViewConfiguration source) {
        copyFrom(source);
    }

    void copyFrom(DashboardItemViewConfiguration source) {
        this.dashboardDescriptor = source.dashboardDescriptor;
        this.viewType = source.viewType;
        this.index = source.index;
        this.widthRatio = source.widthRatio;
        this.updatePeriod = source.updatePeriod;
        this.maxItems = source.maxItems;
        this.maxAge = source.maxAge;

        this.legendVisible = source.legendVisible;
        this.domainTicksVisible = source.domainTicksVisible;
        this.rangeTicksVisible = source.rangeTicksVisible;

        this.description = source.description;
    }

    void serialize(XMLBuilder xml) throws IOException {
        xml.addAttribute("id", dashboardDescriptor.getId());
        xml.addAttribute("viewType", viewType.getId());
        xml.addAttribute("index", index);
        xml.addAttribute("widthRatio", widthRatio);
        xml.addAttribute("updatePeriod", updatePeriod);
        xml.addAttribute("maxItems", maxItems);
        xml.addAttribute("maxAge", maxAge);

        xml.addAttribute("legendVisible", legendVisible);
        xml.addAttribute("domainTicksVisible", domainTicksVisible);
        xml.addAttribute("rangeTicksVisible", rangeTicksVisible);

        if (!CommonUtils.isEmpty(description)) {
            xml.addAttribute("description", description);
        }
    }

    public DashboardItemViewConfiguration(DashboardDescriptor dashboard, Element element) {
        this.dashboardDescriptor = dashboard;

        String viewTypeId = element.getAttribute("viewType");
        if (viewTypeId != null) {
            this.viewType = DashboardRegistry.getInstance().getViewType(viewTypeId);
        }
        if (this.viewType == null) {
            this.viewType = dashboard.getDefaultViewType();
        }
        this.viewType = viewTypeId == null ? dashboard.getDefaultViewType() : DashboardRegistry.getInstance().getViewType(viewTypeId);
        this.index = CommonUtils.toInt(element.getAttribute("index"));
        this.widthRatio = (float) CommonUtils.toDouble(element.getAttribute("widthRatio"), dashboardDescriptor.getWidthRatio());
        this.updatePeriod = CommonUtils.toLong(element.getAttribute("updatePeriod"), dashboardDescriptor.getUpdatePeriod());
        this.maxItems = CommonUtils.toInt(element.getAttribute("maxItems"), dashboardDescriptor.getMaxItems());
        this.maxAge = CommonUtils.toLong(element.getAttribute("maxAge"), dashboardDescriptor.getMaxAge());

        legendVisible = CommonUtils.getBoolean(element.getAttribute("legendVisible"), true);
        domainTicksVisible = CommonUtils.getBoolean(element.getAttribute("domainTicksVisible"), true);
        rangeTicksVisible = CommonUtils.getBoolean(element.getAttribute("rangeTicksVisible"), true);

        this.description = element.getAttribute("description");
    }

    @Override
    public String toString() {
        return dashboardDescriptor.getId() + ":" + index;
    }
}

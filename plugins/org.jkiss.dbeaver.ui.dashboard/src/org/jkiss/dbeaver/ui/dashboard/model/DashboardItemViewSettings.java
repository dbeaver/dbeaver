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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.dashboard.DBDashboardContext;
import org.jkiss.dbeaver.model.dashboard.DashboardConstants;
import org.jkiss.dbeaver.model.dashboard.registry.DashboardItemConfiguration;
import org.jkiss.dbeaver.model.dashboard.registry.DashboardRegistry;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.dashboard.registry.DashboardRendererDescriptor;
import org.jkiss.dbeaver.ui.dashboard.registry.DashboardUIRegistry;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.XMLBuilder;
import org.w3c.dom.Element;

import java.io.IOException;

public class DashboardItemViewSettings {
    private static final Log log = Log.getLog(DashboardItemViewSettings.class);

    private final DashboardConfiguration viewConfiguration;
    private String itemId;
    private DashboardItemConfiguration dashboardItem;

    private String viewTypeId;
    private int index;
    private float widthRatio;
    private long updatePeriod;
    private int maxItems;
    private long maxAge;
    private boolean legendVisible;
    private boolean gridVisible;
    private boolean domainTicksVisible;
    private boolean rangeTicksVisible;
    private String description;

    DashboardItemViewSettings(DashboardConfiguration viewConfiguration, DashboardItemConfiguration dashboardDescriptor, int index) throws DBException {
        this.viewConfiguration = viewConfiguration;
        this.dashboardItem = dashboardDescriptor;
        this.viewTypeId = dashboardDescriptor.getDashboardRenderer();
        this.index = index;
        this.widthRatio = dashboardDescriptor.getWidthRatio();
        this.updatePeriod = dashboardDescriptor.getUpdatePeriod();
        this.maxItems = dashboardDescriptor.getMaxItems();
        this.maxAge = dashboardDescriptor.getMaxAge();

        this.legendVisible = true;
        this.gridVisible = true;
        this.domainTicksVisible = true;
        this.rangeTicksVisible = true;

        this.description = dashboardDescriptor.getDescription();
    }


    public DashboardItemViewSettings(DashboardConfiguration viewConfiguration, String id, Element element) {
        this.viewConfiguration = viewConfiguration;
        this.itemId = id;

        this.viewTypeId = element.getAttribute("viewType");
        this.index = CommonUtils.toInt(element.getAttribute("index"));
        this.widthRatio = (float) CommonUtils.toDouble(element.getAttribute("widthRatio"));
        this.updatePeriod = CommonUtils.toLong(element.getAttribute("updatePeriod"));
        this.maxItems = CommonUtils.toInt(element.getAttribute("maxItems"));
        this.maxAge = CommonUtils.toLong(element.getAttribute("maxAge"));

        this.legendVisible = CommonUtils.getBoolean(element.getAttribute("legendVisible"), true);
        this.gridVisible = CommonUtils.getBoolean(element.getAttribute("gridVisible"), true);
        this.domainTicksVisible = CommonUtils.getBoolean(element.getAttribute("domainTicksVisible"), true);
        this.rangeTicksVisible = CommonUtils.getBoolean(element.getAttribute("rangeTicksVisible"), true);

        this.description = element.getAttribute("description");
    }

    public DashboardItemViewSettings(DashboardItemViewSettings source) {
        this.viewConfiguration = source.viewConfiguration;
        copyFrom(source);
    }

    public DashboardConfiguration getViewConfiguration() {
        return viewConfiguration;
    }

    public DashboardItemConfiguration getDashboardItem() {
        return dashboardItem;
    }

    public String getItemId() {
        return dashboardItem == null ? itemId : dashboardItem.getId();
    }

    public String getFullDashboardId() {
        if (dashboardItem != null) {
            String path = dashboardItem.getPath();
            if (path != null) {
                path += "/";
            } else {
                path = "";
            }
            return dashboardItem.getDashboardProvider().getId() + ":" + path + dashboardItem.getId();
        }
        return itemId;
    }

    @Nullable
    public DashboardItemConfiguration getItemConfiguration() {
        if (dashboardItem == null) {
            try {
                DBPDataSourceContainer dataSourceContainer = viewConfiguration.getDataSourceContainer();
                dashboardItem = DashboardRegistry.getInstance().findDashboardItem(
                    new VoidProgressMonitor(),
                    dataSourceContainer != null ?
                        new DBDashboardContext(dataSourceContainer) :
                        new DBDashboardContext(viewConfiguration.getProject()),
                    itemId);
                if (dashboardItem == null) {
                    log.debug("Configuration item '" + itemId + "' not found in registry");
                }
            } catch (DBException e) {
                log.debug("Dashboard '" + itemId + "' not found", e);
                return null;
            }
        }
        return dashboardItem;
    }

    public DashboardRendererType getViewType() {
        String vtId = viewTypeId;
        if (CommonUtils.isEmpty(vtId)) {
            DashboardItemConfiguration dashboard = getItemConfiguration();
            vtId = dashboard == null ? DashboardConstants.DEF_DASHBOARD_VIEW_TYPE : dashboard.getDashboardRenderer();
        }
        return DashboardUIRegistry.getInstance().getViewType(vtId);
    }

    public void setViewType(DashboardRendererType viewType) {
        this.viewTypeId = viewType.getId();
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

    public boolean isGridVisible() {
        return gridVisible;
    }

    public void setGridVisible(boolean gridVisible) {
        this.gridVisible = gridVisible;
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

    void copyFrom(DashboardItemViewSettings source) {
        this.dashboardItem = source.dashboardItem;
        this.viewTypeId = source.viewTypeId;
        this.index = source.index;
        this.widthRatio = source.widthRatio;
        this.updatePeriod = source.updatePeriod;
        this.maxItems = source.maxItems;
        this.maxAge = source.maxAge;

        this.legendVisible = source.legendVisible;
        this.gridVisible = source.gridVisible;
        this.domainTicksVisible = source.domainTicksVisible;
        this.rangeTicksVisible = source.rangeTicksVisible;

        this.description = source.description;
    }

    void serialize(XMLBuilder xml) throws IOException {
        DashboardRendererDescriptor viewType = DashboardUIRegistry.getInstance().getViewType(viewTypeId);
        boolean isNativeRenderer = viewType != null && viewType.isNativeRenderer();

        xml.addAttribute("id", getFullDashboardId());
        xml.addAttribute("viewType", viewTypeId);

        xml.addAttribute("index", index);
        xml.addAttribute("widthRatio", widthRatio);
        xml.addAttribute("updatePeriod", updatePeriod);

        if (isNativeRenderer) {
            xml.addAttribute("maxItems", maxItems);
            xml.addAttribute("maxAge", maxAge);

            xml.addAttribute("legendVisible", legendVisible);
            xml.addAttribute("gridVisible", gridVisible);
            xml.addAttribute("domainTicksVisible", domainTicksVisible);
            xml.addAttribute("rangeTicksVisible", rangeTicksVisible);
        }

        if (!CommonUtils.isEmpty(description)) {
            xml.addAttribute("description", description);
        }
    }

    @Override
    public String toString() {
        return getItemId() + ":" + index;
    }
}

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
package org.jkiss.dbeaver.ui.dashboard.registry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.impl.AbstractContextDescriptor;
import org.jkiss.dbeaver.ui.dashboard.internal.UIDashboardActivator;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardCalcType;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardConstants;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardFetchType;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardQuery;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.XMLBuilder;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * DashboardDescriptor
 */
public class DashboardDescriptor extends AbstractContextDescriptor implements DBPNamedObject
{
    public static final String EXTENSION_ID = "org.jkiss.dbeaver.dashboard"; //$NON-NLS-1$

    private String id;
    private String name;
    private String description;
    private String group;
    private boolean showByDefault;
    private DashboardTypeDescriptor type;
    private String[] tags;
    private final List<DataSourceMapping> dataSourceMappings = new ArrayList<>();
    private final List<QueryMapping> queries = new ArrayList<>();

    private float widthRatio;
    private DashboardCalcType calcType;
    private DashboardFetchType fetchType;
    private long updatePeriod;
    private int maxItems;
    private long maxAge;

    private boolean isCustom;

    private static class DataSourceMapping {
        private final String dataSourceProvider;
        private final String driverId;
        private final String driverClass;

        DataSourceMapping(IConfigurationElement config) {
            this.dataSourceProvider = config.getAttribute("id");
            this.driverId = config.getAttribute("driver");
            this.driverClass = config.getAttribute("driverClass");
        }

        DataSourceMapping(Element config) {
            this.dataSourceProvider = config.getAttribute("id");
            this.driverId = config.getAttribute("driver");
            this.driverClass = config.getAttribute("driverClass");
        }

        boolean matches(DBPDataSourceContainer dataSource) {
            if (this.dataSourceProvider != null && !this.dataSourceProvider.equals(dataSource.getDriver().getProviderId())) {
                return false;
            }
            if (this.driverId != null && !this.driverId.equals(dataSource.getDriver().getId())) {
                return false;
            }
            if (this.driverClass != null && !this.driverClass.equals(dataSource.getDriver().getDriverClassName())) {
                return false;
            }
            return true;
        }

        void serialize(XMLBuilder xml) throws IOException {
            if (dataSourceProvider != null) xml.addAttribute("id", dataSourceProvider);
            if (driverId != null) xml.addAttribute("driver", driverId);
            if (driverClass != null) xml.addAttribute("driverClass", driverClass);
        }
    }

    public static class QueryMapping implements DashboardQuery {
        private String queryText;

        QueryMapping(IConfigurationElement config) {
            this.queryText = config.getValue();
        }

        QueryMapping(Element config) {
            this.queryText = XMLUtils.getElementBody(config);
        }

        @Override
        public String getQueryText() {
            return queryText;
        }

        void serialize(XMLBuilder xml) throws IOException {
            xml.addText(queryText);
        }
    }

    DashboardDescriptor(
        DashboardRegistry registry,
        IConfigurationElement config)
    {
        super(config);

        this.id = config.getAttribute("id");
        this.name = config.getAttribute("label");
        this.description = config.getAttribute("description");
        this.group = config.getAttribute("group");
        this.tags = CommonUtils.notEmpty(config.getAttribute("tags")).split(",");
        this.showByDefault = CommonUtils.toBoolean(config.getAttribute("showByDefault"));

        this.type = registry.getDashboardType(config.getAttribute("type"));
        this.widthRatio = (float) CommonUtils.toDouble(config.getAttribute("ratio"), DashboardConstants.DEF_DASHBOARD_WIDTH_RATIO); // Default ratio is 2 to 3
        this.calcType = CommonUtils.valueOf(DashboardCalcType.class, config.getAttribute("calc"), DashboardConstants.DEF_DASHBOARD_CALC_TYPE);
        this.fetchType = CommonUtils.valueOf(DashboardFetchType.class, config.getAttribute("fetch"), DashboardConstants.DEF_DASHBOARD_FETCH_TYPE);
        this.updatePeriod = CommonUtils.toLong(config.getAttribute("updatePeriod"), DashboardConstants.DEF_DASHBOARD_UPDATE_PERIOD); // Default ratio is 2 to 3
        this.maxItems = CommonUtils.toInt(config.getAttribute("maxItems"), DashboardConstants.DEF_DASHBOARD_MAXIMUM_ITEM_COUNT);
        this.maxAge = CommonUtils.toLong(config.getAttribute("maxAge"), DashboardConstants.DEF_DASHBOARD_MAXIMUM_AGE);

        for (IConfigurationElement ds : config.getChildren("datasource")) {
            dataSourceMappings.add(new DataSourceMapping(ds));
        }

        for (IConfigurationElement ds : config.getChildren("query")) {
            queries.add(new QueryMapping(ds));
        }

        this.isCustom = false;
    }

    DashboardDescriptor(DashboardRegistry registry, Element config) {
        super(UIDashboardActivator.PLUGIN_ID);

        this.id = config.getAttribute("id");
        this.name = config.getAttribute("label");
        this.description = config.getAttribute("description");
        this.group = config.getAttribute("group");

        this.tags = CommonUtils.notEmpty(config.getAttribute("tags")).split(",");
        this.showByDefault = CommonUtils.toBoolean(config.getAttribute("showByDefault"));

        this.type = registry.getDashboardType(config.getAttribute("type"));
        this.widthRatio = (float) CommonUtils.toDouble(config.getAttribute("ratio"), DashboardConstants.DEF_DASHBOARD_WIDTH_RATIO);
        this.calcType = CommonUtils.valueOf(DashboardCalcType.class, config.getAttribute("calc"), DashboardConstants.DEF_DASHBOARD_CALC_TYPE);
        this.fetchType = CommonUtils.valueOf(DashboardFetchType.class, config.getAttribute("fetch"), DashboardConstants.DEF_DASHBOARD_FETCH_TYPE);
        this.updatePeriod = CommonUtils.toLong(config.getAttribute("updatePeriod"), DashboardConstants.DEF_DASHBOARD_UPDATE_PERIOD); // Default ratio is 2 to 3
        this.maxItems = CommonUtils.toInt(config.getAttribute("maxItems"), DashboardConstants.DEF_DASHBOARD_MAXIMUM_ITEM_COUNT);
        this.maxAge = CommonUtils.toLong(config.getAttribute("maxAge"), DashboardConstants.DEF_DASHBOARD_MAXIMUM_AGE);

        for (Element ds : XMLUtils.getChildElementList(config, "datasource")) {
            dataSourceMappings.add(new DataSourceMapping(ds));
        }

        for (Element ds : XMLUtils.getChildElementList(config, "query")) {
            queries.add(new QueryMapping(ds));
        }

        this.isCustom = true;
    }

    public DashboardDescriptor(DashboardDescriptor source) {
        super(source.getPluginId());

        this.id = source.id;
        this.name = source.name;
        this.description = source.description;
        this.group = source.group;
        this.tags = source.tags;
        this.showByDefault = source.showByDefault;

        this.type = source.type;
        this.widthRatio = source.widthRatio;
        this.calcType = source.calcType;
        this.fetchType = source.fetchType;
        this.updatePeriod = source.updatePeriod;
        this.maxItems = source.maxItems;
        this.maxAge = source.maxAge;

        this.dataSourceMappings.addAll(source.dataSourceMappings);

        this.queries.addAll(source.queries);

        this.isCustom = source.isCustom;
    }

    public DashboardDescriptor(String id, String name, String description, String group) {
        super(UIDashboardActivator.PLUGIN_ID);
        this.id = id;
        this.name = name;
        this.description = description;
        this.group = group;

        this.isCustom = true;
    }

    @NotNull
    public String getId() {
        return id;
    }

    @Override
    @NotNull
    public String getName()
    {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public boolean isShowByDefault() {
        return showByDefault;
    }

    public DashboardTypeDescriptor getType() {
        return type;
    }

    public void setType(DashboardTypeDescriptor type) {
        this.type = type;
    }

    public String[] getTags() {
        return tags;
    }

    public void setTags(String[] tags) {
        this.tags = tags;
    }

    public float getWidthRatio() {
        return widthRatio;
    }

    public void setWidthRatio(float widthRatio) {
        this.widthRatio = widthRatio;
    }

    public DashboardCalcType getCalcType() {
        return calcType;
    }

    public void setCalcType(DashboardCalcType calcType) {
        this.calcType = calcType;
    }

    public DashboardFetchType getFetchType() {
        return fetchType;
    }

    public void setFetchType(DashboardFetchType fetchType) {
        this.fetchType = fetchType;
    }

    public List<QueryMapping> getQueries() {
        return queries;
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

    public boolean isCustom() {
        return isCustom;
    }

    public boolean matches(DBPDataSourceContainer dataSource) {
        for (DataSourceMapping dsm : dataSourceMappings) {
            if (!dsm.matches(dataSource)) {
                return false;
            }
        }

        return true;
    }

    void serialize(XMLBuilder xml) throws IOException {
        xml.addAttribute("id", id);
        xml.addAttribute("label", name);
        xml.addAttribute("description", description);
        xml.addAttribute("group", group);

        xml.addAttribute("tags", String.join(",", tags));
        xml.addAttribute("showByDefault", showByDefault);

        xml.addAttribute("type", type.getId());
        xml.addAttribute("ratio", widthRatio);
        xml.addAttribute("calc", calcType.name());
        xml.addAttribute("fetch", fetchType.name());
        xml.addAttribute("updatePeriod", updatePeriod);
        xml.addAttribute("maxItems", maxItems);
        xml.addAttribute("maxAge", maxAge);

        for (DataSourceMapping mapping : dataSourceMappings) {
            xml.startElement("datasource");
            mapping.serialize(xml);
            xml.endElement();
        }

        for (QueryMapping qm : queries) {
            xml.startElement("query");
            qm.serialize(xml);
            xml.endElement();
        }

        this.isCustom = true;
    }

    @Override
    public String toString() {
        return id;
    }
}

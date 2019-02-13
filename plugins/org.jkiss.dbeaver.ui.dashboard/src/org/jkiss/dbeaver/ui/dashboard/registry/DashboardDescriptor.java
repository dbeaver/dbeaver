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

import org.apache.commons.jexl2.Expression;
import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderDescriptor;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.impl.AbstractContextDescriptor;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.dashboard.internal.UIDashboardActivator;
import org.jkiss.dbeaver.ui.dashboard.model.*;
import org.jkiss.utils.ArrayUtils;
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
    private static final Log log = Log.getLog(DashboardDescriptor.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.dashboard"; //$NON-NLS-1$

    private String id;
    private String name;
    private String description;
    private String group;
    private String measure;
    private boolean showByDefault;
    private DashboardViewTypeDescriptor defaultViewType;

    private DashboardMapQueryDescriptor mapQuery;
    private String mapKey;
    private String mapFormula;
    private Expression mapFormulaExpr;

    private String[] tags;
    private final List<DataSourceMapping> dataSourceMappings = new ArrayList<>();
    private final List<QueryMapping> queries = new ArrayList<>();

    private DashboardDataType dataType;
    private float widthRatio;
    private DashboardCalcType calcType;
    private DashboardFetchType fetchType;
    private long updatePeriod;
    private int maxItems;
    private long maxAge;

    private boolean isCustom;
    private DashboardValueType valueType;

    private static class DataSourceMapping {
        private final String dataSourceProvider;
        private final String driverId;
        private final String driverClass;

        DataSourceMapping(IConfigurationElement config) {
            this.dataSourceProvider = CommonUtils.nullIfEmpty(config.getAttribute("id"));
            this.driverId = CommonUtils.nullIfEmpty(config.getAttribute("driver"));
            this.driverClass = CommonUtils.nullIfEmpty(config.getAttribute("driverClass"));
        }

        DataSourceMapping(Element config) {
            this.dataSourceProvider = CommonUtils.nullIfEmpty(config.getAttribute("id"));
            this.driverId = CommonUtils.nullIfEmpty(config.getAttribute("driver"));
            this.driverClass = CommonUtils.nullIfEmpty(config.getAttribute("driverClass"));
        }

        public DataSourceMapping(String dataSourceProvider, String driverId, String driverClass) {
            this.dataSourceProvider = dataSourceProvider;
            this.driverId = driverId;
            this.driverClass = driverClass;
        }

        boolean matches(String providerId, String checkDriverId, String checkDriverClass) {
            if (this.dataSourceProvider != null && !this.dataSourceProvider.equals(providerId)) {
                return false;
            }
            if (checkDriverId != null && this.driverId != null && !this.driverId.equals(checkDriverId)) {
                return false;
            }
            if (checkDriverClass != null && this.driverClass != null && !this.driverClass.equals(checkDriverClass)) {
                return false;
            }
            return true;
        }

        void serialize(XMLBuilder xml) throws IOException {
            if (!CommonUtils.isEmpty(dataSourceProvider)) xml.addAttribute("id", dataSourceProvider);
            if (!CommonUtils.isEmpty(driverId)) xml.addAttribute("driver", driverId);
            if (!CommonUtils.isEmpty(driverClass)) xml.addAttribute("driverClass", driverClass);
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

        public QueryMapping(String queryText) {
            this.queryText = queryText;
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
        this.measure = config.getAttribute("measure");
        this.tags = CommonUtils.notEmpty(config.getAttribute("tags")).split(",");
        this.showByDefault = CommonUtils.toBoolean(config.getAttribute("showByDefault"));

        this.dataType = CommonUtils.valueOf(DashboardDataType.class, config.getAttribute("dataType"), DashboardConstants.DEF_DASHBOARD_DATA_TYPE);
        this.defaultViewType = registry.getViewType(config.getAttribute("defaultView"));
        if (this.defaultViewType == null) {
            this.defaultViewType = registry.getViewType(DashboardConstants.DEF_DASHBOARD_VIEW_TYPE);
        }
        this.widthRatio = (float) CommonUtils.toDouble(config.getAttribute("ratio"), DashboardConstants.DEF_DASHBOARD_WIDTH_RATIO); // Default ratio is 2 to 3
        this.calcType = CommonUtils.valueOf(DashboardCalcType.class, config.getAttribute("calc"), DashboardConstants.DEF_DASHBOARD_CALC_TYPE);
        this.valueType = CommonUtils.valueOf(DashboardValueType.class, config.getAttribute("value"), DashboardConstants.DEF_DASHBOARD_VALUE_TYPE);
        this.fetchType = CommonUtils.valueOf(DashboardFetchType.class, config.getAttribute("fetch"), DashboardConstants.DEF_DASHBOARD_FETCH_TYPE);
        this.updatePeriod = CommonUtils.toLong(config.getAttribute("updatePeriod"), DashboardConstants.DEF_DASHBOARD_UPDATE_PERIOD); // Default ratio is 2 to 3
        this.maxItems = CommonUtils.toInt(config.getAttribute("maxItems"), DashboardConstants.DEF_DASHBOARD_MAXIMUM_ITEM_COUNT);
        this.maxAge = CommonUtils.toLong(config.getAttribute("maxAge"), DashboardConstants.DEF_DASHBOARD_MAXIMUM_AGE);

        {
            String mapQueryId = config.getAttribute("mapQuery");
            if (!CommonUtils.isEmpty(mapQueryId)) {
                this.mapQuery = registry.getMapQuery(mapQueryId);
                if (this.mapQuery != null) {
                    this.mapKey = config.getAttribute("mapKey");
                    this.mapFormula = config.getAttribute("mapFormula");
                    if (!CommonUtils.isEmpty(this.mapFormula)) {
                        try {
                            this.mapFormulaExpr = AbstractDescriptor.parseExpression(this.mapFormula);
                        } catch (DBException e) {
                            log.warn(e);
                        }
                    }
                }
            }
        }

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
        this.measure = config.getAttribute("measure");

        this.tags = CommonUtils.notEmpty(config.getAttribute("tags")).split(",");
        this.showByDefault = CommonUtils.toBoolean(config.getAttribute("showByDefault"));

        this.dataType = CommonUtils.valueOf(DashboardDataType.class, config.getAttribute("dataType"), DashboardConstants.DEF_DASHBOARD_DATA_TYPE);
        this.defaultViewType = registry.getViewType(config.getAttribute("defaultView"));
        if (this.defaultViewType == null) {
            this.defaultViewType = registry.getViewType(DashboardConstants.DEF_DASHBOARD_VIEW_TYPE);
        }
        this.widthRatio = (float) CommonUtils.toDouble(config.getAttribute("ratio"), DashboardConstants.DEF_DASHBOARD_WIDTH_RATIO);
        this.calcType = CommonUtils.valueOf(DashboardCalcType.class, config.getAttribute("calc"), DashboardConstants.DEF_DASHBOARD_CALC_TYPE);
        this.valueType = CommonUtils.valueOf(DashboardValueType.class, config.getAttribute("value"), DashboardConstants.DEF_DASHBOARD_VALUE_TYPE);
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
        this.measure = source.measure;
        this.tags = source.tags;
        this.showByDefault = source.showByDefault;

        this.dataType = source.dataType;
        this.defaultViewType = source.defaultViewType;
        this.widthRatio = source.widthRatio;
        this.calcType = source.calcType;
        this.valueType = source.valueType;
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

        this.dataType = DashboardDataType.timeseries;
        this.defaultViewType = DashboardRegistry.getInstance().getViewType(DashboardConstants.DEF_DASHBOARD_VIEW_TYPE);
        this.widthRatio = DashboardConstants.DEF_DASHBOARD_WIDTH_RATIO;
        this.calcType = DashboardConstants.DEF_DASHBOARD_CALC_TYPE;
        this.valueType = DashboardConstants.DEF_DASHBOARD_VALUE_TYPE;
        this.fetchType = DashboardConstants.DEF_DASHBOARD_FETCH_TYPE;
        this.updatePeriod = DashboardConstants.DEF_DASHBOARD_UPDATE_PERIOD;
        this.maxItems = DashboardConstants.DEF_DASHBOARD_MAXIMUM_ITEM_COUNT;
        this.maxAge = DashboardConstants.DEF_DASHBOARD_MAXIMUM_AGE;

        this.isCustom = true;
    }

    @NotNull
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getMeasure() {
        return measure;
    }

    public void setMeasure(String measure) {
        this.measure = measure;
    }

    public boolean isShowByDefault() {
        return showByDefault;
    }

    public DashboardDataType getDataType() {
        return dataType;
    }

    public void setDataType(DashboardDataType dataType) {
        this.dataType = dataType;
    }

    public DashboardViewTypeDescriptor getDefaultViewType() {
        return defaultViewType;
    }

    public void setDefaultViewType(DashboardViewTypeDescriptor defaultViewType) {
        this.defaultViewType = defaultViewType;
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

    public DashboardValueType getValueType() {
        return valueType;
    }

    public void setValueType(DashboardValueType valueType) {
        this.valueType = valueType;
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

    public void setQueries(String[] queryStrings) {
        queries.clear();
        for (String qs : queryStrings) {
            queries.add(new QueryMapping(qs.trim()));
        }
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

    public DashboardMapQueryDescriptor getMapQuery() {
        return mapQuery;
    }

    public String getMapKey() {
        return mapKey;
    }

    public Expression getMapFormulaExpr() {
        return mapFormulaExpr;
    }

    public boolean isCustom() {
        return isCustom;
    }

    public void setCustom(boolean custom) {
        this.isCustom = custom;
    }

    public List<DBPNamedObject> getDataSourceMappings() {
        return getSupportedSources();
    }

    public void setDataSourceMappings(List<DBPNamedObject> targets) {
        dataSourceMappings.clear();
        for (DBPNamedObject target : targets) {
            if (target instanceof DBPDataSourceProviderDescriptor) {
                dataSourceMappings.add(
                    new DataSourceMapping(((DBPDataSourceProviderDescriptor) target).getId(), null, null));
            } else if (target instanceof DBPDriver) {
                DBPDriver driver = (DBPDriver)target;
                dataSourceMappings.add(new DataSourceMapping(driver.getProviderId(), driver.getId(), null));
            }
        }
    }

    public boolean matches(String providerId, String driverId, String driverClass) {
        for (DataSourceMapping dsm : dataSourceMappings) {
            if (!dsm.matches(providerId, driverId, driverClass)) {
                return false;
            }
        }

        return true;
    }

    public List<DBPNamedObject> getSupportedSources() {
        DBPPlatform platform = DBWorkbench.getPlatform();

        List<DBPNamedObject> results = new ArrayList<>();
        for (DataSourceMapping dsm : dataSourceMappings) {
            if (dsm.dataSourceProvider != null) {
                DBPDataSourceProviderDescriptor provider = platform.getDataSourceProviderRegistry().getDataSourceProvider(dsm.dataSourceProvider);
                if (provider != null) {
                    results.add(provider);
                }
            } else if (dsm.driverId != null) {
                DBPDriver driver = platform.getDataSourceProviderRegistry().findDriver(dsm.driverId);
                if (driver != null) {
                    results.add(driver);
                }
            }
        }
        return results;
    }

    void serialize(XMLBuilder xml) throws IOException {
        xml.addAttribute("id", id);
        xml.addAttribute("label", name);
        if (!CommonUtils.isEmpty(description)) {
            xml.addAttribute("description", description);
        }
        if (!CommonUtils.isEmpty(group)) {
            xml.addAttribute("group", group);
        }
        if (!CommonUtils.isEmpty(measure)) {
            xml.addAttribute("measure", measure);
        }

        if (!ArrayUtils.isEmpty(tags)) {
            xml.addAttribute("tags", String.join(",", tags));
        }
        xml.addAttribute("showByDefault", showByDefault);

        xml.addAttribute("viewType", defaultViewType.getId());
        xml.addAttribute("ratio", widthRatio);
        xml.addAttribute("calc", calcType.name());
        xml.addAttribute("value", valueType.name());
        xml.addAttribute("fetch", fetchType.name());
        xml.addAttribute("updatePeriod", updatePeriod);
        xml.addAttribute("maxItems", maxItems);
        xml.addAttribute("maxAge", maxAge);

        if (mapQuery != null) {
            xml.addAttribute("mapQuery", mapQuery.getId());
        }
        if (!CommonUtils.isEmpty(mapKey)) {
            xml.addAttribute("mapKey", mapKey);
        }
        if (!CommonUtils.isEmpty(mapFormula)) {
            xml.addAttribute("mapFormula", mapFormula);
        }

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

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
package org.jkiss.dbeaver.model.dashboard.registry;

import org.apache.commons.jexl3.JexlExpression;
import org.eclipse.core.runtime.IConfigurationElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderDescriptor;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.dashboard.*;
import org.jkiss.dbeaver.model.impl.AbstractContextDescriptor;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
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
public class DashboardDescriptor extends AbstractContextDescriptor implements DBDashboard {

    private static final Log log = Log.getLog(DashboardDescriptor.class);

    public static final String EXTENSION_ID = "org.jkiss.dbeaver.dashboard"; //$NON-NLS-1$

    private DashboardProviderDescriptor provider;

    private String id;
    private String name;
    private String description;
    private String group;
    private String measure;
    private boolean showByDefault;
    private String renderer;

    private DashboardMapQueryDescriptor mapQuery;
    private String[] mapKeys;
    private String[] mapLabels;
    private String mapFormula;
    private JexlExpression mapFormulaExpr;

    private String[] tags;
    private final List<DataSourceMapping> dataSourceMappings = new ArrayList<>();
    private final List<QueryMapping> queries = new ArrayList<>();

    private DBDashboardDataType dataType;
    private float widthRatio;
    private DBDashboardCalcType calcType;
    private DBDashboardFetchType fetchType;
    private long updatePeriod;
    private int maxItems;
    private long maxAge;

    private boolean isCustom;
    private DBDashboardValueType valueType;
    private DBDashboardInterval interval;

    public static class QueryMapping implements DBDashboardQuery {
        private final String queryText;

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

    public interface MapQueryProvider {
        DashboardMapQueryDescriptor getMapQuery(String id);
    }

    DashboardDescriptor(
        DashboardProviderDescriptor provider,
        MapQueryProvider mapQueryProvider,
        IConfigurationElement config
    ) {
        super(config);

        this.provider = provider;
        this.id = config.getAttribute("id");
        this.name = config.getAttribute("label");
        this.description = config.getAttribute("description");
        this.group = config.getAttribute("group");
        this.measure = config.getAttribute("measure");
        this.tags = CommonUtils.split(config.getAttribute("tags"), ",");
        this.showByDefault = CommonUtils.toBoolean(config.getAttribute("showByDefault"));

        this.dataType = CommonUtils.valueOf(DBDashboardDataType.class, config.getAttribute("dataType"), DashboardConstants.DEF_DASHBOARD_DATA_TYPE);
        this.renderer = config.getAttribute("defaultView");
        if (CommonUtils.isEmpty(this.renderer)) {
            this.renderer = DashboardConstants.DEF_DASHBOARD_VIEW_TYPE;
        }
        this.widthRatio = (float) CommonUtils.toDouble(config.getAttribute("ratio"), DashboardConstants.DEF_DASHBOARD_WIDTH_RATIO); // Default ratio is 2 to 3
        this.calcType = CommonUtils.valueOf(DBDashboardCalcType.class, config.getAttribute("calc"), DashboardConstants.DEF_DASHBOARD_CALC_TYPE);
        this.valueType = CommonUtils.valueOf(DBDashboardValueType.class, config.getAttribute("value"), DashboardConstants.DEF_DASHBOARD_VALUE_TYPE);
        this.interval = CommonUtils.valueOf(DBDashboardInterval.class, config.getAttribute("interval"), DashboardConstants.DEF_DASHBOARD_INTERVAL);
        this.fetchType = CommonUtils.valueOf(DBDashboardFetchType.class, config.getAttribute("fetch"), DashboardConstants.DEF_DASHBOARD_FETCH_TYPE);
        this.updatePeriod = CommonUtils.toLong(config.getAttribute("updatePeriod"), DashboardConstants.DEF_DASHBOARD_UPDATE_PERIOD); // Default ratio is 2 to 3
        this.maxItems = CommonUtils.toInt(config.getAttribute("maxItems"), DashboardConstants.DEF_DASHBOARD_MAXIMUM_ITEM_COUNT);
        this.maxAge = CommonUtils.toLong(config.getAttribute("maxAge"), DashboardConstants.DEF_DASHBOARD_MAXIMUM_AGE);

        {
            String mapQueryId = config.getAttribute("mapQuery");
            if (!CommonUtils.isEmpty(mapQueryId)) {
                this.mapQuery = mapQueryProvider.getMapQuery(mapQueryId);
                if (this.mapQuery != null) {
                    this.mapKeys = CommonUtils.split(config.getAttribute("mapKeys"), ",");
                    this.mapLabels = CommonUtils.split(config.getAttribute("mapLabels"), ",");
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
        super(DashboardConstants.DASHBOARDS_PLUGIN_ID);

        this.id = config.getAttribute("id");
        String providerId = config.getAttribute("provider");
        if (CommonUtils.isEmpty(providerId)) {
            providerId = DashboardConstants.DEF_DASHBOARD_PROVIDER;
        }
        this.provider = registry.getDashboardProvider(providerId);
        if (provider == null) {
            log.error("Dashboard provider '" + providerId + "' not found for saved dashboard '" + this.id + "'");
        }

        this.name = config.getAttribute("label");
        this.description = config.getAttribute("description");
        this.group = config.getAttribute("group");
        this.measure = config.getAttribute("measure");

        this.tags = CommonUtils.split(config.getAttribute("tags"), ",");
        this.showByDefault = CommonUtils.toBoolean(config.getAttribute("showByDefault"));

        this.dataType = CommonUtils.valueOf(DBDashboardDataType.class, config.getAttribute("dataType"), DashboardConstants.DEF_DASHBOARD_DATA_TYPE);
        this.renderer = config.getAttribute("defaultView");
        if (CommonUtils.isEmpty(this.renderer)) {
            this.renderer = DashboardConstants.DEF_DASHBOARD_VIEW_TYPE;
        }
        this.widthRatio = (float) CommonUtils.toDouble(config.getAttribute("ratio"), DashboardConstants.DEF_DASHBOARD_WIDTH_RATIO);
        this.calcType = CommonUtils.valueOf(DBDashboardCalcType.class, config.getAttribute("calc"), DashboardConstants.DEF_DASHBOARD_CALC_TYPE);
        this.valueType = CommonUtils.valueOf(DBDashboardValueType.class, config.getAttribute("value"), DashboardConstants.DEF_DASHBOARD_VALUE_TYPE);
        this.interval = CommonUtils.valueOf(DBDashboardInterval.class, config.getAttribute("interval"), DashboardConstants.DEF_DASHBOARD_INTERVAL);
        this.fetchType = CommonUtils.valueOf(DBDashboardFetchType.class, config.getAttribute("fetch"), DashboardConstants.DEF_DASHBOARD_FETCH_TYPE);
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
        this.provider = source.provider;
        this.name = source.name;
        this.description = source.description;
        this.group = source.group;
        this.measure = source.measure;
        this.tags = source.tags;
        this.showByDefault = source.showByDefault;

        this.dataType = source.dataType;
        this.renderer = source.renderer;
        this.widthRatio = source.widthRatio;
        this.calcType = source.calcType;
        this.valueType = source.valueType;
        this.interval = source.interval;
        this.fetchType = source.fetchType;
        this.updatePeriod = source.updatePeriod;
        this.maxItems = source.maxItems;
        this.maxAge = source.maxAge;

        this.dataSourceMappings.addAll(source.dataSourceMappings);

        this.queries.addAll(source.queries);

        this.isCustom = source.isCustom;
    }

    public DashboardDescriptor(DashboardProviderDescriptor provider, String id, String name, String description, String group) {
        super(DashboardConstants.DASHBOARDS_PLUGIN_ID);
        this.provider = provider;
        this.id = id;
        this.name = name;
        this.description = description;
        this.group = group;

        this.dataType = DBDashboardDataType.timeseries;
        this.renderer = DashboardConstants.DEF_DASHBOARD_VIEW_TYPE;
        this.widthRatio = DashboardConstants.DEF_DASHBOARD_WIDTH_RATIO;
        this.calcType = DashboardConstants.DEF_DASHBOARD_CALC_TYPE;
        this.valueType = DashboardConstants.DEF_DASHBOARD_VALUE_TYPE;
        this.interval = DashboardConstants.DEF_DASHBOARD_INTERVAL;
        this.fetchType = DashboardConstants.DEF_DASHBOARD_FETCH_TYPE;
        this.updatePeriod = DashboardConstants.DEF_DASHBOARD_UPDATE_PERIOD;
        this.maxItems = DashboardConstants.DEF_DASHBOARD_MAXIMUM_ITEM_COUNT;
        this.maxAge = DashboardConstants.DEF_DASHBOARD_MAXIMUM_AGE;

        this.isCustom = true;
    }

    @NotNull
    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    @NotNull
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
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

    public DBDashboardDataType getDataType() {
        return dataType;
    }

    public void setDataType(DBDashboardDataType dataType) {
        this.dataType = dataType;
    }

    @NotNull
    @Override
    public String getDashboardRenderer() {
        return renderer;
    }

    public void setRenderer(String renderer) {
        this.renderer = renderer;
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

    public DBDashboardCalcType getCalcType() {
        return calcType;
    }

    public void setCalcType(DBDashboardCalcType calcType) {
        this.calcType = calcType;
    }

    public DBDashboardValueType getValueType() {
        return valueType;
    }

    public void setValueType(DBDashboardValueType valueType) {
        this.valueType = valueType;
    }

    public DBDashboardInterval getInterval() {
        return interval;
    }

    public void setInterval(DBDashboardInterval interval) {
        this.interval = interval;
    }

    public DBDashboardFetchType getFetchType() {
        return fetchType;
    }

    public void setFetchType(DBDashboardFetchType fetchType) {
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

    public String[] getMapKeys() {
        return mapKeys;
    }

    public String[] getMapLabels() {
        return mapLabels;
    }

    public JexlExpression getMapFormulaExpr() {
        return mapFormulaExpr;
    }

    @Override
    public boolean isCustom() {
        return isCustom;
    }

    @NotNull
    @Override
    public DashboardProviderDescriptor getDashboardProvider() {
        return provider;
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
                DBPDriver driver = (DBPDriver) target;
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
        if (provider != null) {
            xml.addAttribute("provider", provider.getId());
        }
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

        xml.addAttribute("viewType", renderer);
        xml.addAttribute("ratio", widthRatio);
        xml.addAttribute("calc", calcType.name());
        xml.addAttribute("value", valueType.name());
        xml.addAttribute("interval", interval.name());
        xml.addAttribute("fetch", fetchType.name());
        xml.addAttribute("updatePeriod", updatePeriod);
        xml.addAttribute("maxItems", maxItems);
        xml.addAttribute("maxAge", maxAge);

        if (mapQuery != null) {
            xml.addAttribute("mapQuery", mapQuery.getId());
        }
        if (!ArrayUtils.isEmpty(mapKeys)) {
            xml.addAttribute("mapKeys", String.join(",", mapKeys));
        }
        if (!CommonUtils.isEmpty(mapFormula)) {
            xml.addAttribute("mapFormula", mapFormula);
        }

        for (DataSourceMapping mapping : dataSourceMappings) {
            try (var ignored = xml.startElement("datasource")) {
                mapping.serialize(xml);
            }
        }

        for (QueryMapping qm : queries) {
            try (var ignored = xml.startElement("query")) {
                qm.serialize(xml);
            }
        }

        this.isCustom = true;
    }

    @Override
    public String toString() {
        return id;
    }

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
                // Try finding a matching child provider
                DBPDataSourceProviderDescriptor provider = DBWorkbench.getPlatform()
                    .getDataSourceProviderRegistry()
                    .getDataSourceProvider(this.dataSourceProvider);
                boolean childHasMatch = false;
                if (provider != null) {
                    List<DBPDataSourceProviderDescriptor> childrenProviders = provider.getChildrenProviders();
                    childHasMatch = childrenProviders.stream().map(DBPDataSourceProviderDescriptor::getId).anyMatch(providerId::equals);
                }
                if (!childHasMatch) {
                    return false;
                }
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

}

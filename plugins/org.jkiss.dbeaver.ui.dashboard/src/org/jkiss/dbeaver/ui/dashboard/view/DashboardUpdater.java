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

import org.apache.commons.jexl2.JexlContext;
import org.eclipse.ui.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.dashboard.control.DashboardListViewer;
import org.jkiss.dbeaver.ui.dashboard.model.*;
import org.jkiss.dbeaver.ui.dashboard.model.data.DashboardDataset;
import org.jkiss.dbeaver.ui.dashboard.model.data.DashboardDatasetRow;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class DashboardUpdater {

    private static final Log log = Log.getLog(DashboardUpdater.class);
    private Map<DBPDataSourceContainer, List<MapQueryInfo>> mapQueries = new HashMap<>();

    private static class MapQueryInfo {
        private final DashboardViewContainer viewContainer;
        private final DashboardMapQuery mapQuery;
        public Date timestamp;
        private Map<String, Object> mapValue = new HashMap<>();

        public MapQueryInfo(DashboardViewContainer viewContainer, DashboardMapQuery mapQuery) {
            this.viewContainer = viewContainer;
            this.mapQuery = mapQuery;
        }
    }

    public DashboardUpdater() {
    }

    public void updateDashboards(DBRProgressMonitor monitor) {
        List<DashboardContainer> dashboards = getDashboardsToUpdate();

        updateDashboards(monitor, dashboards);

    }

    private void updateDashboards(DBRProgressMonitor monitor, List<DashboardContainer> dashboards) {
        // Get all map queries used by dashboards
        for (DashboardContainer dashboard : dashboards) {
            DashboardMapQuery mapQuery = dashboard.getMapQuery();
            if (mapQuery != null) {
                List<MapQueryInfo> queryList = mapQueries.computeIfAbsent(
                    dashboard.getDataSourceContainer(), k -> new ArrayList<>());
                boolean found = false;
                for (MapQueryInfo mqi : queryList) {
                    if (mqi.mapQuery == mapQuery) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    queryList.add(new MapQueryInfo(dashboard.getGroup().getView(), mapQuery));
                }
            }
        }

        for (Map.Entry<DBPDataSourceContainer, List<MapQueryInfo>> mqEntry : mapQueries.entrySet()) {
            DBPDataSourceContainer dsContainer = mqEntry.getKey();
            DBPDataSource dataSource = dsContainer.getDataSource();
            if (dataSource == null) {
                continue;
            }
            try {
                DBUtils.tryExecuteRecover(dashboards, dataSource, param -> {
                    try {
                        for (MapQueryInfo mqi : mqEntry.getValue()) {
                            readMapQueryData(monitor, mqi);
                        }
                    } catch (Throwable e) {
                        throw new InvocationTargetException(e);
                    }
                });
            } catch (DBException e) {
                log.debug("Error reading map query data for '" + dsContainer.getName() + "'", e);
            }
        }

        for (DashboardContainer dashboard : dashboards) {
            DBPDataSource dataSource = dashboard.getDataSourceContainer().getDataSource();
            if (dataSource == null) {
                continue;
            }
            try {
                DBUtils.tryExecuteRecover(dashboards, dataSource, param -> {
                    try {
                        updateDashboard(monitor, dashboard);
                    } catch (Throwable e) {
                        throw new InvocationTargetException(e);
                    }
                });
            } catch (DBException e) {
                log.debug("Error reading dashboard '" + dashboard.getDashboardId() + "' data", e);
            }
        }
    }

    private void readMapQueryData(DBRProgressMonitor monitor, MapQueryInfo mqInfo) throws DBCException {
        DBCExecutionContext executionContext = mqInfo.viewContainer.getExecutionContext();
        if (executionContext == null) {
            return;
        }
        try (DBCSession session = executionContext.openSession(
            monitor, DBCExecutionPurpose.UTIL, "Read map query '" + mqInfo.mapQuery.getId() + "' data")) {
            session.enableLogging(false);
            try (DBCStatement dbStat = session.prepareStatement(DBCStatementType.QUERY, mqInfo.mapQuery.getQueryText(), false, false, false)) {
                if (dbStat.executeStatement()) {
                    try (DBCResultSet dbResults = dbStat.openResultSet()) {
                        mqInfo.timestamp = new Date();
                        while (dbResults.nextRow()) {
                            String mapKey = CommonUtils.toString(dbResults.getAttributeValue(0));
                            Object mapValue = dbResults.getAttributeValue(1);
                            mqInfo.mapValue.put(mapKey, mapValue);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new DBCException("Error reading map query data", e);
        }
    }

    private void updateDashboard(DBRProgressMonitor monitor, DashboardContainer dashboard) throws DBCException {
        if (!dashboard.getDataSourceContainer().isConnected() || DBWorkbench.getPlatform().isShuttingDown()) {
            return;
        }

        if (dashboard.getMapQuery() != null) {
            fetchDashboardMapData(monitor, dashboard);
            return;
        }
        List<? extends DashboardQuery> queries = dashboard.getQueryList();
        if (queries.isEmpty()) {
            return;
        }
        DashboardViewContainer view = dashboard.getGroup().getView();
        DBCExecutionContext executionContext = view.getExecutionContext();
        if (executionContext == null) {
            return;
        }
        try (DBCSession session = executionContext.openSession(
            monitor, DBCExecutionPurpose.UTIL, "Read dashboard '" + dashboard.getDashboardTitle() + "' data")) {
            session.enableLogging(false);
            for (DashboardQuery query : queries) {
                try (DBCStatement dbStat = session.prepareStatement(DBCStatementType.QUERY, query.getQueryText(), false, false, false)) {
                    if (dbStat.executeStatement()) {
                        try (DBCResultSet dbResults = dbStat.openResultSet()) {
                            fetchDashboardData(dashboard, dbResults);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new DBCException("Error updating dashboard " + dashboard.getDashboardId(), e);
        }
    }

    private void fetchDashboardMapData(DBRProgressMonitor monitor, DashboardContainer dashboard) {
        MapQueryInfo mqi = getMapQueryData(dashboard);
        if (mqi == null) {
            return;
        }
        Map<String, Object> mapValue = mqi.mapValue;
        if (mapValue != null) {
            String[] mapKeys = dashboard.getMapKeys();
            String[] mapLabels = dashboard.getMapLabels();
            if (!ArrayUtils.isEmpty(mapKeys)) {
                if (ArrayUtils.isEmpty(mapLabels)) {
                    mapLabels = mapKeys;
                }
                DashboardDataset dataset = new DashboardDataset(mapLabels);
                Object[] mapValues = new Object[mapKeys.length];
                for (int i = 0; i < mapKeys.length; i++) {
                    Object value = mapValue.get(mapKeys[i]);
                    Number numValue;
                    if (value instanceof Number) {
                        numValue = (Number) value;
                    } else {
                        numValue = CommonUtils.toDouble(value);
                    }
                    mapValues[i] = numValue;
                }
                Date timestamp = mqi.timestamp;
                if (timestamp == null) {
                    timestamp = new Date();
                }
                dataset.addRow(new DashboardDatasetRow(timestamp, mapValues));
                dashboard.updateDashboardData(dataset);
            } else if (dashboard.getMapFormula() != null) {
                Map<String, Object> ciMap = new HashMap<>(mapValue.size());
                for (Map.Entry<String, Object> me : mapValue.entrySet()) {
                    ciMap.put(me.getKey().toLowerCase(Locale.ENGLISH), me.getValue());
                }
                JexlContext context = new JexlContext() {

                    @Override
                    public Object get(String name) {
                        if (name.equals("map")) {
                            return ciMap;
                        } else if (name.equals("dashboard")) {
                            return dashboard;
                        }
                        return null;
                    }

                    @Override
                    public void set(String name, Object value) {
                        log.warn("Set is not implemented in DBX model");
                    }

                    @Override
                    public boolean has(String name) {
                        return name.equals("object") || name.equals("dashboard");
                    }
                };

                Object result = dashboard.getMapFormula().evaluate(context);
                if (result instanceof Number) {
                    String columnName = dashboard.getDashboardTitle();
                    if (!ArrayUtils.isEmpty(mapLabels)) {
                        columnName = mapLabels[0];
                    }
                    DashboardDataset dataset = new DashboardDataset(new String[]{ columnName });
                    dataset.addRow(new DashboardDatasetRow(new Date(), new Object[] { result } ));
                    dashboard.updateDashboardData(dataset);
                } else {
                    log.debug("Wrong expression result: " + result);
                }
            }
        }
    }

    private void fetchDashboardData(DashboardContainer dashboard, DBCResultSet dbResults) throws DBCException {
        DBCResultSetMetaData meta = dbResults.getMeta();
        List<DBCAttributeMetaData> rsAttrs = meta.getAttributes();
        List<String> colNames = new ArrayList<>();
        String tsColName = null;
        for (DBCAttributeMetaData rsAttr : rsAttrs) {
            String colName = rsAttr.getLabel();
            if (CommonUtils.isEmpty(colName)) {
                colName = rsAttr.getName();
            }
            if (DashboardConstants.RS_COL_TIMESTAMP.equalsIgnoreCase(colName)) {
                tsColName = colName;
            } else {
                colNames.add(colName);
            }
        }
        DashboardDataset dataset = new DashboardDataset(colNames.toArray(new String[0]));

        while (dbResults.nextRow()) {
            Object[] values = new Object[colNames.size()];
            Date timestamp;
            if (tsColName != null) {
                timestamp = (Date) dbResults.getAttributeValue(tsColName);
            } else {
                timestamp = new Date();
            }
            for (int i = 0; i < colNames.size(); i++) {
                values[i] = dbResults.getAttributeValue(colNames.get(i));
            }
            dataset.addRow(new DashboardDatasetRow(timestamp, values));
            if (dataset.getRows().size() >= dashboard.getDashboardMaxItems()) {
                break;
            }
        }

        switch (dashboard.getDashboardFetchType()) {
            case rows:
                dataset = transposeDataset(dataset);
                break;
        }
        dashboard.updateDashboardData(dataset);
    }

    private DashboardDataset transposeDataset(DashboardDataset dataset) {
        int oldColumnCount = dataset.getColumnNames().length;
        if (oldColumnCount < 2) {
            // Something went wrong
            return dataset;
        }
        // Column names don't matter. Get everything from rows.
        // First column in row is actually column name. The rest are row values (usually 1)
        List<String> colNamesFromRows = new ArrayList<>();
        List<DashboardDatasetRow> oldRows = dataset.getRows();
        Date oldTimestamp = oldRows.get(0).getTimestamp();
        DashboardDatasetRow[] newRows = new DashboardDatasetRow[oldColumnCount - 1];

        for (int i = 0; i < oldRows.size(); i++) {
            DashboardDatasetRow oldRow = oldRows.get(i);
            colNamesFromRows.add(CommonUtils.toString(oldRow.getValues()[0], String.valueOf(i + 1)));
            for (int colIndex = 1; colIndex < oldColumnCount; colIndex++) {
                DashboardDatasetRow newRow = newRows[colIndex - 1];
                if (newRow == null) {
                    newRow = new DashboardDatasetRow(oldTimestamp, new Object[oldRows.size()]);
                    newRows[colIndex - 1] = newRow;
                }
                newRow.getValues()[i] = oldRow.getValues()[colIndex];
            }
        }

        DashboardDataset newDataset = new DashboardDataset(colNamesFromRows.toArray(new String[0]));
        for (DashboardDatasetRow newRow : newRows) {
            newDataset.addRow(newRow);
        }

        return newDataset;
    }

    public List<DashboardContainer> getDashboardsToUpdate() {
        List<DashboardContainer> dashboards = new ArrayList<>();
        for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
            for (IWorkbenchPage page : window.getPages()) {
                for (IViewReference view : page.getViewReferences()) {
                    if (view.getId().equalsIgnoreCase(DashboardView.VIEW_ID)) {
                        IWorkbenchPart part = view.getPart(false);
                        if (part instanceof DashboardView) {
                            getViewDashboards((DashboardView) part, dashboards);
                        }
                    }
                }
            }
        }
        return dashboards;
    }

    private void getViewDashboards(DashboardView view, List<DashboardContainer> dashboards) {
        long currentTime = System.currentTimeMillis();
        DashboardListViewer viewManager = view.getDashboardListViewer();
        if (viewManager == null || !viewManager.getDataSourceContainer().isConnected()) {
            return;
        }
        for (DashboardGroupContainer group : viewManager.getGroups()) {
            for (DashboardContainer dashboard : group.getItems()) {
                Date lastUpdateTime = dashboard.getLastUpdateTime();
                if (lastUpdateTime == null || (currentTime - lastUpdateTime.getTime()) >= dashboard.getUpdatePeriod()) {
                    dashboards.add(dashboard);
                }
            }
        }
    }

    private MapQueryInfo getMapQueryData(DashboardContainer dashboard) {
        List<MapQueryInfo> mapQueryInfos = mapQueries.get(dashboard.getDataSourceContainer());
        if (mapQueryInfos != null) {
            for (MapQueryInfo mqi : mapQueryInfos) {
                if (mqi.mapQuery == dashboard.getMapQuery()) {
                    return mqi;
                }
            }
        }
        return null;
    }

}
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
package org.jkiss.dbeaver.ui.dashboard.view;

import org.apache.commons.jexl3.JexlContext;
import org.eclipse.ui.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.dashboard.DBDashboardDataType;
import org.jkiss.dbeaver.model.dashboard.DBDashboardMapQuery;
import org.jkiss.dbeaver.model.dashboard.DBDashboardQuery;
import org.jkiss.dbeaver.model.dashboard.DashboardConstants;
import org.jkiss.dbeaver.model.dashboard.data.DashboardDataset;
import org.jkiss.dbeaver.model.dashboard.data.DashboardDatasetRow;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.dashboard.control.DashboardListViewer;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardContainer;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardGroupContainer;
import org.jkiss.dbeaver.ui.dashboard.model.DashboardItemContainer;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class DashboardUpdater {

    private static final Log log = Log.getLog(DashboardUpdater.class);
    private final Map<DBPDataSourceContainer, List<MapQueryInfo>> mapQueries = new HashMap<>();

    private static class MapQueryInfo {
        private final DashboardItemContainer dashboard;
        private final DashboardContainer viewContainer;
        private final DBDashboardMapQuery mapQuery;
        public Date timestamp;
        private final Map<String, Object> mapValue = new HashMap<>();

        public MapQueryInfo(DashboardItemContainer dashboard, DashboardContainer viewContainer, DBDashboardMapQuery mapQuery) {
            this.dashboard = dashboard;
            this.viewContainer = viewContainer;
            this.mapQuery = mapQuery;
        }
    }

    public DashboardUpdater() {
    }

    /**
     * 
     * @return true if need pause to update dashboard, false if not
     */
    public boolean updateDashboards(@NotNull DBRProgressMonitor monitor) {
        List<DashboardItemContainer> dashboards = new ArrayList<>();
        if (getDashboardsToUpdate(dashboards)) {
            return true;
        }

        updateDashboards(monitor, dashboards);
        
        return false;
    }

    private void updateDashboards(@NotNull DBRProgressMonitor monitor, @NotNull List<DashboardItemContainer> dashboards) {
        monitor.beginTask("Update dashboards", dashboards.size());

        // Get all map queries used by dashboards
        for (DashboardItemContainer dashboard : dashboards) {
            DBDashboardMapQuery mapQuery = dashboard.getMapQuery();
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
                    queryList.add(new MapQueryInfo(dashboard, dashboard.getGroup().getView(), mapQuery));
                }
            }
        }

        for (Map.Entry<DBPDataSourceContainer, List<MapQueryInfo>> mqEntry : mapQueries.entrySet()) {
            monitor.subTask("Read dashboard data");
            DBPDataSourceContainer dsContainer = mqEntry.getKey();
            DBPDataSource dataSource = dsContainer.getDataSource();
            if (dataSource == null) {
                continue;
            }
            try {
                DBExecUtils.tryExecuteRecover(dashboards, dataSource, param -> {
                    try {
                        for (MapQueryInfo mqi : mqEntry.getValue()) {
                            if (!mqi.dashboard.isAutoUpdateEnabled()) {
                                continue;
                            }

                            try {
                                readMapQueryData(monitor, mqi);
                            } catch (DBCException e) {
                                log.debug("Datasource '" + mqi.dashboard.getDataSourceContainer().getName() + "' dashboard query failed. Stopping update of dashboard queries for this datasource.");
                                mqi.dashboard.disableAutoUpdate();
                                throw e;
                            }
                        }
                    } catch (Throwable e) {
                        throw new InvocationTargetException(e);
                    }
                });
            } catch (DBException e) {
                log.debug("Error reading map query data for '" + dsContainer.getName() + "'", e);
            }
        }

        for (DashboardItemContainer dashboard : dashboards) {
            if (!dashboard.isAutoUpdateEnabled()) {
                continue;
            }
            DBPDataSource dataSource = dashboard.getDataSourceContainer().getDataSource();
            if (dataSource == null) {
                continue;
            }
            try {
                DBExecUtils.tryExecuteRecover(dashboards, dataSource, param -> {
                    try {
                        updateDashboard(monitor, dashboard);
                    } catch (Throwable e) {
                        log.debug("Datasource '" + dashboard.getDataSourceContainer().getName() + "' dashboard query failed. Stopping update of dashboards for this datasource.");
                        dashboard.disableAutoUpdate();
                        throw new InvocationTargetException(e);
                    }
                });
            } catch (DBException e) {
                log.debug("Error reading dashboard '" + dashboard.getItemDescriptor().getId() + "' data: " + CommonUtils.getRootCause(e).getMessage());
            }
            monitor.worked(1);
        }
        monitor.done();
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

    private void updateDashboard(DBRProgressMonitor monitor, DashboardItemContainer dashboard) throws DBCException {
        if (!dashboard.getDataSourceContainer().isConnected() || DBWorkbench.getPlatform().isShuttingDown()) {
            return;
        }

        if (dashboard.getMapQuery() != null) {
            fetchDashboardMapData(monitor, dashboard);
            return;
        }
        List<? extends DBDashboardQuery> queries = dashboard.getQueryList();
        if (queries.isEmpty()) {
            return;
        }
        DashboardContainer view = dashboard.getGroup().getView();
        DBCExecutionContext executionContext = view.getExecutionContext();
        if (executionContext == null) {
            return;
        }
        try (DBCSession session = executionContext.openSession(
            monitor, DBCExecutionPurpose.UTIL, "Read dashboard '" + dashboard.getItemDescriptor().getName() + "' data")) {
            session.enableLogging(false);

            DBCTransactionManager txnManager = DBUtils.getTransactionManager(session.getExecutionContext());
            boolean revertTxn = false;
            if (false) {
                // FIXME: dashboards must be queued in auto-commit mode?
                // FIXME: we can't switch to auto-commit because connection may be used by another tasks (e.g. SQL editor)
                if (txnManager != null && txnManager.isSupportsTransactions() && !txnManager.isAutoCommit()) {
                    txnManager.setAutoCommit(monitor, true);
                    revertTxn = true;
                }
            }
            try {
                for (DBDashboardQuery query : queries) {
                    try (DBCStatement dbStat = session.prepareStatement(DBCStatementType.QUERY, query.getQueryText(), false, false, false)) {
                        if (dbStat.executeStatement()) {
                            try (DBCResultSet dbResults = dbStat.openResultSet()) {
                                if (dbResults != null) {
                                    fetchDashboardData(dashboard, dbResults);
                                }
                            }
                        }
                    } catch (Exception e) {
                        throw new DBCException("Error updating dashboard " + dashboard.getItemDescriptor().getId(), e, session.getExecutionContext());
                    }
                }
            } finally {
                if (revertTxn) {
                    txnManager.setAutoCommit(monitor, false);
                }
            }
        }
    }

    private void fetchDashboardMapData(DBRProgressMonitor monitor, DashboardItemContainer dashboard) {
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
                    if (value instanceof Number number) {
                        numValue = number;
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
                    String columnName = dashboard.getItemDescriptor().getName();
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

    private void fetchDashboardData(DashboardItemContainer dashboardContainer, DBCResultSet dbResults) throws DBCException {
        DBCResultSetMetaData meta = dbResults.getMeta();
        List<? extends DBCAttributeMetaData> rsAttrs = meta.getAttributes();
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
            if (dataset.getRows().size() >= dashboardContainer.getDashboardMaxItems()) {
                break;
            }
        }

        switch (dashboardContainer.getItemDescriptor().getFetchType()) {
            case rows:
                dataset = transposeDataset(dataset);
                break;
        }
        dashboardContainer.updateDashboardData(dataset);
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

    public boolean getDashboardsToUpdate(List<DashboardItemContainer> dashboards) {
        boolean pauseDashboardUpdate = true;
        for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
            for (IWorkbenchPage page : window.getPages()) {
                for (IViewReference view : page.getViewReferences()) {
                    if (view.getId().equalsIgnoreCase(DataSourceDashboardView.VIEW_ID)) {
                        IWorkbenchPart part = view.getPart(false);
                        if (part instanceof DataSourceDashboardView dv && checkViewDashboards(dv)) {
                            getViewDashboards(dv, dashboards);
                            pauseDashboardUpdate = false;
                        }
                    }
                }
            }
        }
        return pauseDashboardUpdate;
    }
    
    private boolean checkViewDashboards(DataSourceDashboardView view) {
        DashboardListViewer viewManager = view.getDashboardListViewer();
        return viewManager != null && viewManager.getDataSourceContainer().isConnected();
    }

    private void getViewDashboards(DataSourceDashboardView view, List<DashboardItemContainer> dashboards) {
        long currentTime = System.currentTimeMillis();
        DashboardListViewer viewManager = view.getDashboardListViewer();
        for (DashboardGroupContainer group : viewManager.getGroups()) {
            for (DashboardItemContainer dashboardContainer : group.getItems()) {
                if (dashboardContainer.getItemDescriptor().getDataType() == DBDashboardDataType.provided) {
                    // Skip all provided
                    continue;
                }
                Date lastUpdateTime = dashboardContainer.getLastUpdateTime();
                if (lastUpdateTime == null || (currentTime - lastUpdateTime.getTime()) >= dashboardContainer.getUpdatePeriod()) {
                    dashboards.add(dashboardContainer);
                }
            }
        }
    }

    private MapQueryInfo getMapQueryData(DashboardItemContainer dashboard) {
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
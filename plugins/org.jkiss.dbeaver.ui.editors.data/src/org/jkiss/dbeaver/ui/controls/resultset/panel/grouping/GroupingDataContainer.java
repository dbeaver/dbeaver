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
package org.jkiss.dbeaver.ui.controls.resultset.panel.grouping;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;

public class GroupingDataContainer implements DBSDataContainer {

    private static final Log log = Log.getLog(GroupingDataContainer.class);

    private IResultSetController parentController;
    private String query;

    public GroupingDataContainer(IResultSetController parentController) {
        this.parentController = parentController;
    }

    @Override
    public DBSObject getParentObject() {
        return parentController.getDataContainer();
    }

    @Override
    public String getName() {
        return "Grouping";
    }

    @Override
    public String getDescription() {
        return "Grouping data";
    }

    @Override
    public DBPDataSource getDataSource() {
        return parentController.getDataContainer().getDataSource();
    }

    @Override
    public int getSupportedFeatures() {
        return DATA_SELECT;
    }

    @Override
    public DBCStatistics readData(DBCExecutionSource source, DBCSession session, DBDDataReceiver dataReceiver, DBDDataFilter dataFilter, long firstRow, long maxRows, long flags) throws DBCException {
        DBCStatistics statistics = new DBCStatistics();
        boolean hasLimits = firstRow >= 0 && maxRows > 0;

        DBRProgressMonitor monitor = session.getProgressMonitor();

        StringBuilder sqlQuery = new StringBuilder(this.query);
        SQLUtils.appendQueryOrder(getDataSource(), sqlQuery, null, dataFilter);

        statistics.setQueryText(sqlQuery.toString());
        statistics.addStatementsCount();

        monitor.subTask(ModelMessages.model_jdbc_fetch_table_data);

        try (DBCStatement dbStat = DBUtils.makeStatement(
            source,
            session,
            DBCStatementType.SCRIPT,
            sqlQuery.toString(),
            firstRow,
            maxRows))
        {
            if (monitor.isCanceled()) {
                return statistics;
            }
            long startTime = System.currentTimeMillis();
            boolean executeResult = dbStat.executeStatement();
            statistics.setExecuteTime(System.currentTimeMillis() - startTime);
            if (executeResult) {
                try (DBCResultSet dbResult = dbStat.openResultSet()) {
                    try {
                        dataReceiver.fetchStart(session, dbResult, firstRow, maxRows);

                        startTime = System.currentTimeMillis();
                        long rowCount = 0;
                        while (dbResult.nextRow()) {
                            if (monitor.isCanceled() || (hasLimits && rowCount >= maxRows)) {
                                // Fetch not more than max rows
                                break;
                            }
                            dataReceiver.fetchRow(session, dbResult);
                            rowCount++;
                        }
                        statistics.setFetchTime(System.currentTimeMillis() - startTime);
                        statistics.setRowsFetched(rowCount);
                    } finally {
                        // Signal that fetch was ended
                        try {
                            dataReceiver.fetchEnd(session, dbResult);
                        } catch (Throwable e) {
                            log.error("Error while finishing result set fetch", e); //$NON-NLS-1$
                        }
                    }
                }
            }
            return statistics;
        } finally {
            dataReceiver.close();
        }
    }

    @Override
    public long countData(DBCExecutionSource source, DBCSession session, DBDDataFilter dataFilter, long flags) throws DBCException {
        return 0;
    }

    @Override
    public boolean isPersisted() {
        return false;
    }

    public void setGroupingQuery(String sql) {
        this.query = sql;
    }
}

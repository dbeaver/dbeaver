/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLGroupingAttribute;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.Pair;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GroupingDataContainer implements DBDAttributeTransformerProvider, DBSDataContainer {

    private static final Log log = Log.getLog(GroupingDataContainer.class);

    private IResultSetController parentController;
    private String query;
    private SQLGroupingAttribute[] attributes;
    @Nullable
    private Pair<Integer, DBDAttributeTransformer> attributeBindingNumberToTransformer;

    public GroupingDataContainer(IResultSetController parentController) {
        this.parentController = parentController;
    }

    @Override
    public DBSObject getParentObject() {
        return parentController.getDataContainer();
    }

    @NotNull
    @Override
    public String getName() {
        if (ArrayUtils.isEmpty(this.attributes)) {
            return "Grouping";
        } else {
            return Arrays.stream(this.attributes).map(SQLGroupingAttribute::getDisplayName).collect(Collectors.joining(","));
        }
    }

    @Override
    public String getDescription() {
        return "Grouping data";
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource() {
        return parentController.getDataContainer().getDataSource();
    }

    @NotNull
    @Override
    public String[] getSupportedFeatures() {
        return new String[] {FEATURE_DATA_SELECT, FEATURE_DATA_FILTER};
    }

    @NotNull
    @Override
    public DBCStatistics readData(
        @Nullable DBCExecutionSource source,
        @NotNull DBCSession session,
        @NotNull DBDDataReceiver dataReceiver,
        DBDDataFilter dataFilter,
        long firstRow,
        long maxRows,
        long flags,
        int fetchSize
    ) throws DBException {
        DBCStatistics statistics = new DBCStatistics();
        if (query == null) {
            statistics.addMessage("Empty query");
            return statistics;
        }
        boolean hasLimits = firstRow >= 0 && maxRows > 0;

        DBRProgressMonitor monitor = session.getProgressMonitor();

        StringBuilder sqlQuery = new StringBuilder(this.query);
        DBPDataSource dataSource = getDataSource();
        if (dataSource != null) {
            SQLUtils.appendQueryOrder(dataSource, sqlQuery, null, dataFilter);
        }

        String sql = sqlQuery.toString();
        if (dataSource != null && dataFilter.hasConditions()) {
            sqlQuery.setLength(0);
            String gbAlias = "gbq_";
            try {
                SQLUtils.appendQueryConditions(dataSource, sqlQuery, gbAlias, dataFilter);
            } catch (DBException e) {
                throw new DBCException("Can't generate query conditions", e, session.getExecutionContext());
            }
            sql = "SELECT * FROM (" + sql + ") " + gbAlias + " " + sqlQuery;
        }

        statistics.setQueryText(sql);
        statistics.addStatementsCount();

        monitor.subTask(ModelMessages.model_jdbc_fetch_table_data);

        try (DBCStatement dbStat = DBUtils.makeStatement(
            source,
            session,
            DBCStatementType.SCRIPT,
            sql,
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
                    DBDDataReceiver.startFetchWorkflow(dataReceiver, session, dbResult, firstRow, maxRows);

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
                }
            }
            return statistics;
        } finally {
            dataReceiver.close();
        }
    }

    @Override
    public long countData(@NotNull DBCExecutionSource source, @NotNull DBCSession session, @Nullable DBDDataFilter dataFilter, long flags) throws DBCException {
        return 0;
    }

    @Override
    public boolean isPersisted() {
        return false;
    }

    public void setGroupingQuery(String sql) {
        this.query = sql;
    }

    public void setGroupingAttributes(@Nullable SQLGroupingAttribute[] attributes) {
        this.attributes = attributes;
    }

    @NotNull
    @Override
    public List<DBDAttributeTransformer> findTransformerForBinding(@NotNull DBDAttributeBinding attributeBinding) {
        DBDAttributeTransformer transformer = attributeBindingNumberToTransformer != null
            && attributeBindingNumberToTransformer.getFirst().equals(attributeBinding.getOrdinalPosition())
            ? attributeBindingNumberToTransformer.getSecond()
            : null;
        return transformer != null ? Collections.singletonList(transformer) : Collections.emptyList();
    }

    public void setAttributeTransformer(int attributeIndex, @NotNull DBDAttributeTransformer transformer) {
        attributeBindingNumberToTransformer = Pair.of(attributeIndex, transformer);
    }

    public void removeAttributeTransformer() {
        attributeBindingNumberToTransformer = null;
    }

    @Nullable
    public SQLGroupingAttribute[] getGroupingAttributes() {
        return this.attributes;
    }

    @Override
    public String toString() {
        return getName();
    }
}

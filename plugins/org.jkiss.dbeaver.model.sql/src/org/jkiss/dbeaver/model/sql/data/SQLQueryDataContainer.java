/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sql.data;

import org.eclipse.jface.text.Document;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.*;
import org.jkiss.dbeaver.model.sql.parser.SQLParserContext;
import org.jkiss.dbeaver.model.sql.parser.SQLRuleManager;
import org.jkiss.dbeaver.model.sql.parser.SQLScriptParser;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

/**
 * Data container for single SQL query.
 * Doesn't support multiple resulsets.
 */
public class SQLQueryDataContainer implements DBSDataContainer, SQLQueryContainer, DBPContextProvider, DBPImageProvider {

    private DBPContextProvider contextProvider;
    private SQLQuery query;
    private SQLScriptContext scriptContext;
    private Log log;

    public SQLQueryDataContainer(DBPContextProvider contextProvider, SQLQuery query, SQLScriptContext scriptContext, Log log) {
        this.contextProvider = contextProvider;
        this.query = query;
        this.scriptContext = scriptContext;
        this.log = log;
    }

    @Override
    public DBCExecutionContext getExecutionContext() {
        return contextProvider.getExecutionContext();
    }

    @Override
    public int getSupportedFeatures() {
        return DATA_SELECT;
    }

    public SQLScriptContext getScriptContext() {
        return scriptContext;
    }

    @NotNull
    @Override
    public DBCStatistics readData(@NotNull DBCExecutionSource source, @NotNull DBCSession session, @NotNull DBDDataReceiver dataReceiver, DBDDataFilter dataFilter, long firstRow, long maxRows, long flags, int fetchSize) throws DBCException
    {
        DBCStatistics statistics = new DBCStatistics();
        // Modify query (filters + parameters)
        DBPDataSource dataSource = session.getDataSource();
        SQLQuery sqlQuery = query;
        String queryText = sqlQuery.getOriginalText();//.trim();
        if (dataFilter != null && dataFilter.hasFilters()) {
            String filteredQueryText = dataSource.getSQLDialect().addFiltersToQuery(
                session.getProgressMonitor(),
                dataSource, queryText, dataFilter);
            sqlQuery = new SQLQuery(dataSource, filteredQueryText, sqlQuery);
        } else {
            sqlQuery = new SQLQuery(dataSource, queryText, sqlQuery);
        }

        if (scriptContext != null) {
            SQLSyntaxManager syntaxManager = new SQLSyntaxManager();
            syntaxManager.init(dataSource);
            SQLRuleManager ruleManager = new SQLRuleManager(syntaxManager);
            ruleManager.loadRules(dataSource, false);
            SQLParserContext parserContext = new SQLParserContext(this, syntaxManager, ruleManager, new Document(query.getOriginalText()));
            sqlQuery.setParameters(SQLScriptParser.parseParameters(parserContext, 0, sqlQuery.getLength()));
            if (!scriptContext.fillQueryParameters(sqlQuery, CommonUtils.isBitSet(flags, DBSDataContainer.FLAG_REFRESH))) {
                // User canceled
                return statistics;
            }
        }

        final SQLQueryResult curResult = new SQLQueryResult(sqlQuery);
        if (firstRow > 0) {
            curResult.setRowOffset(firstRow);
        }
        statistics.setQueryText(sqlQuery.getText());

        long startTime = System.currentTimeMillis();

        try (final DBCStatement dbcStatement = DBUtils.makeStatement(
            source,
            session,
            DBCStatementType.SCRIPT,
            sqlQuery,
            firstRow,
            maxRows))
        {
            DBExecUtils.setStatementFetchSize(dbcStatement, firstRow, maxRows, fetchSize);

            // Execute statement

            session.getProgressMonitor().subTask("Execute query");

            boolean hasResultSet = dbcStatement.executeStatement();

            statistics.addExecuteTime(System.currentTimeMillis() - startTime);
            statistics.addStatementsCount();

            curResult.setHasResultSet(hasResultSet);

            if (hasResultSet) {
                DBCResultSet resultSet = dbcStatement.openResultSet();
                if (resultSet != null) {
                    SQLQueryResult.ExecuteResult executeResult = curResult.addExecuteResult(true);
                    DBRProgressMonitor monitor = session.getProgressMonitor();
                    monitor.subTask("Fetch result set");
                    DBFetchProgress fetchProgress = new DBFetchProgress(session.getProgressMonitor());

                    dataReceiver.fetchStart(session, resultSet, firstRow, maxRows);

                    try {
                        long fetchStartTime = System.currentTimeMillis();

                        // Fetch all rows
                        while (!fetchProgress.isMaxRowsFetched(maxRows) && !fetchProgress.isCanceled() && resultSet.nextRow()) {
                            dataReceiver.fetchRow(session, resultSet);
                            fetchProgress.monitorRowFetch();
                        }
                        statistics.addFetchTime(System.currentTimeMillis() - fetchStartTime);
                    }
                    finally {
                        try {
                            resultSet.close();
                        } catch (Throwable e) {
                            log.error("Error while closing resultset", e);
                        }
                        try {
                            dataReceiver.fetchEnd(session, resultSet);
                        } catch (Throwable e) {
                            log.error("Error while handling end of result set fetch", e);
                        }
                        dataReceiver.close();
                    }

                    if (executeResult != null) {
                        executeResult.setRowCount(fetchProgress.getRowCount());
                    }
                    statistics.setRowsFetched(fetchProgress.getRowCount());
                    monitor.subTask(fetchProgress.getRowCount() + " rows fetched");
                }
            } else {
                log.warn("No results returned by query execution");
            }
            try {
                curResult.addWarnings(dbcStatement.getStatementWarnings());
            } catch (Throwable e) {
                log.warn("Can't read execution warnings", e);
            }
        }

        return statistics;
    }

    @Override
    public long countData(@NotNull DBCExecutionSource source, @NotNull DBCSession session, DBDDataFilter dataFilter, long flags)
        throws DBCException
    {
        return -1;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return "SQL Query";
    }

    @Nullable
    @Override
    public DBSObject getParentObject()
    {
        return getDataSource();
    }

    @Nullable
    @Override
    public DBPDataSource getDataSource()
    {
        DBCExecutionContext executionContext = getExecutionContext();
        return executionContext == null ? null : executionContext.getDataSource();
    }

    @Override
    public boolean isPersisted() {
        return false;
    }

    @NotNull
    @Override
    public String getName()
    {
        String name = query.getOriginalText();
        if (name == null) {
            name = "SQL";
        }
        return name;
    }

    @Nullable
    @Override
    public DBPDataSourceContainer getDataSourceContainer() {
        DBPDataSource dataSource = getDataSource();
        return dataSource == null ? null : dataSource.getContainer();
    }

    @Override
    public String toString() {
        return query.getOriginalText();
    }

    @Override
    public SQLScriptElement getQuery() {
        return query;
    }

    @Override
    public Map<String, Object> getQueryParameters() {
        return scriptContext.getAllParameters();
    }

    @Nullable
    @Override
    public DBPImage getObjectImage() {
        return DBIcon.TREE_FILE;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SQLQueryDataContainer &&
            CommonUtils.equalObjects(query, ((SQLQueryDataContainer) obj).query);
    }
}

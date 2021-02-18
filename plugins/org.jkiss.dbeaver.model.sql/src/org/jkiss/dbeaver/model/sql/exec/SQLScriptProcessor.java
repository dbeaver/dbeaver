/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sql.exec;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBFetchProgress;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.AbstractExecutionSource;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.*;
import org.jkiss.dbeaver.model.sql.data.SQLQueryDataContainer;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * SQLScriptProcessor
 */
public class SQLScriptProcessor {
    private static final String STAT_LOG_PREFIX = "-----------------> ";

    private final DBCExecutionContext executionContext;
    private final List<SQLScriptElement> queries;
    private final SQLScriptContext scriptContext;
    private final DBDDataReceiver dataReceiver;
    private final Log log;

    private Throwable lastError = null;

    private DBCStatistics statistics;

    private int fetchSize;
    private long fetchFlags;
    private SQLScriptCommitType commitType = SQLScriptCommitType.AUTOCOMMIT;
    private SQLScriptErrorHandling errorHandling = SQLScriptErrorHandling.STOP_ROLLBACK;

    public SQLScriptProcessor(
        @NotNull DBCExecutionContext executionContext,
        @NotNull List<SQLScriptElement> queries,
        @NotNull SQLScriptContext scriptContext,
        @NotNull DBDDataReceiver dataReceiver,
        @NotNull Log log) {
        this.executionContext = executionContext;
        this.queries = queries;
        this.scriptContext = scriptContext;
        this.dataReceiver = dataReceiver;
        this.log = log;
    }

    public void setFetchSize(int fetchSize) {
        this.fetchSize = fetchSize;
    }

    public void setFetchFlags(long fetchFlags) {
        this.fetchFlags = fetchFlags;
    }

    public SQLScriptCommitType getCommitType() {
        return commitType;
    }

    public void setCommitType(SQLScriptCommitType commitType) {
        this.commitType = commitType;
    }

    public SQLScriptErrorHandling getErrorHandling() {
        return errorHandling;
    }

    public void setErrorHandling(SQLScriptErrorHandling errorHandling) {
        this.errorHandling = errorHandling;
    }

    public void runScript(DBRProgressMonitor monitor) throws DBCException {
        RuntimeUtils.setThreadName("SQL script execution");
        statistics = new DBCStatistics();
        try {
            DBCTransactionManager txnManager = DBUtils.getTransactionManager(executionContext);
            try (DBCSession session = executionContext.openSession(monitor, DBCExecutionPurpose.USER_SCRIPT, "SQL Query")) {
                // Set transaction settings (only if autocommit is off)
                if (session.isLoggingEnabled()) {
                    QMUtils.getDefaultHandler().handleScriptBegin(session);
                }

                boolean oldAutoCommit = txnManager == null || txnManager.isAutoCommit();
                boolean newAutoCommit = (commitType == SQLScriptCommitType.AUTOCOMMIT);
                if (txnManager != null && txnManager.isSupportsTransactions() && oldAutoCommit != newAutoCommit) {
                    txnManager.setAutoCommit(monitor, newAutoCommit);
                }

                monitor.beginTask("Execute queries (" + queries.size() + ")", queries.size());

                for (SQLScriptElement query : queries) {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    // Execute query
                    boolean runNext = executeSingleQuery(session, query);
                    if (!runNext) {
                        if (lastError == null) {
                            // Execution cancel
                            break;
                        }
                        if (errorHandling != SQLScriptErrorHandling.IGNORE) {
                            log.error(lastError);
                            break;
                        } else {
                            log.warn("Query failed: " + lastError.getMessage());
                        }
                    }

                    monitor.worked(1);
                }
                monitor.done();

                // Commit data
                if (txnManager != null && txnManager.isSupportsTransactions() && !oldAutoCommit && commitType != SQLScriptCommitType.AUTOCOMMIT) {
                    monitor.beginTask("Finish transaction", 1);
                    if (lastError == null || errorHandling == SQLScriptErrorHandling.STOP_COMMIT) {
                        if (commitType != SQLScriptCommitType.NO_COMMIT) {
                            monitor.subTask("Commit");
                            txnManager.commit(session);
                        }
                    } else if (errorHandling == SQLScriptErrorHandling.STOP_ROLLBACK) {
                        monitor.subTask("Rollback");
                        txnManager.rollback(session, null);
                    } else {
                        // Just ignore error
                        monitor.subTask("Script executed with errors. Changes were not committed.");
                    }
                    monitor.done();
                }

                // Restore transactions settings
                if (txnManager != null && txnManager.isSupportsTransactions() && oldAutoCommit != newAutoCommit) {
                    txnManager.setAutoCommit(monitor, oldAutoCommit);
                }
                if (session.isLoggingEnabled()) {
                    QMUtils.getDefaultHandler().handleScriptEnd(session);
                }
            }
        } catch (Throwable ex) {
            throw new DBCException("Error during SQL script execution", ex);
        }

        if (lastError != null && errorHandling != SQLScriptErrorHandling.IGNORE) {
            throw new DBCException("Script execute failed", lastError);
        }
    }

    private boolean executeSingleQuery(@NotNull DBCSession session, @NotNull SQLScriptElement element) {
        if (element instanceof SQLControlCommand) {
            log.debug(STAT_LOG_PREFIX + "Execute command\n" + element.getText());
            try {
                return scriptContext.executeControlCommand((SQLControlCommand) element);
            } catch (Throwable e) {
                if (!(e instanceof DBException)) {
                    log.error("Unexpected error while processing SQL command", e);
                }
                lastError = e;
                return false;
            }
        }
        SQLQuery sqlQuery = (SQLQuery) element;
        scriptContext.fillQueryParameters(sqlQuery, true);
        lastError = null;

        try {
            statistics.reset();
            statistics.setQueryText(sqlQuery.getText());

            DBExecUtils.tryExecuteRecover(session, session.getDataSource(), param -> {
                try {
                    long execStartTime = System.currentTimeMillis();
                    executeStatement(session, sqlQuery, execStartTime);
                } catch (Throwable e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (Throwable ex) {
            if (!(ex instanceof DBException)) {
                log.error("Unexpected error while processing SQL", ex);
            }
            lastError = ex;
        } finally {
            scriptContext.clearStatementContext();
        }

        return lastError == null || errorHandling == SQLScriptErrorHandling.IGNORE;
    }

    private void executeStatement(@NotNull DBCSession session, SQLQuery sqlQuery, long startTime) throws DBCException {
        SQLQueryDataContainer dataContainer = new SQLQueryDataContainer(() -> executionContext, sqlQuery, scriptContext, log);
        DBCExecutionSource source = new AbstractExecutionSource(dataContainer, session.getExecutionContext(), this, sqlQuery);
        final DBCStatement statement = DBUtils.makeStatement(
            source,
            session,
            DBCStatementType.SCRIPT,
            sqlQuery,
            0,
            0);
        DBExecUtils.setStatementFetchSize(statement, 0, 0, fetchSize);

        // Execute statement
        try {
            DBRProgressMonitor monitor = session.getProgressMonitor();
            log.debug(STAT_LOG_PREFIX + "Execute query\n" + sqlQuery.getText());
            boolean hasResultSet = statement.executeStatement();

            statistics.addExecuteTime(System.currentTimeMillis() - startTime);
            statistics.addStatementsCount();

            long updateCount = -1;
            while (true) {
                // Fetch data only if we have to fetch all results or if it is rs requested
                {
                    if (hasResultSet) {
                        DBCResultSet resultSet = statement.openResultSet();
                        if (resultSet == null) {
                            // Kind of bug in the driver. It says it has resultset but returns null
                            break;
                        } else {
                            hasResultSet = fetchQueryData(session, resultSet, dataReceiver);
                        }
                    }
                }
                if (!hasResultSet) {
                    try {
                        updateCount = statement.getUpdateRowCount();
                        if (updateCount >= 0) {
                            statistics.addRowsUpdated(updateCount);
                        }
                    } catch (DBCException e) {
                        // In some cases we can't read update count
                        // This is bad but we can live with it
                        // Just print a warning
                        log.warn("Can't obtain update count", e);
                    }
                }
                if (!hasResultSet && updateCount < 0) {
                    // Nothing else to fetch
                    break;
                }

                if (session.getDataSource().getInfo().supportsMultipleResults()) {
                    try {
                        hasResultSet = statement.nextResults();
                    } catch (DBCException e) {
                        if (session.getDataSource().getInfo().isMultipleResultsFetchBroken()) {
                            log.error(e);
                            // #2792: Check this twice. Some drivers (e.g. Sybase jConnect)
                            // throw error on n'th result fetch - but it still can keep fetching next results
                            hasResultSet = statement.nextResults();
                        } else {
                            throw e;
                        }
                    }
                    updateCount = hasResultSet ? -1 : 0;
                } else {
                    break;
                }
            }
        } finally {
            try {
                Throwable[] warnings = statement.getStatementWarnings();
                if (warnings != null) {
                    for (Throwable warning : warnings) {
                        scriptContext.getOutputWriter().println(warning.getMessage());
                    }
                }
            } catch (Throwable e) {
                log.warn("Can't read execution warnings", e);
            }
            try {
                statement.close();
            } catch (Throwable e) {
                log.error("Error closing statement", e);
            }
            log.debug(STAT_LOG_PREFIX + "Time: " + RuntimeUtils.formatExecutionTime(statistics.getExecuteTime()) +
                (statistics.getRowsFetched() >= 0 ? ", fetched " + statistics.getRowsFetched() + " row(s)" : "") +
                (statistics.getRowsUpdated() >= 0 ? ", updated " + statistics.getRowsUpdated() + " row(s)" : ""));
        }
    }

    private boolean fetchQueryData(DBCSession session, DBCResultSet resultSet, DBDDataReceiver dataReceiver)
        throws DBCException {
        if (dataReceiver == null) {
            // No data pump - skip fetching stage
            return false;
        }
        if (resultSet == null) {
            return false;
        }
        DBRProgressMonitor monitor = session.getProgressMonitor();
        monitor.subTask("Fetch result set");
        DBFetchProgress fetchProgress = new DBFetchProgress(session.getProgressMonitor());

        dataReceiver.fetchStart(session, resultSet, 0, 0);

        try {
            long fetchStartTime = System.currentTimeMillis();

            // Fetch all rows
            while (!fetchProgress.isCanceled() && resultSet.nextRow()) {
                dataReceiver.fetchRow(session, resultSet);
                fetchProgress.monitorRowFetch();
            }
            statistics.addFetchTime(System.currentTimeMillis() - fetchStartTime);
        } finally {
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

        statistics.setRowsFetched(fetchProgress.getRowCount());
        monitor.subTask(fetchProgress.getRowCount() + " rows fetched");

        return true;
    }

    public DBCStatistics getStatistics() {
        return statistics;
    }

}

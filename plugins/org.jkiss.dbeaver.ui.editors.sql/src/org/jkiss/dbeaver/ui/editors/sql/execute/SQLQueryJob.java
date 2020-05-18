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
package org.jkiss.dbeaver.ui.editors.sql.execute;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBFetchProgress;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionType;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.data.DBDDataReceiverInteractive;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.AbstractExecutionSource;
import org.jkiss.dbeaver.model.impl.local.StatResultSet;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.*;
import org.jkiss.dbeaver.model.sql.parser.SQLSemanticProcessor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.runtime.sql.SQLResultsConsumer;
import org.jkiss.dbeaver.runtime.ui.DBPPlatformUI;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetPreferences;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.dialogs.exec.ExecutionQueueErrorJob;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorActivator;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * SQLQueryJob
 *
 * @author Serge Rider
 */
public class SQLQueryJob extends DataSourceJob
{
    private static final Log log = Log.getLog(SQLQueryJob.class);

    public static final Object STATS_RESULTS = new Object();

    private final DBSDataContainer dataContainer;
    private final List<SQLScriptElement> queries;
    private final SQLScriptContext scriptContext;
    private final SQLResultsConsumer resultsConsumer;
    private final SQLQueryListener listener;
    private final IWorkbenchPartSite partSite;

    private DBDDataFilter dataFilter;
    private boolean connectionInvalidated = false;

    private SQLScriptCommitType commitType;
    private SQLScriptErrorHandling errorHandling;
    private boolean fetchResultSets;
    private long rsOffset;
    private long rsMaxRows;

    private DBCStatement curStatement;
    private final List<DBCResultSet> curResultSets = new ArrayList<>();
    private Throwable lastError = null;

    private DBCStatistics statistics;
    private int fetchResultSetNumber;
    private int resultSetNumber;
    private SQLQuery lastGoodQuery;

    private boolean skipConfirmation;
    private int fetchSize;
    private long fetchFlags;

    public SQLQueryJob(
        @NotNull IWorkbenchPartSite partSite,
        @NotNull String name,
        @NotNull DBCExecutionContext executionContext,
        @Nullable DBSDataContainer dataContainer,
        @NotNull List<SQLScriptElement> queries,
        @NotNull SQLScriptContext scriptContext,
        @Nullable SQLResultsConsumer resultsConsumer,
        @Nullable SQLQueryListener listener)
    {
        super(name, executionContext);
        this.dataContainer = dataContainer;
        this.partSite = partSite;
        this.queries = queries;
        this.scriptContext = scriptContext;
        this.resultsConsumer = resultsConsumer;
        this.listener = listener;

        {
            // Read config form preference store
            DBPPreferenceStore preferenceStore = getDataSourceContainer().getPreferenceStore();
            this.commitType = SQLScriptCommitType.valueOf(preferenceStore.getString(SQLPreferenceConstants.SCRIPT_COMMIT_TYPE));
            this.errorHandling = SQLScriptErrorHandling.valueOf(preferenceStore.getString(SQLPreferenceConstants.SCRIPT_ERROR_HANDLING));
            this.fetchResultSets = queries.size() == 1 || preferenceStore.getBoolean(SQLPreferenceConstants.SCRIPT_FETCH_RESULT_SETS);
            this.rsMaxRows = preferenceStore.getInt(ModelPreferences.RESULT_SET_MAX_ROWS);
        }
    }

    public void setFetchResultSets(boolean fetchResultSets)
    {
        this.fetchResultSets = fetchResultSets;
    }

    public SQLScriptElement getLastQuery()
    {
        return queries.isEmpty() ? null : queries.get(0);
    }

    public SQLScriptElement getLastGoodQuery() {
        return lastGoodQuery;
    }

    public DBCStatement getCurrentStatement() {
        return curStatement;
    }

    private boolean hasLimits()
    {
        return rsOffset >= 0 && rsMaxRows > 0;
    }

    public void setResultSetLimit(long offset, long maxRows) {
        this.rsOffset = offset;
        this.rsMaxRows = maxRows;
    }

    public void setFetchSize(int fetchSize) {
        this.fetchSize = fetchSize;
    }


    public void setFetchFlags(long fetchFlags) {
        this.fetchFlags = fetchFlags;
    }

    @Override
    protected IStatus run(DBRProgressMonitor monitor)
    {
        RuntimeUtils.setThreadName("SQL script execution");
        statistics = new DBCStatistics();
        skipConfirmation = false;
        try {
            DBCExecutionContext context = getExecutionContext();
            DBCTransactionManager txnManager = DBUtils.getTransactionManager(context);
            DBCExecutionPurpose purpose = queries.size() > 1 ? DBCExecutionPurpose.USER_SCRIPT : DBCExecutionPurpose.USER;
            try (DBCSession session = context.openSession(monitor, purpose, "SQL Query")) {
                // Set transaction settings (only if autocommit is off)
                if (session.isLoggingEnabled()) {
                    QMUtils.getDefaultHandler().handleScriptBegin(session);
                }

                boolean oldAutoCommit = txnManager == null || txnManager.isAutoCommit();
                boolean newAutoCommit = (commitType == SQLScriptCommitType.AUTOCOMMIT);
                if (txnManager != null && !oldAutoCommit && newAutoCommit) {
                    txnManager.setAutoCommit(monitor, true);
                }

                monitor.beginTask(this.getName(), queries.size());

                // Notify job start
                if (listener != null) {
                    try {
                        listener.onStartScript();
                    } catch (Exception e) {
                        log.error(e);
                    }
                }

                resultSetNumber = 0;
                for (int queryNum = 0; queryNum < queries.size(); ) {
                    // Execute query
                    SQLScriptElement query = queries.get(queryNum);

                    fetchResultSetNumber = resultSetNumber;
                    boolean runNext = executeSingleQuery(session, query, true);
                    if (!runNext) {
                        if (lastError == null) {
                            // Execution cancel
                            break;
                        }
                        // Ask to continue
                        log.error(lastError);
                        boolean isQueue = queryNum < queries.size() - 1;
                        DBPPlatformUI.UserResponse response = ExecutionQueueErrorJob.showError(
                            isQueue ? "SQL script execution" : "SQL query execution",
                            lastError,
                            isQueue);

                        boolean stopScript = false;
                        switch (response) {
                            case STOP:
                                // just stop execution
                                stopScript = true;
                                break;
                            case RETRY:
                                // just make it again
                                continue;
                            case IGNORE:
                                // Just do nothing
                                break;
                            case IGNORE_ALL:
                                errorHandling = SQLScriptErrorHandling.IGNORE;
                                break;
                        }

                        if (stopScript) {
                            break;
                        }
                    }

                    // Check monitor
                    if (monitor.isCanceled()) {
                        break;
                    }
                    monitor.worked(1);
                    queryNum++;
                }
                if (statistics.getStatementsCount() > 0) {
                    showExecutionResult(session);
                }
                monitor.done();

                // Commit data
                if (txnManager != null && !oldAutoCommit && commitType != SQLScriptCommitType.AUTOCOMMIT) {
                    if (lastError == null || errorHandling == SQLScriptErrorHandling.STOP_COMMIT) {
                        if (commitType != SQLScriptCommitType.NO_COMMIT) {
                            monitor.beginTask("Commit data", 1);
                            txnManager.commit(session);
                            monitor.done();
                        }
                    } else if (errorHandling == SQLScriptErrorHandling.STOP_ROLLBACK) {
                        monitor.beginTask("Rollback data", 1);
                        txnManager.rollback(session, null);
                        monitor.done();
                    } else {
                        // Just ignore error
                        log.info("Script executed with errors. Changes were not commmitted.");
                    }
                }

                // Restore transactions settings
                if (txnManager != null && !oldAutoCommit && newAutoCommit) {
                    txnManager.setAutoCommit(monitor, false);
                }
                if (session.isLoggingEnabled()) {
                    QMUtils.getDefaultHandler().handleScriptEnd(session);
                }

                // Return success
                return new Status(
                    Status.OK,
                    SQLEditorActivator.PLUGIN_ID,
                    "SQL job completed");
            }
        }
        catch (Throwable ex) {
            return new Status(
                Status.ERROR,
                SQLEditorActivator.PLUGIN_ID,
                "Error during SQL job execution: " + ex.getMessage());
        }
        finally {
            // Notify job end
            if (listener != null) {
                try {
                    listener.onEndScript(statistics, lastError != null);
                } catch (Exception e) {
                    log.error(e);
                }
            }
        }
    }

    private boolean executeSingleQuery(@NotNull DBCSession session, @NotNull SQLScriptElement element, final boolean fireEvents)
    {
        if (element instanceof SQLControlCommand) {
            try {
                return scriptContext.executeControlCommand((SQLControlCommand)element);
            } catch (Throwable e) {
                if (!(e instanceof DBException)) {
                    log.error("Unexpected error while processing SQL command", e);
                }
                lastError = e;
                return false;
            }
        }
        SQLQuery sqlQuery = (SQLQuery) element;
        lastError = null;

        if (!skipConfirmation && getDataSourceContainer().getConnectionConfiguration().getConnectionType().isConfirmExecute()) {
            // Validate all transactional queries
            if (!SQLSemanticProcessor.isSelectQuery(session.getDataSource().getSQLDialect(), element.getText())) {

                int confirmResult = confirmQueryExecution((SQLQuery)element, queries.size() > 1);
                switch (confirmResult) {
                    case IDialogConstants.NO_ID:
                        return true;
                    case IDialogConstants.YES_ID:
                        break;
                    case IDialogConstants.YES_TO_ALL_ID:
                        skipConfirmation = true;
                        break;
                    default:
                        return false;
                }
            }
        }

        final DBCExecutionContext executionContext = getExecutionContext();
        final DBPDataSource dataSource = executionContext.getDataSource();

        final SQLQuery originalQuery = sqlQuery;

        DBRProgressMonitor monitor = session.getProgressMonitor();
        monitor.beginTask("Get data receiver", 1);
        monitor.subTask("Create results view");
        DBDDataReceiver dataReceiver = resultsConsumer.getDataReceiver(sqlQuery, resultSetNumber);
        try {
            if (dataReceiver instanceof DBDDataReceiverInteractive) {
                ((DBDDataReceiverInteractive) dataReceiver).setDataReceivePaused(true);
            }
            if (!scriptContext.fillQueryParameters((SQLQuery) element, CommonUtils.isBitSet(fetchFlags, DBSDataContainer.FLAG_REFRESH))) {
                // User canceled
                return false;
            }
        } finally {
            if (dataReceiver instanceof DBDDataReceiverInteractive) {
                ((DBDDataReceiverInteractive) dataReceiver).setDataReceivePaused(false);
            }
        }
        monitor.done();

        long startTime = System.currentTimeMillis();
        boolean startQueryAlerted = false;

        // Modify query (filters + parameters)
        String queryText = originalQuery.getText();//.trim();
        if (dataFilter != null && dataFilter.hasFilters()) {
            String filteredQueryText = dataSource.getSQLDialect().addFiltersToQuery(
                session.getProgressMonitor(),
                dataSource, queryText, dataFilter);
            sqlQuery = new SQLQuery(executionContext.getDataSource(), filteredQueryText, sqlQuery);
        } else {
            sqlQuery = new SQLQuery(executionContext.getDataSource(), queryText, sqlQuery);
        }

        final SQLQueryResult curResult = new SQLQueryResult(sqlQuery);
        if (rsOffset > 0) {
            curResult.setRowOffset(rsOffset);
        }

        monitor.beginTask("Process query", 1);
        try {
            // Prepare statement
            closeStatement();

            // Check and invalidate connection
            if (!connectionInvalidated && dataSource.getContainer().getPreferenceStore().getBoolean(SQLPreferenceConstants.STATEMENT_INVALIDATE_BEFORE_EXECUTE)) {
                executionContext.invalidateContext(monitor, true);
                connectionInvalidated = true;
            }

            statistics.setQueryText(sqlQuery.getText());

            // Notify query start
            if (fireEvents && listener != null) {
                // Notify query start
                try {
                    listener.onStartQuery(session, sqlQuery);
                } catch (Exception e) {
                    log.error(e);
                }
                startQueryAlerted = true;
            }

            startTime = System.currentTimeMillis();

            SQLQuery execStatement = sqlQuery;
            DBExecUtils.tryExecuteRecover(session, session.getDataSource(), param -> {
                try {
                    // We can't reset statistics here (we can be in script mode)
                    //statistics.setStatementsCount(0);
                    //statistics.setExecuteTime(0);
                    //statistics.setFetchTime(0);
                    //statistics.setRowsUpdated(0);
                    long execStartTime = System.currentTimeMillis();
                    executeStatement(session, execStatement, execStartTime, curResult);
                } catch (Throwable e) {
                    throw new InvocationTargetException(e);
                }
            });
        }
        catch (Throwable ex) {
            if (!(ex instanceof DBException)) {
                log.error("Unexpected error while processing SQL", ex);
            }
            curResult.setError(ex);
            lastError = ex;
        }
        finally {
            curResult.setQueryTime(System.currentTimeMillis() - startTime);

            if (fireEvents && listener != null && startQueryAlerted) {
                // Notify query end
                try {
                    listener.onEndQuery(session, curResult, statistics);
                } catch (Exception e) {
                    log.error(e);
                }
            }

            scriptContext.clearStatementContext();

            monitor.done();
        }

        if (curResult.getError() != null && errorHandling != SQLScriptErrorHandling.IGNORE) {
            return false;
        }
        // Success
        lastGoodQuery = originalQuery;
        return true;
    }

    private void executeStatement(@NotNull DBCSession session, SQLQuery sqlQuery, long startTime, SQLQueryResult curResult) throws DBCException {
        AbstractExecutionSource source = new AbstractExecutionSource(dataContainer, session.getExecutionContext(), partSite.getPart(), sqlQuery);
        source.setScriptContext(scriptContext);
        final DBCStatement dbcStatement = DBUtils.makeStatement(
            source,
            session,
            DBCStatementType.SCRIPT,
            sqlQuery,
            rsOffset,
            rsMaxRows);
        DBExecUtils.setStatementFetchSize(dbcStatement, rsOffset, rsMaxRows, fetchSize);
        curStatement = dbcStatement;

        int statementTimeout = getDataSourceContainer().getPreferenceStore().getInt(SQLPreferenceConstants.STATEMENT_TIMEOUT);
        if (statementTimeout > 0) {
            try {
                dbcStatement.setStatementTimeout(statementTimeout);
            } catch (Throwable e) {
                log.debug("Can't set statement timeout:" + e.getMessage());
            }
        }

        // Execute statement
        try {
            DBRProgressMonitor monitor = session.getProgressMonitor();
            monitor.subTask("Execute query");

            boolean hasResultSet = dbcStatement.executeStatement();

            statistics.addExecuteTime(System.currentTimeMillis() - startTime);
            statistics.addStatementsCount();

            curResult.setHasResultSet(hasResultSet);

            long updateCount = -1;
            while (true) {
                // Fetch data only if we have to fetch all results or if it is rs requested
                if (fetchResultSetNumber < 0 || fetchResultSetNumber == resultSetNumber) {
                    if (hasResultSet && fetchResultSets) {
                        DBCResultSet resultSet = dbcStatement.openResultSet();
                        if (resultSet == null) {
                            // Kind of bug in the driver. It says it has resultset but returns null
                            break;
                        } else {
                            DBDDataReceiver dataReceiver = resultsConsumer.getDataReceiver(sqlQuery, resultSetNumber);
                            if (dataReceiver != null) {
                                hasResultSet = fetchQueryData(session, resultSet, curResult, curResult.addExecuteResult(true), dataReceiver, true);
                            }
                        }
                    }
                }
                if (!hasResultSet) {
                    try {
                        updateCount = dbcStatement.getUpdateRowCount();
                        if (updateCount >= 0) {
                            curResult.addExecuteResult(false).setUpdateCount(updateCount);
                            statistics.addRowsUpdated(updateCount);
                        }
                    } catch (DBCException e) {
                        // In some cases we can't read update count
                        // This is bad but we can live with it
                        // Just print a warning
                        log.warn("Can't obtain update count", e);
                    }
                }
                if (hasResultSet && fetchResultSets) {
                    resultSetNumber++;
                    fetchResultSetNumber = resultSetNumber;
                }
                if (!hasResultSet && updateCount < 0) {
                    // Nothing else to fetch
                    break;
                }

                if (session.getDataSource().getInfo().supportsMultipleResults()) {
                    try {
                        hasResultSet = dbcStatement.nextResults();
                    } catch (DBCException e) {
                        if (session.getDataSource().getInfo().isMultipleResultsFetchBroken()) {
                            log.error(e);
                            // #2792: Check this twice. Some drivers (e.g. Sybase jConnect)
                            // throw error on n'th result fetch - but it still can keep fetching next results
                            hasResultSet = dbcStatement.nextResults();
                        } else {
                            throw e;
                        }
                    }
                    updateCount = hasResultSet ? -1 : 0;
                } else {
                    break;
                }
            };
        }
        finally {
            try {
                curResult.addWarnings(dbcStatement.getStatementWarnings());
            } catch (Throwable e) {
                log.warn("Can't read execution warnings", e);
            }
            //monitor.subTask("Close query");
            if (!keepStatementOpen()) {
                closeStatement();
            }
        }
    }

    private void showExecutionResult(DBCSession session) {
        if (statistics.getStatementsCount() > 1 || resultSetNumber == 0) {
            SQLQuery query = new SQLQuery(session.getDataSource(), "", -1, -1);
            if (queries.size() == 1) {
                query.setText(queries.get(0).getText());
            }
            query.setData(STATS_RESULTS); // It will set tab name to "Stats"
            DBDDataReceiver dataReceiver = resultsConsumer.getDataReceiver(query, resultSetNumber);
            if (dataReceiver != null) {
                try {
                    fetchExecutionResult(session, dataReceiver, query);
                } catch (DBCException e) {
                    log.error("Error generating execution result stats", e);
                }
            }
        }
    }

    private void fetchExecutionResult(@NotNull DBCSession session, @NotNull DBDDataReceiver dataReceiver, @NotNull SQLQuery query) throws DBCException
    {
        // Fetch fake result set
        StatResultSet fakeResultSet = new StatResultSet(session, curStatement);
        SQLQueryResult resultInfo = new SQLQueryResult(query);
        SQLQueryResult.ExecuteResult executeResult = resultInfo.addExecuteResult(true);

        if (statistics.getStatementsCount() > 1) {
            // Multiple statements - show script statistics
            fakeResultSet.addColumn("Queries", DBPDataKind.NUMERIC);
            fakeResultSet.addColumn("Updated Rows", DBPDataKind.NUMERIC);
            fakeResultSet.addColumn("Execute time (ms)", DBPDataKind.NUMERIC);
            fakeResultSet.addColumn("Fetch time (ms)", DBPDataKind.NUMERIC);
            fakeResultSet.addColumn("Total time (ms)", DBPDataKind.NUMERIC);
            fakeResultSet.addColumn("Finish time", DBPDataKind.DATETIME);
            fakeResultSet.addRow(
                statistics.getStatementsCount(),
                statistics.getRowsUpdated(),
                statistics.getExecuteTime(),
                statistics.getFetchTime(),
                statistics.getTotalTime(),
                new Date());
            executeResult.setResultSetName("Statistics");
        } else {
            // Single statement
            long updateCount = statistics.getRowsUpdated();
            fakeResultSet.addColumn("Updated Rows", DBPDataKind.NUMERIC);
            fakeResultSet.addColumn("Query", DBPDataKind.STRING);
            fakeResultSet.addColumn("Finish time", DBPDataKind.DATETIME);
            fakeResultSet.addRow(updateCount, query.getText(), new Date());

            executeResult.setResultSetName("Result");
        }
        fetchQueryData(session, fakeResultSet, resultInfo, executeResult, dataReceiver, false);
    }

    private boolean fetchQueryData(DBCSession session, DBCResultSet resultSet, SQLQueryResult result, SQLQueryResult.ExecuteResult executeResult, DBDDataReceiver dataReceiver, boolean updateStatistics)
        throws DBCException
    {
        if (dataReceiver == null) {
            // No data pump - skip fetching stage
            return false;
        }
        if (resultSet == null) {
            return false;
        }
        boolean keepCursor = keepStatementOpen();

        if (keepCursor) {
            curResultSets.add(resultSet);
        }
        DBRProgressMonitor monitor = session.getProgressMonitor();
        monitor.subTask("Fetch result set");
        DBFetchProgress fetchProgress = new DBFetchProgress(session.getProgressMonitor());

        dataReceiver.fetchStart(session, resultSet, rsOffset, rsMaxRows);

        try {
            String sourceName = null;//resultSet.getResultSetName();
            if (result != null) {
                final String queryTitle = result.getStatement().getQueryTitle();
                if (!CommonUtils.isEmpty(queryTitle)) {
                    sourceName = queryTitle;
                } else {
                    // Retrieve source entity
                    DBCResultSetMetaData rsMeta = resultSet.getMeta();
                    for (DBCAttributeMetaData attr : rsMeta.getAttributes()) {
                        String entityName = attr.getEntityName();
                        if (!CommonUtils.isEmpty(entityName)) {
                            if (sourceName == null) {
                                sourceName = entityName;
                            } else if (!sourceName.equals(entityName)) {
                                // Multiple source entities
                                sourceName += "(+)";
                                break;
                            }
                        }
                    }
                    if (CommonUtils.isEmpty(sourceName)) {
                        try {
                            sourceName = resultSet.getResultSetName();
                        } catch (Exception e) {
                            // This will happen quite often, do not log it
                        }
                    }
                }
                if (CommonUtils.isEmpty(sourceName)) {
                    sourceName = "Result";
                }
                executeResult.setResultSetName(sourceName);
            }
            long fetchStartTime = System.currentTimeMillis();

            // Fetch all rows
            while ((!hasLimits() || !fetchProgress.isMaxRowsFetched(rsMaxRows)) && !fetchProgress.isCanceled() && resultSet.nextRow()) {
                dataReceiver.fetchRow(session, resultSet);
                fetchProgress.monitorRowFetch();
            }
            if (updateStatistics) {
                statistics.addFetchTime(System.currentTimeMillis() - fetchStartTime);
            }
        }
        finally {
            if (!keepCursor) {
                try {
                    resultSet.close();
                } catch (Throwable e) {
                    log.error("Error while closing resultset", e);
                }
            }
            try {
                dataReceiver.fetchEnd(session, resultSet);
            } catch (Throwable e) {
                log.error("Error while handling end of result set fetch", e);
            }
            dataReceiver.close();
        }

        if (result != null) {
            executeResult.setRowCount(fetchProgress.getRowCount());
        }
        if (updateStatistics) {
            statistics.setRowsFetched(fetchProgress.getRowCount());
        }
        monitor.subTask(fetchProgress.getRowCount() + " rows fetched");

        return true;
    }

    private boolean keepStatementOpen()
    {
        // Only in single query mode and if pref option set to true
        return queries.size() == 1 &&
            getDataSourceContainer().getPreferenceStore().getBoolean(ResultSetPreferences.KEEP_STATEMENT_OPEN);
    }

    private void closeStatement()
    {
        if (curStatement != null) {
            try {
                for (DBCResultSet resultSet : curResultSets) {
                    resultSet.close();
                }
            } finally {
                curResultSets.clear();

                try {
                    curStatement.close();
                } catch (Throwable e) {
                    log.error("Error closing statement", e);
                } finally {
                    curStatement = null;
                }
            }
        }
    }

/*
    protected void canceling()
    {
        // Cancel statement only for the second time cancel is called
        */
/*if (!statementCancel) {
            statementCancel = true;
        } else *//*

        {
            if (!statementCanceled && curStatement != null) {
                try {
                    curStatement.cancelBlock();
                } catch (DBException e) {
                    log.error("Can't cancel execution: " + e.getMessage());
                }
                statementCanceled = true;
            }
        }
    }
*/

    public void extractData(@NotNull DBCSession session, @NotNull SQLScriptElement query, int resultNumber)
        throws DBCException
    {
        // Reset query to original. Otherwise multiple filters will corrupt it
        query.reset();

        statistics = new DBCStatistics();
        resultSetNumber = resultNumber;
        //session.getProgressMonitor().beginTask(CommonUtils.truncateString(query.getText(), 512), 1);
        session.getProgressMonitor().subTask(CommonUtils.truncateString(query.getText(), 512));

        boolean result = executeSingleQuery(session, query, true);
        if (!result && lastError != null) {
            if (lastError instanceof DBCException) {
                throw (DBCException) lastError;
            } else {
                throw new DBCException(lastError, getExecutionContext());
            }
        } else if (result && statistics.getStatementsCount() > 0) {
            showExecutionResult(session);
        }
    }

    public void setDataFilter(DBDDataFilter dataFilter)
    {
        this.dataFilter = dataFilter;
    }

    public DBCStatistics getStatistics()
    {
        return statistics;
    }

    public void setFetchResultSetNumber(int fetchResultSetNumber)
    {
        this.fetchResultSetNumber = fetchResultSetNumber;
    }

    public boolean isJobOpen() {
        return curStatement != null;
    }

    public void closeJob() {
        closeStatement();
    }

    private int confirmQueryExecution(@NotNull final SQLQuery query, final boolean scriptMode) {
        final DBPConnectionType connectionType = getDataSourceContainer().getConnectionConfiguration().getConnectionType();
        return new UITask<Integer>() {
            @Override
            protected Integer runTask() {
                MessageDialogWithToggle dialog = new MessageDialogWithToggle(
                        UIUtils.getActiveWorkbenchShell(),
                        "Confirm query execution",
                        null,
                        "You are in '" + connectionType.getName() + "' connection.\nDo you confirm query execution?",
                        MessageDialog.WARNING, ConfirmationDialog.getButtonLabels(ConfirmationDialog.QUESTION_WITH_CANCEL), 0,
                        "Do not ask for " + connectionType.getName() + " connections", false)
                {
                    @Override
                    protected boolean isResizable() {
                        return true;
                    }

                    @Override
                    protected IDialogSettings getDialogBoundsSettings() {
                        return UIUtils.getDialogSettings("DBeaver.SQLQueryConfirmDialog"); //$NON-NLS-1$
                    }

                    @Override
                    protected void createDialogAndButtonArea(Composite parent) {
                        dialogArea = createDialogArea(parent);
                        if (dialogArea.getLayoutData() instanceof GridData) {
                            ((GridData) dialogArea.getLayoutData()).grabExcessVerticalSpace = false;
                        }
                        Text messageText = new Text(parent, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL);
                        messageText.setText(query.getText());
                        GridData gd = new GridData(GridData.FILL_BOTH);
                        gd.heightHint = UIUtils.getFontHeight(messageText) * 4 + 10;
                        gd.horizontalSpan = 2;
                        messageText.setLayoutData(gd);
                        buttonBar = createButtonBar(parent);
                        // Apply to the parent so that the message gets it too.
                        applyDialogFont(parent);
                    }

                    @Override
                    protected void createButtonsForButtonBar(Composite parent)
                    {
                        createButton(parent, IDialogConstants.YES_ID, IDialogConstants.YES_LABEL, true);
                        createButton(parent, IDialogConstants.NO_ID, IDialogConstants.NO_LABEL, false);
                        if (scriptMode) {
                            createButton(parent, IDialogConstants.YES_TO_ALL_ID, IDialogConstants.YES_TO_ALL_LABEL, false);
                            createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
                        }
                    }
                };
                int result = dialog.open();
                if (dialog.getToggleState()) {
                    connectionType.setConfirmExecute(false);
                    DBWorkbench.getPlatform().getDataSourceProviderRegistry().saveConnectionTypes();
                }
                return result;
            }
        }.execute();
    }

}

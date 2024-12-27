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
package org.jkiss.dbeaver.ui.editors.sql.execute;

import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.osgi.util.NLS;
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
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.connection.DBPConnectionType;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.AbstractExecutionSource;
import org.jkiss.dbeaver.model.impl.local.StatResultSet;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableParametrized;
import org.jkiss.dbeaver.model.sql.*;
import org.jkiss.dbeaver.model.sql.data.SQLQueryDataContainer;
import org.jkiss.dbeaver.model.sql.parser.SQLSemanticProcessor;
import org.jkiss.dbeaver.model.sql.registry.SQLCommandsRegistry;
import org.jkiss.dbeaver.model.sql.registry.SQLPragmaHandlerDescriptor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.runtime.ui.DBPPlatformUI;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.ui.ISmartTransactionManager;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetPreferences;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.dialogs.exec.ExecutionQueueErrorJob;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants.StatisticsTabOnExecutionBehavior;
import org.jkiss.dbeaver.ui.editors.sql.SQLResultsConsumer;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorActivator;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * SQLQueryJob
 *
 * @author Serge Rider
 */
public class SQLQueryJob extends DataSourceJob
{
    private static final Log log = Log.getLog(SQLQueryJob.class);

    public static final Object STATS_RESULTS = new Object();
    private static final int MAX_QUERY_PREVIEW_LENGTH = 8192;
    private static final int MAX_UPDATE_COUNT_READS = 1000;

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
    private SQLQueryResult curResult;

    private transient int rowsFetched;

    public SQLQueryJob(
        @NotNull IWorkbenchPartSite partSite,
        @NotNull String name,
        @NotNull DBCExecutionContext executionContext,
        @Nullable DBSDataContainer dataContainer,
        @NotNull List<SQLScriptElement> queries,
        @NotNull SQLScriptContext scriptContext,
        @Nullable SQLResultsConsumer resultsConsumer,
        @Nullable SQLQueryListener listener,
        boolean isDisableFetchResultSet)
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
            this.fetchResultSets = queries.size() == 1 || (
                preferenceStore.getBoolean(SQLPreferenceConstants.SCRIPT_FETCH_RESULT_SETS) && !isDisableFetchResultSet
            );
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

    public SQLQueryResult getCurrentQueryResult() {
        return curResult;
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
        monitor.beginTask("Execute SQL script", queries.size());
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
                    if (txnManager != null && txnManager.isSupportsTransactions()
                        && !oldAutoCommit && commitType != SQLScriptCommitType.AUTOCOMMIT
                        && query instanceof SQLQuery sqlQuery
                    ) {
                        handleTransactionStatements(txnManager, session, sqlQuery);
                    }
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
                if (txnManager != null && txnManager.isSupportsTransactions() && !oldAutoCommit && commitType != SQLScriptCommitType.AUTOCOMMIT) {
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
                        log.info("Script executed with errors. Changes were not committed.");
                    }
                }

                // Restore transactions settings
                if (txnManager != null && !oldAutoCommit && newAutoCommit) {
                    txnManager.setAutoCommit(monitor, false);
                }
                if (session.isLoggingEnabled()) {
                    QMUtils.getDefaultHandler().handleScriptEnd(session);
                }

                if (listener != null) {
                    listener.onEndSqlJob(session, getSqlJobResult());
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
            monitor.done();

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

    @NotNull
    private SqlJobResult getSqlJobResult() {
        if (queries.get(queries.size() - 1) == lastGoodQuery && lastError == null) {
            return SqlJobResult.SUCCESS;
        } else if (lastGoodQuery != null) {
            return SqlJobResult.PARTIAL_SUCCESS;
        } else {
            return SqlJobResult.FAILURE;
        }
    }

    protected void handleTransactionStatements(
        @NotNull DBCTransactionManager txnManager,
        @NotNull DBCSession session,
        @NotNull SQLQuery query
    ) throws DBCException {
        if (query.getType().equals(SQLQueryType.COMMIT)) {
            txnManager.commit(session);
        } else if (query.getType().equals(SQLQueryType.ROLLBACK)) {
            txnManager.rollback(session, null);
        }
    }

    private boolean executeSingleQuery(@NotNull DBCSession session, @NotNull SQLScriptElement element, final boolean fireEvents)
    {

        if (!scriptContext.getPragmas().isEmpty() && element instanceof SQLQuery) {
            final SQLQueryDataContainer container = new SQLQueryDataContainer(this::getExecutionContext, (SQLQuery) element, scriptContext, log);

            for (var it = scriptContext.getPragmas().entrySet().iterator(); it.hasNext(); ) {
                final Map.Entry<String, Map<String, Object>> entry = it.next();
                final String id = entry.getKey();
                final SQLPragmaHandlerDescriptor descriptor = SQLCommandsRegistry.getInstance().getPragmaHandler(id);

                if (descriptor != null) {
                    final int result;

                    try {
                        result = descriptor.createHandler().processPragma(session.getProgressMonitor(), container, entry.getValue());
                    } catch (DBException e) {
                        lastError = e;
                        return false;
                    }

                    if (CommonUtils.isBitSet(result, SQLPragmaHandler.RESULT_CONSUME_PRAGMA)) {
                        it.remove();
                    }

                    if (CommonUtils.isBitSet(result, SQLPragmaHandler.RESULT_CONSUME_QUERY)) {
                        return false;
                    }
                }
            }
        }
        if (element instanceof SQLControlCommand) {
            try {
                return scriptContext.executeControlCommand((SQLControlCommand)element);
            } catch (Throwable e) {
                if (!(e instanceof DBException)) {
                    log.error("Unexpected error while processing SQL command", e);
                }
                lastError = e;
                return false;
            } finally {
                statistics.addStatementsCount();
                statistics.addMessage("Command " + ((SQLControlCommand) element).getCommand() + " processed");
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
        if (!scriptContext.fillQueryParameters(
            originalQuery,
            () -> resultsConsumer.getDataReceiver(originalQuery, resultSetNumber),
            CommonUtils.isBitSet(fetchFlags, DBSDataContainer.FLAG_REFRESH)
        )) {
            // User canceled
            return false;
        }
        monitor.done();

        long startTime = System.currentTimeMillis();
        boolean startQueryAlerted = false;

        // Modify query (filters + parameters)
        String queryText = originalQuery.getText();//.trim();
        if (dataFilter != null && dataFilter.hasFilters()) {
            String filteredQueryText;
            try {
                filteredQueryText = dataSource.getSQLDialect().addFiltersToQuery(
                    session.getProgressMonitor(),
                    dataSource,
                    queryText,
                    dataFilter
                );
            } catch (DBException e) {
                log.error("Unable to add filters to query", e);
                lastError = e;
                return false;
            }
            sqlQuery = new SQLQuery(executionContext.getDataSource(), filteredQueryText, sqlQuery);
        } else {
            sqlQuery = new SQLQuery(executionContext.getDataSource(), queryText, sqlQuery);
        }

        curResult = new SQLQueryResult(sqlQuery);
        if (rsOffset > 0) {
            curResult.setRowOffset(rsOffset);
        }

        monitor.beginTask("Process query", 1);
        monitor.subTask("Initialize context");
        try {
            // Prepare statement
            closeStatement();

            // Check and invalidate connection
            if (!connectionInvalidated && dataSource.getContainer().getPreferenceStore().getBoolean(SQLPreferenceConstants.STATEMENT_INVALIDATE_BEFORE_EXECUTE)) {
                executionContext.invalidateContext(monitor);
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

            monitor.subTask("Execute query");
            startTime = System.currentTimeMillis();

            SQLQuery execStatement = sqlQuery;
            DBRRunnableParametrized<DBCSession> executor = param -> {
                boolean changedToManualCommit = false;
                try {
                    // We can't reset statistics here (we can be in script mode)
                    //statistics.setStatementsCount(0);
                    //statistics.setExecuteTime(0);
                    //statistics.setFetchTime(0);
                    //statistics.setRowsUpdated(0);

                    // Toggle smart commit mode
                    if (resultsConsumer instanceof ISmartTransactionManager && ((ISmartTransactionManager) resultsConsumer).isSmartAutoCommit()) {
                        changedToManualCommit = DBExecUtils.checkSmartAutoCommit(session, execStatement.getText());
                    }
                    long execStartTime = System.currentTimeMillis();
                    executeStatement(session, execStatement, execStartTime, curResult);
                } catch (Throwable e) {
                    // We just switched to manual mode, there is no other statements in transaction
                    // let's return back to auto commit
                    if (changedToManualCommit) {
                        try {
                            DBCTransactionManager transactionManager = DBUtils.getTransactionManager(session.getExecutionContext());
                            if (transactionManager != null) {
                                transactionManager.setAutoCommit(monitor, true);
                            }
                        } catch (DBCException ex) {
                            log.warn("Error returning to auto commit");
                        }
                    }
                    throw new InvocationTargetException(e);
                }
            };

            if (shouldRecoverQuery(execStatement)) {
                DBExecUtils.tryExecuteRecover(session, session.getDataSource(), executor);
            } else {
                try {
                    executor.run(session);
                } catch (InvocationTargetException e) {
                    throw e.getTargetException();
                }
            }
            DBCTransactionManager txnManager = DBUtils.getTransactionManager(session.getExecutionContext());
            if (txnManager != null && txnManager.isSupportsTransactions()
                && !txnManager.isAutoCommit() && commitType != SQLScriptCommitType.AUTOCOMMIT
            ) {
                handleTransactionStatements(txnManager, session, sqlQuery);
            }
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
                notifyQueryExecutionEnd(session, curResult);
            }

            monitor.done();
        }

        if (curResult.getError() != null && errorHandling != SQLScriptErrorHandling.IGNORE) {
            return false;
        }
        // Success
        lastGoodQuery = originalQuery;
        return true;
    }

    private boolean shouldRecoverQuery(SQLQuery query) {
        Statement statement = query.getStatement();
        if (statement instanceof Insert ||
            statement instanceof Delete ||
            statement instanceof Update ||
            (statement instanceof Select &&
                ((Select) statement).getSelectBody() instanceof PlainSelect &&
                !CommonUtils.isEmpty(((PlainSelect) ((Select) statement).getSelectBody()).getIntoTables())))
        {
            return false;
        }
        return true;
    }

    public void notifyQueryExecutionEnd(DBCSession session, SQLQueryResult curResult) {
        // Notify query end
        try {
            listener.onEndQuery(session, curResult, statistics);
        } catch (Exception e) {
            log.error(e);
        }
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
            // Some databases (especially NoSQL) may produce a lot of
            // result sets, we should warn user because it may lead to UI freeze
            int resultSetCounter = 0;
            int updateCountReads = 0;
            boolean confirmed = false;
            while (true) {
                // Fetch data only if we have to fetch all results or if it is rs requested
                if (fetchResultSetNumber < 0 || fetchResultSetNumber == resultSetNumber) {
                    if (hasResultSet && !confirmed && resultSetCounter >= getDataSourceContainer().getPreferenceStore()
                        .getInt(SQLPreferenceConstants.RESULT_SET_MAX_TABS_PER_QUERY)) {
                        hasResultSet = DBWorkbench.getPlatformUI().confirmAction(
                            SQLEditorMessages.editors_sql_warning_many_subtables_title,
                            NLS.bind(
                                SQLEditorMessages.editors_sql_warning_many_subtables_text,
                                getDataSourceContainer().getPreferenceStore()
                                    .getInt(SQLPreferenceConstants.RESULT_SET_MAX_TABS_PER_QUERY)
                            ),
                            true
                        );
                        confirmed = hasResultSet;
                    }
                    if (hasResultSet && fetchResultSets) {
                        DBCResultSet resultSet;
                        try {
                            resultSet = dbcStatement.openResultSet();
                            resultSetCounter++;
                        } catch (DBCException e) {
                            DBPErrorAssistant.ErrorType errorType = DBExecUtils.discoverErrorType(session.getDataSource(), e);
                            if (errorType == DBPErrorAssistant.ErrorType.RESULT_SET_MISSING) {
                                // We need to ignore this error and try to get next results
                                if (dbcStatement.nextResults()) {
                                    continue;
                                }
                            }
                            throw e;
                        }
                        if (resultSet == null) {
                            // Kind of bug in the driver. It says it has resultset but returns null
                            break;
                        } else {
                            DBDDataReceiver dataReceiver = resultsConsumer.getDataReceiver(sqlQuery, resultSetNumber);
                            if (dataReceiver != null) {
                                try {
                                    hasResultSet = fetchQueryData(session, resultSet, curResult, curResult.addExecuteResult(true), dataReceiver, true);
                                } catch (DBCException e) {
                                    if (rowsFetched == 0) {
                                        throw e;
                                    } else {
                                        // Some rows were fetched, so we don't want to fail entire query
                                        // Ad error as a warning
                                        log.warn("Fetch failed", e);
                                        statistics.setRowsFetched(rowsFetched);
                                        statistics.setError(e);
                                    }
                                }
                            }
                        }
                    }
                }
                if (!hasResultSet) {
                    try {
                        updateCount = dbcStatement.getUpdateRowCount();
                        SQLQueryResult.ExecuteResult executeResult = curResult.addExecuteResult(false);
                        if (updateCount >= 0) {
                            executeResult.setUpdateCount(updateCount);
                            statistics.addRowsUpdated(updateCount);
                            updateCountReads++;
                        }
                    } catch (DBCException e) {
                        // In some cases we can't read update count
                        // This is bad, but we can live with it
                        // Just print a warning
                        log.warn("Can't obtain update count", e);
                    }
                }
                if (hasResultSet && fetchResultSets) {
                    resultSetNumber++;
                    fetchResultSetNumber = resultSetNumber;
                }
                if (!hasResultSet) {
                    if (updateCount <= 0 && updateCountReads >= MAX_UPDATE_COUNT_READS) {
                        // Exhausted all read attempts with no success
                        break;
                    }
                    if (updateCount < 0) {
                        // Nothing else to fetch
                        break;
                    }
                }

                DBPDataSourceInfo dataSourceInfo = session.getDataSource().getInfo();
                if (dataSourceInfo.supportsMultipleResults()) {
                    if (hasLimits() && rowsFetched >= rsMaxRows && dataSourceInfo.isMultipleResultsFailsOnMaxRows()) {
                        log.trace("Max rows exceeded. Additional resultsets extraction is disabled");
                        hasResultSet = false;
                    } else {
                        try {
                            hasResultSet = dbcStatement.nextResults();
                        } catch (DBCException e) {
                            if (dataSourceInfo.isMultipleResultsFetchBroken()) {
                                statistics.addWarning(e);
                                statistics.setError(e);
                                // #2792: Check this twice. Some drivers (e.g. Sybase jConnect)
                                // throw error on n'th result fetch - but it still can keep fetching next results
                                hasResultSet = dbcStatement.nextResults();
                            } else {
                                throw e;
                            }
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
            if (!CommonUtils.isEmpty(statistics.getWarnings())) {
                curResult.addWarnings(statistics.getWarnings().toArray(new Throwable[0]));
            }
            //monitor.subTask("Close query");
            if (!keepStatementOpen()) {
                closeStatement();
            }
        }
    }

    private void showExecutionResult(DBCSession session) {
        if (isShowExecutionResult()) { // Single statement with some stats
            SQLQuery query = new SQLQuery(session.getDataSource(), "", -1, -1);
            if (queries.size() == 1) {
                query.setText(queries.get(0).getText());
            }
            query.setData(STATS_RESULTS); // It will set tab name to "Stats"
            DBDDataReceiver dataReceiver = resultsConsumer.getDataReceiver(query, resultSetNumber);
            if (dataReceiver != null && !(dataReceiver instanceof IDataTransferConsumer)) {
                try {
                    fetchExecutionResult(session, dataReceiver, query);
                } catch (DBCException e) {
                    log.error("Error generating execution result stats", e);
                }
            }
        }
    }

    private boolean isShowExecutionResult() {
        final DBPPreferenceStore store = getDataSourceContainer().getPreferenceStore();
        StatisticsTabOnExecutionBehavior statisticsTabOnExecutionBehavior = StatisticsTabOnExecutionBehavior.getByName(
            store.getString(SQLPreferenceConstants.SHOW_STATISTICS_ON_EXECUTION));
        switch (statisticsTabOnExecutionBehavior) {
            case ALWAYS:
                return true;
            case NEVER:
                return resultSetNumber <= 0 || statistics.getRowsFetched() <= 0;
            case FOR_MULTIPLE_QUERIES:
                if (resultSetNumber <= 0 || statistics.getRowsUpdated() >= 0) {
                    // If there are no results or we have updated some rows, always display statistics
                    return true;
                } else {
                    // Otherwise, display statistics if the option is set
                    return statistics.getStatementsCount() > 1;
                }
            default:
                return false;
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
            fakeResultSet.addColumn("Execute time", DBPDataKind.NUMERIC);
            fakeResultSet.addColumn("Fetch time", DBPDataKind.NUMERIC);
            fakeResultSet.addColumn("Total time", DBPDataKind.NUMERIC);
            fakeResultSet.addColumn("Start time", DBPDataKind.DATETIME);
            fakeResultSet.addColumn("Finish time", DBPDataKind.DATETIME);
            fakeResultSet.addRow(
                statistics.getStatementsCount(),
                statistics.getRowsUpdated() < 0 ? 0 : statistics.getRowsUpdated(),
                RuntimeUtils.formatExecutionTime(statistics.getExecuteTime()),
                RuntimeUtils.formatExecutionTime(statistics.getFetchTime()),
                RuntimeUtils.formatExecutionTime(statistics.getTotalTime()),
                new SimpleDateFormat(DBConstants.DEFAULT_TIMESTAMP_FORMAT).format(new Date(statistics.getStartTime())),
                new SimpleDateFormat(DBConstants.DEFAULT_TIMESTAMP_FORMAT).format(new Date()));
            executeResult.setResultSetName(SQLEditorMessages.editors_sql_statistics);
        } else {
            // Single statement
            long updateCount = statistics.getRowsUpdated();
            fakeResultSet.addColumn("Query", DBPDataKind.STRING);
            fakeResultSet.addColumn("Updated Rows", DBPDataKind.NUMERIC);
            fakeResultSet.addColumn("Execute time", DBPDataKind.NUMERIC);
            fakeResultSet.addColumn("Start time", DBPDataKind.DATETIME);
            fakeResultSet.addColumn("Finish time", DBPDataKind.DATETIME);
            fakeResultSet.addRow(
                query.getText(),
                updateCount,
                RuntimeUtils.formatExecutionTime(statistics.getExecuteTime()),
                new Date(statistics.getStartTime()),
                new Date());
            executeResult.setResultSetName(SQLEditorMessages.editors_sql_data_grid);
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
                    sourceName = SQLEditorMessages.editors_sql_data_grid;
                }
                executeResult.setResultSetName(sourceName);
            }
            long fetchStartTime = System.currentTimeMillis();

            // Fetch all rows
            rowsFetched = 0;
            while ((!hasLimits() || !fetchProgress.isMaxRowsFetched(rsMaxRows)) && !fetchProgress.isCanceled() && resultSet.nextRow()) {
                dataReceiver.fetchRow(session, resultSet);
                rowsFetched++;
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

    public void extractData(
        @NotNull DBCSession session,
        @NotNull SQLScriptElement query,
        int resultNumber,
        boolean fireEvents,
        boolean allowStatistics
    ) throws DBCException {
        // Reset query to original. Otherwise multiple filters will corrupt it
        query.reset();

        statistics = new DBCStatistics();
        resultSetNumber = resultNumber;
        //session.getProgressMonitor().beginTask(CommonUtils.truncateString(query.getText(), 512), 1);
        session.getProgressMonitor().subTask(CommonUtils.truncateString(query.getText(), 512));

        boolean result = executeSingleQuery(session, query, fireEvents);

        if (listener != null) {
            listener.onEndSqlJob(session, getSqlJobResult());
        }

        if (!result && lastError != null) {
            if (lastError instanceof DBCException dbce) {
                throw dbce;
            } else {
                throw new DBCException(lastError, getExecutionContext());
            }
        } else if (allowStatistics && result && statistics.getStatementsCount() > 0) {
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
                        String text = query.getText();
                        if (text.length() > MAX_QUERY_PREVIEW_LENGTH) {
                            // Truncate string. Too big strings may freeze UI
                            text = CommonUtils.truncateString(text, MAX_QUERY_PREVIEW_LENGTH) +
                                "... (truncated " + (text.length() - MAX_QUERY_PREVIEW_LENGTH) + " characters)";
                        }
                        messageText.setText(text);
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

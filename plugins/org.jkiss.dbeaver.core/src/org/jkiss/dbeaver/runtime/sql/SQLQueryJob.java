/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.runtime.sql;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPPreferenceStore;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.AbstractExecutionSource;
import org.jkiss.dbeaver.model.impl.local.StatResultSet;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDataSource;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLQueryParameter;
import org.jkiss.dbeaver.model.sql.SQLQueryResult;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.runtime.RunnableWithResult;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.exec.ExecutionQueueErrorJob;
import org.jkiss.dbeaver.ui.dialogs.exec.ExecutionQueueErrorResponse;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * SQLQueryJob
 *
 * @author Serge Rider
 */
public class SQLQueryJob extends DataSourceJob
{
    static final Log log = Log.getLog(SQLQueryJob.class);

    public static final Object STATS_RESULTS = new Object();

    private final DBSDataContainer dataContainer;
    private final List<SQLQuery> queries;
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

    public SQLQueryJob(
        @NotNull IWorkbenchPartSite partSite,
        @NotNull String name,
        @NotNull DBCExecutionContext executionContext,
        @NotNull DBSDataContainer dataContainer,
        @NotNull List<SQLQuery> queries,
        @NotNull SQLResultsConsumer resultsConsumer,
        @Nullable SQLQueryListener listener)
    {
        super(name, DBeaverIcons.getImageDescriptor(UIIcon.SQL_SCRIPT_EXECUTE), executionContext);
        this.dataContainer = dataContainer;
        this.partSite = partSite;
        this.queries = queries;
        this.resultsConsumer = resultsConsumer;
        this.listener = listener;

        {
            // Read config form preference store
            DBPPreferenceStore preferenceStore = getDataSourceContainer().getPreferenceStore();
            this.commitType = SQLScriptCommitType.valueOf(preferenceStore.getString(DBeaverPreferences.SCRIPT_COMMIT_TYPE));
            this.errorHandling = SQLScriptErrorHandling.valueOf(preferenceStore.getString(DBeaverPreferences.SCRIPT_ERROR_HANDLING));
            this.fetchResultSets = queries.size() == 1 || preferenceStore.getBoolean(DBeaverPreferences.SCRIPT_FETCH_RESULT_SETS);
            this.rsMaxRows = preferenceStore.getInt(DBeaverPreferences.RESULT_SET_MAX_ROWS);
        }
    }

    public void setFetchResultSets(boolean fetchResultSets)
    {
        this.fetchResultSets = fetchResultSets;
    }

    public SQLQuery getLastQuery()
    {
        return queries.isEmpty() ? null : queries.get(0);
    }

    public SQLQuery getLastGoodQuery() {
        return lastGoodQuery;
    }

    public boolean hasLimits()
    {
        return rsOffset >= 0 && rsMaxRows > 0;
    }

    public void setResultSetLimit(long offset, long maxRows)
    {
        this.rsOffset = offset;
        this.rsMaxRows = maxRows;
    }

    @Override
    protected IStatus run(DBRProgressMonitor monitor)
    {
        RuntimeUtils.setThreadName("SQL script execution");
        statistics = new DBCStatistics();
        try {
            DBCExecutionContext context = getExecutionContext();
            DBCTransactionManager txnManager = DBUtils.getTransactionManager(context);
            DBCExecutionPurpose purpose = queries.size() > 1 ? DBCExecutionPurpose.USER_SCRIPT : DBCExecutionPurpose.USER;
            try (DBCSession session = context.openSession(monitor, purpose, "SQL Query")) {
                // Set transaction settings (only if autocommit is off)
                QMUtils.getDefaultHandler().handleScriptBegin(session);

                boolean oldAutoCommit = txnManager == null || txnManager.isAutoCommit();
                boolean newAutoCommit = (commitType == SQLScriptCommitType.AUTOCOMMIT);
                if (txnManager != null && !oldAutoCommit && newAutoCommit) {
                    txnManager.setAutoCommit(monitor, true);
                }

                monitor.beginTask(this.getName(), queries.size());

                // Notify job start
                if (listener != null) {
                    listener.onStartScript();
                }

                resultSetNumber = 0;
                for (int queryNum = 0; queryNum < queries.size(); ) {
                    // Execute query
                    SQLQuery query = queries.get(queryNum);

                    fetchResultSetNumber = resultSetNumber;
                    boolean runNext = executeSingleQuery(session, query, true);
                    if (!runNext) {
                        // Ask to continue
                        if (lastError != null) {
                            log.error(lastError);
                        }
                        boolean isQueue = queryNum < queries.size() - 1;
                        ExecutionQueueErrorResponse response = ExecutionQueueErrorJob.showError(
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
                showExecutionResult(session);
                monitor.done();

                // Commit data
                if (txnManager != null && !oldAutoCommit && commitType != SQLScriptCommitType.AUTOCOMMIT) {
                    if (lastError == null || errorHandling == SQLScriptErrorHandling.STOP_COMMIT) {
                        if (commitType != SQLScriptCommitType.NO_COMMIT) {
                            monitor.beginTask("Commit data", 1);
                            txnManager.commit(session);
                            monitor.done();
                        }
                    } else {
                        monitor.beginTask("Rollback data", 1);
                        txnManager.rollback(session, null);
                        monitor.done();
                    }
                }

                // Restore transactions settings
                if (txnManager != null && !oldAutoCommit && newAutoCommit) {
                    txnManager.setAutoCommit(monitor, false);
                }

                QMUtils.getDefaultHandler().handleScriptEnd(session);

                // Return success
                return new Status(
                    Status.OK,
                    DBeaverCore.getCorePluginID(),
                    "SQL job completed");
            }
        }
        catch (Throwable ex) {
            return new Status(
                Status.ERROR,
                DBeaverCore.getCorePluginID(),
                "Error during SQL job execution: " + ex.getMessage());
        }
        finally {
            // Notify job end
            if (listener != null) {
                listener.onEndScript(statistics, lastError != null);
            }
        }
    }

    private boolean executeSingleQuery(@NotNull DBCSession session, @NotNull SQLQuery sqlQuery, boolean fireEvents)
    {
        lastError = null;

        final String originalQueryText = sqlQuery.getQuery();
        DBCExecutionContext executionContext = getExecutionContext();
        SQLQueryResult curResult = new SQLQueryResult(sqlQuery);
        if (rsOffset > 0) {
            curResult.setRowOffset(rsOffset);
        }
        SQLQuery originalQuery = sqlQuery;
        long startTime = System.currentTimeMillis();

        if (fireEvents && listener != null) {
            // Notify query start
            listener.onStartQuery(sqlQuery);
        }

        try {
            // Prepare statement
            closeStatement();

            // Check and invalidate connection
            DBPDataSource dataSource = executionContext.getDataSource();
            if (!connectionInvalidated && dataSource.getContainer().getPreferenceStore().getBoolean(DBeaverPreferences.STATEMENT_INVALIDATE_BEFORE_EXECUTE)) {
                executionContext.invalidateContext(session.getProgressMonitor());
                connectionInvalidated = true;
            }

            try {
                // Modify query (filters + parameters)
                if (dataFilter != null && dataFilter.hasFilters() && dataSource instanceof SQLDataSource) {
                    String filteredQueryText = ((SQLDataSource) dataSource).getSQLDialect().addFiltersToQuery(dataSource, originalQueryText, dataFilter);
                    sqlQuery = new SQLQuery(
                        filteredQueryText,
                        sqlQuery.getOffset(),
                        sqlQuery.getLength());
                }
            } catch (DBException e) {
                throw new DBCException("Can't apply query filter", e);
            }

            Boolean hasParameters = prepareStatementParameters(sqlQuery);
            if (hasParameters == null) {
                return false;
            }

            statistics.setQueryText(originalQueryText);

            startTime = System.currentTimeMillis();
            DBCExecutionSource source = new AbstractExecutionSource(dataContainer, executionContext, partSite.getPart(), sqlQuery);
            curStatement = DBUtils.prepareStatement(
                source,
                session,
                hasParameters ? DBCStatementType.QUERY : DBCStatementType.SCRIPT,
                sqlQuery,
                rsOffset, rsMaxRows);

            if (hasParameters) {
                bindStatementParameters(session, sqlQuery);
            }

            // Execute statement
            try {
                boolean hasResultSet = curStatement.executeStatement();
                curResult.setHasResultSet(hasResultSet);
                statistics.addExecuteTime(System.currentTimeMillis() - startTime);
                statistics.addStatementsCount();

                long updateCount = -1;
                while (hasResultSet || resultSetNumber == 0 || updateCount >= 0) {
                    // Fetch data only if we have to fetch all results or if it is rs requested
                    if (fetchResultSetNumber < 0 || fetchResultSetNumber == resultSetNumber) {
                        if (hasResultSet && fetchResultSets) {
                            DBDDataReceiver dataReceiver = resultsConsumer.getDataReceiver(sqlQuery, resultSetNumber);
                            if (dataReceiver != null) {
                                hasResultSet = fetchQueryData(session, curStatement.openResultSet(), curResult, dataReceiver, true);
                            }
                        }
                    }
                    if (!hasResultSet) {
                        try {
                            updateCount = curStatement.getUpdateRowCount();
                            if (updateCount >= 0) {
                                curResult.setUpdateCount(updateCount);
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

                    if (dataSource.getInfo().supportsMultipleResults()) {
                        hasResultSet = curStatement.nextResults();
                        updateCount = hasResultSet ? -1 : 0;
                    } else {
                        break;
                    }
                }

                try {
                    curResult.setWarnings(curStatement.getStatementWarnings());
                } catch (Throwable e) {
                    log.warn("Can't read execution warnings", e);
                }
            }
            finally {
                //monitor.subTask("Close query");
                if (!keepStatementOpen()) {
                    closeStatement();
                }

                // Release parameters
                releaseStatementParameters(sqlQuery);
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

            if (fireEvents && listener != null) {
                // Notify query end
                listener.onEndQuery(curResult);
            }
        }

        if (curResult.getError() != null && errorHandling != SQLScriptErrorHandling.IGNORE) {
            return false;
        }
        // Success
        lastGoodQuery = originalQuery;
        return true;
    }

    private void showExecutionResult(DBCSession session) throws DBCException {
        if (statistics.getStatementsCount() > 1 || resultSetNumber == 0) {
            SQLQuery query = new SQLQuery("", -1, -1);
            if (queries.size() == 1) {
                query.setQuery(queries.get(0).getQuery());
            }
            query.setData(STATS_RESULTS); // It will set tab name to "Stats"
            DBDDataReceiver dataReceiver = resultsConsumer.getDataReceiver(query, resultSetNumber);
            if (dataReceiver != null) {
                fetchExecutionResult(session, dataReceiver, query);
            }
        }
    }

    private void fetchExecutionResult(@NotNull DBCSession session, @NotNull DBDDataReceiver dataReceiver, @NotNull SQLQuery query) throws DBCException
    {
        // Fetch fake result set
        //DBCStatement statsStatement;
        StatResultSet fakeResultSet = new StatResultSet(session, curStatement);
        SQLQueryResult resultInfo = new SQLQueryResult(query);
        if (statistics.getStatementsCount() > 1) {
            // Multiple statements - show script statistics
            fakeResultSet.addColumn("Queries", DBPDataKind.NUMERIC);
            fakeResultSet.addColumn("Updated Rows", DBPDataKind.NUMERIC);
            fakeResultSet.addColumn("Execute time", DBPDataKind.NUMERIC);
            fakeResultSet.addColumn("Fetch time", DBPDataKind.NUMERIC);
            fakeResultSet.addColumn("Total time", DBPDataKind.NUMERIC);
            fakeResultSet.addRow(
                statistics.getStatementsCount(),
                statistics.getRowsUpdated(),
                statistics.getExecuteTime(),
                statistics.getFetchTime(),
                statistics.getTotalTime());
            resultInfo.setResultSetName("Statistics");
        } else {
            // Single statement
            long updateCount = statistics.getRowsUpdated();
            if (updateCount >= 0) {
                fakeResultSet.addColumn("Query", DBPDataKind.STRING);
                fakeResultSet.addColumn("Updated Rows", DBPDataKind.NUMERIC);
                fakeResultSet.addRow(query.getQuery(), updateCount);
            } else {
                fakeResultSet.addColumn("Result", DBPDataKind.NUMERIC);
            }
            resultInfo.setResultSetName("Result");
        }
        fetchQueryData(session, fakeResultSet, resultInfo, dataReceiver, false);
    }

    private Boolean prepareStatementParameters(SQLQuery sqlStatement) {
        // Bind parameters
        if (!CommonUtils.isEmpty(sqlStatement.getParameters())) {
            List<SQLQueryParameter> unresolvedParams = new ArrayList<>();
            for (SQLQueryParameter param : sqlStatement.getParameters()) {
                if (!param.isResolved()) {
                    unresolvedParams.add(param);
                }
            }
            if (!CommonUtils.isEmpty(unresolvedParams)) {
                // Resolve parameters
                if (!fillStatementParameters(unresolvedParams)) {
                    return null;
                }
            }
            // Set values for all parameters
            return true;
        }
        return false;
    }

    private boolean fillStatementParameters(final List<SQLQueryParameter> parameters)
    {
        final RunnableWithResult<Boolean> binder = new RunnableWithResult<Boolean>() {
            @Override
            public void run()
            {
                SQLQueryParameterBindDialog dialog = new SQLQueryParameterBindDialog(
                    partSite,
                    getExecutionContext(),
                    parameters);
                result = (dialog.open() == IDialogConstants.OK_ID);
            }
        };
        UIUtils.runInUI(partSite.getShell(), binder);
        Boolean result = binder.getResult();
        return result != null && result;
    }

    private void bindStatementParameters(DBCSession session, SQLQuery sqlStatement) throws DBCException {
        // Bind them
        for (SQLQueryParameter param : sqlStatement.getParameters()) {
            if (param.isResolved()) {
                // convert value to native form
                Object realValue = param.getValueHandler().getValueFromObject(session, param, param.getValue(), false);
                // bind
                param.getValueHandler().bindValueObject(
                    session,
                    curStatement,
                    param,
                    param.getOrdinalPosition(),
                    realValue);
            }
        }
    }

    private void releaseStatementParameters(SQLQuery sqlStatement) {
        if (!CommonUtils.isEmpty(sqlStatement.getParameters())) {
            for (SQLQueryParameter param : sqlStatement.getParameters()) {
                if (param.isResolved()) {
                    param.getValueHandler().releaseValueObject(param.getValue());
                }
            }
        }
    }

    private boolean fetchQueryData(DBCSession session, DBCResultSet resultSet, SQLQueryResult result, DBDDataReceiver dataReceiver, boolean updateStatistics)
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
        long rowCount = 0;

        dataReceiver.fetchStart(session, resultSet, rsOffset, rsMaxRows);

        try {
            // Retrieve source entity
            if (result != null) {
                DBCResultSetMetaData rsMeta = resultSet.getMeta();
                String sourceName = null;//resultSet.getResultSetName();
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
/*
                if (CommonUtils.isEmpty(sourceName)) {
                    try {
                        sourceName = resultSet.getResultSetName();
                    } catch (DBCException e) {
                        log.debug(e);
                    }
                }
*/
                if (CommonUtils.isEmpty(sourceName)) {
                    sourceName = "Result";
                }
                result.setResultSetName(sourceName);
            }
            long fetchStartTime = System.currentTimeMillis();

            // Fetch all rows
            while ((!hasLimits() || rowCount < rsMaxRows) && resultSet.nextRow()) {
                if (monitor.isCanceled()) {
                    break;
                }
                rowCount++;

                if (rowCount > 0 && rowCount % 100 == 0) {
                    monitor.subTask(rowCount + " rows fetched");
                    monitor.worked(100);
                }

                dataReceiver.fetchRow(session, resultSet);
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
            result.setRowCount(rowCount);
        }
        if (updateStatistics) {
            statistics.setRowsFetched(rowCount);
        }
        monitor.subTask(rowCount + " rows fetched");

        return true;
    }

    private boolean keepStatementOpen()
    {
        // Only in single query mode and if pref option set to true
        return queries.size() == 1 &&
            getDataSourceContainer().getPreferenceStore().getBoolean(DBeaverPreferences.KEEP_STATEMENT_OPEN);
    }

    private void closeStatement()
    {
        if (curStatement != null) {
            for (DBCResultSet resultSet : curResultSets) {
                resultSet.close();
            }
            curResultSets.clear();

            try {
                curStatement.close();
            } catch (Throwable e) {
                log.error("Error closing statement", e);
            }
            curStatement = null;
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

    public void extractData(DBCSession session)
        throws DBCException
    {
        statistics = new DBCStatistics();
        if (queries.size() != 1) {
            throw new DBCException("Invalid state of SQL Query job");
        }
        resultSetNumber = 0;
        SQLQuery query = queries.get(0);
        session.getProgressMonitor().beginTask(query.getQuery(), 1);
        try {
            boolean result = executeSingleQuery(session, query, true);
            if (!result && lastError != null) {
                if (lastError instanceof DBCException) {
                    throw (DBCException) lastError;
                } else {
                    throw new DBCException(lastError, getExecutionContext().getDataSource());
                }
            } else if (result) {
                showExecutionResult(session);
            }
        } finally {
            session.getProgressMonitor().done();
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

}

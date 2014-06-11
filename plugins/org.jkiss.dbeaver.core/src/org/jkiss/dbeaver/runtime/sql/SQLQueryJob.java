/*
 * Copyright (C) 2010-2014 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.runtime.sql;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.local.LocalResultSet;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDataSource;
import org.jkiss.dbeaver.model.sql.parser.SQLSemanticProcessor;
import org.jkiss.dbeaver.runtime.RunnableWithResult;
import org.jkiss.dbeaver.runtime.exec.ExecutionQueueErrorJob;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
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
    static final Log log = LogFactory.getLog(SQLQueryJob.class);

    private final List<SQLStatementInfo> queries;
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
    private final List<DBCResultSet> curResultSets = new ArrayList<DBCResultSet>();
    private Throwable lastError = null;

    private DBCStatistics statistics;
    private int fetchResultSetNumber;

    public SQLQueryJob(
        IWorkbenchPartSite partSite,
        String name,
        DBPDataSource dataSource,
        List<SQLStatementInfo> queries,
        SQLResultsConsumer resultsConsumer,
        SQLQueryListener listener)
    {
        super(name, DBIcon.SQL_SCRIPT_EXECUTE.getImageDescriptor(), dataSource);
        this.partSite = partSite;
        this.queries = queries;
        this.resultsConsumer = resultsConsumer;
        this.listener = listener;

        {
            // Read config form preference store
            IPreferenceStore preferenceStore = getDataSource().getContainer().getPreferenceStore();
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

    public SQLStatementInfo getLastQuery()
    {
        return queries.isEmpty() ? null : queries.get(0);
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
        statistics = new DBCStatistics();
        try {
            DBCSession session = getDataSource().openSession(monitor, queries.size() > 1 ? DBCExecutionPurpose.USER_SCRIPT : DBCExecutionPurpose.USER, "SQL Query");
            try {
                // Set transaction settings (only if autocommit is off)
                QMUtils.getDefaultHandler().handleScriptBegin(session);

                DBCTransactionManager txnManager = session.getTransactionManager();
                boolean oldAutoCommit = txnManager.isAutoCommit();
                boolean newAutoCommit = (commitType == SQLScriptCommitType.AUTOCOMMIT);
                if (!oldAutoCommit && newAutoCommit != oldAutoCommit) {
                    txnManager.setAutoCommit(newAutoCommit);
                }

                monitor.beginTask(this.getName(), queries.size());

                // Notify job start
                if (listener != null) {
                    listener.onStartScript();
                }

                for (int queryNum = 0; queryNum < queries.size(); ) {
                    // Execute query
                    SQLStatementInfo query = queries.get(queryNum);

                    boolean runNext = executeSingleQuery(session, query, true);
                    if (!runNext) {
                        // Ask to continue
                        if (lastError != null) {
                            log.error(lastError);
                        }
                        boolean isQueue = queryNum < queries.size() - 1;
                        ExecutionQueueErrorJob errorJob = new ExecutionQueueErrorJob(
                            isQueue ? "SQL script execution" : "SQL query execution",
                            lastError,
                            isQueue);
                        errorJob.schedule();
                        try {
                            errorJob.join();
                        }
                        catch (InterruptedException e) {
                            log.error(e);
                        }

                        boolean stopScript = false;
                        switch (errorJob.getResponse()) {
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
                monitor.done();

                {
                    // Fetch script execution results
                    DBDDataReceiver dataReceiver = resultsConsumer.getDataReceiver(null, 0);
                    if (dataReceiver != null) {
                        fetchExecutionResult(session, dataReceiver);
                    }
                }

                // Commit data
                if (!oldAutoCommit && commitType != SQLScriptCommitType.AUTOCOMMIT) {
                    if (lastError == null || errorHandling == SQLScriptErrorHandling.STOP_COMMIT) {
                        if (commitType != SQLScriptCommitType.NO_COMMIT) {
                            monitor.beginTask("Commit data", 1);
                            txnManager.commit();
                            monitor.done();
                        }
                    } else {
                        monitor.beginTask("Rollback data", 1);
                        txnManager.rollback(null);
                        monitor.done();
                    }
                }

                // Restore transactions settings
                if (!oldAutoCommit && newAutoCommit) {
                    txnManager.setAutoCommit(oldAutoCommit);
                }

                QMUtils.getDefaultHandler().handleScriptEnd(session);

                // Return success
                return new Status(
                    Status.OK,
                    DBeaverCore.getCorePluginID(),
                    "SQL job completed");
            }
            finally {
                session.close();
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

    private boolean executeSingleQuery(DBCSession session, SQLStatementInfo sqlStatement, boolean fireEvents)
    {
        lastError = null;

        if (fireEvents && listener != null) {
            // Notify query start
            listener.onStartQuery(sqlStatement);
        }

        long startTime = System.currentTimeMillis();
        String sqlQuery = sqlStatement.getQuery();
        DBPDataSource dataSource = getDataSource();
        if (dataFilter != null && dataFilter.hasFilters()) {

            try {
                if (dataSource instanceof SQLDataSource && ((SQLDataSource) dataSource).getSQLDialect().supportsSubqueries()) {
                    sqlQuery = SQLSemanticProcessor.wrapQuery(dataSource, sqlQuery, dataFilter);
                } else {
                    sqlQuery = SQLSemanticProcessor.patchQuery(dataSource, sqlQuery, dataFilter);
                }
            } catch (DBException e) {
                // Can't parse query semantics
                lastError = e;
                return false;
            }
        }
        statistics.setQueryText(sqlQuery);
        SQLQueryResult curResult = new SQLQueryResult(sqlStatement);
        if (rsOffset > 0) {
            curResult.setRowOffset(rsOffset);
        }
        //if (rsMaxRows > 0) {
        //    result.setRowCount(rsMaxRows);
        //}

        try {
            // Prepare statement
            closeStatement();

            // Check and invalidate connection
            if (!connectionInvalidated && dataSource.getContainer().getPreferenceStore().getBoolean(DBeaverPreferences.STATEMENT_INVALIDATE_BEFORE_EXECUTE)) {
                dataSource.invalidateContext(session.getProgressMonitor());
                connectionInvalidated = true;
            }

            boolean hasParameters = false;
            // Bind parameters
            if (!CommonUtils.isEmpty(sqlStatement.getParameters())) {
                List<SQLStatementParameter> unresolvedParams = new ArrayList<SQLStatementParameter>();
                for (SQLStatementParameter param : sqlStatement.getParameters()) {
                    if (!param.isResolved()) {
                        unresolvedParams.add(param);
                    }
                }
                if (!CommonUtils.isEmpty(unresolvedParams)) {
                    // Resolve parameters
                    hasParameters = bindStatementParameters(unresolvedParams);
                } else {
                    // Set values for all parameters
                    hasParameters = true;
                }
            }

            curStatement = DBUtils.prepareStatement(
                session,
                hasParameters ? DBCStatementType.QUERY : DBCStatementType.SCRIPT,
                sqlQuery,
                rsOffset,
                rsMaxRows);
            curStatement.setStatementSource(partSite.getPart());

            if (hasParameters) {
                // Bind them
                for (SQLStatementParameter param : sqlStatement.getParameters()) {
                    if (param.isResolved()) {
                        param.getValueHandler().bindValueObject(
                            session,
                            curStatement,
                            param,
                            param.getOrdinalPosition(),
                            param.getValue());
                    }
                }
            }
/*
            // Bind parameters
            if (!CommonUtils.isEmpty(sqlStatement.getParameters())) {
                List<SQLStatementParameter> unresolvedParams = new ArrayList<SQLStatementParameter>();
                for (SQLStatementParameter param : sqlStatement.getParameters()) {
                    if (!param.isResolved()) {
                        unresolvedParams.add(param);
                    }
                }

                if (!CommonUtils.isEmpty(unresolvedParams)) {
                    if (bindStatementParameters(unresolvedParams)) {
                        // Bind them
                        for (SQLStatementParameter param : sqlStatement.getParameters()) {
                            if (param.isResolved()) {
                                param.getValueHandler().bindValueObject(
                                    session,
                                    curStatement,
                                    param,
                                    param.getIndex(),
                                    param.getCellValue());
                            }
                        }
                    }
                }
            }
*/
            // Execute statement
            try {
                boolean hasResultSet = curStatement.executeStatement();
                curResult.setHasResultSet(hasResultSet);
                statistics.addExecuteTime(System.currentTimeMillis() - startTime);
                statistics.addStatementsCount();

                int resultSetNumber = 0;
                while (hasResultSet || resultSetNumber == 0) {
                    // Fetch data only if we have to fetch all results or if it is rs requested
                    if (fetchResultSetNumber < 0 || fetchResultSetNumber == resultSetNumber) {
                        DBDDataReceiver dataReceiver = resultsConsumer.getDataReceiver(sqlStatement, resultSetNumber);
                        // Show results only if we are not in the script execution
                        // Probably it doesn't matter what result executeStatement() return. It seems that some drivers
                        // return messy results here
                        if (hasResultSet && fetchResultSets && dataReceiver != null) {
                            hasResultSet = fetchQueryData(session, curStatement.openResultSet(), curResult, dataReceiver, true);
                        }
                        long updateCount = -1;
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
                            if (fetchResultSets && dataReceiver != null) {
                                fetchExecutionResult(session, dataReceiver);
                            }
                        }
                        if (!hasResultSet && updateCount < 0) {
                            // Something is wrong
                            // Possibly driver is broken and we are in infinite loop
                            break;
                        }
                    }
                    hasResultSet = curStatement.nextResults();
                    resultSetNumber++;
                }
            }
            finally {
                //monitor.subTask("Close query");
                if (!keepStatementOpen()) {
                    closeStatement();
                }

                // Release parameters
                if (!CommonUtils.isEmpty(sqlStatement.getParameters())) {
                    for (SQLStatementParameter param : sqlStatement.getParameters()) {
                        if (param.isResolved()) {
                            param.getValueHandler().releaseValueObject(param.getValue());
                        }
                    }
                }
            }
        }
        catch (DBCException ex) {
            curResult.setError(ex);
            lastError = ex;
        }
        catch (Throwable ex) {
            log.error("Unexpected error while processing SQL", ex);
        }
        curResult.setQueryTime(System.currentTimeMillis() - startTime);

        if (fireEvents && listener != null) {
            // Notify query end
            listener.onEndQuery(curResult);
        }

        if (curResult.getError() != null && errorHandling != SQLScriptErrorHandling.IGNORE) {
            return false;
        }
        // Success
        return true;
    }

    private void fetchExecutionResult(DBCSession session, DBDDataReceiver dataReceiver) throws DBCException
    {
        // Fetch fake result set
        LocalResultSet fakeResultSet = new LocalResultSet(session, curStatement);
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
        } else {
            // Single statement
            long updateCount = statistics.getRowsUpdated();
            if (updateCount >= 0) {
                fakeResultSet.addColumn("Updated Rows", DBPDataKind.NUMERIC);
                fakeResultSet.addRow(updateCount);
            } else {
                fakeResultSet.addColumn("Result", DBPDataKind.NUMERIC);
            }
        }
        fetchQueryData(session, fakeResultSet, null, dataReceiver, false);
    }

    private boolean bindStatementParameters(final List<SQLStatementParameter> parameters)
    {
        final RunnableWithResult<Boolean> binder = new RunnableWithResult<Boolean>() {
            @Override
            public void run()
            {
                SQLQueryParameterBindDialog dialog = new SQLQueryParameterBindDialog(
                        partSite,
                    getDataSource(),
                    parameters);
                result = (dialog.open() == IDialogConstants.OK_ID);
            }
        };
        UIUtils.runInUI(partSite.getShell(), binder);
        return binder.getResult();
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

        curResultSets.add(resultSet);
        DBRProgressMonitor monitor = session.getProgressMonitor();
        monitor.subTask("Fetch result set");
        long rowCount = 0;

        dataReceiver.fetchStart(session, resultSet);

        try {
            // Retrieve source entity
            if (result != null) {
                DBCResultSetMetaData rsMeta = resultSet.getResultSetMetaData();
                String sourceName = null;
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
                if (!CommonUtils.isEmpty(sourceName)) {
                    result.setResultSetName(sourceName);
                }
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
                statistics.setFetchTime(System.currentTimeMillis() - fetchStartTime);
            }
        }
        finally {
            try {
                dataReceiver.fetchEnd(session);
            } catch (DBCException e) {
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
            getDataSource().getContainer().getPreferenceStore().getBoolean(DBeaverPreferences.KEEP_STATEMENT_OPEN);
    }

    private void closeStatement()
    {
        if (curStatement != null) {
            for (DBCResultSet resultSet : curResultSets) {
                resultSet.close();
            }
            curResultSets.clear();

            curStatement.close();
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
        SQLStatementInfo query = queries.get(0);
        session.getProgressMonitor().beginTask(query.getQuery(), 1);
        try {
            boolean result = executeSingleQuery(session, query, true);
            if (!result && lastError != null) {
                if (lastError instanceof DBCException) {
                    throw (DBCException) lastError;
                } else {
                    throw new DBCException(lastError, getDataSource());
                }
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

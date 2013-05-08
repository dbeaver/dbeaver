/*
 * Copyright (C) 2010-2012 Serge Rieder
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
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.local.LocalResultSet;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataKind;
import org.jkiss.dbeaver.runtime.RunnableWithResult;
import org.jkiss.dbeaver.runtime.exec.ExecutionQueueErrorJob;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * SQLQueryJob
 *
 * @author Serge Rider
 */
public abstract class SQLQueryJob extends DataSourceJob
{
    static final Log log = LogFactory.getLog(SQLQueryJob.class);

    private static final int SUBTASK_COUNT = 5;
    //private static final int DEFAULT_MAX_ROWS = 500;

    private SQLEditorBase editor;
    private List<SQLStatementInfo> queries;
    private DBDDataFilter dataFilter;
    private boolean connectionInvalidated = false;

    private SQLScriptCommitType commitType;
    private SQLScriptErrorHandling errorHandling;
    private boolean fetchResultSets;
    private long rsOffset;
    private long rsMaxRows;
    private long rowCount;

    private DBCStatement curStatement;
    private DBCResultSet curResultSet;
    //private boolean statementCancel = false;
    private boolean statementCanceled = false;
    private Throwable lastError = null;

    private SQLQueryResult curResult;

    private List<ISQLQueryListener> queryListeners = new ArrayList<ISQLQueryListener>();
    private static final String NESTED_QUERY_AlIAS = "origdbvr";

    public SQLQueryJob(
        String name,
        SQLEditorBase editor,
        List<SQLStatementInfo> queries)
    {
        super(
            name,
            DBIcon.SQL_SCRIPT_EXECUTE.getImageDescriptor(),
            editor.getDataSource());
        this.editor = editor;
        this.queries = queries;

        {
            // Read config form preference store
            IPreferenceStore preferenceStore = getDataSource().getContainer().getPreferenceStore();
            this.commitType = SQLScriptCommitType.valueOf(preferenceStore.getString(PrefConstants.SCRIPT_COMMIT_TYPE));
            this.errorHandling = SQLScriptErrorHandling.valueOf(preferenceStore.getString(PrefConstants.SCRIPT_ERROR_HANDLING));
            if (queries.size() == 1) {
                this.fetchResultSets = true;
            } else {
                this.fetchResultSets = preferenceStore.getBoolean(PrefConstants.SCRIPT_FETCH_RESULT_SETS);
            }
            this.rsMaxRows = preferenceStore.getInt(PrefConstants.RESULT_SET_MAX_ROWS);
        }
    }

    protected abstract DBDDataReceiver getDataReceiver();

    public void setFetchResultSets(boolean fetchResultSets)
    {
        this.fetchResultSets = fetchResultSets;
    }

    public SQLStatementInfo getLastQuery()
    {
        return queries.isEmpty() ? null : queries.get(0);
    }

    public SQLQueryResult getLastResult()
    {
        return curResult;
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

    public void addQueryListener(ISQLQueryListener listener)
    {
        this.queryListeners.add(listener);
    }

    public void removeQueryListener(ISQLQueryListener listener)
    {
        this.queryListeners.remove(listener);
    }

    @Override
    protected IStatus run(DBRProgressMonitor monitor)
    {
        try {
            DBCExecutionContext context = getDataSource().openContext(monitor, queries.size() > 1 ? DBCExecutionPurpose.USER_SCRIPT : DBCExecutionPurpose.USER, "SQL Query");
            try {
// Set transaction settings (only if autocommit is off)
                QMUtils.getDefaultHandler().handleScriptBegin(context);

                DBCTransactionManager txnManager = context.getTransactionManager();
                boolean oldAutoCommit = txnManager.isAutoCommit();
                boolean newAutoCommit = (commitType == SQLScriptCommitType.AUTOCOMMIT);
                if (!oldAutoCommit && newAutoCommit != oldAutoCommit) {
                    txnManager.setAutoCommit(newAutoCommit);
                }

                monitor.beginTask(this.getName(), queries.size());

                // Notify job start
                for (ISQLQueryListener listener : queryListeners) {
                    listener.onStartJob();
                }

                for (int queryNum = 0; queryNum < queries.size(); ) {
                    // Execute query
                    SQLStatementInfo query = queries.get(queryNum);

                    boolean runNext = executeSingleQuery(context, query, true);
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

/*
            if (lastError != null) {
                int code = lastError instanceof DBException ? ((DBException)lastError).getErrorCode() : 0;
                // Return error
                return new Status(
                    Status.ERROR,
                    DBeaverCore.getInstance().getCorePluginID(),
                    code,
                    lastError.getMessage(),
                    null);
            } else {
                // Return success
                return new Status(
                    Status.OK,
                    DBeaverCore.getInstance().getCorePluginID(),
                    "SQL job completed");
            }
*/
                QMUtils.getDefaultHandler().handleScriptEnd(context);

                // Return success
                return new Status(
                    Status.OK,
                    DBeaverCore.getCorePluginID(),
                    "SQL job completed");
            }
            finally {
                context.close();
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
            for (ISQLQueryListener listener : queryListeners) {
                listener.onEndJob(lastError != null);
            }
        }
    }

    private boolean executeSingleQuery(DBCExecutionContext context, SQLStatementInfo sqlStatement, boolean fireEvents)
    {
        lastError = null;

        if (fireEvents) {
            // Notify query start
            for (ISQLQueryListener listener : queryListeners) {
                listener.onStartQuery(sqlStatement);
            }
        }

        long startTime = System.currentTimeMillis();
        String sqlQuery = sqlStatement.getQuery();
        if (dataFilter != null && dataFilter.hasConditions()) {
            // Append filter conditions to query
            StringBuilder modifiedQuery = new StringBuilder(sqlQuery.length() + 100);
            modifiedQuery.append("SELECT * FROM (\n");
            modifiedQuery.append(sqlQuery);
            modifiedQuery.append("\n) ").append(NESTED_QUERY_AlIAS).append(" WHERE ");
            dataFilter.appendConditionString(getDataSource(), NESTED_QUERY_AlIAS, modifiedQuery);
            sqlQuery = modifiedQuery.toString();
        }
        curResult = new SQLQueryResult(sqlStatement);
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
            if (!connectionInvalidated && getDataSource().getContainer().getPreferenceStore().getBoolean(PrefConstants.STATEMENT_INVALIDATE_BEFORE_EXECUTE)) {
                getDataSource().invalidateConnection(context.getProgressMonitor());
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
                    hasParameters = bindStatementParameters(unresolvedParams);
                }
            }

            curStatement = DBUtils.prepareStatement(
                context,
                hasParameters ? DBCStatementType.QUERY : DBCStatementType.SCRIPT,
                sqlQuery,
                rsOffset,
                rsMaxRows);
            curStatement.setUserData(editor);

            if (hasParameters) {
                // Bind them
                for (SQLStatementParameter param : sqlStatement.getParameters()) {
                    if (param.isResolved()) {
                        param.getValueHandler().bindValueObject(
                            context,
                            curStatement,
                            param,
                            param.getIndex(),
                            param.getValue());
                    }
                }
            }

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
                                    context,
                                    curStatement,
                                    param,
                                    param.getIndex(),
                                    param.getValue());
                            }
                        }
                    }
                }
            }

            // Execute statement
            try {
                //monitor.subTask("Execute query");
                boolean hasResultSet = curStatement.executeStatement();
                curResult.setHasResultSet(hasResultSet);
                // Show results only if we are not in the script execution
                // Probably it doesn't matter what result executeStatement() return. It seems that some drivers
                // return messy results here
                if (hasResultSet && fetchResultSets) {
                    fetchQueryData(context, curStatement.openResultSet());
                }
                if (!hasResultSet) {
                    long updateCount = -1;
                    try {
                        updateCount = curStatement.getUpdateRowCount();
                        if (updateCount >= 0) {
                            curResult.setUpdateCount(updateCount);
                        }
                    } catch (DBCException e) {
                        // In some cases we can't read update count
                        // This is bad but we can live with it
                        // Just print a warning
                        log.warn("Can't obtain update count", e);
                    }
                    if (fetchResultSets) {
                        // Fetch fake result set
                        LocalResultSet fakeResultSet = new LocalResultSet(context, curStatement);
                        if (updateCount > 0) {
                            fakeResultSet.addColumn("Updated Rows", DBSDataKind.NUMERIC);
                            fakeResultSet.addRow(updateCount);
                        } else {
                            fakeResultSet.addColumn("Result", DBSDataKind.NUMERIC);
                        }
                        fetchQueryData(context, fakeResultSet);
                    }
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
        catch (Throwable ex) {
            curResult.setError(ex);
            lastError = ex;
        }
        curResult.setQueryTime(System.currentTimeMillis() - startTime);

        if (fireEvents) {
            // Notify query end
            for (ISQLQueryListener listener : queryListeners) {
                listener.onEndQuery(curResult);
            }
        }

        if (curResult.getError() != null && errorHandling != SQLScriptErrorHandling.IGNORE) {
            return false;
        }
        // Success
        return true;
    }

    private boolean bindStatementParameters(final List<SQLStatementParameter> parameters)
    {
        final Shell shell = DBeaverUI.getActiveWorkbenchShell();
        final RunnableWithResult<Boolean> binder = new RunnableWithResult<Boolean>() {
            @Override
            public void run()
            {
                SQLQueryParameterBindDialog dialog = new SQLQueryParameterBindDialog(
                    editor.getSite(),
                    getDataSource(),
                    parameters);
                result = (dialog.open() == IDialogConstants.OK_ID);
            }
        };
        UIUtils.runInUI(shell, binder);
        return binder.getResult();
    }

    private void fetchQueryData(DBCExecutionContext context, DBCResultSet resultSet)
        throws DBCException
    {
        DBDDataReceiver dataReceiver = getDataReceiver();
        if (dataReceiver == null) {
            // No data pump - skip fetching stage
            return;
        }
        closeResultSet();
        curResultSet = resultSet;
        if (curResultSet == null) {
            return;
        }
        DBRProgressMonitor monitor = context.getProgressMonitor();
        monitor.subTask("Fetch result set");
        rowCount = 0;

        dataReceiver.fetchStart(context, curResultSet);

        try {
            // Retrieve source entity
            {
                DBCResultSetMetaData rsMeta = curResultSet.getResultSetMetaData();
                String sourceName = null;
                for (DBCAttributeMetaData attr : rsMeta.getAttributes()) {
                    String entityName = attr.getEntityName();
                    if (!CommonUtils.isEmpty(entityName)) {
                        if (sourceName == null) {
                            sourceName = entityName;
                        } else if (!sourceName.equals(entityName)) {
                            // Multiple source entities
                            sourceName = null;
                            break;
                        }
                    }
                }
                if (!CommonUtils.isEmpty(sourceName)) {
                    curResult.setSourceEntity(sourceName);
                }
            }
            // Fetch all rows
            while ((!hasLimits() || rowCount < rsMaxRows) && curResultSet.nextRow()) {
                if (monitor.isCanceled()) {
                    break;
                }
                rowCount++;

                if (rowCount > 0 && rowCount % 100 == 0) {
                    monitor.subTask(rowCount + " rows fetched");
                    monitor.worked(100);
                }

                dataReceiver.fetchRow(context, curResultSet);
            }
        }
        finally {
            if (!keepStatementOpen()) {
                closeResultSet();
            }

            try {
                dataReceiver.fetchEnd(context);
            } catch (DBCException e) {
                log.error("Error while handling end of result set fetch", e);
            }
            dataReceiver.close();
        }

        curResult.setRowCount(rowCount);
        monitor.subTask(rowCount + " rows fetched");
    }

    private boolean keepStatementOpen()
    {
        // Only in single query mode and if pref option set to true
        return queries.size() == 1 &&
            getDataSource().getContainer().getPreferenceStore().getBoolean(PrefConstants.KEEP_STATEMENT_OPEN);
    }

    private void closeStatement()
    {
        closeResultSet();
        if (curStatement != null) {
            curStatement.close();
            curStatement = null;
        }
    }

    private void closeResultSet()
    {
        if (curResultSet != null) {
            curResultSet.close();
            curResultSet = null;
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

    public long extractData(DBCExecutionContext context)
        throws DBException
    {
        if (queries.size() != 1) {
            throw new DBException("Invalid state of SQL Query job");
        }
        boolean result = executeSingleQuery(context, queries.get(0), true);
        if (!result && lastError != null) {
            if (lastError instanceof DBException) {
                throw (DBException) lastError;
            } else {
                throw new DBException(lastError);
            }
        }
        return rowCount;
    }

    public void setDataFilter(DBDDataFilter dataFilter)
    {
        this.dataFilter = dataFilter;
    }
}

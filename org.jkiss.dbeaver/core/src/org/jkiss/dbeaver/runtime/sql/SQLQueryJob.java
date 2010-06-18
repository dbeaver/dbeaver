/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.sql;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.dbc.DBCResultSet;
import org.jkiss.dbeaver.model.dbc.DBCStatement;
import org.jkiss.dbeaver.model.dbc.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPTransactionManager;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.DBException;

import java.util.ArrayList;
import java.util.List;

/**
 * SQLQueryJob
 */
public class SQLQueryJob extends DataSourceJob
{
    static Log log = LogFactory.getLog(SQLQueryJob.class);

    private static final int SUBTASK_COUNT = 5;
    //private static final int DEFAULT_MAX_ROWS = 500;

    private List<SQLStatementInfo> queries;
    private ISQLQueryDataPump dataPump;
    private DBSObject dataContainer;

    private SQLScriptCommitType commitType;
    private SQLScriptErrorHandling errorHandling;
    private boolean fetchResultSets;
    private int maxResults;

    private DBCStatement curStatement;
    //private boolean statementCancel = false;
    private boolean statementCanceled = false;
    private Throwable lastError = null;

    private List<ISQLQueryListener> queryListeners = new ArrayList<ISQLQueryListener>();

    public SQLQueryJob(
        String name,
        DBPDataSource dataSource,
        List<SQLStatementInfo> queries,
        ISQLQueryDataPump dataPump)
    {
        super(
            name,
            DBIcon.SQL_SCRIPT_EXECUTE.getImageDescriptor(),
            dataSource);
        this.queries = queries;
        this.dataPump = dataPump;

        {
            // Read config form preference store
            IPreferenceStore preferenceStore = getDataSource().getContainer().getPreferenceStore();
            this.commitType = SQLScriptCommitType.valueOf(preferenceStore.getString(PrefConstants.SCRIPT_COMMIT_TYPE));
            this.errorHandling = SQLScriptErrorHandling.valueOf(preferenceStore.getString(PrefConstants.SCRIPT_ERROR_HANDLING));
            this.fetchResultSets = (queries.size() == 1);
            this.maxResults = preferenceStore.getInt(PrefConstants.RESULT_SET_MAX_ROWS);
        }
    }

    public DBSObject getDataContainer() {
        return dataContainer;
    }

    public void setDataContainer(DBSObject dataContainer) {
        this.dataContainer = dataContainer;
    }

    public void addQueryListener(ISQLQueryListener listener)
    {
        this.queryListeners.add(listener);
    }

    public void removeQueryListener(ISQLQueryListener listener)
    {
        this.queryListeners.remove(listener);
    }

    /**
     * Exeucutes queries immediately (without job scheduling)
     * @param monitor progress monitor
     * @return status
     */
    public IStatus runImmediately(DBRProgressMonitor monitor)
    {
        return this.run(monitor);
    }

    protected IStatus run(DBRProgressMonitor monitor)
    {
        startJob();
        try {
            DBCExecutionContext context = getDataSource().openContext(monitor, "SQL Query");
            try {
// Set transction settings (only if autocommit is off)
                DBPTransactionManager txnManager = context.getTransactionManager();
                boolean oldAutoCommit = txnManager.isAutoCommit();
                boolean newAutoCommit = (commitType == SQLScriptCommitType.AUTOCOMMIT);
                if (!oldAutoCommit && newAutoCommit != oldAutoCommit) {
                    txnManager.setAutoCommit(newAutoCommit);
                }

                monitor.beginTask(this.getName(), queries.size() * SUBTASK_COUNT);

                // Notify job start
                for (ISQLQueryListener listener : queryListeners) {
                    listener.onStartJob();
                }

                for (int queryNum = 0; queryNum < queries.size(); ) {
                    // Execute query
                    SQLStatementInfo query = queries.get(queryNum);

                    boolean runNext = executeSingleQuery(context, query);
                    if (!runNext) {
                        // Ask to continue
                        if (lastError != null) {
                            log.error(lastError);
                        }
                        SQLQueryErrorJob errorJob = new SQLQueryErrorJob(
                            lastError,
                            queryNum < queries.size() - 1);
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
                    DBeaverCore.getInstance().getPluginID(),
                    code,
                    lastError.getMessage(),
                    null);
            } else {
                // Return success
                return new Status(
                    Status.OK,
                    DBeaverCore.getInstance().getPluginID(),
                    "SQL job completed");
            }
*/
                // Return success
                return new Status(
                    Status.OK,
                    DBeaverCore.getInstance().getPluginID(),
                    "SQL job completed");
            }
            finally {
                context.close();
            }
        }
        catch (Throwable ex) {
            return new Status(
                Status.ERROR,
                DBeaverCore.getInstance().getPluginID(),
                "Error during SQL job execution: " + ex.getMessage());
        }
        finally {
            // Notify job end
            for (ISQLQueryListener listener : queryListeners) {
                listener.onEndJob(lastError != null);
            }

            super.endJob();
        }
    }

    private boolean executeSingleQuery(DBCExecutionContext context, SQLStatementInfo query)
    {
        lastError = null;

        // Notify query start
        for (ISQLQueryListener listener : queryListeners) {
            listener.onStartQuery(query);
        }

        long startTime = System.currentTimeMillis();
        String sqlQuery = query.getQuery();
        SQLQueryResult result = new SQLQueryResult(query);
        try {
            // Prepare statement
            curStatement = context.prepareStatement(sqlQuery, false, false);
            if (dataContainer != null) {
                curStatement.setDataContainer(dataContainer);
            }
            curStatement.setLimit(0, maxResults);

            // Bind parameters
            if (!CommonUtils.isEmpty(query.getParameters())) {
                for (SQLStatementParameter param : query.getParameters()) {
                    param.getValueHandler().bindValueObject(curStatement, param.getParamType(), param.getIndex(), param.getValue());
                }
            }

            // Execute statement
            try {
                //monitor.subTask("Execute query");
                boolean hasResultSet = curStatement.executeStatement();
                // Show results only if we are not in the script execution
                if (hasResultSet && fetchResultSets) {
                    fetchQueryData(result, context);
                }
                int updateCount = curStatement.getUpdateRowCount();
                if (updateCount >= 0) {
                    result.setUpdateCount(updateCount);
                }
            }
            finally {
                //monitor.subTask("Close query");
                if (curStatement != null) {
                    curStatement.close();
                    curStatement = null;
                }

                // Release parameters
                if (!CommonUtils.isEmpty(query.getParameters())) {
                    for (SQLStatementParameter param : query.getParameters()) {
                        param.getValueHandler().releaseValueObject(param.getValue());
                    }
                }
            }
        }
        catch (Throwable ex) {
            result.setError(ex);
            lastError = ex;
        }
        result.setQueryTime(System.currentTimeMillis() - startTime);
        // Notify query end
        for (ISQLQueryListener listener : queryListeners) {
            listener.onEndQuery(result);
        }

        if (result.getError() != null && errorHandling != SQLScriptErrorHandling.IGNORE) {
            return false;
        }
        // Success
        return true;
    }

    private void fetchQueryData(SQLQueryResult result, DBCExecutionContext context)
        throws DBCException
    {
        if (dataPump == null) {
            // No data pump - skip fetching stage
            return;
        }
        DBRProgressMonitor monitor = context.getProgressMonitor();
        monitor.subTask("Fetch result set");
        DBCResultSet resultSet = curStatement.openResultSet();
        if (resultSet != null) {
            int rowCount = 0;

            dataPump.fetchStart(resultSet, context.getProgressMonitor());

            try {

                while (rowCount < maxResults && resultSet.nextRow()) {
                    rowCount++;

                    if (rowCount % 10 == 0) {
                        monitor.subTask(rowCount + " rows fetched");
                    }

                    dataPump.fetchRow(resultSet, monitor);
                }
            }
            finally {
                resultSet.close();
            }

            try {
                dataPump.fetchEnd(monitor);
            } catch (DBCException e) {
                log.error("Error while handling end of result set fetch");
            }

            result.setRowCount(rowCount);
            monitor.subTask(rowCount + " rows fetched");
        }
    }

    protected void canceling()
    {
        // Cancel statement only for the second time cancel is called
        /*if (!statementCancel) {
            statementCancel = true;
        } else */
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

}

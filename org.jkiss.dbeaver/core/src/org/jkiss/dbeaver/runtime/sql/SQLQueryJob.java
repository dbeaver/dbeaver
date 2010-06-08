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
import org.jkiss.dbeaver.model.dbc.DBCSession;
import org.jkiss.dbeaver.model.dbc.DBCStatement;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DataSourceJob;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;
import org.jkiss.dbeaver.ui.DBIcon;

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

    private DBCSession session;
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
        DBCSession session,
        List<SQLStatementInfo> queries,
        ISQLQueryDataPump dataPump)
    {
        super(
            name,
            DBIcon.SQL_SCRIPT_EXECUTE.getImageDescriptor(),
            session.getDataSource());
        this.session = session;
        this.queries = queries;
        this.dataPump = dataPump;

        {
            // Read config form preference store
            IPreferenceStore preferenceStore = session.getDataSource().getContainer().getPreferenceStore();
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
            // Set transction settings
            boolean oldAutoCommit = session.isAutoCommit();
            boolean newAutoCommit = (commitType == SQLScriptCommitType.AUTOCOMMIT);
            if (newAutoCommit != oldAutoCommit) {
                session.setAutoCommit(newAutoCommit);
            }

            monitor.beginTask(this.getName(), queries.size() * SUBTASK_COUNT);

            // Notify job start
            for (ISQLQueryListener listener : queryListeners) {
                listener.onStartJob();
            }

            for (int queryNum = 0; queryNum < queries.size(); ) {
                // Execute query
                SQLStatementInfo query = queries.get(queryNum);

                boolean runNext = executeSingleQuery(monitor, query);
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
            if (commitType != SQLScriptCommitType.AUTOCOMMIT) {
                if (lastError == null || errorHandling == SQLScriptErrorHandling.STOP_COMMIT) {
                    if (commitType != SQLScriptCommitType.NO_COMMIT) {
                        monitor.beginTask("Commit data", 1);
                        session.commit();
                        monitor.done();
                    }
                } else {
                    monitor.beginTask("Rollback data", 1);
                    session.rollback();
                    monitor.done();
                }
            }

            // Restore transactions settings
            if (newAutoCommit != oldAutoCommit) {
                session.setAutoCommit(oldAutoCommit);
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

    private boolean executeSingleQuery(DBRProgressMonitor monitor, SQLStatementInfo query)
    {
        lastError = null;

        // Notify query start
        for (ISQLQueryListener listener : queryListeners) {
            listener.onStartQuery(query);
        }

        int subTasksPerformed = 0;
        long startTime = System.currentTimeMillis();
        String sqlQuery = query.getQuery();
        SQLQueryResult result = new SQLQueryResult(query);
        try {
            // Prepare statement
            monitor.subTask(sqlQuery);
            curStatement = session.makeStatement(sqlQuery);
            if (dataContainer != null) {
                curStatement.setDataContainer(dataContainer);
            }
            curStatement.setMaxResults(maxResults);
            monitor.worked(1);
            subTasksPerformed++;

            // Bind parameters
            if (!CommonUtils.isEmpty(query.getParameters())) {
                for (SQLStatementParameter param : query.getParameters()) {
                    param.getValueHandler().bindValueObject(curStatement, param.getParamType(), param.getIndex(), param.getValue());
                }
            }
            monitor.worked(1);
            subTasksPerformed++;

            // Execute statement
            try {
                //monitor.subTask("Execute query");
                curStatement.execute();
                monitor.worked(1);
                subTasksPerformed++;
                // Show results only if we are not in the script execution
                if (fetchResultSets) {
                    fetchQueryData(result, monitor);
                }
                int updateCount = curStatement.getUpdateCount();
                if (updateCount >= 0) {
                    result.setUpdateCount(updateCount);
                }
                monitor.worked(1);
                subTasksPerformed++;
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

                monitor.worked(1);
                subTasksPerformed++;
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
        if (subTasksPerformed < SUBTASK_COUNT) {
            monitor.worked(SUBTASK_COUNT - subTasksPerformed);
        }

        if (result.getError() != null && errorHandling != SQLScriptErrorHandling.IGNORE) {
            return false;
        }
        // Success
        return true;
    }

    private void fetchQueryData(SQLQueryResult result, DBRProgressMonitor monitor)
        throws DBCException
    {
        if (dataPump == null) {
            // No data pump - skip fetching stage
            return;
        }
        monitor.subTask("Fetch result set");
        if (curStatement.hasResultSet()) {
            DBCResultSet resultSet = curStatement.getResultSet();
            if (resultSet != null) {
                int rowCount = 0;

                dataPump.fetchStart(resultSet);

                try {

                    while (rowCount < maxResults && resultSet.next()) {
                        rowCount++;
                        
                        if (rowCount % 10 == 0) {
                            monitor.subTask(rowCount + " rows fetched");
                        }

                        dataPump.fetchRow(resultSet);
                    }
                }
                finally {
                    curStatement.closeResultSet();
                }

                try {
                    dataPump.fetchEnd();
                } catch (DBCException e) {
                    log.error("Error while handling end of result set fetch");
                }

                result.setRowCount(rowCount);
                monitor.subTask(rowCount + " rows fetched");
            }
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
                    curStatement.cancel();
                } catch (DBCException e) {
                    log.error("Can't cancel execution: " + e.getMessage());
                }
                statementCanceled = true;
            }
        }
    }

}

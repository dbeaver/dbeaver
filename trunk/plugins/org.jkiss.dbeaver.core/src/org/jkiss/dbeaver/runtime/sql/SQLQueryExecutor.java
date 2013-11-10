/*
 * Copyright (C) 2010-2013 Serge Rieder
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
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.local.LocalResultSet;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.RunnableWithResult;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * SQLQueryJob
 *
 * @author Serge Rider
 */
public abstract class SQLQueryExecutor
{
    static final Log log = LogFactory.getLog(SQLQueryExecutor.class);

    private final IWorkbenchPartSite ownerSite;
    private final Object source;
    private boolean connectionInvalidated = false;

    private DBCStatement curStatement;
    private DBCResultSet curResultSet;
    private Throwable lastError = null;

    private SQLQueryResult curResult;

    private static final String NESTED_QUERY_AlIAS = "origdbvr";

    protected SQLQueryExecutor(IWorkbenchPartSite ownerSite, Object source)
    {
        this.ownerSite = ownerSite;
        this.source = source;
    }

    public boolean executeQuery(
        DBCSession session,
        SQLStatementInfo sqlStatement,
        DBDDataFilter dataFilter,
        long firstRow,
        long maxRows,
        boolean fetchResults,
        boolean keepResults,
        DBCStatistics statistics,
        ISQLQueryListener listener)
    {
        lastError = null;

        if (listener != null) {
            // Notify query start
            listener.onStartQuery(sqlStatement);
        }

        long startTime = System.currentTimeMillis();
        String sqlQuery = sqlStatement.getQuery();
        if (dataFilter != null && dataFilter.hasFilters()) {
            // Append filter conditions to query
            StringBuilder modifiedQuery = new StringBuilder(sqlQuery.length() + 100);
            modifiedQuery.append("SELECT * FROM (\n");
            modifiedQuery.append(sqlQuery);
            modifiedQuery.append("\n) ").append(NESTED_QUERY_AlIAS);
            if (dataFilter.hasConditions()) {
                modifiedQuery.append(" WHERE ");
                dataFilter.appendConditionString(session.getDataSource(), NESTED_QUERY_AlIAS, modifiedQuery);
            }
            if (dataFilter.hasOrdering()) {
                modifiedQuery.append(" ORDER BY "); //$NON-NLS-1$
                dataFilter.appendOrderString(session.getDataSource(), NESTED_QUERY_AlIAS, modifiedQuery);
            }
            sqlQuery = modifiedQuery.toString();
        }
        curResult = new SQLQueryResult(sqlStatement);
        if (firstRow > 0) {
            curResult.setRowOffset(firstRow);
        }
        //if (rsMaxRows > 0) {
        //    curResult.setRowCount(rsMaxRows);
        //}

        try {
            // Check and invalidate connection
            if (!connectionInvalidated && session.getDataSource().getContainer().getPreferenceStore().getBoolean(PrefConstants.STATEMENT_INVALIDATE_BEFORE_EXECUTE)) {
                session.getDataSource().invalidateContext(session.getProgressMonitor());
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
                hasParameters = CommonUtils.isEmpty(unresolvedParams) || bindStatementParameters(session, unresolvedParams);
            }

            curStatement = DBUtils.prepareStatement(
                session,
                hasParameters ? DBCStatementType.QUERY : DBCStatementType.SCRIPT,
                sqlQuery,
                firstRow,
                maxRows);
            curStatement.setSource(source);

            if (hasParameters) {
                // Bind them
                for (SQLStatementParameter param : sqlStatement.getParameters()) {
                    if (param.isResolved()) {
                        param.getValueHandler().bindValueObject(
                            session,
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
                    if (bindStatementParameters(session, unresolvedParams)) {
                        // Bind them
                        for (SQLStatementParameter param : sqlStatement.getParameters()) {
                            if (param.isResolved()) {
                                param.getValueHandler().bindValueObject(
                                    session,
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
                boolean hasResultSet = curStatement.executeStatement();
                curResult.setHasResultSet(hasResultSet);
                statistics.addExecuteTime(System.currentTimeMillis() - startTime);
                statistics.addStatementsCount();
                // Show results only if we are not in the script execution
                // Probably it doesn't matter what result executeStatement() return. It seems that some drivers
                // return messy results here
                if (hasResultSet && fetchResults) {
                    fetchQueryData(session, curStatement.openResultSet(), maxRows, keepResults, 0, statistics);
                }
                if (!hasResultSet) {
                    long updateCount = -1;
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
                    if (fetchResults) {
                        fetchExecutionResult(session, statistics);
                    }
                }
            }
            finally {
                //monitor.subTask("Close query");
                if (!keepResults) {
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

        if (listener != null) {
            // Notify query end
            listener.onEndQuery(curResult);
        }

        // Success
        return true;
    }

    public void cleanup()
    {
        closeStatement();
    }

    private void fetchExecutionResult(DBCSession session, DBCStatistics statistics) throws DBCException
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
            if (updateCount > 0) {
                fakeResultSet.addColumn("Updated Rows", DBPDataKind.NUMERIC);
                fakeResultSet.addRow(updateCount);
            } else {
                fakeResultSet.addColumn("Result", DBPDataKind.NUMERIC);
            }
        }
        fetchQueryData(session, fakeResultSet, -1, false, 0, null);
    }

    private boolean bindStatementParameters(final DBCSession session, final List<SQLStatementParameter> parameters)
    {
        final Shell shell = DBeaverUI.getActiveWorkbenchShell();
        final RunnableWithResult<Boolean> binder = new RunnableWithResult<Boolean>() {
            @Override
            public void run()
            {
                SQLQueryParameterBindDialog dialog = new SQLQueryParameterBindDialog(
                    ownerSite,
                    session.getDataSource(),
                    parameters);
                result = (dialog.open() == IDialogConstants.OK_ID);
            }
        };
        UIUtils.runInUI(shell, binder);
        return binder.getResult();
    }

    private void fetchQueryData(
        DBCSession session,
        DBCResultSet resultSet,
        long maxRows,
        boolean keepResults,
        int resultsNum,
        DBCStatistics statistics)
        throws DBCException
    {
        closeResultSet();
        curResultSet = resultSet;
        if (curResultSet == null) {
            return;
        }
        DBRProgressMonitor monitor = session.getProgressMonitor();
        monitor.subTask("Fetch result set");
        long rowCount = 0;

        // Retrieve source entity
        String resultsName = null;
        {
            DBCResultSetMetaData rsMeta = curResultSet.getResultSetMetaData();
            for (DBCAttributeMetaData attr : rsMeta.getAttributes()) {
                String entityName = attr.getEntityName();
                if (!CommonUtils.isEmpty(entityName)) {
                    if (resultsName == null) {
                        resultsName = entityName;
                    } else if (!resultsName.equals(entityName)) {
                        // Multiple source entities
                        resultsName = null;
                        break;
                    }
                }
            }
        }

        DBDDataReceiver dataReceiver = obtainDataReceiver(resultsName, resultsNum);
        dataReceiver.fetchStart(session, curResultSet);

        try {
            long fetchStartTime = System.currentTimeMillis();

            // Fetch all rows
            while ((maxRows <= 0 || rowCount < maxRows) && curResultSet.nextRow()) {
                if (monitor.isCanceled()) {
                    break;
                }
                rowCount++;

                if (rowCount > 0 && rowCount % 100 == 0) {
                    monitor.subTask(rowCount + " rows fetched");
                    monitor.worked(100);
                }

                dataReceiver.fetchRow(session, curResultSet);
            }
            if (statistics != null) {
                statistics.setFetchTime(System.currentTimeMillis() - fetchStartTime);
            }
        }
        finally {
            if (!keepResults) {
                closeResultSet();
            }

            try {
                dataReceiver.fetchEnd(session);
            } catch (DBCException e) {
                log.error("Error while handling end of result set fetch", e);
            }
            dataReceiver.close();
        }

        curResult.setRowCount(rowCount);
        if (statistics != null) {
            statistics.setRowsFetched(rowCount);
        }
        monitor.subTask(rowCount + " rows fetched");
    }

    protected abstract DBDDataReceiver obtainDataReceiver(String name, int resultsNum);

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
    public void extractData(DBCSession session, SQLStatementInfo sqlStatement)
        throws DBCException
    {
        boolean result = executeQuery(session, sqlStatement, true, true);
        if (!result && lastError != null) {
            if (lastError instanceof DBCException) {
                throw (DBCException) lastError;
            } else {
                throw new DBCException(lastError, getDataSource());
            }
        }
    }
*/

}

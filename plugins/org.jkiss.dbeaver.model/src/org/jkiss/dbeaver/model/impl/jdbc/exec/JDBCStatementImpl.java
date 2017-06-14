/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.impl.jdbc.exec;

import org.jkiss.dbeaver.Log;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.exec.DBCExecutionSource;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Managable statement.
 * Stores information about execution in query manager and operated progress monitor.
 */
public class JDBCStatementImpl<STATEMENT extends Statement> implements JDBCStatement {

    private static final Log log = Log.getLog(JDBCStatementImpl.class);

    protected final JDBCSession connection;
    protected final STATEMENT original;

    private String query;

    private long rsOffset = -1;
    private long rsMaxRows = -1;

    private DBCExecutionSource source;
    private int updateCount;
    private Throwable executeError;

    private boolean disableLogging;

    public JDBCStatementImpl(@NotNull JDBCSession connection, @NotNull STATEMENT original, boolean disableLogging)
    {
        this.connection = connection;
        this.original = original;
        this.disableLogging = disableLogging;
        if (isQMLoggingEnabled()) {
            QMUtils.getDefaultHandler().handleStatementOpen(this);
        }
    }

    protected STATEMENT getOriginal()
    {
        return original;
    }

    protected boolean isQMLoggingEnabled() {
        return !disableLogging;
    }


    protected void startBlock()
    {
        this.connection.getProgressMonitor().startBlock(
            this, null/*this.query == null ? "?" : JDBCUtils.limitQueryLength(query, 200)*/);
    }

    protected void endBlock()
    {
        connection.getProgressMonitor().endBlock();
    }

    @Override
    public void cancelBlock()
        throws DBException
    {
        try {
            this.cancel();
        }
        catch (SQLException e) {
            throw new DBException(e, connection.getDataSource());
        }
    }

    @NotNull
    @Override
    public JDBCSession getConnection()
    {
        return connection;
    }

    ////////////////////////////////////////////////////////////////////
    // DBC Statement overrides
    ////////////////////////////////////////////////////////////////////

    @NotNull
    @Override
    public JDBCSession getSession()
    {
        return connection;
    }

    @Nullable
    @Override
    public String getQueryString()
    {
        return query;
    }

    @Override
    public void setQueryString(@Nullable String query)
    {
        this.query = query;
    }

    @Override
    public boolean executeStatement()
        throws DBCException
    {
        try {
            return execute(query);
        }
        catch (SQLException e) {
            throw new DBCException(e, connection.getDataSource());
        }
    }

    @Override
    public void addToBatch() throws DBCException
    {
        try {
            addBatch(query);
        }
        catch (SQLException e) {
            throw new DBCException(e, connection.getDataSource());
        }
    }

    @Override
    public int[] executeStatementBatch() throws DBCException
    {
        try {
            return executeBatch();
        }
        catch (SQLException e) {
            throw new DBCException(e, connection.getDataSource());
        }
    }

    @Nullable
    @Override
    public JDBCResultSet openResultSet() throws DBCException
    {
        // Some driver perform real RS fetch at this moment.
        // So let's start thge block
        this.startBlock();
        try {
            return getResultSet();
        }
        catch (SQLException e) {
            throw new DBCException(e, connection.getDataSource());
        }
        finally {
            this.endBlock();
        }
    }

    @Nullable
    @Override
    public JDBCResultSet openGeneratedKeysResultSet()
        throws DBCException
    {
        try {
            return makeResultSet(getOriginal().getGeneratedKeys());
        }
        catch (SQLException e) {
            throw new DBCException(e, connection.getDataSource());
        }
    }

    @Override
    public int getUpdateRowCount() throws DBCException
    {
        try {
            return getUpdateCount();
        } catch (SQLException e) {
            throw new DBCException(e, connection.getDataSource());
        }
    }

    @Override
    public boolean nextResults()
        throws DBCException
    {
        try {
            return getOriginal().getMoreResults();
        } catch (SQLException e) {
            throw new DBCException(e, connection.getDataSource());
        }
    }

    @Override
    public void setLimit(long offset, long limit) throws DBCException
    {
        int totalRows;
        if (offset <= 0) {
            // Just set max row num
            totalRows = (int)limit;
            this.rsMaxRows = limit;
        } else {
            // Remember limit values - we'll use them in resultset fetch routine
            this.rsOffset = offset;
            this.rsMaxRows = limit;
            totalRows = limit > 0 ? (int)(offset + limit) : -1;
        }
        if (totalRows > 0 && connection.getDataSource().getInfo().supportsResultSetLimit()) {
            try {
                setMaxRows(totalRows);
            }
            catch (SQLException e) {
                // [JDBC:ODBC] Probably setMaxRows is not supported. Just log this error
                // We'll use rsOffset and rsMaxRows anyway
                log.debug(getOriginal().getClass().getName() + ".setMaxRows not supported?", e);
            }
        }
    }

    @Nullable
    @Override
    public DBCExecutionSource getStatementSource()
    {
        return this.source;
    }

    @Override
    public void setStatementSource(DBCExecutionSource source)
    {
        this.source = source;
    }

    @Nullable
    protected JDBCResultSet makeResultSet(@Nullable ResultSet resultSet)
        throws SQLException
    {
        if (resultSet == null) {
            return null;
        }
        JDBCResultSet dbResult = createResultSetImpl(resultSet);
        // Scroll original result set if needed
        if (rsOffset > 0) {
            JDBCUtils.scrollResultSet(resultSet, rsOffset, !getConnection().getDataSource().getInfo().supportsResultSetScroll());
        }

        if (rsMaxRows > 0 && connection.getDataSource().getInfo().supportsResultSetLimit()) {
            dbResult.setMaxRows(rsMaxRows);
        }
        return dbResult;
    }

    protected JDBCResultSet createResultSetImpl(ResultSet resultSet)
        throws SQLException
    {
        return connection.getDataSource().getJdbcFactory().createResultSet(connection, this, resultSet, null, disableLogging);
    }

    ////////////////////////////////////////////////////////////////////
    // Statement overrides
    ////////////////////////////////////////////////////////////////////

    protected boolean handleExecuteResult(boolean result)
    {
        return result;
    }

    protected int handleExecuteResult(int result)
    {
        updateCount = result;
        return result;
    }

    protected SQLException handleExecuteError(Throwable ex)
    {
        executeError = ex;
        if (connection.getDataSource().getContainer().getPreferenceStore().getBoolean(ModelPreferences.QUERY_ROLLBACK_ON_ERROR)) {
            try {
                if (!connection.isClosed() && !connection.getAutoCommit()) {
                    connection.rollback();
                }
            } catch (SQLException e) {
                log.error("Can't rollback connection after error (" + ex.getMessage() + ")", e); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
        if (ex instanceof SQLException) {
            return (SQLException) ex;
        } else {
            return new SQLException(ModelMessages.model_jdbc_exception_internal_jdbc_driver_error, ex);
        }
    }

    protected void beforeExecute()
    {
        this.updateCount = -1;
        this.executeError = null;
        if (isQMLoggingEnabled()) {
            QMUtils.getDefaultHandler().handleStatementExecuteBegin(this);
        }
        this.startBlock();
    }

    protected void afterExecute()
    {
        this.endBlock();
        if (isQMLoggingEnabled()) {
            QMUtils.getDefaultHandler().handleStatementExecuteEnd(this, this.updateCount, this.executeError);
        }
    }

    ////////////////////////////////////
    // Executions

    @Override
    public boolean execute(String sql)
        throws SQLException
    {
        setQueryString(sql);
        this.beforeExecute();
        try {
            return handleExecuteResult(getOriginal().execute(sql));
        } catch (Throwable e) {
            throw this.handleExecuteError(e);
        } finally {
            this.afterExecute();
        }
    }

    @Nullable
    @Override
    public JDBCResultSet executeQuery(String sql)
        throws SQLException
    {
        setQueryString(sql);
        this.beforeExecute();
        try {
            return makeResultSet(getOriginal().executeQuery(sql));
        } catch (Throwable e) {
            throw this.handleExecuteError(e);
        } finally {
            this.afterExecute();
        }
    }

    @Override
    public int executeUpdate(String sql)
        throws SQLException
    {
        setQueryString(sql);
        this.beforeExecute();
        try {
            return handleExecuteResult(getOriginal().executeUpdate(sql));
        } catch (Throwable e) {
            throw this.handleExecuteError(e);
        } finally {
            this.afterExecute();
        }
    }

    @Override
    public int[] executeBatch()
        throws SQLException
    {
        this.beforeExecute();
        try {
            return getOriginal().executeBatch();
        } catch (Throwable e) {
            throw this.handleExecuteError(e);
        } finally {
            this.afterExecute();
        }
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys)
        throws SQLException
    {
        setQueryString(sql);
        this.beforeExecute();
        try {
            return handleExecuteResult(getOriginal().executeUpdate(sql, autoGeneratedKeys));
        } catch (Throwable e) {
            throw this.handleExecuteError(e);
        } finally {
            this.afterExecute();
        }
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes)
        throws SQLException
    {
        setQueryString(sql);
        this.beforeExecute();
        try {
            return handleExecuteResult(getOriginal().executeUpdate(sql, columnIndexes));
        } catch (Throwable e) {
            throw this.handleExecuteError(e);
        } finally {
            this.afterExecute();
        }
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames)
        throws SQLException
    {
        setQueryString(sql);
        this.beforeExecute();
        try {
            return handleExecuteResult(getOriginal().executeUpdate(sql, columnNames));
        } catch (Throwable e) {
            throw this.handleExecuteError(e);
        } finally {
            this.afterExecute();
        }
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys)
        throws SQLException
    {
        setQueryString(sql);
        this.beforeExecute();
        try {
            return handleExecuteResult(getOriginal().execute(sql, autoGeneratedKeys));
        } catch (Throwable e) {
            throw this.handleExecuteError(e);
        } finally {
            this.afterExecute();
        }
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes)
        throws SQLException
    {
        setQueryString(sql);
        this.beforeExecute();
        try {
            return handleExecuteResult(getOriginal().execute(sql, columnIndexes));
        } catch (Throwable e) {
            throw this.handleExecuteError(e);
        } finally {
            this.afterExecute();
        }
    }

    @Override
    public boolean execute(String sql, String[] columnNames)
        throws SQLException
    {
        setQueryString(sql);
        this.beforeExecute();
        try {
            return handleExecuteResult(getOriginal().execute(sql, columnNames));
        } catch (Throwable e) {
            throw this.handleExecuteError(e);
        } finally {
            this.afterExecute();
        }
    }

    ////////////////////////////////////
    // Close

    @Override
    public void close()
    {
/*
        // Do not check for warnings here
        // Sometimes warnings are cached in connection and as a result we got a lot of spam in log
        // for each closed statement on this connection (MySQL)
        try {
            JDBCUtils.reportWarnings(getOriginal().getWarnings());
            getOriginal().clearWarnings();
        } catch (Throwable e) {
            log.debug("Can't check for statement warnings", e);
        }
*/

        if (isQMLoggingEnabled()) {
            // Handle close
            QMUtils.getDefaultHandler().handleStatementClose(this, updateCount);
        }

        // Close statement
        try {
            getOriginal().close();
        }
        catch (Throwable e) {
            log.error("Can't close statement", e); //$NON-NLS-1$
        }
    }

    ////////////////////////////////////
    // Other

    @Override
    public int getMaxFieldSize()
        throws SQLException
    {
        return getOriginal().getMaxFieldSize();
    }

    @Override
    public void setMaxFieldSize(int max)
        throws SQLException
    {
        getOriginal().setMaxFieldSize(max);
    }

    @Override
    public int getMaxRows()
        throws SQLException
    {
        return getOriginal().getMaxRows();
    }

    @Override
    public void setMaxRows(int max)
        throws SQLException
    {
        getOriginal().setMaxRows(max);
    }

    @Override
    public void setEscapeProcessing(boolean enable)
        throws SQLException
    {
        getOriginal().setEscapeProcessing(enable);
    }

    @Override
    public int getQueryTimeout()
        throws SQLException
    {
        return getOriginal().getQueryTimeout();
    }

    @Override
    public void setQueryTimeout(int seconds)
        throws SQLException
    {
        getOriginal().setQueryTimeout(seconds);
    }

    @Override
    public void cancel()
        throws SQLException
    {
        getOriginal().cancel();
    }

    @Override
    public SQLWarning getWarnings()
        throws SQLException
    {
        return getOriginal().getWarnings();
    }

    @Override
    public void clearWarnings()
        throws SQLException
    {
        getOriginal().clearWarnings();
    }

    @Override
    public void setCursorName(String name)
        throws SQLException
    {
        getOriginal().setCursorName(name);
    }

    @Nullable
    @Override
    public JDBCResultSet getResultSet()
        throws SQLException
    {
        return makeResultSet(getOriginal().getResultSet());
    }

    @Nullable
    @Override
    public Throwable[] getStatementWarnings() throws DBCException {
        try {
            List<Throwable> warnings = null;
            for (SQLWarning warning = getWarnings(); warning != null; warning = warning.getNextWarning()) {
                if (warning.getMessage() == null && warning.getErrorCode() == 0) {
                    // Skip trash [Excel driver]
                    continue;
                }
                if (warnings == null) {
                    warnings = new ArrayList<>();
                }
                if (warnings.contains(warning)) {
                    // Cycle
                    break;
                }
                warnings.add(warning);
            }
            if (!CommonUtils.isEmpty(warnings)) {
                try {
                    clearWarnings();
                } catch (Throwable e) {
                    log.debug("Internal error during clearWarnings", e);
                }
            }
            return warnings == null ? null : warnings.toArray(new Throwable[warnings.size()]);
        } catch (SQLException e) {
            throw new DBCException(e, getSession().getDataSource());
        }
    }

    @Override
    public void setStatementTimeout(int timeout) throws DBCException {
        try {
            getOriginal().setQueryTimeout(timeout);
        } catch (SQLException e) {
            throw new DBCException(e, connection.getDataSource());
        }
    }

    @Override
    public int getUpdateCount() throws SQLException
    {
        return (updateCount = getOriginal().getUpdateCount());
    }

    @Override
    public boolean getMoreResults()
        throws SQLException
    {
        return getOriginal().getMoreResults();
    }

    @Override
    public void setFetchDirection(int direction)
        throws SQLException
    {
        getOriginal().setFetchDirection(direction);
    }

    @Override
    public int getFetchDirection()
        throws SQLException
    {
        return getOriginal().getFetchDirection();
    }

    @Override
    public void setFetchSize(int rows)
        throws SQLException
    {
        getOriginal().setFetchSize(rows);
    }

    @Override
    public int getFetchSize()
        throws SQLException
    {
        return getOriginal().getFetchSize();
    }

    @Override
    public int getResultSetConcurrency()
        throws SQLException
    {
        return getOriginal().getResultSetConcurrency();
    }

    @Override
    public int getResultSetType()
        throws SQLException
    {
        return getOriginal().getResultSetType();
    }

    @Override
    public void addBatch(String sql)
        throws SQLException
    {
        getOriginal().addBatch(sql);
    }

    @Override
    public void clearBatch()
        throws SQLException
    {
        getOriginal().clearBatch();
    }

    @Override
    public boolean getMoreResults(int current)
        throws SQLException
    {
        return getOriginal().getMoreResults(current);
    }

    @Nullable
    @Override
    public ResultSet getGeneratedKeys()
        throws SQLException
    {
        return makeResultSet(getOriginal().getGeneratedKeys());
    }

    @Override
    public int getResultSetHoldability()
        throws SQLException
    {
        return getOriginal().getResultSetHoldability();
    }

    @Override
    public boolean isClosed()
        throws SQLException
    {
        return getOriginal().isClosed();
    }

    @Override
    public void setPoolable(boolean poolable)
        throws SQLException
    {
        getOriginal().setPoolable(poolable);
    }

    @Override
    public boolean isPoolable()
        throws SQLException
    {
        return getOriginal().isPoolable();
    }

    @Override
    public void closeOnCompletion() throws SQLException {
        JDBCUtils.callMethod17(getOriginal(), "closeOnCompletion", null, null);
    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        Boolean closeOnCompletion = JDBCUtils.callMethod17(getOriginal(), "isCloseOnCompletion", Boolean.TYPE, null);
        return closeOnCompletion == null ? false : closeOnCompletion;
    }

    @Override
    public <T> T unwrap(Class<T> iface)
        throws SQLException
    {
        return getOriginal().unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface)
        throws SQLException
    {
        return getOriginal().isWrapperFor(iface);
    }

    @Override
    public String toString() {
        return "JDBC Statement [" + query + "]";
    }
}

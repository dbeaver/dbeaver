/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCQueryPurpose;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;

import java.sql.*;

/**
 * Managable statement.
 * Stores information about execution in query manager and operated progress monitor.
 */
public abstract class JDBCStatementImpl implements JDBCStatement {

    static final Log log = LogFactory.getLog(JDBCStatementImpl.class);

    private JDBCExecutionContext connection;

    private String query;
    private String description;
    private DBCQueryPurpose queryPurpose;

    private long rsOffset = -1;
    private long rsMaxRows = -1;

    private DBSObject dataContainer;
    private int updateCount;
    private Throwable executeError;
    private Object userData;

    public JDBCStatementImpl(JDBCExecutionContext connection)
    {
        this.connection = connection;
        QMUtils.getDefaultHandler().handleStatementOpen(this);
    }

    protected abstract Statement getOriginal();

    protected void startBlock()
    {
        this.connection.getProgressMonitor().startBlock(
            this,
            this.description == null ?
                (query == null ? "?" : query)  :
                this.description);
    }

    protected void endBlock()
    {
        connection.getProgressMonitor().endBlock();
    }

    public void cancelBlock()
        throws DBException
    {
        try {
            this.cancel();
        }
        catch (SQLException e) {
            throw new DBException(e);
        }
    }

    public JDBCExecutionContext getConnection()
    {
        return connection;
    }

    ////////////////////////////////////////////////////////////////////
    // DBC Statement overrides
    ////////////////////////////////////////////////////////////////////

    public DBCExecutionContext getContext()
    {
        return connection;
    }

    public String getQueryString()
    {
        return query;
    }

    public void setQueryString(String query)
    {
        this.query = query;
    }

    public String getDescription()
    {
        return description;
    }

    protected void setDescription(String description)
    {
        this.description = description;
    }

    public DBCQueryPurpose getQueryPurpose()
    {
        return queryPurpose;
    }

    public void setQueryPurpose(DBCQueryPurpose queryPurpose)
    {
        this.queryPurpose = queryPurpose;
    }

    public JDBCResultSet openResultSet() throws DBCException
    {
        try {
            return getResultSet();
        }
        catch (SQLException e) {
            throw new DBCException(e);
        }
    }

    public JDBCResultSet openGeneratedKeysResultSet()
        throws DBCException
    {
        try {
            return makeResultSet(getOriginal().getGeneratedKeys());
        }
        catch (SQLException e) {
            throw new DBCException(e);
        }
    }

    public int getUpdateRowCount() throws DBCException
    {
        try {
            return getOriginal().getUpdateCount();
        }
        catch (SQLException e) {
            throw new DBCException(e);
        }
    }

    public void setLimit(long offset, long limit) throws DBCException
    {
        try {
            if (offset <= 0) {
                // Just set max row num
                getOriginal().setMaxRows((int)limit);
                this.rsMaxRows = limit;
            } else {
                // Remember limit values - we'll use them in resultset fetch routine
                this.rsOffset = offset;
                this.rsMaxRows = limit;
                getOriginal().setMaxRows( (int)(offset + limit));
            }
        }
        catch (SQLException e) {
            throw new DBCException(e);
        }
    }

    public DBSObject getDataContainer()
    {
        return this.dataContainer;
    }

    public void setDataContainer(DBSObject container)
    {
        this.dataContainer = container;
    }

    public Object getUserData()
    {
        return userData;
    }

    public void setUserData(Object userData)
    {
        this.userData = userData;
    }

    private JDBCResultSetImpl makeResultSet(ResultSet resultSet)
        throws SQLException {
        if (this instanceof JDBCPreparedStatementImpl) {
            JDBCResultSetImpl dbResult = new JDBCResultSetImpl((JDBCPreparedStatementImpl) this, resultSet);
            // Scroll original result set if needed
            if (rsOffset > 0) {
                JDBCUtils.scrollResultSet(resultSet, rsOffset);
            }
            if (rsMaxRows > 0) {
                dbResult.setMaxRows(rsMaxRows);
            }
            return dbResult;
        } else {
            throw new SQLFeatureNotSupportedException();
        }
    }

    ////////////////////////////////////////////////////////////////////
    // Statement overrides
    ////////////////////////////////////////////////////////////////////

    protected boolean handleExecuteResult(boolean result)
    {
        if (!result) {
            try {
                updateCount = getOriginal().getUpdateCount();
            } catch (SQLException e) {
                log.warn("Could not obtain update count", e);
            }
        } else {
            updateCount = 0;
        }
        return result;
    }

    protected int handleExecuteResult(int result)
    {
        updateCount = result;
        return result;
    }

    protected void handleExecuteError(SQLException ex)
    {
        executeError = ex;
        if (connection.getDataSource().getContainer().getPreferenceStore().getBoolean(PrefConstants.QUERY_ROLLBACK_ON_ERROR)) {
            try {
                if (!connection.isClosed() && !connection.getAutoCommit()) {
                    connection.rollback();
                }
            } catch (SQLException e) {
                log.error("Can't rollback connection after error (" + ex.getMessage() + ")", e);
            }
        }
    }

    protected void beforeExecute()
    {
        this.updateCount = 0;
        this.executeError = null;
        QMUtils.getDefaultHandler().handleStatementExecuteBegin(this);
        this.startBlock();
    }

    protected void afterExecute()
    {
        this.endBlock();
        QMUtils.getDefaultHandler().handleStatementExecuteEnd(this, this.updateCount, this.executeError);
    }

    ////////////////////////////////////
    // Executions

    public boolean execute(String sql)
        throws SQLException
    {
        setQueryString(sql);
        this.beforeExecute();
        try {
            return handleExecuteResult(getOriginal().execute(sql));
        } catch (SQLException e) {
            this.handleExecuteError(e);
            throw e;
        } finally {
            this.afterExecute();
        }
    }

    public JDBCResultSet executeQuery(String sql)
        throws SQLException
    {
        if (this instanceof JDBCPreparedStatementImpl) {
            setQueryString(sql);
            this.beforeExecute();
            try {
                return makeResultSet(getOriginal().executeQuery(sql));
            } catch (SQLException e) {
                this.handleExecuteError(e);
                throw e;
            } finally {
                this.afterExecute();
            }
        } else {
            throw new SQLFeatureNotSupportedException();
        }
    }

    public int executeUpdate(String sql)
        throws SQLException
    {
        setQueryString(sql);
        this.beforeExecute();
        try {
            return handleExecuteResult(getOriginal().executeUpdate(sql));
        } catch (SQLException e) {
            this.handleExecuteError(e);
            throw e;
        } finally {
            this.afterExecute();
        }
    }

    public int[] executeBatch()
        throws SQLException
    {
        this.beforeExecute();
        try {
            return getOriginal().executeBatch();
        } catch (SQLException e) {
            this.handleExecuteError(e);
            throw e;
        } finally {
            this.afterExecute();
        }
    }

    public int executeUpdate(String sql, int autoGeneratedKeys)
        throws SQLException
    {
        setQueryString(sql);
        this.beforeExecute();
        try {
            return handleExecuteResult(getOriginal().executeUpdate(sql, autoGeneratedKeys));
        } catch (SQLException e) {
            this.handleExecuteError(e);
            throw e;
        } finally {
            this.afterExecute();
        }
    }

    public int executeUpdate(String sql, int[] columnIndexes)
        throws SQLException
    {
        setQueryString(sql);
        this.beforeExecute();
        try {
            return handleExecuteResult(getOriginal().executeUpdate(sql, columnIndexes));
        } catch (SQLException e) {
            this.handleExecuteError(e);
            throw e;
        } finally {
            this.afterExecute();
        }
    }

    public int executeUpdate(String sql, String[] columnNames)
        throws SQLException
    {
        setQueryString(sql);
        this.beforeExecute();
        try {
            return handleExecuteResult(getOriginal().executeUpdate(sql, columnNames));
        } catch (SQLException e) {
            this.handleExecuteError(e);
            throw e;
        } finally {
            this.afterExecute();
        }
    }

    public boolean execute(String sql, int autoGeneratedKeys)
        throws SQLException
    {
        setQueryString(sql);
        this.beforeExecute();
        try {
            return handleExecuteResult(getOriginal().execute(sql, autoGeneratedKeys));
        } catch (SQLException e) {
            this.handleExecuteError(e);
            throw e;
        } finally {
            this.afterExecute();
        }
    }

    public boolean execute(String sql, int[] columnIndexes)
        throws SQLException
    {
        setQueryString(sql);
        this.beforeExecute();
        try {
            return handleExecuteResult(getOriginal().execute(sql, columnIndexes));
        } catch (SQLException e) {
            this.handleExecuteError(e);
            throw e;
        } finally {
            this.afterExecute();
        }
    }

    public boolean execute(String sql, String[] columnNames)
        throws SQLException
    {
        setQueryString(sql);
        this.beforeExecute();
        try {
            return handleExecuteResult(getOriginal().execute(sql, columnNames));
        } catch (SQLException e) {
            this.handleExecuteError(e);
            throw e;
        } finally {
            this.afterExecute();
        }
    }

    ////////////////////////////////////
    // Close

    public void close()
    {
        QMUtils.getDefaultHandler().handleStatementClose(this);
        try {
            getOriginal().close();
        }
        catch (SQLException e) {
            log.error("Could not close statement", e);
        }
    }

    ////////////////////////////////////
    // Other

    public int getMaxFieldSize()
        throws SQLException
    {
        return getOriginal().getMaxFieldSize();
    }

    public void setMaxFieldSize(int max)
        throws SQLException
    {
        getOriginal().setMaxFieldSize(max);
    }

    public int getMaxRows()
        throws SQLException
    {
        return getOriginal().getMaxRows();
    }

    public void setMaxRows(int max)
        throws SQLException
    {
        getOriginal().setMaxRows(max);
    }

    public void setEscapeProcessing(boolean enable)
        throws SQLException
    {
        getOriginal().setEscapeProcessing(enable);
    }

    public int getQueryTimeout()
        throws SQLException
    {
        return getOriginal().getQueryTimeout();
    }

    public void setQueryTimeout(int seconds)
        throws SQLException
    {
        getOriginal().setQueryTimeout(seconds);
    }

    public void cancel()
        throws SQLException
    {
        getOriginal().cancel();
    }

    public SQLWarning getWarnings()
        throws SQLException
    {
        return getOriginal().getWarnings();
    }

    public void clearWarnings()
        throws SQLException
    {
        getOriginal().clearWarnings();
    }

    public void setCursorName(String name)
        throws SQLException
    {
        getOriginal().setCursorName(name);
    }

    public JDBCResultSet getResultSet()
        throws SQLException
    {
        return makeResultSet(getOriginal().getResultSet());
    }

    public int getUpdateCount()
        throws SQLException
    {
        return getOriginal().getUpdateCount();
    }

    public boolean getMoreResults()
        throws SQLException
    {
        return getOriginal().getMoreResults();
    }

    public void setFetchDirection(int direction)
        throws SQLException
    {
        getOriginal().setFetchDirection(direction);
    }

    public int getFetchDirection()
        throws SQLException
    {
        return getOriginal().getFetchDirection();
    }

    public void setFetchSize(int rows)
        throws SQLException
    {
        getOriginal().setFetchSize(rows);
    }

    public int getFetchSize()
        throws SQLException
    {
        return getOriginal().getFetchSize();
    }

    public int getResultSetConcurrency()
        throws SQLException
    {
        return getOriginal().getResultSetConcurrency();
    }

    public int getResultSetType()
        throws SQLException
    {
        return getOriginal().getResultSetType();
    }

    public void addBatch(String sql)
        throws SQLException
    {
        getOriginal().addBatch(sql);
    }

    public void clearBatch()
        throws SQLException
    {
        getOriginal().clearBatch();
    }

    public boolean getMoreResults(int current)
        throws SQLException
    {
        return getOriginal().getMoreResults(current);
    }

    public ResultSet getGeneratedKeys()
        throws SQLException
    {
        return makeResultSet(getOriginal().getGeneratedKeys());
    }

    public int getResultSetHoldability()
        throws SQLException
    {
        return getOriginal().getResultSetHoldability();
    }

    public boolean isClosed()
        throws SQLException
    {
        return getOriginal().isClosed();
    }

    public void setPoolable(boolean poolable)
        throws SQLException
    {
        getOriginal().setPoolable(poolable);
    }

    public boolean isPoolable()
        throws SQLException
    {
        return getOriginal().isPoolable();
    }

    public <T> T unwrap(Class<T> iface)
        throws SQLException
    {
        return getOriginal().unwrap(iface);
    }

    public boolean isWrapperFor(Class<?> iface)
        throws SQLException
    {
        return getOriginal().isWrapperFor(iface);
    }

}
/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.dbc.DBCExecutionContext;
import org.jkiss.dbeaver.model.dbc.DBCQueryPurpose;
import org.jkiss.dbeaver.model.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;

import java.sql.*;

/**
 * Managable statement.
 * Stores information about execution in query manager and operated progress monitor.
 */
public abstract class StatementManagable implements JDBCStatement {

    static final Log log = LogFactory.getLog(StatementManagable.class);

    private ConnectionManagable connection;

    private String query;
    private String description;
    private DBCQueryPurpose queryPurpose;
    //private boolean blockStarted = false;
    private int rsOffset = -1;
    private int rsMaxRows = -1;

    private DBSObject dataContainer;

    public StatementManagable(ConnectionManagable connection)
    {
        this.connection = connection;
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

    protected void handleExecuteError(SQLException ex)
    {
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

    public ConnectionManagable getConnection()
    {
        return connection;
    }

    public String getQuery()
    {
        return query;
    }

    public void setQuery(String query)
    {
        this.query = query;
    }

    public String getDescription()
    {
        return description;
    }

    public JDBCStatement setDescription(String description)
    {
        this.description = description;
        return this;
    }

    public DBCQueryPurpose getQueryPurpose()
    {
        return queryPurpose;
    }

    public JDBCStatement setQueryPurpose(DBCQueryPurpose queryPurpose)
    {
        this.queryPurpose = queryPurpose;
        return this;
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

    public void setLimit(int offset, int limit) throws DBCException
    {
        try {
            if (offset <= 0) {
                // Just set max row num
                getOriginal().setMaxRows(limit);
                this.rsMaxRows = limit;
            } else {
                // Remember limit values - we'll use them in resultset fetch routine
                this.rsOffset = offset;
                this.rsMaxRows = limit;
                getOriginal().setMaxRows(offset + limit);
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

    private ResultSetManagable makeResultSet(ResultSet resultSet)
        throws SQLException {
        if (this instanceof PreparedStatementManagable) {
            ResultSetManagable dbResult = new ResultSetManagable((PreparedStatementManagable) this, resultSet);
            // Scroll result set if needed
            if (rsOffset > 0) {
                JDBCUtils.scrollResultSet(dbResult, rsOffset);
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

    ////////////////////////////////////
    // Executions

    public boolean execute(String sql)
        throws SQLException
    {
        setQuery(sql);
        this.startBlock();
        try {
            return getOriginal().execute(sql);
        } catch (SQLException e) {
            this.handleExecuteError(e);
            throw e;
        } finally {
            this.endBlock();
        }
    }

    public JDBCResultSet executeQuery(String sql)
        throws SQLException
    {
        if (this instanceof PreparedStatementManagable) {
            setQuery(sql);
            this.startBlock();
            try {
                return makeResultSet(getOriginal().executeQuery(sql));
            } catch (SQLException e) {
                this.handleExecuteError(e);
                throw e;
            } finally {
                this.endBlock();
            }
        } else {
            throw new SQLFeatureNotSupportedException();
        }
    }

    public int executeUpdate(String sql)
        throws SQLException
    {
        setQuery(sql);
        this.startBlock();
        try {
            return getOriginal().executeUpdate(sql);
        } catch (SQLException e) {
            this.handleExecuteError(e);
            throw e;
        } finally {
            this.endBlock();
        }
    }

    public int[] executeBatch()
        throws SQLException
    {
        this.startBlock();
        try {
            return getOriginal().executeBatch();
        } catch (SQLException e) {
            this.handleExecuteError(e);
            throw e;
        } finally {
            this.endBlock();
        }
    }

    public int executeUpdate(String sql, int autoGeneratedKeys)
        throws SQLException
    {
        setQuery(sql);
        this.startBlock();
        try {
            return getOriginal().executeUpdate(sql, autoGeneratedKeys);
        } catch (SQLException e) {
            this.handleExecuteError(e);
            throw e;
        } finally {
            this.endBlock();
        }
    }

    public int executeUpdate(String sql, int[] columnIndexes)
        throws SQLException
    {
        setQuery(sql);
        this.startBlock();
        try {
            return getOriginal().executeUpdate(sql, columnIndexes);
        } catch (SQLException e) {
            this.handleExecuteError(e);
            throw e;
        } finally {
            this.endBlock();
        }
    }

    public int executeUpdate(String sql, String[] columnNames)
        throws SQLException
    {
        setQuery(sql);
        this.startBlock();
        try {
            return getOriginal().executeUpdate(sql, columnNames);
        } catch (SQLException e) {
            this.handleExecuteError(e);
            throw e;
        } finally {
            this.endBlock();
        }
    }

    public boolean execute(String sql, int autoGeneratedKeys)
        throws SQLException
    {
        setQuery(sql);
        this.startBlock();
        try {
            return getOriginal().execute(sql, autoGeneratedKeys);
        } catch (SQLException e) {
            this.handleExecuteError(e);
            throw e;
        } finally {
            this.endBlock();
        }
    }

    public boolean execute(String sql, int[] columnIndexes)
        throws SQLException
    {
        setQuery(sql);
        this.startBlock();
        try {
            return getOriginal().execute(sql, columnIndexes);
        } catch (SQLException e) {
            this.handleExecuteError(e);
            throw e;
        } finally {
            this.endBlock();
        }
    }

    public boolean execute(String sql, String[] columnNames)
        throws SQLException
    {
        setQuery(sql);
        this.startBlock();
        try {
            return getOriginal().execute(sql, columnNames);
        } catch (SQLException e) {
            this.handleExecuteError(e);
            throw e;
        } finally {
            this.endBlock();
        }
    }

    ////////////////////////////////////
    // Close

    public void close()
    {
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
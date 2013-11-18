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
package org.jkiss.dbeaver.model.impl.jdbc.exec;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;

import java.sql.*;

/**
 * Managable statement.
 * Stores information about execution in query manager and operated progress monitor.
 */
public class JDBCStatementImpl<STATEMENT extends Statement> implements JDBCStatement {

    static final Log log = LogFactory.getLog(JDBCStatementImpl.class);

    protected final JDBCSession connection;
    protected final STATEMENT original;

    private String query;
    private String description;

    private long rsOffset = -1;
    private long rsMaxRows = -1;

    private DBSObject dataContainer;
    private int updateCount;
    private Throwable executeError;
    private Object userData;

    public JDBCStatementImpl(JDBCSession connection, STATEMENT original)
    {
        this.connection = connection;
        this.original = original;
        QMUtils.getDefaultHandler().handleStatementOpen(this);
    }

    protected STATEMENT getOriginal()
    {
        return original;
    }

    protected void startBlock()
    {
        this.connection.getProgressMonitor().startBlock(
            this,
            SQLUtils.stripTransformations(
                this.description == null ?
                    (query == null ? "?" : JDBCUtils.limitQueryLength(query, 200)) : //$NON-NLS-1$
                    this.description));
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

    @Override
    public JDBCSession getConnection()
    {
        return connection;
    }

    ////////////////////////////////////////////////////////////////////
    // DBC Statement overrides
    ////////////////////////////////////////////////////////////////////

    @Override
    public JDBCSession getSession()
    {
        return connection;
    }

    @Override
    public String getQueryString()
    {
        return query;
    }

    public void setQueryString(String query)
    {
        this.query = query;
    }

    @Override
    public String getDescription()
    {
        return description;
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

    protected void setDescription(String description)
    {
        this.description = description;
    }

    @Override
    public JDBCResultSet openResultSet() throws DBCException
    {
        try {
            return getResultSet();
        }
        catch (SQLException e) {
            throw new DBCException(e, connection.getDataSource());
        }
    }

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
    public boolean hasMoreResults()
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
            totalRows = (int)(offset + limit);
        }
        if (connection.getDataSource().getInfo().supportsResultSetLimit()) {
            try {
                getOriginal().setMaxRows(totalRows);
            }
            catch (SQLException e) {
                // [JDBC:ODBC] Probably setMaxRows is not supported. Just log this error
                // We'll use rsOffset and rsMaxRows anyway
                log.debug(getOriginal().getClass().getName() + ".setMaxRows not supported?", e);
            }
        }
    }

    @Override
    public DBSObject getDataContainer()
    {
        return this.dataContainer;
    }

    @Override
    public void setDataContainer(DBSObject container)
    {
        this.dataContainer = container;
    }

    @Override
    public Object getSource()
    {
        return userData;
    }

    @Override
    public void setSource(Object userData)
    {
        this.userData = userData;
    }

    private JDBCResultSetImpl makeResultSet(ResultSet resultSet)
        throws SQLException
    {
        if (resultSet == null) {
            return null;
        }
        JDBCResultSetImpl dbResult = createResultSetImpl(resultSet);
        // Scroll original result set if needed
        if (rsOffset > 0) {
            JDBCUtils.scrollResultSet(resultSet, rsOffset);
        }

        if (rsMaxRows > 0 && connection.getDataSource().getInfo().supportsResultSetLimit()) {
            dbResult.setMaxRows(rsMaxRows);
        }
        return dbResult;
    }

    protected JDBCResultSetImpl createResultSetImpl(ResultSet resultSet)
    {
        return new JDBCResultSetImpl(this, resultSet);
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
                log.warn("Could not obtain update count", e); //$NON-NLS-1$
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

    protected SQLException handleExecuteError(Throwable ex)
    {
        executeError = ex;
        if (connection.getDataSource().getContainer().getPreferenceStore().getBoolean(PrefConstants.QUERY_ROLLBACK_ON_ERROR)) {
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
            return new SQLException(CoreMessages.model_jdbc_exception_internal_jdbc_driver_error, ex);
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

    @Override
    public JDBCResultSet executeQuery(String sql)
        throws SQLException
    {
        if (this instanceof JDBCPreparedStatementImpl) {
            setQueryString(sql);
            this.beforeExecute();
            try {
                return makeResultSet(getOriginal().executeQuery(sql));
            } catch (Throwable e) {
                throw this.handleExecuteError(e);
            } finally {
                this.afterExecute();
            }
        } else {
            throw new SQLFeatureNotSupportedException();
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
            log.debug("Could not check for statement warnings", e);
        }
*/

        // Handle close
        QMUtils.getDefaultHandler().handleStatementClose(this);

        // Close statement
        try {
            getOriginal().close();
        }
        catch (SQLException e) {
            log.error("Could not close statement", e); //$NON-NLS-1$
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

    @Override
    public JDBCResultSet getResultSet()
        throws SQLException
    {
        return makeResultSet(getOriginal().getResultSet());
    }

    @Override
    public int getUpdateCount() throws SQLException
    {
        if (updateCount >= 0) {
            return updateCount;
        }
        return getOriginal().getUpdateCount();
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
        return JDBCUtils.callMethod17(getOriginal(), "isCloseOnCompletion", Boolean.TYPE, null);
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

}
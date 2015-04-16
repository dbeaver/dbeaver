/*
 * Copyright (C) 2010-2015 Serge Rieder
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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.*;
import org.jkiss.dbeaver.model.impl.AbstractSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCObjectValueHandler;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.runtime.DBRBlockingObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * Managable connection
 */
public class JDBCConnectionImpl extends AbstractSession implements JDBCSession, DBRBlockingObject {

    static final Log log = Log.getLog(JDBCConnectionImpl.class);

    final JDBCExecutionContext context;

    public JDBCConnectionImpl(JDBCExecutionContext context, DBRProgressMonitor monitor, DBCExecutionPurpose purpose, String taskTitle)
    {
        super(monitor, purpose, taskTitle);
        this.context = context;
    }

    @Override
    public Connection getOriginal() throws SQLException
    {
        return context.getConnection(getProgressMonitor());
    }

    @Override
    public DBCExecutionContext getExecutionContext() {
        return context;
    }

    @Override
    public DBPDataSource getDataSource()
    {
        return context.getDataSource();
    }

    @Override
    public boolean isConnected() {
        try {
            synchronized (context) {
                return !isClosed();
            }
        } catch (SQLException e) {
            log.error("could not check connection state", e);
            return false;
        }
    }

    @Override
    public JDBCStatement prepareStatement(
        DBCStatementType type,
        String sqlQuery,
        boolean scrollable,
        boolean updatable,
        boolean returnGeneratedKeys)
        throws DBCException
    {
        try {
            if (type == DBCStatementType.EXEC) {
                // Execute as call
                try {
                    return prepareCall(
                        sqlQuery,
                        scrollable ? ResultSet.TYPE_SCROLL_INSENSITIVE : ResultSet.TYPE_FORWARD_ONLY,
                        updatable ? ResultSet.CONCUR_UPDATABLE : ResultSet.CONCUR_READ_ONLY);
                }
                catch (SQLFeatureNotSupportedException e) {
                    return prepareCall(sqlQuery);
                }
                catch (UnsupportedOperationException e) {
                    return prepareCall(sqlQuery);
                }
                catch (IncompatibleClassChangeError e) {
                    return prepareCall(sqlQuery);
                }
            } else if (type == DBCStatementType.SCRIPT) {
                // Just simplest statement for scripts
                // Sometimes prepared statements perform additional checks of queries
                // (e.g. in Oracle it parses IN/OUT parameters)
                JDBCStatement statement;
                try {
                    statement = createStatement(
                        scrollable ? ResultSet.TYPE_SCROLL_INSENSITIVE : ResultSet.TYPE_FORWARD_ONLY,
                        updatable ? ResultSet.CONCUR_UPDATABLE : ResultSet.CONCUR_READ_ONLY);
                }
                catch (UnsupportedOperationException e) {
                    statement = createStatement();
                }
                catch (IncompatibleClassChangeError e) {
                    statement = createStatement();
                }
                if (statement instanceof JDBCStatementImpl) {
                    ((JDBCStatementImpl)statement).setQueryString(sqlQuery);
                }
                return statement;
            } else if (returnGeneratedKeys) {
                // Return keys
                try {
                    return prepareStatement(
                        sqlQuery,
                        Statement.RETURN_GENERATED_KEYS);
                }
                catch (SQLFeatureNotSupportedException e) {
                    return prepareStatement(sqlQuery);
                }
                catch (UnsupportedOperationException e) {
                    return prepareStatement(sqlQuery);
                }
                catch (IncompatibleClassChangeError e) {
                    return prepareStatement(sqlQuery);
                }
            } else {
                try {
                    // Generic prepared statement
                    return prepareStatement(
                        sqlQuery,
                        scrollable ? ResultSet.TYPE_SCROLL_INSENSITIVE : ResultSet.TYPE_FORWARD_ONLY,
                        updatable ? ResultSet.CONCUR_UPDATABLE : ResultSet.CONCUR_READ_ONLY);
                }
                catch (UnsupportedOperationException e) {
                    return prepareStatement(sqlQuery);
                }
                catch (IncompatibleClassChangeError e) {
                    return prepareStatement(sqlQuery);
                }
            }
        }
        catch (SQLException e) {
            throw new JDBCException(e, getDataSource());
        }
    }

    @NotNull
    @Override
    public DBDValueHandler getDefaultValueHandler()
    {
        return JDBCObjectValueHandler.INSTANCE;
    }

    private JDBCStatement makeStatement(Statement statement)
        throws SQLFeatureNotSupportedException
    {
        if (statement instanceof CallableStatement) {
            return createCallableStatementImpl((CallableStatement)statement, null);
        } else if (statement instanceof PreparedStatement) {
            return createPreparedStatementImpl((PreparedStatement)statement, null);
        } else {
            throw new SQLFeatureNotSupportedException();
        }
    }

    @Override
    public JDBCStatement createStatement()
        throws SQLException
    {
        return makeStatement(getOriginal().createStatement());
    }

    @Override
    public JDBCPreparedStatement prepareStatement(String sql)
        throws SQLException
    {
        return createPreparedStatementImpl(getOriginal().prepareStatement(sql), sql);
    }

    @Override
    public JDBCCallableStatement prepareCall(String sql)
        throws SQLException
    {
        return createCallableStatementImpl(getOriginal().prepareCall(sql), sql);
    }

    @Override
    public String nativeSQL(String sql)
        throws SQLException
    {
        return getOriginal().nativeSQL(sql);
    }

    @Override
    public void setAutoCommit(boolean autoCommit)
        throws SQLException
    {
        getOriginal().setAutoCommit(autoCommit);

        if (isLoggingEnabled()) {
            QMUtils.getDefaultHandler().handleTransactionAutocommit(context, autoCommit);
        }
    }

    @Override
    public boolean getAutoCommit()
        throws SQLException
    {
        return getOriginal().getAutoCommit();
    }

    @Override
    public void commit()
        throws SQLException
    {
        getOriginal().commit();

        if (isLoggingEnabled()) {
            QMUtils.getDefaultHandler().handleTransactionCommit(context);
        }
    }

    @Override
    public void rollback()
        throws SQLException
    {
        getOriginal().rollback();

        if (isLoggingEnabled()) {
            QMUtils.getDefaultHandler().handleTransactionRollback(context, null);
        }
    }

    @Override
    public void close()
    {
        DBCExecutionPurpose purpose = getPurpose();
        if (purpose == DBCExecutionPurpose.USER || purpose == DBCExecutionPurpose.USER_SCRIPT) {
            // Check for warnings
            try {
                final Connection connection = getOriginal();
                if (connection != null) {
                    JDBCUtils.reportWarnings(this, connection.getWarnings());
                    connection.clearWarnings();
                }
            } catch (Throwable e) {
                log.debug("Could not check for connection warnings", e);
            }
        }

        super.close();
    }

    @Override
    public boolean isClosed()
        throws SQLException
    {
        return getOriginal().isClosed();
    }

    @Override
    public JDBCDatabaseMetaData getMetaData()
        throws SQLException
    {
        return new JDBCDatabaseMetaDataImpl(this, getOriginal().getMetaData());
    }

    @Override
    public void setReadOnly(boolean readOnly)
        throws SQLException
    {
        getOriginal().setReadOnly(readOnly);
    }

    @Override
    public boolean isReadOnly()
        throws SQLException
    {
        return getOriginal().isReadOnly();
    }

    @Override
    public void setCatalog(String catalog)
        throws SQLException
    {
        getOriginal().setCatalog(catalog);
    }

    @Override
    public String getCatalog()
        throws SQLException
    {
        return getOriginal().getCatalog();
    }

    @Override
    public void setTransactionIsolation(int level)
        throws SQLException
    {
        getOriginal().setTransactionIsolation(level);
    }

    @Override
    public int getTransactionIsolation()
        throws SQLException
    {
        return getOriginal().getTransactionIsolation();
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
    public JDBCStatement createStatement(int resultSetType, int resultSetConcurrency)
        throws SQLException
    {
        return createStatementImpl(
            getOriginal().createStatement(resultSetType, resultSetConcurrency));
    }

    @Override
    public JDBCPreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
        throws SQLException
    {
        return createPreparedStatementImpl(
            getOriginal().prepareStatement(sql, resultSetType, resultSetConcurrency),
            sql);
    }

    @Override
    public JDBCCallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency)
        throws SQLException
    {
        return createCallableStatementImpl(getOriginal().prepareCall(sql, resultSetType, resultSetConcurrency), sql);
    }

    @Override
    public Map<String, Class<?>> getTypeMap()
        throws SQLException
    {
        return getOriginal().getTypeMap();
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map)
        throws SQLException
    {
        getOriginal().setTypeMap(map);
    }

    @Override
    public void setHoldability(int holdability)
        throws SQLException
    {
        getOriginal().setHoldability(holdability);
    }

    @Override
    public int getHoldability()
        throws SQLException
    {
        return getOriginal().getHoldability();
    }

    @Override
    public Savepoint setSavepoint()
        throws SQLException
    {
        Savepoint savepoint = getOriginal().setSavepoint();

        JDBCSavepointImpl jdbcSavepoint = new JDBCSavepointImpl(context, savepoint);

        if (isLoggingEnabled()) {
            QMUtils.getDefaultHandler().handleTransactionSavepoint(jdbcSavepoint);
        }

        return jdbcSavepoint;
    }

    @Override
    public Savepoint setSavepoint(String name)
        throws SQLException
    {
        Savepoint savepoint = getOriginal().setSavepoint(name);

        JDBCSavepointImpl jdbcSavepoint = new JDBCSavepointImpl(context, savepoint);

        if (isLoggingEnabled()) {
            QMUtils.getDefaultHandler().handleTransactionSavepoint(jdbcSavepoint);
        }

        return jdbcSavepoint;
    }

    @Override
    public void rollback(Savepoint savepoint)
        throws SQLException
    {
        if (savepoint instanceof JDBCSavepointImpl) {
            savepoint = ((JDBCSavepointImpl)savepoint).getOriginal();
        }
        getOriginal().rollback(savepoint);

        if (isLoggingEnabled()) {
            QMUtils.getDefaultHandler().handleTransactionRollback(context, savepoint instanceof DBCSavepoint ? (DBCSavepoint) savepoint : null);
        }
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint)
        throws SQLException
    {
        if (savepoint instanceof JDBCSavepointImpl) {
            savepoint = ((JDBCSavepointImpl)savepoint).getOriginal();
        }
        getOriginal().releaseSavepoint(savepoint);
    }

    @Override
    public JDBCStatement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
        throws SQLException
    {
        return makeStatement(getOriginal().createStatement(resultSetType, resultSetConcurrency, resultSetHoldability));
    }

    @Override
    public JDBCPreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
        throws SQLException
    {
        return createPreparedStatementImpl(
            getOriginal().prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability),
            sql);
    }

    @Override
    public JDBCCallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
        throws SQLException
    {
        return createCallableStatementImpl(
            getOriginal().prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability),
            sql);
    }

    @Override
    public JDBCPreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
        throws SQLException
    {
        return createPreparedStatementImpl(getOriginal().prepareStatement(sql, autoGeneratedKeys), sql);
    }

    @Override
    public JDBCPreparedStatement prepareStatement(String sql, int[] columnIndexes)
        throws SQLException
    {
        return createPreparedStatementImpl(getOriginal().prepareStatement(sql, columnIndexes), sql);
    }

    @Override
    public JDBCPreparedStatement prepareStatement(String sql, String[] columnNames)
        throws SQLException
    {
        return createPreparedStatementImpl(getOriginal().prepareStatement(sql, columnNames), sql);
    }

    @Nullable
    @Override
    public String getSchema() throws SQLException
    {
        return JDBCUtils.callMethod17(getOriginal(), "getSchema", String.class, null);
    }

    @Override
    public void setSchema(String schema) throws SQLException
    {
        JDBCUtils.callMethod17(getOriginal(), "setSchema", null, new Class[]{String.class}, schema);
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        JDBCUtils.callMethod17(getOriginal(), "abort", null, new Class[]{Executor.class}, executor);
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        JDBCUtils.callMethod17(getOriginal(), "setNetworkTimeout", null, new Class[]{Executor.class, Integer.TYPE}, executor, milliseconds);
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        Integer networkTimeout = JDBCUtils.callMethod17(getOriginal(), "getNetworkTimeout", Integer.class, null);
        return networkTimeout == null ? 0 : networkTimeout;
    }

    @Override
    public Clob createClob()
        throws SQLException
    {
        return getOriginal().createClob();
    }

    @Override
    public Blob createBlob()
        throws SQLException
    {
        return getOriginal().createBlob();
    }

    @Override
    public NClob createNClob()
        throws SQLException
    {
        return getOriginal().createNClob();
    }

    @Override
    public SQLXML createSQLXML()
        throws SQLException
    {
        return getOriginal().createSQLXML();
    }

    @Override
    public boolean isValid(int timeout)
        throws SQLException
    {
        return getOriginal().isValid(timeout);
    }

    @Override
    public void setClientInfo(String name, String value)
        throws SQLClientInfoException
    {
        try {
            getOriginal().setClientInfo(name, value);
        } catch (SQLException e) {
            if (e instanceof SQLClientInfoException) {
                throw (SQLClientInfoException)e;
            } else {
                throw new SQLClientInfoException();
            }
        }
    }

    @Override
    public void setClientInfo(Properties properties)
        throws SQLClientInfoException
    {
        try {
            getOriginal().setClientInfo(properties);
        } catch (SQLException e) {
            if (e instanceof SQLClientInfoException) {
                throw (SQLClientInfoException)e;
            } else {
                log.debug(e);
                throw new SQLClientInfoException();
            }
        }
    }

    @Override
    public String getClientInfo(String name)
        throws SQLException
    {
        return getOriginal().getClientInfo(name);
    }

    @Override
    public Properties getClientInfo()
        throws SQLException
    {
        return getOriginal().getClientInfo();
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements)
        throws SQLException
    {
        return getOriginal().createArrayOf(typeName, elements);
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes)
        throws SQLException
    {
        return getOriginal().createStruct(typeName, attributes);
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
    public void cancelBlock()
        throws DBException
    {
        try {
            getOriginal().close();
        }
        catch (SQLException e) {
            throw new DBCException(e, getDataSource());
        }
    }

    protected JDBCStatement createStatementImpl(Statement original)
        throws SQLFeatureNotSupportedException
    {
        return new JDBCStatementImpl<Statement>(this, original, !isLoggingEnabled());
    }

    protected JDBCPreparedStatement createPreparedStatementImpl(PreparedStatement original, @Nullable String sql)
        throws SQLFeatureNotSupportedException
    {
        return new JDBCPreparedStatementImpl(this, original, sql, !isLoggingEnabled());
    }

    protected JDBCCallableStatement createCallableStatementImpl(CallableStatement original, @Nullable String sql)
        throws SQLFeatureNotSupportedException
    {
        return new JDBCCallableStatementImpl(this, original, sql, !isLoggingEnabled());
    }

}

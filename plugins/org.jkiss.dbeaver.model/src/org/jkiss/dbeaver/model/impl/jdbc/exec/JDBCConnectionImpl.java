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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPErrorAssistant;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.*;
import org.jkiss.dbeaver.model.impl.AbstractSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
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

    private static final Log log = Log.getLog(JDBCConnectionImpl.class);

    @NotNull
    final JDBCExecutionContext context;

    public JDBCConnectionImpl(@NotNull JDBCExecutionContext context, @NotNull DBRProgressMonitor monitor, @NotNull DBCExecutionPurpose purpose, @NotNull String taskTitle)
    {
        super(monitor, purpose, taskTitle);
        this.context = context;
    }

    @Override
    public Connection getOriginal() throws SQLException
    {
        return context.getConnection(getProgressMonitor());
    }

    @NotNull
    @Override
    public DBCExecutionContext getExecutionContext() {
        return context;
    }

    @NotNull
    @Override
    public JDBCDataSource getDataSource()
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

    @NotNull
    @Override
    public JDBCStatement prepareStatement(
        @NotNull DBCStatementType type,
        @NotNull String sqlQuery,
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
                catch (SQLFeatureNotSupportedException | UnsupportedOperationException | IncompatibleClassChangeError e) {
                    return prepareCall(sqlQuery);
                }
                catch (SQLException e) {
                    if (DBUtils.discoverErrorType(getDataSource(), e) == DBPErrorAssistant.ErrorType.FEATURE_UNSUPPORTED) {
                        return prepareCall(sqlQuery);
                    } else {
                        throw e;
                    }
                }
            } else if (type == DBCStatementType.SCRIPT) {
                // Just simplest statement for scripts
                // Sometimes prepared statements perform additional checks of queries
                // (e.g. in Oracle it parses IN/OUT parameters)
                JDBCStatement statement;
                try {
                    if (!scrollable && !updatable) {
                        statement = createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                    } else {
                        statement = createStatement(
                                scrollable ? ResultSet.TYPE_SCROLL_INSENSITIVE : ResultSet.TYPE_FORWARD_ONLY,
                                updatable ? ResultSet.CONCUR_UPDATABLE : ResultSet.CONCUR_READ_ONLY);
                    }
                }
                catch (Throwable e) {
                    try {
                        statement = createStatement();
                    } catch (Throwable e1) {
                        try {
                            statement = prepareStatement(
                                sqlQuery,
                                scrollable ? ResultSet.TYPE_SCROLL_INSENSITIVE : ResultSet.TYPE_FORWARD_ONLY,
                                updatable ? ResultSet.CONCUR_UPDATABLE : ResultSet.CONCUR_READ_ONLY);
                        } catch (Throwable e2) {
                            log.debug(e);
                            statement = prepareStatement(sqlQuery);
                        }
                    }
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
                catch (SQLFeatureNotSupportedException | UnsupportedOperationException | IncompatibleClassChangeError e) {
                    return prepareStatement(sqlQuery);
                }
                catch (SQLException e) {
                    if (DBUtils.discoverErrorType(getDataSource(), e) == DBPErrorAssistant.ErrorType.FEATURE_UNSUPPORTED) {
                        return prepareStatement(sqlQuery);
                    } else {
                        throw e;
                    }
                }
            } else {
                try {
                    // Generic prepared statement
                    return prepareStatement(
                        sqlQuery,
                        scrollable ? ResultSet.TYPE_SCROLL_INSENSITIVE : ResultSet.TYPE_FORWARD_ONLY,
                        updatable ? ResultSet.CONCUR_UPDATABLE : ResultSet.CONCUR_READ_ONLY);
                }
                catch (SQLFeatureNotSupportedException | UnsupportedOperationException | IncompatibleClassChangeError e) {
                    return prepareStatement(sqlQuery);
                }
                catch (SQLException e) {
                    if (DBUtils.discoverErrorType(getDataSource(), e) == DBPErrorAssistant.ErrorType.FEATURE_UNSUPPORTED) {
                        return prepareStatement(sqlQuery);
                    } else {
                        throw e;
                    }
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
        return context.getDataSource().getDefaultValueHandler();
    }

    private JDBCStatement makeStatement(Statement statement)
        throws SQLException
    {
        if (statement instanceof CallableStatement) {
            return createCallableStatementImpl((CallableStatement)statement, null);
        } else if (statement instanceof PreparedStatement) {
            return createPreparedStatementImpl((PreparedStatement)statement, null);
        } else {
            return createStatementImpl(statement);
        }
    }

    @NotNull
    @Override
    public JDBCStatement createStatement()
        throws SQLException
    {
        return makeStatement(getOriginal().createStatement());
    }

    @NotNull
    @Override
    public JDBCPreparedStatement prepareStatement(String sql)
        throws SQLException
    {
        return createPreparedStatementImpl(getOriginal().prepareStatement(sql), sql);
    }

    @NotNull
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
        if (purpose.isUser()) {
            // Check for warnings
            try {
                final Connection connection = getOriginal();
                if (connection != null) {
                    JDBCUtils.reportWarnings(this, connection.getWarnings());
                    connection.clearWarnings();
                }
            } catch (Throwable e) {
                log.debug("Can't check for connection warnings", e);
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

    @NotNull
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
        return createStatementImpl(getOriginal().createStatement(resultSetType, resultSetConcurrency));
    }

    @NotNull
    @Override
    public JDBCPreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
        throws SQLException
    {
        return createPreparedStatementImpl(
            getOriginal().prepareStatement(sql, resultSetType, resultSetConcurrency),
            sql);
    }

    @NotNull
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

    @NotNull
    @Override
    public JDBCStatement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
        throws SQLException
    {
        return makeStatement(getOriginal().createStatement(resultSetType, resultSetConcurrency, resultSetHoldability));
    }

    @NotNull
    @Override
    public JDBCPreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
        throws SQLException
    {
        return createPreparedStatementImpl(
            getOriginal().prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability),
            sql);
    }

    @NotNull
    @Override
    public JDBCCallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
        throws SQLException
    {
        return createCallableStatementImpl(
            getOriginal().prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability),
            sql);
    }

    @NotNull
    @Override
    public JDBCPreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
        throws SQLException
    {
        return createPreparedStatementImpl(getOriginal().prepareStatement(sql, autoGeneratedKeys), sql);
    }

    @NotNull
    @Override
    public JDBCPreparedStatement prepareStatement(String sql, int[] columnIndexes)
        throws SQLException
    {
        return createPreparedStatementImpl(getOriginal().prepareStatement(sql, columnIndexes), sql);
    }

    @NotNull
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
        JDBCUtils.callMethod17(getOriginal(), "setSchema", null, new Class<?>[]{String.class}, schema);
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        JDBCUtils.callMethod17(getOriginal(), "abort", null, new Class<?>[]{Executor.class}, executor);
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        JDBCUtils.callMethod17(getOriginal(), "setNetworkTimeout", null, new Class<?>[]{Executor.class, Integer.TYPE}, executor, milliseconds);
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
        throws SQLException,IllegalArgumentException
    {
        if (original == null) {
            throw new IllegalArgumentException("Null statement");
        }
        return context.getDataSource().getJdbcFactory().createStatement(this, original, !isLoggingEnabled());
    }

    protected JDBCPreparedStatement createPreparedStatementImpl(PreparedStatement original, @Nullable String sql)
        throws SQLException,IllegalArgumentException
    {
        if (original == null) {
            throw new IllegalArgumentException("Null statement");
        }
        return context.getDataSource().getJdbcFactory().createPreparedStatement(this, original, sql, !isLoggingEnabled());
    }

    protected JDBCCallableStatement createCallableStatementImpl(CallableStatement original, @Nullable String sql)
        throws SQLException,IllegalArgumentException
    {
        if (original == null) {
            throw new IllegalArgumentException("Null statement");
        }
        return context.getDataSource().getJdbcFactory().createCallableStatement(this, original, sql, !isLoggingEnabled());
    }


}

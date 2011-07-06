/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.*;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCTransactionIsolation;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.runtime.DBRBlockingObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.*;
import java.util.Map;
import java.util.Properties;

/**
 * Managable connection
 */
public class JDBCConnectionImpl implements JDBCExecutionContext, DBRBlockingObject {

    static final Log log = LogFactory.getLog(JDBCConnectionImpl.class);

    private JDBCConnector connector;
    private DBRProgressMonitor monitor;
    private DBCExecutionPurpose purpose;
    private String taskTitle;
    private boolean isolated;
    private Connection isolatedConnection;
    private DBDDataFormatterProfile dataFormatterProfile;

    public JDBCConnectionImpl(JDBCConnector connector, DBRProgressMonitor monitor, DBCExecutionPurpose purpose, String taskTitle, boolean isolated)
    {
        this.connector = connector;
        this.monitor = monitor;
        this.purpose = purpose;
        this.taskTitle = taskTitle;
        this.isolated = isolated;
        this.isolatedConnection = null;
        this.dataFormatterProfile = connector.getDataSource().getContainer().getDataFormatterProfile();

        if (taskTitle != null) {
            monitor.startBlock(this, taskTitle);
        }

        QMUtils.getDefaultHandler().handleContextOpen(this);
    }

    public Connection getOriginal()
    {
        if (isolatedConnection != null) {
            return isolatedConnection;
        } else {
            return connector.getConnection();
        }
    }

    private Connection getConnection()
        throws SQLException
    {
        if (isolated) {
            if (isolatedConnection == null) {
                isolatedConnection = connector.openIsolatedConnection();
            }
            return isolatedConnection;
        } else {
            return connector.getConnection();
        }
    }

    public String getTaskTitle()
    {
        return taskTitle;
    }

    public DBPDataSource getDataSource()
    {
        return connector.getDataSource();
    }

    public boolean isConnected() {
        try {
            return !isClosed();
        } catch (SQLException e) {
            log.error("could not check connection state", e);
            return false;
        }
    }

    public DBRProgressMonitor getProgressMonitor()
    {
        return monitor;
    }

    public DBCTransactionManager getTransactionManager()
    {
        return new TransactionManager();
    }

    public DBCExecutionPurpose getPurpose()
    {
        return purpose;
    }

    public DBDDataFormatterProfile getDataFormatterProfile()
    {
        return dataFormatterProfile;
    }

    public void setDataFormatterProfile(DBDDataFormatterProfile formatterProfile)
    {
        dataFormatterProfile = formatterProfile;
    }

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
                try {
                    return prepareCall(
                        sqlQuery,
                        scrollable ? ResultSet.TYPE_SCROLL_INSENSITIVE : ResultSet.TYPE_FORWARD_ONLY,
                        updatable ? ResultSet.CONCUR_UPDATABLE : ResultSet.CONCUR_READ_ONLY);
                }
                catch (AbstractMethodError e) {
                    return prepareCall(sqlQuery);
                }
                catch (UnsupportedOperationException e) {
                    return prepareCall(sqlQuery);
                }
            } else if (returnGeneratedKeys) {
                try {
                    return prepareStatement(
                        sqlQuery,
                        Statement.RETURN_GENERATED_KEYS);
                }
                catch (AbstractMethodError e) {
                    return prepareStatement(sqlQuery);
                }
                catch (UnsupportedOperationException e) {
                    return prepareStatement(sqlQuery);
                }
                catch (SQLException e) {
                    return prepareStatement(sqlQuery);
                }
            } else {
                return prepareStatement(
                    sqlQuery,
                    scrollable ? ResultSet.TYPE_SCROLL_INSENSITIVE : ResultSet.TYPE_FORWARD_ONLY,
                    updatable ? ResultSet.CONCUR_UPDATABLE : ResultSet.CONCUR_READ_ONLY);
            }
        }
        catch (SQLException e) {
            throw new JDBCException(e);
        }
    }

    private JDBCStatement makeStatement(Statement statement)
        throws SQLFeatureNotSupportedException
    {
        if (statement instanceof CallableStatement) {
            return new JDBCCallableStatementImpl(this, (CallableStatement)statement, null);
        } else if (statement instanceof PreparedStatement) {
            return new JDBCPreparedStatementImpl(this, (PreparedStatement)statement, null);
        } else {
            throw new SQLFeatureNotSupportedException();
        }
    }

    public JDBCStatement createStatement()
        throws SQLException
    {
        return makeStatement(getConnection().createStatement());
    }

    public JDBCPreparedStatement prepareStatement(String sql)
        throws SQLException
    {
        return new JDBCPreparedStatementImpl(this, getConnection().prepareStatement(sql), sql);
    }

    public JDBCCallableStatement prepareCall(String sql)
        throws SQLException
    {
        return new JDBCCallableStatementImpl(this, getConnection().prepareCall(sql), sql);
    }

    public String nativeSQL(String sql)
        throws SQLException
    {
        return getConnection().nativeSQL(sql);
    }

    public void setAutoCommit(boolean autoCommit)
        throws SQLException
    {
        getConnection().setAutoCommit(autoCommit);

        QMUtils.getDefaultHandler().handleTransactionAutocommit(this, autoCommit);
    }

    public boolean getAutoCommit()
        throws SQLException
    {
        return getConnection().getAutoCommit();
    }

    public void commit()
        throws SQLException
    {
        getConnection().commit();

        QMUtils.getDefaultHandler().handleTransactionCommit(this);
    }

    public void rollback()
        throws SQLException
    {
        getConnection().rollback();

        QMUtils.getDefaultHandler().handleTransactionRollback(this, null);
    }

    public void close()
    {
        // Check for warnings
        try {
            final Connection connection = getConnection();
            if (connection != null) {
                JDBCUtils.reportWarnings(this, connection.getWarnings());
                connection.clearWarnings();
            }
        } catch (Throwable e) {
            log.debug("Could not check for connection warnings", e);
        }
        // End context
        if (taskTitle != null) {
            monitor.endBlock();
        }
        if (isolatedConnection != null) {
            try {
                isolatedConnection.close();
            } catch (SQLException e) {
                log.error("Error closing isolated connection", e);
            }
            isolatedConnection = null;
        } else {
            // do nothing
            // closing of context doesn't close real connection
        }

        QMUtils.getDefaultHandler().handleContextClose(this);
    }

    public boolean isClosed()
        throws SQLException
    {
        return getConnection().isClosed();
    }

    public JDBCDatabaseMetaData getMetaData()
        throws SQLException
    {
        return new JDBCDatabaseMetaDataImpl(this, getConnection().getMetaData());
    }

    public void setReadOnly(boolean readOnly)
        throws SQLException
    {
        getConnection().setReadOnly(readOnly);
    }

    public boolean isReadOnly()
        throws SQLException
    {
        return getConnection().isReadOnly();
    }

    public void setCatalog(String catalog)
        throws SQLException
    {
        getConnection().setCatalog(catalog);
    }

    public String getCatalog()
        throws SQLException
    {
        return getConnection().getCatalog();
    }

    public void setTransactionIsolation(int level)
        throws SQLException
    {
        getConnection().setTransactionIsolation(level);
    }

    public int getTransactionIsolation()
        throws SQLException
    {
        return getConnection().getTransactionIsolation();
    }

    public SQLWarning getWarnings()
        throws SQLException
    {
        return getConnection().getWarnings();
    }

    public void clearWarnings()
        throws SQLException
    {
        getConnection().clearWarnings();
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency)
        throws SQLException
    {
        return getConnection().createStatement(resultSetType, resultSetConcurrency);
    }

    public JDBCPreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
        throws SQLException
    {
        return new JDBCPreparedStatementImpl(
            this,
            getConnection().prepareStatement(sql, resultSetType, resultSetConcurrency),
            sql);
    }

    public JDBCCallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency)
        throws SQLException
    {
        return new JDBCCallableStatementImpl(this, getConnection().prepareCall(sql, resultSetType, resultSetConcurrency), sql);
    }

    public Map<String, Class<?>> getTypeMap()
        throws SQLException
    {
        return getConnection().getTypeMap();
    }

    public void setTypeMap(Map<String, Class<?>> map)
        throws SQLException
    {
        getConnection().setTypeMap(map);
    }

    public void setHoldability(int holdability)
        throws SQLException
    {
        getConnection().setHoldability(holdability);
    }

    public int getHoldability()
        throws SQLException
    {
        return getConnection().getHoldability();
    }

    public Savepoint setSavepoint()
        throws SQLException
    {
        Savepoint savepoint = getConnection().setSavepoint();

        JDBCSavepointImpl jdbcSavepoint = new JDBCSavepointImpl(this, savepoint);

        QMUtils.getDefaultHandler().handleTransactionSavepoint(jdbcSavepoint);

        return jdbcSavepoint;
    }

    public Savepoint setSavepoint(String name)
        throws SQLException
    {
        Savepoint savepoint = getConnection().setSavepoint(name);

        JDBCSavepointImpl jdbcSavepoint = new JDBCSavepointImpl(this, savepoint);

        QMUtils.getDefaultHandler().handleTransactionSavepoint(jdbcSavepoint);

        return jdbcSavepoint;
    }

    public void rollback(Savepoint savepoint)
        throws SQLException
    {
        if (savepoint instanceof JDBCSavepointImpl) {
            savepoint = ((JDBCSavepointImpl)savepoint).getOriginal();
        }
        getConnection().rollback(savepoint);

        QMUtils.getDefaultHandler().handleTransactionRollback(this, savepoint instanceof DBCSavepoint ? (DBCSavepoint) savepoint : null);
    }

    public void releaseSavepoint(Savepoint savepoint)
        throws SQLException
    {
        if (savepoint instanceof JDBCSavepointImpl) {
            savepoint = ((JDBCSavepointImpl)savepoint).getOriginal();
        }
        getConnection().releaseSavepoint(savepoint);
    }

    public JDBCStatement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
        throws SQLException
    {
        return makeStatement(getConnection().createStatement(resultSetType, resultSetConcurrency, resultSetHoldability));
    }

    public JDBCPreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
        throws SQLException
    {
        return new JDBCPreparedStatementImpl(
            this,
            getConnection().prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability),
            sql);
    }

    public JDBCCallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
        throws SQLException
    {
        return new JDBCCallableStatementImpl(
            this,
            getConnection().prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability),
            sql);
    }

    public JDBCPreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
        throws SQLException
    {
        return new JDBCPreparedStatementImpl(this, getConnection().prepareStatement(sql, autoGeneratedKeys), sql);
    }

    public JDBCPreparedStatement prepareStatement(String sql, int[] columnIndexes)
        throws SQLException
    {
        return new JDBCPreparedStatementImpl(this, getConnection().prepareStatement(sql, columnIndexes), sql);
    }

    public JDBCPreparedStatement prepareStatement(String sql, String[] columnNames)
        throws SQLException
    {
        return new JDBCPreparedStatementImpl(this, getConnection().prepareStatement(sql, columnNames), sql);
    }

    public Clob createClob()
        throws SQLException
    {
        return getConnection().createClob();
    }

    public Blob createBlob()
        throws SQLException
    {
        return getConnection().createBlob();
    }

    public NClob createNClob()
        throws SQLException
    {
        return getConnection().createNClob();
    }

    public SQLXML createSQLXML()
        throws SQLException
    {
        return getConnection().createSQLXML();
    }

    public boolean isValid(int timeout)
        throws SQLException
    {
        return getConnection().isValid(timeout);
    }

    public void setClientInfo(String name, String value)
        throws SQLClientInfoException
    {
        try {
            getConnection().setClientInfo(name, value);
        } catch (SQLException e) {
            if (e instanceof SQLClientInfoException) {
                throw (SQLClientInfoException)e;
            } else {
                throw new SQLClientInfoException();
            }
        }
    }

    public void setClientInfo(Properties properties)
        throws SQLClientInfoException
    {
        try {
            getConnection().setClientInfo(properties);
        } catch (SQLException e) {
            if (e instanceof SQLClientInfoException) {
                throw (SQLClientInfoException)e;
            } else {
                throw new SQLClientInfoException();
            }
        }
    }

    public String getClientInfo(String name)
        throws SQLException
    {
        return getConnection().getClientInfo(name);
    }

    public Properties getClientInfo()
        throws SQLException
    {
        return getConnection().getClientInfo();
    }

    public Array createArrayOf(String typeName, Object[] elements)
        throws SQLException
    {
        return getConnection().createArrayOf(typeName, elements);
    }

    public Struct createStruct(String typeName, Object[] attributes)
        throws SQLException
    {
        return getConnection().createStruct(typeName, attributes);
    }

    public <T> T unwrap(Class<T> iface)
        throws SQLException
    {
        return getConnection().unwrap(iface);
    }

    public boolean isWrapperFor(Class<?> iface)
        throws SQLException
    {
        return getConnection().isWrapperFor(iface);
    }

    public void cancelBlock()
        throws DBException
    {
        try {
            getConnection().close();
        }
        catch (SQLException e) {
            throw new DBCException("Could not close connection", e);
        }
    }

    private class TransactionManager implements DBCTransactionManager {

        public DBPDataSource getDataSource()
        {
            return JDBCConnectionImpl.this.getDataSource();
        }

        public DBPTransactionIsolation getTransactionIsolation()
            throws DBCException
        {
            try {
                return JDBCTransactionIsolation.getByCode(JDBCConnectionImpl.this.getTransactionIsolation());
            } catch (SQLException e) {
                throw new JDBCException(e);
            }
        }

        public void setTransactionIsolation(DBPTransactionIsolation transactionIsolation)
            throws DBCException
        {
            if (!(transactionIsolation instanceof JDBCTransactionIsolation)) {
                throw new JDBCException("Invalid transaction isolation parameter");
            }
            JDBCTransactionIsolation jdbcTIL = (JDBCTransactionIsolation) transactionIsolation;
            try {
                JDBCConnectionImpl.this.setTransactionIsolation(jdbcTIL.getCode());
            } catch (SQLException e) {
                throw new JDBCException(e);
            }

            QMUtils.getDefaultHandler().handleTransactionIsolation(JDBCConnectionImpl.this, jdbcTIL);
        }

        public boolean isAutoCommit()
            throws DBCException
        {
            try {
                return JDBCConnectionImpl.this.getAutoCommit();
            }
            catch (SQLException e) {
                throw new JDBCException(e);
            }
        }

        public void setAutoCommit(boolean autoCommit)
            throws DBCException
        {
            try {
                JDBCConnectionImpl.this.setAutoCommit(autoCommit);
            }
            catch (SQLException e) {
                throw new JDBCException(e);
            }
        }

        public boolean supportsSavepoints()
        {
            return getDataSource().getInfo().supportsSavepoints();
        }

        public DBCSavepoint setSavepoint(String name)
            throws DBCException
        {
            Savepoint savepoint;
            try {
                if (name == null) {
                    savepoint = JDBCConnectionImpl.this.setSavepoint();
                } else {
                    savepoint = JDBCConnectionImpl.this.setSavepoint(name);
                }
            }
            catch (SQLException e) {
                throw new DBCException(e);
            }
            return new JDBCSavepointImpl(JDBCConnectionImpl.this, savepoint);
        }

        public void releaseSavepoint(DBCSavepoint savepoint)
            throws DBCException
        {
            try {
                if (savepoint instanceof Savepoint) {
                    JDBCConnectionImpl.this.releaseSavepoint((Savepoint)savepoint);
                } else {
                    throw new SQLFeatureNotSupportedException("Bad savepoint object");
                }
            }
            catch (SQLException e) {
                throw new JDBCException(e);
            }
        }

        public void commit()
            throws DBCException
        {
            try {
                JDBCConnectionImpl.this.commit();
            }
            catch (SQLException e) {
                throw new JDBCException(e);
            }
        }

        public void rollback(DBCSavepoint savepoint)
            throws DBCException
        {
            try {
                if (savepoint != null) {
                    if (savepoint instanceof Savepoint) {
                        JDBCConnectionImpl.this.rollback((Savepoint)savepoint);
                    } else {
                        throw new SQLFeatureNotSupportedException("Bad savepoint object");
                    }
                }
                JDBCConnectionImpl.this.rollback();
            }
            catch (SQLException e) {
                throw new JDBCException(e);
            }
        }
    }

}

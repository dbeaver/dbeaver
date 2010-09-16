/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.api;

import org.jkiss.dbeaver.model.jdbc.JDBCConnector;
import org.jkiss.dbeaver.model.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.jdbc.JDBCCallableStatement;
import org.jkiss.dbeaver.model.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.dbc.DBCTransactionManager;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCTransactionIsolation;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCException;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.dbc.DBCSavepoint;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRBlockingObject;
import org.jkiss.dbeaver.DBException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Savepoint;
import java.sql.Clob;
import java.sql.Blob;
import java.sql.NClob;
import java.sql.SQLXML;
import java.sql.SQLClientInfoException;
import java.sql.Array;
import java.sql.Struct;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.CallableStatement;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Map;
import java.util.Properties;

/**
 * Managable connection
 */
public class ConnectionManagable implements JDBCExecutionContext, DBRBlockingObject {

    static final Log log = LogFactory.getLog(ConnectionManagable.class);

    private JDBCConnector connector;
    private DBRProgressMonitor monitor;
    private String taskTitle;
    private boolean isolated;
    private Connection isolatedConnection;

    public ConnectionManagable(JDBCConnector connector, DBRProgressMonitor monitor, String taskTitle, boolean isolated)
    {
        this.connector = connector;
        this.monitor = monitor;
        this.taskTitle = taskTitle;
        this.isolated = isolated;
        this.isolatedConnection = null;

        if (taskTitle != null) {
            monitor.startBlock(this, taskTitle);
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

    public JDBCStatement prepareStatement(
        String sqlQuery,
        boolean scrollable,
        boolean updatable,
        boolean returnGeneratedKeys)
        throws DBCException
    {
        try {
            if (returnGeneratedKeys) {
                try {
                    return prepareStatement(
                        sqlQuery,
                        Statement.RETURN_GENERATED_KEYS);
                }
                catch (AbstractMethodError e) {
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
            return new CallableStatementManagable(this, (CallableStatement)statement, null);
        } else if (statement instanceof PreparedStatement) {
            return new PreparedStatementManagable(this, (PreparedStatement)statement, null);
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
        return new PreparedStatementManagable(this, getConnection().prepareStatement(sql), sql);
    }

    public JDBCCallableStatement prepareCall(String sql)
        throws SQLException
    {
        return new CallableStatementManagable(this, getConnection().prepareCall(sql), sql);
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
    }

    public void rollback()
        throws SQLException
    {
        getConnection().rollback();
    }

    public void close()
        //throws SQLException
    {
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
    }

    public boolean isClosed()
        throws SQLException
    {
        return getConnection().isClosed();
    }

    public JDBCDatabaseMetaData getMetaData()
        throws SQLException
    {
        return new DatabaseMetaDataManagable(this, getConnection().getMetaData());
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
        return new PreparedStatementManagable(
            this,
            getConnection().prepareStatement(sql, resultSetType, resultSetConcurrency),
            sql);
    }

    public JDBCCallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency)
        throws SQLException
    {
        return new CallableStatementManagable(this, getConnection().prepareCall(sql, resultSetType, resultSetConcurrency), sql);
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
        return getConnection().setSavepoint();
    }

    public Savepoint setSavepoint(String name)
        throws SQLException
    {
        return getConnection().setSavepoint(name);
    }

    public void rollback(Savepoint savepoint)
        throws SQLException
    {
        if (savepoint instanceof SavepointManagable) {
            savepoint = ((SavepointManagable)savepoint).getOriginal();
        }
        getConnection().rollback(savepoint);
    }

    public void releaseSavepoint(Savepoint savepoint)
        throws SQLException
    {
        if (savepoint instanceof SavepointManagable) {
            savepoint = ((SavepointManagable)savepoint).getOriginal();
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
        return new PreparedStatementManagable(
            this,
            getConnection().prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability),
            sql);
    }

    public JDBCCallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
        throws SQLException
    {
        return new CallableStatementManagable(
            this,
            getConnection().prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability),
            sql);
    }

    public JDBCPreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
        throws SQLException
    {
        return new PreparedStatementManagable(this, getConnection().prepareStatement(sql, autoGeneratedKeys), sql);
    }

    public JDBCPreparedStatement prepareStatement(String sql, int[] columnIndexes)
        throws SQLException
    {
        return new PreparedStatementManagable(this, getConnection().prepareStatement(sql, columnIndexes), sql);
    }

    public JDBCPreparedStatement prepareStatement(String sql, String[] columnNames)
        throws SQLException
    {
        return new PreparedStatementManagable(this, getConnection().prepareStatement(sql, columnNames), sql);
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
            return ConnectionManagable.this.getDataSource();
        }

        public DBPTransactionIsolation getTransactionIsolation()
            throws DBCException
        {
            try {
                return JDBCTransactionIsolation.getByCode(ConnectionManagable.this.getTransactionIsolation());
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
            try {
                ConnectionManagable.this.setTransactionIsolation(((JDBCTransactionIsolation)transactionIsolation).getCode());
            } catch (SQLException e) {
                throw new JDBCException(e);
            }
        }

        public boolean isAutoCommit()
            throws DBCException
        {
            try {
                return ConnectionManagable.this.getAutoCommit();
            }
            catch (SQLException e) {
                throw new JDBCException(e);
            }
        }

        public void setAutoCommit(boolean autoCommit)
            throws DBCException
        {
            try {
                ConnectionManagable.this.setAutoCommit(autoCommit);
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
                    savepoint = ConnectionManagable.this.setSavepoint();
                } else {
                    savepoint = ConnectionManagable.this.setSavepoint(name);
                }
            }
            catch (SQLException e) {
                throw new DBCException(e);
            }
            return new SavepointManagable(ConnectionManagable.this, savepoint);
        }

        public void releaseSavepoint(DBCSavepoint savepoint)
            throws DBCException
        {
            try {
                if (savepoint instanceof Savepoint) {
                    ConnectionManagable.this.releaseSavepoint((Savepoint)savepoint);
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
                ConnectionManagable.this.commit();
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
                        ConnectionManagable.this.rollback((Savepoint)savepoint);
                    } else {
                        throw new SQLFeatureNotSupportedException("Bad savepoint object");
                    }
                }
                ConnectionManagable.this.rollback();
            }
            catch (SQLException e) {
                throw new JDBCException(e);
            }
        }
    }

}

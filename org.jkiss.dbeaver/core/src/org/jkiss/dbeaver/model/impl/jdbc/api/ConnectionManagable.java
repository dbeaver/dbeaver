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

    private DBPDataSource dataSource;
    private Connection original;
    private DBRProgressMonitor monitor;
    private String taskTitle;

    public ConnectionManagable(JDBCConnector connector, DBRProgressMonitor monitor, String taskTitle)
    {
        this.dataSource = connector.getDataSource();
        this.original = connector.getConnection();
        this.monitor = monitor;
        this.taskTitle = taskTitle;
        if (taskTitle != null) {
            monitor.startBlock(this, taskTitle);
        }
    }

    public String getTaskTitle()
    {
        return taskTitle;
    }

    public DBPDataSource getDataSource()
    {
        return dataSource;
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
        return makeStatement(original.createStatement());
    }

    public JDBCPreparedStatement prepareStatement(String sql)
        throws SQLException
    {
        return new PreparedStatementManagable(this, original.prepareStatement(sql), sql);
    }

    public JDBCCallableStatement prepareCall(String sql)
        throws SQLException
    {
        return new CallableStatementManagable(this, original.prepareCall(sql), sql);
    }

    public String nativeSQL(String sql)
        throws SQLException
    {
        return original.nativeSQL(sql);
    }

    public void setAutoCommit(boolean autoCommit)
        throws SQLException
    {
        original.setAutoCommit(autoCommit);
    }

    public boolean getAutoCommit()
        throws SQLException
    {
        return original.getAutoCommit();
    }

    public void commit()
        throws SQLException
    {
        original.commit();
    }

    public void rollback()
        throws SQLException
    {
        original.rollback();
    }

    public void close()
        //throws SQLException
    {
        if (taskTitle != null) {
            monitor.endBlock();
        }
        // do nothing
        // closing of context doesn't close real connection
        //original.close();
        //log.warn("Close of execution context is obsolete");
    }

    public boolean isClosed()
        throws SQLException
    {
        return original.isClosed();
    }

    public JDBCDatabaseMetaData getMetaData()
        throws SQLException
    {
        return new DatabaseMetaDataManagable(this, original.getMetaData());
    }

    public void setReadOnly(boolean readOnly)
        throws SQLException
    {
        original.setReadOnly(readOnly);
    }

    public boolean isReadOnly()
        throws SQLException
    {
        return original.isReadOnly();
    }

    public void setCatalog(String catalog)
        throws SQLException
    {
        original.setCatalog(catalog);
    }

    public String getCatalog()
        throws SQLException
    {
        return original.getCatalog();
    }

    public void setTransactionIsolation(int level)
        throws SQLException
    {
        original.setTransactionIsolation(level);
    }

    public int getTransactionIsolation()
        throws SQLException
    {
        return original.getTransactionIsolation();
    }

    public SQLWarning getWarnings()
        throws SQLException
    {
        return original.getWarnings();
    }

    public void clearWarnings()
        throws SQLException
    {
        original.clearWarnings();
    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency)
        throws SQLException
    {
        return original.createStatement(resultSetType, resultSetConcurrency);
    }

    public JDBCPreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
        throws SQLException
    {
        return new PreparedStatementManagable(
            this,
            original.prepareStatement(sql, resultSetType, resultSetConcurrency),
            sql);
    }

    public JDBCCallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency)
        throws SQLException
    {
        return new CallableStatementManagable(this, original.prepareCall(sql, resultSetType, resultSetConcurrency), sql);
    }

    public Map<String, Class<?>> getTypeMap()
        throws SQLException
    {
        return original.getTypeMap();
    }

    public void setTypeMap(Map<String, Class<?>> map)
        throws SQLException
    {
        original.setTypeMap(map);
    }

    public void setHoldability(int holdability)
        throws SQLException
    {
        original.setHoldability(holdability);
    }

    public int getHoldability()
        throws SQLException
    {
        return original.getHoldability();
    }

    public Savepoint setSavepoint()
        throws SQLException
    {
        return original.setSavepoint();
    }

    public Savepoint setSavepoint(String name)
        throws SQLException
    {
        return original.setSavepoint(name);
    }

    public void rollback(Savepoint savepoint)
        throws SQLException
    {
        if (savepoint instanceof SavepointManagable) {
            savepoint = ((SavepointManagable)savepoint).getOriginal();
        }
        original.rollback(savepoint);
    }

    public void releaseSavepoint(Savepoint savepoint)
        throws SQLException
    {
        if (savepoint instanceof SavepointManagable) {
            savepoint = ((SavepointManagable)savepoint).getOriginal();
        }
        original.releaseSavepoint(savepoint);
    }

    public JDBCStatement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
        throws SQLException
    {
        return makeStatement(original.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability));
    }

    public JDBCPreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
        throws SQLException
    {
        return new PreparedStatementManagable(
            this,
            original.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability),
            sql);
    }

    public JDBCCallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability)
        throws SQLException
    {
        return new CallableStatementManagable(
            this,
            original.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability),
            sql);
    }

    public JDBCPreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
        throws SQLException
    {
        return new PreparedStatementManagable(this, original.prepareStatement(sql, autoGeneratedKeys), sql);
    }

    public JDBCPreparedStatement prepareStatement(String sql, int[] columnIndexes)
        throws SQLException
    {
        return new PreparedStatementManagable(this, original.prepareStatement(sql, columnIndexes), sql);
    }

    public JDBCPreparedStatement prepareStatement(String sql, String[] columnNames)
        throws SQLException
    {
        return new PreparedStatementManagable(this, original.prepareStatement(sql, columnNames), sql);
    }

    public Clob createClob()
        throws SQLException
    {
        return original.createClob();
    }

    public Blob createBlob()
        throws SQLException
    {
        return original.createBlob();
    }

    public NClob createNClob()
        throws SQLException
    {
        return original.createNClob();
    }

    public SQLXML createSQLXML()
        throws SQLException
    {
        return original.createSQLXML();
    }

    public boolean isValid(int timeout)
        throws SQLException
    {
        return original.isValid(timeout);
    }

    public void setClientInfo(String name, String value)
        throws SQLClientInfoException
    {
        original.setClientInfo(name, value);
    }

    public void setClientInfo(Properties properties)
        throws SQLClientInfoException
    {
        original.setClientInfo(properties);
    }

    public String getClientInfo(String name)
        throws SQLException
    {
        return original.getClientInfo(name);
    }

    public Properties getClientInfo()
        throws SQLException
    {
        return original.getClientInfo();
    }

    public Array createArrayOf(String typeName, Object[] elements)
        throws SQLException
    {
        return original.createArrayOf(typeName, elements);
    }

    public Struct createStruct(String typeName, Object[] attributes)
        throws SQLException
    {
        return original.createStruct(typeName, attributes);
    }

    public <T> T unwrap(Class<T> iface)
        throws SQLException
    {
        return original.unwrap(iface);
    }

    public boolean isWrapperFor(Class<?> iface)
        throws SQLException
    {
        return original.isWrapperFor(iface);
    }

    public void cancelBlock()
        throws DBException
    {
        try {
            original.close();
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
            return dataSource.getInfo().supportsSavepoints();
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

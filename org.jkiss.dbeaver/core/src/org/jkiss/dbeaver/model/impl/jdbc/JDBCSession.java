/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.jdbc.JDBCConnector;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.dbc.DBCSession;
import org.jkiss.dbeaver.model.dbc.DBCStatement;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;

/**
 * JDBCSession
 */
public class JDBCSession implements DBCSession
{
    private DBPDataSource dataSource;
    private Connection connection;
    private JDBCConnector conector;

    public JDBCSession(JDBCConnector conector, Connection connection)
    {
        this.dataSource = conector.getDataSource();
        this.conector = conector;
        this.connection = connection;
    }

    public JDBCSession(JDBCConnector conector)
    {
        this.dataSource = conector.getDataSource();
        this.conector = conector;
    }

    public Connection getConnection()
    {
        return connection == null ? conector.getConnection() : connection;
    }

    public DBPDataSource getDataSource()
    {
        return dataSource;
    }

    public DBPTransactionIsolation getTransactionIsolation()
         throws DBCException
    {
        try {
            return JDBCTransactionIsolation.getByCode(getConnection().getTransactionIsolation());
        } catch (SQLException e) {
            throw new JDBCException(e);
        }
    }

    public void setTransactionIsolation(DBPTransactionIsolation transactionIsolation)
        throws DBCException
    {
        if (!(transactionIsolation instanceof JDBCTransactionIsolation)) {
            throw new DBCException("Invalid transaction isolation parameter");
        }
        try {
            getConnection().setTransactionIsolation(((JDBCTransactionIsolation)transactionIsolation).getCode());
        } catch (SQLException e) {
            throw new JDBCException(e);
        }
    }

    public boolean isAutoCommit()
        throws DBCException
    {
        try {
            return getConnection().getAutoCommit();
        } catch (SQLException e) {
            throw new JDBCException(e);
        }
    }

    public void setAutoCommit(boolean autoCommit)
        throws DBCException
    {
        try {
            getConnection().setAutoCommit(autoCommit);
        } catch (SQLException e) {
            throw new JDBCException(e);
        }
    }

    public DBCStatement prepareStatement(
        DBRProgressMonitor monitor,
        String sqlQuery,
        boolean scrollable,
        boolean updatable)
        throws DBCException
    {
        try {
            return conector.getExecutionContext(monitor).prepareStatement(
                sqlQuery,
                scrollable ? ResultSet.TYPE_SCROLL_SENSITIVE : ResultSet.TYPE_FORWARD_ONLY,
                updatable ? ResultSet.CONCUR_UPDATABLE : ResultSet.CONCUR_READ_ONLY);
        }
        catch (SQLException e) {
            throw new DBCException(e);
        }
    }

    public void commit()
        throws DBCException
    {
        try {
            getConnection().commit();
        } catch (SQLException e) {
            throw new JDBCException(e);
        }
    }

    public void rollback()
        throws DBCException
    {
        try {
            getConnection().rollback();
        } catch (SQLException e) {
            throw new JDBCException(e);
        }
    }

    public void close()
        throws DBCException
    {
        if (this.connection != null) {
            try {
                this.connection.close();
            } catch (SQLException e) {
                throw new JDBCException(e);
            }
        }
    }

    public void cancelBlock()
        throws DBException
    {
        this.close();
    }
}

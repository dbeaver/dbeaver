/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPTransactionIsolation;
import org.jkiss.dbeaver.model.dbc.DBCConnector;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.dbc.DBCSession;
import org.jkiss.dbeaver.model.dbc.DBCStatement;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * JDBCSession
 */
public class JDBCSession implements DBCSession
{
    private DBPDataSource dataSource;
    private Connection connection;
    private DBCConnector conector;

    public JDBCSession(DBPDataSource dataSource, Connection connection)
    {
        this.dataSource = dataSource;
        this.connection = connection;
    }

    public JDBCSession(DBPDataSource dataSource, DBCConnector conector)
    {
        this.dataSource = dataSource;
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

    public DBCStatement makeStatement(String sqlQuery)
        throws DBCException
    {
        try {
            return new JDBCStatement(this, getConnection(), sqlQuery);
        } catch (SQLException e) {
            throw new JDBCException(e);
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
}

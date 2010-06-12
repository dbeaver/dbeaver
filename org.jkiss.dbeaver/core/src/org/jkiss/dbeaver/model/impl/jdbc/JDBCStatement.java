/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.dbc.DBCResultSet;
import org.jkiss.dbeaver.model.dbc.DBCStatement;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * JDBCStatement
 */
public class JDBCStatement implements DBCStatement
{
    static Log log = LogFactory.getLog(JDBCStatement.class);

    private DBPDataSource dataSource;
    private PreparedStatement statement;
    private boolean hasResultSet;
    private JDBCResultSet resultSet;
    private DBSObject dataContainer;

    public JDBCStatement(DBPDataSource dataSource, PreparedStatement statement)
    {
        this.dataSource = dataSource;
        this.statement = statement;
    }

    public Object getNestedStatement() {
        return statement;
    }

    public DBPDataSource getDataSource()
    {
        return dataSource;
    }

    public Statement getStatement()
        throws SQLException
    {
        return statement;
    }

    public void execute()
        throws DBCException
    {
        try {
            this.hasResultSet = statement.execute();
            if (this.hasResultSet) {
                this.getResultSet();
            }
        } catch (SQLException ex) {
            throw new JDBCException(ex);
        }
    }

    public boolean hasResultSet()
    {
        return hasResultSet;
    }

    public DBCResultSet getResultSet()
        throws DBCException
    {
        try {
            if (resultSet == null) {
                resultSet = new JDBCResultSet(this, statement.getResultSet());
            }
            return resultSet;
        } catch (SQLException e) {
            throw new JDBCException(e);
        }
    }

    public void closeResultSet()
    {
        if (resultSet != null) {
            resultSet.close();
            resultSet = null;
        }
    }

    public int getUpdateCount()
        throws DBCException
    {
        try {
            return statement.getUpdateCount();
        } catch (SQLException e) {
            throw new JDBCException(e);
        }
    }

    public void cancel()
        throws DBCException
    {
        if (statement != null) {
            try {
                statement.cancel();
            } catch (SQLException e) {
                throw new JDBCException(e);
            }
        }
    }

    public void close()
    {
        closeResultSet();
        if (statement != null) {
            JDBCUtils.safeClose(statement);
            statement = null;
        }
    }

    public void setFirstResult(int offset)
        throws DBCException
    {
        throw new UnsupportedOperationException("Resultset scrolling is not supported");
    }

    public void setMaxResults(int limit)
        throws DBCException
    {
        if (statement != null) {
            try {
                statement.setMaxRows(limit);
            } catch (SQLException ex) {
                throw new DBCException(ex);
            }
        }
    }

    public DBSObject getDataContainer() {
        return dataContainer;
    }

    public void setDataContainer(DBSObject container) {
        dataContainer = container;
    }

    public void cancelBlock()
        throws DBCException
    {
        this.cancel();
    }

}

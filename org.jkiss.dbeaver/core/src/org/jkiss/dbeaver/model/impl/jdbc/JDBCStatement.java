/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.dbc.DBCResultSet;
import org.jkiss.dbeaver.model.dbc.DBCSession;
import org.jkiss.dbeaver.model.dbc.DBCStatement;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.*;

/**
 * JDBCStatement
 */
public class JDBCStatement implements DBCStatement
{
    static Log log = LogFactory.getLog(JDBCStatement.class);

    private DBCSession session;
    private PreparedStatement statement;
    private boolean hasResultSet;
    private JDBCResultSet resultSet;
    private DBSObject dataContainer;

    public JDBCStatement(DBCSession session, Connection connection, String sqlQuery)
        throws SQLException
    {
        this.session = session;
        this.statement = connection.prepareStatement(sqlQuery, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    }

    public Object getNestedStatement() {
        return statement;
    }

    public DBCSession getSession()
    {
        return session;
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
// Dirty hacks
/*
            // Patch MySQL resultsets
            if (statement.getClass().getName().indexOf("mysql") != -1) {
                statement.setFetchSize(Integer.MIN_VALUE);
                statement.setMaxRows(10000);
            }
*/

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
        throws DBCException
    {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (DBCException e) {
                log.error(e.getMessage());
            } finally {
                resultSet = null;
            }
        }
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                throw new JDBCException(e);
            }
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
}

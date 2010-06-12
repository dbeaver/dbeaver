/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc;

import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.dbc.DBCResultSet;
import org.jkiss.dbeaver.model.dbc.DBCResultSetMetaData;
import org.jkiss.dbeaver.model.impl.data.JDBCContentBLOB;
import org.jkiss.dbeaver.model.impl.data.JDBCContentCLOB;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * JDBCResultSet
 */
public class JDBCResultSet implements DBCResultSet
{
    private JDBCStatement statement;
    private ResultSet resultSet;
    private JDBCResultSetMetaData metaData;

    public JDBCResultSet(JDBCStatement statement, ResultSet resultSet)
    {
        this.statement = statement;
        this.resultSet = resultSet;
    }

    public Object getNestedResultSet()
    {
        return resultSet;
    }

    public JDBCStatement getStatement()
    {
        return this.statement;
    }

    public ResultSet getResultSet()
    {
        return resultSet;
    }

    public Object getObject(int index)
        throws DBCException
    {
        Object object = JDBCUtils.getParameter(
            resultSet,
            index,
            metaData.getColumns().get(index - 1).getValueType());
        if (object instanceof Blob) {
            object = new JDBCContentBLOB((Blob)object);
        } else if (object instanceof Clob) {
            object = new JDBCContentCLOB((Clob)object);
        }
        return object;
    }

    public boolean wasNull()
        throws DBCException
    {
        try {
            return resultSet.wasNull();
        } catch (SQLException e) {
            throw new JDBCException(e);
        }
    }

    public boolean next()
        throws DBCException
    {
        try {
            return resultSet.next();
        } catch (SQLException e) {
            throw new JDBCException(e);
        }
    }

    public String getCursorName()
        throws DBCException
    {
        try {
            return resultSet.getCursorName();
        } catch (SQLException e) {
            throw new JDBCException(e);
        }
    }

    public DBCResultSetMetaData getMetaData()
        throws DBCException
    {
        try {
            if (metaData == null) {
                metaData = new JDBCResultSetMetaData(this);
            }
            return metaData;
        } catch (SQLException e) {
            throw new JDBCException(e);
        }
    }

    public void close()
    {
        if (resultSet != null) {
            JDBCUtils.safeClose(resultSet);
            resultSet = null;
        }
    }
}

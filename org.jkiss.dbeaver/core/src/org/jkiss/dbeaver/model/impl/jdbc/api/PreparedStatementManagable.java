/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.api;

import org.jkiss.dbeaver.model.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.dbc.DBCException;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;

/**
 * Managable prepared statement.
 * Stores information about execution in query manager and operated progress monitor.
 */
public class PreparedStatementManagable extends StatementManagable implements JDBCPreparedStatement {

    protected final PreparedStatement original;

    public PreparedStatementManagable(
        ConnectionManagable connection,
        PreparedStatement original,
        String query)
    {
        super(connection);
        this.original = original;
        setQuery(query);
    }

    protected PreparedStatement getOriginal()
    {
        return original;
    }

    ////////////////////////////////////////////////////////////////////
    // DBC Statement overrides
    ////////////////////////////////////////////////////////////////////

    public boolean executeStatement()
        throws DBCException
    {
        try {
            return execute();
        }
        catch (SQLException e) {
            throw new DBCException(e);
        }
    }

    ////////////////////////////////////////////////////////////////////
    // Statement overrides
    ////////////////////////////////////////////////////////////////////

    public JDBCResultSet executeQuery()
        throws SQLException
    {
        super.startBlock();
        try {
            return new ResultSetManagable(this, getOriginal().executeQuery());
        } catch (SQLException e) {
            super.handleExecuteError(e);
            throw e;
        } finally {
            super.endBlock();
        }
    }

    public int executeUpdate()
        throws SQLException
    {
        super.startBlock();
        try {
            return getOriginal().executeUpdate();
        } catch (SQLException e) {
            super.handleExecuteError(e);
            throw e;
        } finally {
            super.endBlock();
        }
    }

    public boolean execute()
        throws SQLException
    {
        super.startBlock();
        try {
            return getOriginal().execute();
        } catch (SQLException e) {
            super.handleExecuteError(e);
            throw e;
        } finally {
            super.endBlock();
        }
    }

    public void setNull(int parameterIndex, int sqlType)
        throws SQLException
    {
        getOriginal().setNull(parameterIndex, sqlType);
    }

    public void setBoolean(int parameterIndex, boolean x)
        throws SQLException
    {
        getOriginal().setBoolean(parameterIndex, x);
    }

    public void setByte(int parameterIndex, byte x)
        throws SQLException
    {
        getOriginal().setByte(parameterIndex, x);
    }

    public void setShort(int parameterIndex, short x)
        throws SQLException
    {
        getOriginal().setShort(parameterIndex, x);
    }

    public void setInt(int parameterIndex, int x)
        throws SQLException
    {
        getOriginal().setInt(parameterIndex, x);
    }

    public void setLong(int parameterIndex, long x)
        throws SQLException
    {
        getOriginal().setLong(parameterIndex, x);
    }

    public void setFloat(int parameterIndex, float x)
        throws SQLException
    {
        getOriginal().setFloat(parameterIndex, x);
    }

    public void setDouble(int parameterIndex, double x)
        throws SQLException
    {
        getOriginal().setDouble(parameterIndex, x);
    }

    public void setBigDecimal(int parameterIndex, BigDecimal x)
        throws SQLException
    {
        getOriginal().setBigDecimal(parameterIndex, x);
    }

    public void setString(int parameterIndex, String x)
        throws SQLException
    {
        getOriginal().setString(parameterIndex, x);
    }

    public void setBytes(int parameterIndex, byte[] x)
        throws SQLException
    {
        getOriginal().setBytes(parameterIndex, x);
    }

    public void setDate(int parameterIndex, Date x)
        throws SQLException
    {
        getOriginal().setDate(parameterIndex, x);
    }

    public void setTime(int parameterIndex, Time x)
        throws SQLException
    {
        getOriginal().setTime(parameterIndex, x);
    }

    public void setTimestamp(
        int parameterIndex, Timestamp x)
        throws SQLException
    {
        getOriginal().setTimestamp(parameterIndex, x);
    }

    public void setAsciiStream(int parameterIndex, InputStream x, int length)
        throws SQLException
    {
        getOriginal().setAsciiStream(parameterIndex, x, length);
    }

    @Deprecated
    public void setUnicodeStream(int parameterIndex, InputStream x, int length)
        throws SQLException
    {
        getOriginal().setUnicodeStream(parameterIndex, x, length);
    }

    public void setBinaryStream(int parameterIndex, InputStream x, int length)
        throws SQLException
    {
        getOriginal().setBinaryStream(parameterIndex, x, length);
    }

    public void clearParameters()
        throws SQLException
    {
        getOriginal().clearParameters();
    }

    public void setObject(int parameterIndex, Object x, int targetSqlType)
        throws SQLException
    {
        getOriginal().setObject(parameterIndex, x, targetSqlType);
    }

    public void setObject(int parameterIndex, Object x)
        throws SQLException
    {
        getOriginal().setObject(parameterIndex, x);
    }

    public void addBatch()
        throws SQLException
    {
        getOriginal().addBatch();
    }

    public void setCharacterStream(int parameterIndex, Reader reader, int length)
        throws SQLException
    {
        getOriginal().setCharacterStream(parameterIndex, reader, length);
    }

    public void setRef(int parameterIndex, Ref x)
        throws SQLException
    {
        getOriginal().setRef(parameterIndex, x);
    }

    public void setBlob(int parameterIndex, Blob x)
        throws SQLException
    {
        getOriginal().setBlob(parameterIndex, x);
    }

    public void setClob(int parameterIndex, Clob x)
        throws SQLException
    {
        getOriginal().setClob(parameterIndex, x);
    }

    public void setArray(int parameterIndex, Array x)
        throws SQLException
    {
        getOriginal().setArray(parameterIndex, x);
    }

    public ResultSetMetaData getMetaData()
        throws SQLException
    {
        return getOriginal().getMetaData();
    }

    public void setDate(int parameterIndex, Date x, Calendar cal)
        throws SQLException
    {
        getOriginal().setDate(parameterIndex, x, cal);
    }

    public void setTime(int parameterIndex, Time x, Calendar cal)
        throws SQLException
    {
        getOriginal().setTime(parameterIndex, x, cal);
    }

    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal)
        throws SQLException
    {
        getOriginal().setTimestamp(parameterIndex, x, cal);
    }

    public void setNull(int parameterIndex, int sqlType, String typeName)
        throws SQLException
    {
        getOriginal().setNull(parameterIndex, sqlType, typeName);
    }

    public void setURL(int parameterIndex, URL x)
        throws SQLException
    {
        getOriginal().setURL(parameterIndex, x);
    }

    public ParameterMetaData getParameterMetaData()
        throws SQLException
    {
        return getOriginal().getParameterMetaData();
    }

    public void setRowId(int parameterIndex, RowId x)
        throws SQLException
    {
        getOriginal().setRowId(parameterIndex, x);
    }

    public void setNString(int parameterIndex, String value)
        throws SQLException
    {
        getOriginal().setNString(parameterIndex, value);
    }

    public void setNCharacterStream(int parameterIndex, Reader value, long length)
        throws SQLException
    {
        getOriginal().setNCharacterStream(parameterIndex, value, length);
    }

    public void setNClob(int parameterIndex, NClob value)
        throws SQLException
    {
        getOriginal().setNClob(parameterIndex, value);
    }

    public void setClob(int parameterIndex, Reader reader, long length)
        throws SQLException
    {
        getOriginal().setClob(parameterIndex, reader, length);
    }

    public void setBlob(int parameterIndex, InputStream inputStream, long length)
        throws SQLException
    {
        getOriginal().setBlob(parameterIndex, inputStream, length);
    }

    public void setNClob(int parameterIndex, Reader reader, long length)
        throws SQLException
    {
        getOriginal().setNClob(parameterIndex, reader, length);
    }

    public void setSQLXML(int parameterIndex, SQLXML xmlObject)
        throws SQLException
    {
        getOriginal().setSQLXML(parameterIndex, xmlObject);
    }

    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength)
        throws SQLException
    {
        getOriginal().setObject(parameterIndex, x, targetSqlType, scaleOrLength);
    }

    public void setAsciiStream(int parameterIndex, InputStream x, long length)
        throws SQLException
    {
        getOriginal().setAsciiStream(parameterIndex, x, length);
    }

    public void setBinaryStream(int parameterIndex, InputStream x, long length)
        throws SQLException
    {
        getOriginal().setBinaryStream(parameterIndex, x, length);
    }

    public void setCharacterStream(int parameterIndex, Reader reader, long length)
        throws SQLException
    {
        getOriginal().setCharacterStream(parameterIndex, reader, length);
    }

    public void setAsciiStream(int parameterIndex, InputStream x)
        throws SQLException
    {
        getOriginal().setAsciiStream(parameterIndex, x);
    }

    public void setBinaryStream(int parameterIndex, InputStream x)
        throws SQLException
    {
        getOriginal().setBinaryStream(parameterIndex, x);
    }

    public void setCharacterStream(int parameterIndex, Reader reader)
        throws SQLException
    {
        getOriginal().setCharacterStream(parameterIndex, reader);
    }

    public void setNCharacterStream(int parameterIndex, Reader value)
        throws SQLException
    {
        getOriginal().setNCharacterStream(parameterIndex, value);
    }

    public void setClob(int parameterIndex, Reader reader)
        throws SQLException
    {
        getOriginal().setClob(parameterIndex, reader);
    }

    public void setBlob(int parameterIndex, InputStream inputStream)
        throws SQLException
    {
        getOriginal().setBlob(parameterIndex, inputStream);
    }

    public void setNClob(int parameterIndex, Reader reader)
        throws SQLException
    {
        getOriginal().setNClob(parameterIndex, reader);
    }

}

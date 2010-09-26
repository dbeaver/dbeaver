/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.api;

import org.jkiss.dbeaver.model.jdbc.JDBCCallableStatement;
import org.jkiss.dbeaver.model.jdbc.JDBCExecutionContext;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;
import java.util.Map;

/**
 * Managable base statement.
 * Stores information about execution in query manager and operated progress monitor.
 */
public class CallableStatementManagable extends PreparedStatementManagable implements JDBCCallableStatement {

    public CallableStatementManagable(
        JDBCExecutionContext connection,
        CallableStatement original,
        String query)
    {
        super(connection, original, query);
    }

    protected CallableStatement getOriginal()
    {
        return (CallableStatement)original;
    }

    ////////////////////////////////////////////////////////////////////
    // JDBC Callable Statement overrides
    ////////////////////////////////////////////////////////////////////

    public void registerOutParameter(int parameterIndex, int sqlType)
        throws SQLException
    {
        getOriginal().registerOutParameter(parameterIndex, sqlType);
    }

    public void registerOutParameter(int parameterIndex, int sqlType, int scale)
        throws SQLException
    {
        getOriginal().registerOutParameter(parameterIndex, sqlType, scale);
    }

    public boolean wasNull()
        throws SQLException
    {
        return getOriginal().wasNull();
    }

    public String getString(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getString(parameterIndex);
    }

    public boolean getBoolean(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getBoolean(parameterIndex);
    }

    public byte getByte(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getByte(parameterIndex);
    }

    public short getShort(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getShort(parameterIndex);
    }

    public int getInt(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getInt(parameterIndex);
    }

    public long getLong(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getLong(parameterIndex);
    }

    public float getFloat(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getFloat(parameterIndex);
    }

    public double getDouble(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getDouble(parameterIndex);
    }

    @Deprecated
    public BigDecimal getBigDecimal(int parameterIndex, int scale)
        throws SQLException
    {
        return getOriginal().getBigDecimal(parameterIndex, scale);
    }

    public byte[] getBytes(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getBytes(parameterIndex);
    }

    public Date getDate(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getDate(parameterIndex);
    }

    public Time getTime(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getTime(parameterIndex);
    }

    public Timestamp getTimestamp(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getTimestamp(parameterIndex);
    }

    public Object getObject(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getObject(parameterIndex);
    }

    public BigDecimal getBigDecimal(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getBigDecimal(parameterIndex);
    }

    public Object getObject(int parameterIndex, Map<String, Class<?>> map)
        throws SQLException
    {
        return getOriginal().getObject(parameterIndex, map);
    }

    public Ref getRef(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getRef(parameterIndex);
    }

    public Blob getBlob(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getBlob(parameterIndex);
    }

    public Clob getClob(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getClob(parameterIndex);
    }

    public Array getArray(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getArray(parameterIndex);
    }

    public Date getDate(int parameterIndex, Calendar cal)
        throws SQLException
    {
        return getOriginal().getDate(parameterIndex, cal);
    }

    public Time getTime(int parameterIndex, Calendar cal)
        throws SQLException
    {
        return getOriginal().getTime(parameterIndex, cal);
    }

    public Timestamp getTimestamp(int parameterIndex, Calendar cal)
        throws SQLException
    {
        return getOriginal().getTimestamp(parameterIndex, cal);
    }

    public void registerOutParameter(int parameterIndex, int sqlType, String typeName)
        throws SQLException
    {
        getOriginal().registerOutParameter(parameterIndex, sqlType, typeName);
    }

    public void registerOutParameter(String parameterName, int sqlType)
        throws SQLException
    {
        getOriginal().registerOutParameter(parameterName, sqlType);
    }

    public void registerOutParameter(String parameterName, int sqlType, int scale)
        throws SQLException
    {
        getOriginal().registerOutParameter(parameterName, sqlType, scale);
    }

    public void registerOutParameter(String parameterName, int sqlType, String typeName)
        throws SQLException
    {
        getOriginal().registerOutParameter(parameterName, sqlType, typeName);
    }

    public URL getURL(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getURL(parameterIndex);
    }

    public void setURL(String parameterName, URL val)
        throws SQLException
    {
        getOriginal().setURL(parameterName, val);
    }

    public void setNull(String parameterName, int sqlType)
        throws SQLException
    {
        getOriginal().setNull(parameterName, sqlType);
    }

    public void setBoolean(String parameterName, boolean x)
        throws SQLException
    {
        getOriginal().setBoolean(parameterName, x);
    }

    public void setByte(String parameterName, byte x)
        throws SQLException
    {
        getOriginal().setByte(parameterName, x);
    }

    public void setShort(String parameterName, short x)
        throws SQLException
    {
        getOriginal().setShort(parameterName, x);
    }

    public void setInt(String parameterName, int x)
        throws SQLException
    {
        getOriginal().setInt(parameterName, x);
    }

    public void setLong(String parameterName, long x)
        throws SQLException
    {
        getOriginal().setLong(parameterName, x);
    }

    public void setFloat(String parameterName, float x)
        throws SQLException
    {
        getOriginal().setFloat(parameterName, x);
    }

    public void setDouble(String parameterName, double x)
        throws SQLException
    {
        getOriginal().setDouble(parameterName, x);
    }

    public void setBigDecimal(String parameterName, BigDecimal x)
        throws SQLException
    {
        getOriginal().setBigDecimal(parameterName, x);
    }

    public void setString(String parameterName, String x)
        throws SQLException
    {
        getOriginal().setString(parameterName, x);
    }

    public void setBytes(String parameterName, byte[] x)
        throws SQLException
    {
        getOriginal().setBytes(parameterName, x);
    }

    public void setDate(String parameterName, Date x)
        throws SQLException
    {
        getOriginal().setDate(parameterName, x);
    }

    public void setTime(String parameterName, Time x)
        throws SQLException
    {
        getOriginal().setTime(parameterName, x);
    }

    public void setTimestamp(String parameterName, Timestamp x)
        throws SQLException
    {
        getOriginal().setTimestamp(parameterName, x);
    }

    public void setAsciiStream(String parameterName, InputStream x, int length)
        throws SQLException
    {
        getOriginal().setAsciiStream(parameterName, x, length);
    }

    public void setBinaryStream(String parameterName, InputStream x, int length)
        throws SQLException
    {
        getOriginal().setBinaryStream(parameterName, x, length);
    }

    public void setObject(String parameterName, Object x, int targetSqlType, int scale)
        throws SQLException
    {
        getOriginal().setObject(parameterName, x, targetSqlType, scale);
    }

    public void setObject(String parameterName, Object x, int targetSqlType)
        throws SQLException
    {
        getOriginal().setObject(parameterName, x, targetSqlType);
    }

    public void setObject(String parameterName, Object x)
        throws SQLException
    {
        getOriginal().setObject(parameterName, x);
    }

    public void setCharacterStream(String parameterName, Reader reader, int length)
        throws SQLException
    {
        getOriginal().setCharacterStream(parameterName, reader, length);
    }

    public void setDate(String parameterName, Date x, Calendar cal)
        throws SQLException
    {
        getOriginal().setDate(parameterName, x, cal);
    }

    public void setTime(String parameterName, Time x, Calendar cal)
        throws SQLException
    {
        getOriginal().setTime(parameterName, x, cal);
    }

    public void setTimestamp(String parameterName, Timestamp x, Calendar cal)
        throws SQLException
    {
        getOriginal().setTimestamp(parameterName, x, cal);
    }

    public void setNull(String parameterName, int sqlType, String typeName)
        throws SQLException
    {
        getOriginal().setNull(parameterName, sqlType, typeName);
    }

    public String getString(String parameterName)
        throws SQLException
    {
        return getOriginal().getString(parameterName);
    }

    public boolean getBoolean(String parameterName)
        throws SQLException
    {
        return getOriginal().getBoolean(parameterName);
    }

    public byte getByte(String parameterName)
        throws SQLException
    {
        return getOriginal().getByte(parameterName);
    }

    public short getShort(String parameterName)
        throws SQLException
    {
        return getOriginal().getShort(parameterName);
    }

    public int getInt(String parameterName)
        throws SQLException
    {
        return getOriginal().getInt(parameterName);
    }

    public long getLong(String parameterName)
        throws SQLException
    {
        return getOriginal().getLong(parameterName);
    }

    public float getFloat(String parameterName)
        throws SQLException
    {
        return getOriginal().getFloat(parameterName);
    }

    public double getDouble(String parameterName)
        throws SQLException
    {
        return getOriginal().getDouble(parameterName);
    }

    public byte[] getBytes(String parameterName)
        throws SQLException
    {
        return getOriginal().getBytes(parameterName);
    }

    public Date getDate(String parameterName)
        throws SQLException
    {
        return getOriginal().getDate(parameterName);
    }

    public Time getTime(String parameterName)
        throws SQLException
    {
        return getOriginal().getTime(parameterName);
    }

    public Timestamp getTimestamp(String parameterName)
        throws SQLException
    {
        return getOriginal().getTimestamp(parameterName);
    }

    public Object getObject(String parameterName)
        throws SQLException
    {
        return getOriginal().getObject(parameterName);
    }

    public BigDecimal getBigDecimal(String parameterName)
        throws SQLException
    {
        return getOriginal().getBigDecimal(parameterName);
    }

    public Object getObject(String parameterName, Map<String, Class<?>> map)
        throws SQLException
    {
        return getOriginal().getObject(parameterName, map);
    }

    public Ref getRef(String parameterName)
        throws SQLException
    {
        return getOriginal().getRef(parameterName);
    }

    public Blob getBlob(String parameterName)
        throws SQLException
    {
        return getOriginal().getBlob(parameterName);
    }

    public Clob getClob(String parameterName)
        throws SQLException
    {
        return getOriginal().getClob(parameterName);
    }

    public Array getArray(String parameterName)
        throws SQLException
    {
        return getOriginal().getArray(parameterName);
    }

    public Date getDate(String parameterName, Calendar cal)
        throws SQLException
    {
        return getOriginal().getDate(parameterName, cal);
    }

    public Time getTime(String parameterName, Calendar cal)
        throws SQLException
    {
        return getOriginal().getTime(parameterName, cal);
    }

    public Timestamp getTimestamp(String parameterName, Calendar cal)
        throws SQLException
    {
        return getOriginal().getTimestamp(parameterName, cal);
    }

    public URL getURL(String parameterName)
        throws SQLException
    {
        return getOriginal().getURL(parameterName);
    }

    public RowId getRowId(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getRowId(parameterIndex);
    }

    public RowId getRowId(String parameterName)
        throws SQLException
    {
        return getOriginal().getRowId(parameterName);
    }

    public void setRowId(String parameterName, RowId x)
        throws SQLException
    {
        getOriginal().setRowId(parameterName, x);
    }

    public void setNString(String parameterName, String value)
        throws SQLException
    {
        getOriginal().setNString(parameterName, value);
    }

    public void setNCharacterStream(String parameterName, Reader value, long length)
        throws SQLException
    {
        getOriginal().setNCharacterStream(parameterName, value, length);
    }

    public void setNClob(String parameterName, NClob value)
        throws SQLException
    {
        getOriginal().setNClob(parameterName, value);
    }

    public void setClob(String parameterName, Reader reader, long length)
        throws SQLException
    {
        getOriginal().setClob(parameterName, reader, length);
    }

    public void setBlob(String parameterName, InputStream inputStream, long length)
        throws SQLException
    {
        getOriginal().setBlob(parameterName, inputStream, length);
    }

    public void setNClob(String parameterName, Reader reader, long length)
        throws SQLException
    {
        getOriginal().setNClob(parameterName, reader, length);
    }

    public NClob getNClob(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getNClob(parameterIndex);
    }

    public NClob getNClob(String parameterName)
        throws SQLException
    {
        return getOriginal().getNClob(parameterName);
    }

    public void setSQLXML(String parameterName, SQLXML xmlObject)
        throws SQLException
    {
        getOriginal().setSQLXML(parameterName, xmlObject);
    }

    public SQLXML getSQLXML(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getSQLXML(parameterIndex);
    }

    public SQLXML getSQLXML(String parameterName)
        throws SQLException
    {
        return getOriginal().getSQLXML(parameterName);
    }

    public String getNString(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getNString(parameterIndex);
    }

    public String getNString(String parameterName)
        throws SQLException
    {
        return getOriginal().getNString(parameterName);
    }

    public Reader getNCharacterStream(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getNCharacterStream(parameterIndex);
    }

    public Reader getNCharacterStream(String parameterName)
        throws SQLException
    {
        return getOriginal().getNCharacterStream(parameterName);
    }

    public Reader getCharacterStream(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getCharacterStream(parameterIndex);
    }

    public Reader getCharacterStream(String parameterName)
        throws SQLException
    {
        return getOriginal().getCharacterStream(parameterName);
    }

    public void setBlob(String parameterName, Blob x)
        throws SQLException
    {
        getOriginal().setBlob(parameterName, x);
    }

    public void setClob(String parameterName, Clob x)
        throws SQLException
    {
        getOriginal().setClob(parameterName, x);
    }

    public void setAsciiStream(String parameterName, InputStream x, long length)
        throws SQLException
    {
        getOriginal().setAsciiStream(parameterName, x, length);
    }

    public void setBinaryStream(String parameterName, InputStream x, long length)
        throws SQLException
    {
        getOriginal().setBinaryStream(parameterName, x, length);
    }

    public void setCharacterStream(String parameterName, Reader reader, long length)
        throws SQLException
    {
        getOriginal().setCharacterStream(parameterName, reader, length);
    }

    public void setAsciiStream(String parameterName, InputStream x)
        throws SQLException
    {
        getOriginal().setAsciiStream(parameterName, x);
    }

    public void setBinaryStream(String parameterName, InputStream x)
        throws SQLException
    {
        getOriginal().setBinaryStream(parameterName, x);
    }

    public void setCharacterStream(String parameterName, Reader reader)
        throws SQLException
    {
        getOriginal().setCharacterStream(parameterName, reader);
    }

    public void setNCharacterStream(String parameterName, Reader value)
        throws SQLException
    {
        getOriginal().setNCharacterStream(parameterName, value);
    }

    public void setClob(String parameterName, Reader reader)
        throws SQLException
    {
        getOriginal().setClob(parameterName, reader);
    }

    public void setBlob(String parameterName, InputStream inputStream)
        throws SQLException
    {
        getOriginal().setBlob(parameterName, inputStream);
    }

    public void setNClob(String parameterName, Reader reader)
        throws SQLException
    {
        getOriginal().setNClob(parameterName, reader);
    }
}
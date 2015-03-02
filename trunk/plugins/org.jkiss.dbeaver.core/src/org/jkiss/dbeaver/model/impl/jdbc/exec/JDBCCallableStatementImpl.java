/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.model.impl.jdbc.exec;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCCallableStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;

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
public class JDBCCallableStatementImpl extends JDBCPreparedStatementImpl implements JDBCCallableStatement {

    public JDBCCallableStatementImpl(
        JDBCSession connection,
        CallableStatement original,
        @Nullable String query,
        boolean disableLogging)
    {
        super(connection, original, query, disableLogging);
    }

    @Override
    public CallableStatement getOriginal()
    {
        return (CallableStatement)original;
    }

    ////////////////////////////////////////////////////////////////////
    // JDBC Callable Statement overrides
    ////////////////////////////////////////////////////////////////////

    @Override
    public void registerOutParameter(int parameterIndex, int sqlType)
        throws SQLException
    {
        getOriginal().registerOutParameter(parameterIndex, sqlType);
    }

    @Override
    public void registerOutParameter(int parameterIndex, int sqlType, int scale)
        throws SQLException
    {
        getOriginal().registerOutParameter(parameterIndex, sqlType, scale);
    }

    @Override
    public boolean wasNull()
        throws SQLException
    {
        return getOriginal().wasNull();
    }

    @Override
    public String getString(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getString(parameterIndex);
    }

    @Override
    public boolean getBoolean(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getBoolean(parameterIndex);
    }

    @Override
    public byte getByte(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getByte(parameterIndex);
    }

    @Override
    public short getShort(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getShort(parameterIndex);
    }

    @Override
    public int getInt(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getInt(parameterIndex);
    }

    @Override
    public long getLong(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getLong(parameterIndex);
    }

    @Override
    public float getFloat(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getFloat(parameterIndex);
    }

    @Override
    public double getDouble(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getDouble(parameterIndex);
    }

    @Override
    @Deprecated
    public BigDecimal getBigDecimal(int parameterIndex, int scale)
        throws SQLException
    {
        return getOriginal().getBigDecimal(parameterIndex, scale);
    }

    @Override
    public byte[] getBytes(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getBytes(parameterIndex);
    }

    @Override
    public Date getDate(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getDate(parameterIndex);
    }

    @Override
    public Time getTime(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getTime(parameterIndex);
    }

    @Override
    public Timestamp getTimestamp(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getTimestamp(parameterIndex);
    }

    @Override
    public Object getObject(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getObject(parameterIndex);
    }

    @Override
    public BigDecimal getBigDecimal(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getBigDecimal(parameterIndex);
    }

    @Override
    public Object getObject(int parameterIndex, Map<String, Class<?>> map)
        throws SQLException
    {
        return getOriginal().getObject(parameterIndex, map);
    }

    @Override
    public Ref getRef(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getRef(parameterIndex);
    }

    @Override
    public Blob getBlob(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getBlob(parameterIndex);
    }

    @Override
    public Clob getClob(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getClob(parameterIndex);
    }

    @Override
    public Array getArray(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getArray(parameterIndex);
    }

    @Override
    public Date getDate(int parameterIndex, Calendar cal)
        throws SQLException
    {
        return getOriginal().getDate(parameterIndex, cal);
    }

    @Override
    public Time getTime(int parameterIndex, Calendar cal)
        throws SQLException
    {
        return getOriginal().getTime(parameterIndex, cal);
    }

    @Override
    public Timestamp getTimestamp(int parameterIndex, Calendar cal)
        throws SQLException
    {
        return getOriginal().getTimestamp(parameterIndex, cal);
    }

    @Override
    public void registerOutParameter(int parameterIndex, int sqlType, String typeName)
        throws SQLException
    {
        getOriginal().registerOutParameter(parameterIndex, sqlType, typeName);
    }

    @Override
    public void registerOutParameter(String parameterName, int sqlType)
        throws SQLException
    {
        getOriginal().registerOutParameter(parameterName, sqlType);
    }

    @Override
    public void registerOutParameter(String parameterName, int sqlType, int scale)
        throws SQLException
    {
        getOriginal().registerOutParameter(parameterName, sqlType, scale);
    }

    @Override
    public void registerOutParameter(String parameterName, int sqlType, String typeName)
        throws SQLException
    {
        getOriginal().registerOutParameter(parameterName, sqlType, typeName);
    }

    @Override
    public URL getURL(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getURL(parameterIndex);
    }

    @Override
    public void setURL(String parameterName, URL val)
        throws SQLException
    {
        getOriginal().setURL(parameterName, val);
    }

    @Override
    public void setNull(String parameterName, int sqlType)
        throws SQLException
    {
        getOriginal().setNull(parameterName, sqlType);
    }

    @Override
    public void setBoolean(String parameterName, boolean x)
        throws SQLException
    {
        getOriginal().setBoolean(parameterName, x);
    }

    @Override
    public void setByte(String parameterName, byte x)
        throws SQLException
    {
        getOriginal().setByte(parameterName, x);
    }

    @Override
    public void setShort(String parameterName, short x)
        throws SQLException
    {
        getOriginal().setShort(parameterName, x);
    }

    @Override
    public void setInt(String parameterName, int x)
        throws SQLException
    {
        getOriginal().setInt(parameterName, x);
    }

    @Override
    public void setLong(String parameterName, long x)
        throws SQLException
    {
        getOriginal().setLong(parameterName, x);
    }

    @Override
    public void setFloat(String parameterName, float x)
        throws SQLException
    {
        getOriginal().setFloat(parameterName, x);
    }

    @Override
    public void setDouble(String parameterName, double x)
        throws SQLException
    {
        getOriginal().setDouble(parameterName, x);
    }

    @Override
    public void setBigDecimal(String parameterName, BigDecimal x)
        throws SQLException
    {
        getOriginal().setBigDecimal(parameterName, x);
    }

    @Override
    public void setString(String parameterName, String x)
        throws SQLException
    {
        getOriginal().setString(parameterName, x);
    }

    @Override
    public void setBytes(String parameterName, byte[] x)
        throws SQLException
    {
        getOriginal().setBytes(parameterName, x);
    }

    @Override
    public void setDate(String parameterName, Date x)
        throws SQLException
    {
        getOriginal().setDate(parameterName, x);
    }

    @Override
    public void setTime(String parameterName, Time x)
        throws SQLException
    {
        getOriginal().setTime(parameterName, x);
    }

    @Override
    public void setTimestamp(String parameterName, Timestamp x)
        throws SQLException
    {
        getOriginal().setTimestamp(parameterName, x);
    }

    @Override
    public void setAsciiStream(String parameterName, InputStream x, int length)
        throws SQLException
    {
        getOriginal().setAsciiStream(parameterName, x, length);
    }

    @Override
    public void setBinaryStream(String parameterName, InputStream x, int length)
        throws SQLException
    {
        getOriginal().setBinaryStream(parameterName, x, length);
    }

    @Override
    public void setObject(String parameterName, Object x, int targetSqlType, int scale)
        throws SQLException
    {
        getOriginal().setObject(parameterName, x, targetSqlType, scale);
    }

    @Override
    public void setObject(String parameterName, Object x, int targetSqlType)
        throws SQLException
    {
        getOriginal().setObject(parameterName, x, targetSqlType);
    }

    @Override
    public void setObject(String parameterName, Object x)
        throws SQLException
    {
        getOriginal().setObject(parameterName, x);
    }

    @Override
    public void setCharacterStream(String parameterName, Reader reader, int length)
        throws SQLException
    {
        getOriginal().setCharacterStream(parameterName, reader, length);
    }

    @Override
    public void setDate(String parameterName, Date x, Calendar cal)
        throws SQLException
    {
        getOriginal().setDate(parameterName, x, cal);
    }

    @Override
    public void setTime(String parameterName, Time x, Calendar cal)
        throws SQLException
    {
        getOriginal().setTime(parameterName, x, cal);
    }

    @Override
    public void setTimestamp(String parameterName, Timestamp x, Calendar cal)
        throws SQLException
    {
        getOriginal().setTimestamp(parameterName, x, cal);
    }

    @Override
    public void setNull(String parameterName, int sqlType, String typeName)
        throws SQLException
    {
        getOriginal().setNull(parameterName, sqlType, typeName);
    }

    @Override
    public String getString(String parameterName)
        throws SQLException
    {
        return getOriginal().getString(parameterName);
    }

    @Override
    public boolean getBoolean(String parameterName)
        throws SQLException
    {
        return getOriginal().getBoolean(parameterName);
    }

    @Override
    public byte getByte(String parameterName)
        throws SQLException
    {
        return getOriginal().getByte(parameterName);
    }

    @Override
    public short getShort(String parameterName)
        throws SQLException
    {
        return getOriginal().getShort(parameterName);
    }

    @Override
    public int getInt(String parameterName)
        throws SQLException
    {
        return getOriginal().getInt(parameterName);
    }

    @Override
    public long getLong(String parameterName)
        throws SQLException
    {
        return getOriginal().getLong(parameterName);
    }

    @Override
    public float getFloat(String parameterName)
        throws SQLException
    {
        return getOriginal().getFloat(parameterName);
    }

    @Override
    public double getDouble(String parameterName)
        throws SQLException
    {
        return getOriginal().getDouble(parameterName);
    }

    @Override
    public byte[] getBytes(String parameterName)
        throws SQLException
    {
        return getOriginal().getBytes(parameterName);
    }

    @Override
    public Date getDate(String parameterName)
        throws SQLException
    {
        return getOriginal().getDate(parameterName);
    }

    @Override
    public Time getTime(String parameterName)
        throws SQLException
    {
        return getOriginal().getTime(parameterName);
    }

    @Override
    public Timestamp getTimestamp(String parameterName)
        throws SQLException
    {
        return getOriginal().getTimestamp(parameterName);
    }

    @Override
    public Object getObject(String parameterName)
        throws SQLException
    {
        return getOriginal().getObject(parameterName);
    }

    @Override
    public BigDecimal getBigDecimal(String parameterName)
        throws SQLException
    {
        return getOriginal().getBigDecimal(parameterName);
    }

    @Override
    public Object getObject(String parameterName, Map<String, Class<?>> map)
        throws SQLException
    {
        return getOriginal().getObject(parameterName, map);
    }

    @Override
    public Ref getRef(String parameterName)
        throws SQLException
    {
        return getOriginal().getRef(parameterName);
    }

    @Override
    public Blob getBlob(String parameterName)
        throws SQLException
    {
        return getOriginal().getBlob(parameterName);
    }

    @Override
    public Clob getClob(String parameterName)
        throws SQLException
    {
        return getOriginal().getClob(parameterName);
    }

    @Override
    public Array getArray(String parameterName)
        throws SQLException
    {
        return getOriginal().getArray(parameterName);
    }

    @Override
    public Date getDate(String parameterName, Calendar cal)
        throws SQLException
    {
        return getOriginal().getDate(parameterName, cal);
    }

    @Override
    public Time getTime(String parameterName, Calendar cal)
        throws SQLException
    {
        return getOriginal().getTime(parameterName, cal);
    }

    @Override
    public Timestamp getTimestamp(String parameterName, Calendar cal)
        throws SQLException
    {
        return getOriginal().getTimestamp(parameterName, cal);
    }

    @Override
    public URL getURL(String parameterName)
        throws SQLException
    {
        return getOriginal().getURL(parameterName);
    }

    @Override
    public RowId getRowId(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getRowId(parameterIndex);
    }

    @Override
    public RowId getRowId(String parameterName)
        throws SQLException
    {
        return getOriginal().getRowId(parameterName);
    }

    @Override
    public void setRowId(String parameterName, RowId x)
        throws SQLException
    {
        getOriginal().setRowId(parameterName, x);
    }

    @Override
    public void setNString(String parameterName, String value)
        throws SQLException
    {
        getOriginal().setNString(parameterName, value);
    }

    @Override
    public void setNCharacterStream(String parameterName, Reader value, long length)
        throws SQLException
    {
        getOriginal().setNCharacterStream(parameterName, value, length);
    }

    @Override
    public void setNClob(String parameterName, NClob value)
        throws SQLException
    {
        getOriginal().setNClob(parameterName, value);
    }

    @Override
    public void setClob(String parameterName, Reader reader, long length)
        throws SQLException
    {
        getOriginal().setClob(parameterName, reader, length);
    }

    @Override
    public void setBlob(String parameterName, InputStream inputStream, long length)
        throws SQLException
    {
        getOriginal().setBlob(parameterName, inputStream, length);
    }

    @Override
    public void setNClob(String parameterName, Reader reader, long length)
        throws SQLException
    {
        getOriginal().setNClob(parameterName, reader, length);
    }

    @Override
    public NClob getNClob(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getNClob(parameterIndex);
    }

    @Override
    public NClob getNClob(String parameterName)
        throws SQLException
    {
        return getOriginal().getNClob(parameterName);
    }

    @Override
    public void setSQLXML(String parameterName, SQLXML xmlObject)
        throws SQLException
    {
        getOriginal().setSQLXML(parameterName, xmlObject);
    }

    @Override
    public SQLXML getSQLXML(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getSQLXML(parameterIndex);
    }

    @Override
    public SQLXML getSQLXML(String parameterName)
        throws SQLException
    {
        return getOriginal().getSQLXML(parameterName);
    }

    @Override
    public String getNString(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getNString(parameterIndex);
    }

    @Override
    public String getNString(String parameterName)
        throws SQLException
    {
        return getOriginal().getNString(parameterName);
    }

    @Override
    public Reader getNCharacterStream(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getNCharacterStream(parameterIndex);
    }

    @Override
    public Reader getNCharacterStream(String parameterName)
        throws SQLException
    {
        return getOriginal().getNCharacterStream(parameterName);
    }

    @Override
    public Reader getCharacterStream(int parameterIndex)
        throws SQLException
    {
        return getOriginal().getCharacterStream(parameterIndex);
    }

    @Override
    public Reader getCharacterStream(String parameterName)
        throws SQLException
    {
        return getOriginal().getCharacterStream(parameterName);
    }

    @Override
    public void setBlob(String parameterName, Blob x)
        throws SQLException
    {
        getOriginal().setBlob(parameterName, x);
    }

    @Override
    public void setClob(String parameterName, Clob x)
        throws SQLException
    {
        getOriginal().setClob(parameterName, x);
    }

    @Override
    public void setAsciiStream(String parameterName, InputStream x, long length)
        throws SQLException
    {
        getOriginal().setAsciiStream(parameterName, x, length);
    }

    @Override
    public void setBinaryStream(String parameterName, InputStream x, long length)
        throws SQLException
    {
        getOriginal().setBinaryStream(parameterName, x, length);
    }

    @Override
    public void setCharacterStream(String parameterName, Reader reader, long length)
        throws SQLException
    {
        getOriginal().setCharacterStream(parameterName, reader, length);
    }

    @Override
    public void setAsciiStream(String parameterName, InputStream x)
        throws SQLException
    {
        getOriginal().setAsciiStream(parameterName, x);
    }

    @Override
    public void setBinaryStream(String parameterName, InputStream x)
        throws SQLException
    {
        getOriginal().setBinaryStream(parameterName, x);
    }

    @Override
    public void setCharacterStream(String parameterName, Reader reader)
        throws SQLException
    {
        getOriginal().setCharacterStream(parameterName, reader);
    }

    @Override
    public void setNCharacterStream(String parameterName, Reader value)
        throws SQLException
    {
        getOriginal().setNCharacterStream(parameterName, value);
    }

    @Override
    public void setClob(String parameterName, Reader reader)
        throws SQLException
    {
        getOriginal().setClob(parameterName, reader);
    }

    @Override
    public void setBlob(String parameterName, InputStream inputStream)
        throws SQLException
    {
        getOriginal().setBlob(parameterName, inputStream);
    }

    @Override
    public void setNClob(String parameterName, Reader reader)
        throws SQLException
    {
        getOriginal().setNClob(parameterName, reader);
    }

    @Override
    public <T> T getObject(int parameterIndex, Class<T> type) throws SQLException {
        return JDBCUtils.callMethod17(getOriginal(), "getObject", type, new Class[] {Integer.TYPE, Class.class}, parameterIndex, type);
    }

    @Override
    public <T> T getObject(String parameterName, Class<T> type) throws SQLException {
        return JDBCUtils.callMethod17(getOriginal(), "getObject", type, new Class[] {String.class, Class.class}, parameterName, type);
    }
}
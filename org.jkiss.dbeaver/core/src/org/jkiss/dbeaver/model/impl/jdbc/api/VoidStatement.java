/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.api;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;

/**
 * Void PrperedStatement
 */
public class VoidStatement implements PreparedStatement {

    public static final VoidStatement INSTANCE = new VoidStatement(); 

    public ResultSet executeQuery()
        throws SQLException
    {
        return null;
    }

    public int executeUpdate()
        throws SQLException
    {
        return 0;
    }

    public void setNull(int parameterIndex, int sqlType)
        throws SQLException
    {
      
    }

    public void setBoolean(int parameterIndex, boolean x)
        throws SQLException
    {
      
    }

    public void setByte(int parameterIndex, byte x)
        throws SQLException
    {
      
    }

    public void setShort(int parameterIndex, short x)
        throws SQLException
    {
      
    }

    public void setInt(int parameterIndex, int x)
        throws SQLException
    {
      
    }

    public void setLong(int parameterIndex, long x)
        throws SQLException
    {
      
    }

    public void setFloat(int parameterIndex, float x)
        throws SQLException
    {
      
    }

    public void setDouble(int parameterIndex, double x)
        throws SQLException
    {
      
    }

    public void setBigDecimal(int parameterIndex, BigDecimal x)
        throws SQLException
    {
      
    }

    public void setString(int parameterIndex, String x)
        throws SQLException
    {
      
    }

    public void setBytes(int parameterIndex, byte[] x)
        throws SQLException
    {
      
    }

    public void setDate(int parameterIndex, Date x)
        throws SQLException
    {
      
    }

    public void setTime(int parameterIndex, Time x)
        throws SQLException
    {
      
    }

    public void setTimestamp(int parameterIndex, Timestamp x)
        throws SQLException
    {
      
    }

    public void setAsciiStream(int parameterIndex, InputStream x, int length)
        throws SQLException
    {
      
    }

    public void setUnicodeStream(int parameterIndex, InputStream x, int length)
        throws SQLException
    {
      
    }

    public void setBinaryStream(int parameterIndex, InputStream x, int length)
        throws SQLException
    {
      
    }

    public void clearParameters()
        throws SQLException
    {
      
    }

    public void setObject(int parameterIndex, Object x, int targetSqlType)
        throws SQLException
    {
      
    }

    public void setObject(int parameterIndex, Object x)
        throws SQLException
    {
      
    }

    public boolean execute()
        throws SQLException
    {
        return false;
    }

    public void addBatch()
        throws SQLException
    {
      
    }

    public void setCharacterStream(int parameterIndex, Reader reader, int length)
        throws SQLException
    {
      
    }

    public void setRef(int parameterIndex, Ref x)
        throws SQLException
    {
      
    }

    public void setBlob(int parameterIndex, Blob x)
        throws SQLException
    {
      
    }

    public void setClob(int parameterIndex, Clob x)
        throws SQLException
    {
      
    }

    public void setArray(int parameterIndex, Array x)
        throws SQLException
    {
      
    }

    public ResultSetMetaData getMetaData()
        throws SQLException
    {
        return null;
    }

    public void setDate(int parameterIndex, Date x, Calendar cal)
        throws SQLException
    {
      
    }

    public void setTime(int parameterIndex, Time x, Calendar cal)
        throws SQLException
    {
      
    }

    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal)
        throws SQLException
    {
      
    }

    public void setNull(int parameterIndex, int sqlType, String typeName)
        throws SQLException
    {
      
    }

    public void setURL(int parameterIndex, URL x)
        throws SQLException
    {
      
    }

    public ParameterMetaData getParameterMetaData()
        throws SQLException
    {
        return null;
    }

    public void setRowId(int parameterIndex, RowId x)
        throws SQLException
    {
      
    }

    public void setNString(int parameterIndex, String value)
        throws SQLException
    {
      
    }

    public void setNCharacterStream(int parameterIndex, Reader value, long length)
        throws SQLException
    {
      
    }

    public void setNClob(int parameterIndex, NClob value)
        throws SQLException
    {
      
    }

    public void setClob(int parameterIndex, Reader reader, long length)
        throws SQLException
    {
      
    }

    public void setBlob(int parameterIndex, InputStream inputStream, long length)
        throws SQLException
    {
      
    }

    public void setNClob(int parameterIndex, Reader reader, long length)
        throws SQLException
    {
      
    }

    public void setSQLXML(int parameterIndex, SQLXML xmlObject)
        throws SQLException
    {
      
    }

    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength)
        throws SQLException
    {
      
    }

    public void setAsciiStream(int parameterIndex, InputStream x, long length)
        throws SQLException
    {
      
    }

    public void setBinaryStream(int parameterIndex, InputStream x, long length)
        throws SQLException
    {
      
    }

    public void setCharacterStream(int parameterIndex, Reader reader, long length)
        throws SQLException
    {
      
    }

    public void setAsciiStream(int parameterIndex, InputStream x)
        throws SQLException
    {
      
    }

    public void setBinaryStream(int parameterIndex, InputStream x)
        throws SQLException
    {
      
    }

    public void setCharacterStream(int parameterIndex, Reader reader)
        throws SQLException
    {
      
    }

    public void setNCharacterStream(int parameterIndex, Reader value)
        throws SQLException
    {
      
    }

    public void setClob(int parameterIndex, Reader reader)
        throws SQLException
    {
      
    }

    public void setBlob(int parameterIndex, InputStream inputStream)
        throws SQLException
    {
      
    }

    public void setNClob(int parameterIndex, Reader reader)
        throws SQLException
    {
      
    }

    public ResultSet executeQuery(String sql)
        throws SQLException
    {
        return null;
    }

    public int executeUpdate(String sql)
        throws SQLException
    {
        return 0;
    }

    public void close()
        throws SQLException
    {
      
    }

    public int getMaxFieldSize()
        throws SQLException
    {
        return 0;
    }

    public void setMaxFieldSize(int max)
        throws SQLException
    {
      
    }

    public int getMaxRows()
        throws SQLException
    {
        return 0;
    }

    public void setMaxRows(int max)
        throws SQLException
    {
      
    }

    public void setEscapeProcessing(boolean enable)
        throws SQLException
    {
      
    }

    public int getQueryTimeout()
        throws SQLException
    {
        return 0;
    }

    public void setQueryTimeout(int seconds)
        throws SQLException
    {
      
    }

    public void cancel()
        throws SQLException
    {
      
    }

    public SQLWarning getWarnings()
        throws SQLException
    {
        return null;
    }

    public void clearWarnings()
        throws SQLException
    {
      
    }

    public void setCursorName(String name)
        throws SQLException
    {
      
    }

    public boolean execute(String sql)
        throws SQLException
    {
        return false;
    }

    public ResultSet getResultSet()
        throws SQLException
    {
        return null;
    }

    public int getUpdateCount()
        throws SQLException
    {
        return 0;
    }

    public boolean getMoreResults()
        throws SQLException
    {
        return false;
    }

    public void setFetchDirection(int direction)
        throws SQLException
    {
      
    }

    public int getFetchDirection()
        throws SQLException
    {
        return 0;
    }

    public void setFetchSize(int rows)
        throws SQLException
    {
      
    }

    public int getFetchSize()
        throws SQLException
    {
        return 0;
    }

    public int getResultSetConcurrency()
        throws SQLException
    {
        return 0;
    }

    public int getResultSetType()
        throws SQLException
    {
        return 0;
    }

    public void addBatch(String sql)
        throws SQLException
    {
      
    }

    public void clearBatch()
        throws SQLException
    {
      
    }

    public int[] executeBatch()
        throws SQLException
    {
        return new int[0];
    }

    public Connection getConnection()
        throws SQLException
    {
        return null;
    }

    public boolean getMoreResults(int current)
        throws SQLException
    {
        return false;
    }

    public ResultSet getGeneratedKeys()
        throws SQLException
    {
        return null;
    }

    public int executeUpdate(String sql, int autoGeneratedKeys)
        throws SQLException
    {
        return 0;
    }

    public int executeUpdate(String sql, int[] columnIndexes)
        throws SQLException
    {
        return 0;
    }

    public int executeUpdate(String sql, String[] columnNames)
        throws SQLException
    {
        return 0;
    }

    public boolean execute(String sql, int autoGeneratedKeys)
        throws SQLException
    {
        return false;
    }

    public boolean execute(String sql, int[] columnIndexes)
        throws SQLException
    {
        return false;
    }

    public boolean execute(String sql, String[] columnNames)
        throws SQLException
    {
        return false;
    }

    public int getResultSetHoldability()
        throws SQLException
    {
        return 0;
    }

    public boolean isClosed()
        throws SQLException
    {
        return false;
    }

    public void setPoolable(boolean poolable)
        throws SQLException
    {
      
    }

    public boolean isPoolable()
        throws SQLException
    {
        return false;
    }

    public <T> T unwrap(Class<T> iface)
        throws SQLException
    {
        return null;
    }

    public boolean isWrapperFor(Class<?> iface)
        throws SQLException
    {
        return false;
    }
}

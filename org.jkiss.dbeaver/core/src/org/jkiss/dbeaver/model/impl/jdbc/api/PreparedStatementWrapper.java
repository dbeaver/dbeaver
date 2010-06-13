/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.api;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Ref;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Array;
import java.sql.ResultSetMetaData;
import java.sql.ParameterMetaData;
import java.sql.RowId;
import java.sql.NClob;
import java.sql.SQLXML;
import java.sql.SQLWarning;
import java.sql.Connection;
import java.math.BigDecimal;
import java.io.InputStream;
import java.io.Reader;
import java.util.Calendar;
import java.net.URL;

/**
 * PreparedStatement wrapper.
 * Forwards all methods to original statement.
 */
public class PreparedStatementWrapper implements PreparedStatement {

    private PreparedStatement original;

    public PreparedStatementWrapper(PreparedStatement original)
    {
        this.original = original;
    }

    public ResultSet executeQuery()
        throws SQLException
    {
        return original.executeQuery();
    }

    public int executeUpdate()
        throws SQLException
    {
        return original.executeUpdate();
    }

    public void setNull(int parameterIndex, int sqlType)
        throws SQLException
    {
        original.setNull(parameterIndex, sqlType);
    }

    public void setBoolean(int parameterIndex, boolean x)
        throws SQLException
    {
        original.setBoolean(parameterIndex, x);
    }

    public void setByte(int parameterIndex, byte x)
        throws SQLException
    {
        original.setByte(parameterIndex, x);
    }

    public void setShort(int parameterIndex, short x)
        throws SQLException
    {
        original.setShort(parameterIndex, x);
    }

    public void setInt(int parameterIndex, int x)
        throws SQLException
    {
        original.setInt(parameterIndex, x);
    }

    public void setLong(int parameterIndex, long x)
        throws SQLException
    {
        original.setLong(parameterIndex, x);
    }

    public void setFloat(int parameterIndex, float x)
        throws SQLException
    {
        original.setFloat(parameterIndex, x);
    }

    public void setDouble(int parameterIndex, double x)
        throws SQLException
    {
        original.setDouble(parameterIndex, x);
    }

    public void setBigDecimal(int parameterIndex, BigDecimal x)
        throws SQLException
    {
        original.setBigDecimal(parameterIndex, x);
    }

    public void setString(int parameterIndex, String x)
        throws SQLException
    {
        original.setString(parameterIndex, x);
    }

    public void setBytes(int parameterIndex, byte[] x)
        throws SQLException
    {
        original.setBytes(parameterIndex, x);
    }

    public void setDate(int parameterIndex, Date x)
        throws SQLException
    {
        original.setDate(parameterIndex, x);
    }

    public void setTime(int parameterIndex, Time x)
        throws SQLException
    {
        original.setTime(parameterIndex, x);
    }

    public void setTimestamp(
        int parameterIndex, Timestamp x)
        throws SQLException
    {
        original.setTimestamp(parameterIndex, x);
    }

    public void setAsciiStream(int parameterIndex, InputStream x, int length)
        throws SQLException
    {
        original.setAsciiStream(parameterIndex, x, length);
    }

    @SuppressWarnings("deprecated")
    public void setUnicodeStream(int parameterIndex, InputStream x, int length)
        throws SQLException
    {
        original.setUnicodeStream(parameterIndex, x, length);
    }

    public void setBinaryStream(int parameterIndex, InputStream x, int length)
        throws SQLException
    {
        original.setBinaryStream(parameterIndex, x, length);
    }

    public void clearParameters()
        throws SQLException
    {
        original.clearParameters();
    }

    public void setObject(int parameterIndex, Object x, int targetSqlType)
        throws SQLException
    {
        original.setObject(parameterIndex, x, targetSqlType);
    }

    public void setObject(int parameterIndex, Object x)
        throws SQLException
    {
        original.setObject(parameterIndex, x);
    }

    public boolean execute()
        throws SQLException
    {
        return original.execute();
    }

    public void addBatch()
        throws SQLException
    {
        original.addBatch();
    }

    public void setCharacterStream(int parameterIndex, Reader reader, int length)
        throws SQLException
    {
        original.setCharacterStream(parameterIndex, reader, length);
    }

    public void setRef(int parameterIndex, Ref x)
        throws SQLException
    {
        original.setRef(parameterIndex, x);
    }

    public void setBlob(int parameterIndex, Blob x)
        throws SQLException
    {
        original.setBlob(parameterIndex, x);
    }

    public void setClob(int parameterIndex, Clob x)
        throws SQLException
    {
        original.setClob(parameterIndex, x);
    }

    public void setArray(int parameterIndex, Array x)
        throws SQLException
    {
        original.setArray(parameterIndex, x);
    }

    public ResultSetMetaData getMetaData()
        throws SQLException
    {
        return original.getMetaData();
    }

    public void setDate(int parameterIndex, Date x, Calendar cal)
        throws SQLException
    {
        original.setDate(parameterIndex, x, cal);
    }

    public void setTime(int parameterIndex, Time x, Calendar cal)
        throws SQLException
    {
        original.setTime(parameterIndex, x, cal);
    }

    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal)
        throws SQLException
    {
        original.setTimestamp(parameterIndex, x, cal);
    }

    public void setNull(int parameterIndex, int sqlType, String typeName)
        throws SQLException
    {
        original.setNull(parameterIndex, sqlType, typeName);
    }

    public void setURL(int parameterIndex, URL x)
        throws SQLException
    {
        original.setURL(parameterIndex, x);
    }

    public ParameterMetaData getParameterMetaData()
        throws SQLException
    {
        return original.getParameterMetaData();
    }

    public void setRowId(int parameterIndex, RowId x)
        throws SQLException
    {
        original.setRowId(parameterIndex, x);
    }

    public void setNString(int parameterIndex, String value)
        throws SQLException
    {
        original.setNString(parameterIndex, value);
    }

    public void setNCharacterStream(int parameterIndex, Reader value, long length)
        throws SQLException
    {
        original.setNCharacterStream(parameterIndex, value, length);
    }

    public void setNClob(int parameterIndex, NClob value)
        throws SQLException
    {
        original.setNClob(parameterIndex, value);
    }

    public void setClob(int parameterIndex, Reader reader, long length)
        throws SQLException
    {
        original.setClob(parameterIndex, reader, length);
    }

    public void setBlob(int parameterIndex, InputStream inputStream, long length)
        throws SQLException
    {
        original.setBlob(parameterIndex, inputStream, length);
    }

    public void setNClob(int parameterIndex, Reader reader, long length)
        throws SQLException
    {
        original.setNClob(parameterIndex, reader, length);
    }

    public void setSQLXML(int parameterIndex, SQLXML xmlObject)
        throws SQLException
    {
        original.setSQLXML(parameterIndex, xmlObject);
    }

    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength)
        throws SQLException
    {
        original.setObject(parameterIndex, x, targetSqlType, scaleOrLength);
    }

    public void setAsciiStream(int parameterIndex, InputStream x, long length)
        throws SQLException
    {
        original.setAsciiStream(parameterIndex, x, length);
    }

    public void setBinaryStream(int parameterIndex, InputStream x, long length)
        throws SQLException
    {
        original.setBinaryStream(parameterIndex, x, length);
    }

    public void setCharacterStream(int parameterIndex, Reader reader, long length)
        throws SQLException
    {
        original.setCharacterStream(parameterIndex, reader, length);
    }

    public void setAsciiStream(int parameterIndex, InputStream x)
        throws SQLException
    {
        original.setAsciiStream(parameterIndex, x);
    }

    public void setBinaryStream(int parameterIndex, InputStream x)
        throws SQLException
    {
        original.setBinaryStream(parameterIndex, x);
    }

    public void setCharacterStream(int parameterIndex, Reader reader)
        throws SQLException
    {
        original.setCharacterStream(parameterIndex, reader);
    }

    public void setNCharacterStream(int parameterIndex, Reader value)
        throws SQLException
    {
        original.setNCharacterStream(parameterIndex, value);
    }

    public void setClob(int parameterIndex, Reader reader)
        throws SQLException
    {
        original.setClob(parameterIndex, reader);
    }

    public void setBlob(int parameterIndex, InputStream inputStream)
        throws SQLException
    {
        original.setBlob(parameterIndex, inputStream);
    }

    public void setNClob(int parameterIndex, Reader reader)
        throws SQLException
    {
        original.setNClob(parameterIndex, reader);
    }

    public ResultSet executeQuery(String sql)
        throws SQLException
    {
        return original.executeQuery(sql);
    }

    public int executeUpdate(String sql)
        throws SQLException
    {
        return original.executeUpdate(sql);
    }

    public void close()
        throws SQLException
    {
        original.close();
    }

    public int getMaxFieldSize()
        throws SQLException
    {
        return original.getMaxFieldSize();
    }

    public void setMaxFieldSize(int max)
        throws SQLException
    {
        original.setMaxFieldSize(max);
    }

    public int getMaxRows()
        throws SQLException
    {
        return original.getMaxRows();
    }

    public void setMaxRows(int max)
        throws SQLException
    {
        original.setMaxRows(max);
    }

    public void setEscapeProcessing(boolean enable)
        throws SQLException
    {
        original.setEscapeProcessing(enable);
    }

    public int getQueryTimeout()
        throws SQLException
    {
        return original.getQueryTimeout();
    }

    public void setQueryTimeout(int seconds)
        throws SQLException
    {
        original.setQueryTimeout(seconds);
    }

    public void cancel()
        throws SQLException
    {
        original.cancel();
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

    public void setCursorName(String name)
        throws SQLException
    {
        original.setCursorName(name);
    }

    public boolean execute(String sql)
        throws SQLException
    {
        return original.execute(sql);
    }

    public ResultSet getResultSet()
        throws SQLException
    {
        return original.getResultSet();
    }

    public int getUpdateCount()
        throws SQLException
    {
        return original.getUpdateCount();
    }

    public boolean getMoreResults()
        throws SQLException
    {
        return original.getMoreResults();
    }

    public void setFetchDirection(int direction)
        throws SQLException
    {
        original.setFetchDirection(direction);
    }

    public int getFetchDirection()
        throws SQLException
    {
        return original.getFetchDirection();
    }

    public void setFetchSize(int rows)
        throws SQLException
    {
        original.setFetchSize(rows);
    }

    public int getFetchSize()
        throws SQLException
    {
        return original.getFetchSize();
    }

    public int getResultSetConcurrency()
        throws SQLException
    {
        return original.getResultSetConcurrency();
    }

    public int getResultSetType()
        throws SQLException
    {
        return original.getResultSetType();
    }

    public void addBatch(String sql)
        throws SQLException
    {
        original.addBatch(sql);
    }

    public void clearBatch()
        throws SQLException
    {
        original.clearBatch();
    }

    public int[] executeBatch()
        throws SQLException
    {
        return original.executeBatch();
    }

    public Connection getConnection()
        throws SQLException
    {
        return original.getConnection();
    }

    public boolean getMoreResults(int current)
        throws SQLException
    {
        return original.getMoreResults(current);
    }

    public ResultSet getGeneratedKeys()
        throws SQLException
    {
        return original.getGeneratedKeys();
    }

    public int executeUpdate(String sql, int autoGeneratedKeys)
        throws SQLException
    {
        return original.executeUpdate(sql, autoGeneratedKeys);
    }

    public int executeUpdate(String sql, int[] columnIndexes)
        throws SQLException
    {
        return original.executeUpdate(sql, columnIndexes);
    }

    public int executeUpdate(String sql, String[] columnNames)
        throws SQLException
    {
        return original.executeUpdate(sql, columnNames);
    }

    public boolean execute(String sql, int autoGeneratedKeys)
        throws SQLException
    {
        return original.execute(sql, autoGeneratedKeys);
    }

    public boolean execute(String sql, int[] columnIndexes)
        throws SQLException
    {
        return original.execute(sql, columnIndexes);
    }

    public boolean execute(String sql, String[] columnNames)
        throws SQLException
    {
        return original.execute(sql, columnNames);
    }

    public int getResultSetHoldability()
        throws SQLException
    {
        return original.getResultSetHoldability();
    }

    public boolean isClosed()
        throws SQLException
    {
        return original.isClosed();
    }

    public void setPoolable(boolean poolable)
        throws SQLException
    {
        original.setPoolable(poolable);
    }

    public boolean isPoolable()
        throws SQLException
    {
        return original.isPoolable();
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

}

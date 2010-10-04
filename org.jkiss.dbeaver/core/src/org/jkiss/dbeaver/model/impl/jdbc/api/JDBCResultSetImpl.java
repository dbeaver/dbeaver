/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCResultSetMetaData;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.dbc.JDBCResultSetMetaData;
import org.jkiss.dbeaver.model.qm.QMUtils;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;
import java.util.Map;

/**
 * Managable result set
 */
public class JDBCResultSetImpl implements JDBCResultSet {

    static final Log log = LogFactory.getLog(JDBCResultSetImpl.class);

    private DBCExecutionContext context;
    private JDBCPreparedStatementImpl statement;
    private ResultSet original;
    private long rowsFetched;
    private long maxRows = -1;
    private boolean fake;

    public JDBCResultSetImpl(DBCExecutionContext context, ResultSet original, String description)
    {
        this.context = context;
        this.original = original;
        this.statement = new JDBCFakeStatementImpl((JDBCExecutionContext) context, this, description);
        this.fake = true;

        QMUtils.getDefaultHandler().handleResultSetOpen(this);
    }

    public JDBCResultSetImpl(JDBCPreparedStatementImpl statement, ResultSet original)
    {
        this.context = statement.getContext();
        this.statement = statement;
        this.original = original;
        this.fake = false;

        QMUtils.getDefaultHandler().handleResultSetOpen(this);
    }

    protected void beforeFetch()
    {
        //this.context.getProgressMonitor().startBlock(statement, null);
        //QMUtils.getDefaultHandler().handleResultSetFetch(this);
    }

    protected void afterFetch()
    {
        //this.context.getProgressMonitor().endBlock();
    }

    public ResultSet getOriginal()
    {
        return original;
    }

    public DBCExecutionContext getContext()
    {
        return context;
    }

    public DBCStatement getSource()
    {
        return statement;
    }

    public JDBCPreparedStatementImpl getStatement()
    {
        return statement;
    }

    public Object getColumnValue(int index)
        throws DBCException
    {
        try {
            return original.getObject(index);
        }
        catch (SQLException e) {
            throw new DBCException(e);
        }
    }

    public boolean nextRow()
        throws DBCException
    {
        try {
            return this.next();
        }
        catch (SQLException e) {
            throw new DBCException(e);
        }
    }

    public DBCResultSetMetaData getResultSetMetaData()
        throws DBCException
    {
        return new JDBCResultSetMetaData(this);
    }

    public void setMaxRows(long maxRows) {
        this.maxRows = maxRows;
    }

    public boolean next()
        throws SQLException
    {
        // Check max rows
        if (maxRows >= 0 && rowsFetched >= maxRows) {
            return false;
        }

        this.beforeFetch();
        try {
            // Fetch next row
            boolean fetched = original.next();
            if (fetched) {
                rowsFetched++;
            }
            return fetched;
        }
        finally {
            this.afterFetch();
        }
    }

    public void close()
    {
        QMUtils.getDefaultHandler().handleResultSetClose(this);
        try {
            original.close();
        }
        catch (SQLException e) {
            log.error("Could not close result set", e);
        }

        if (this.fake) {
            // Close fake statement
            statement.close();
        }
    }

    public boolean wasNull()
        throws SQLException
    {
        return original.wasNull();
    }

    public String getString(int columnIndex)
        throws SQLException
    {
        return original.getString(columnIndex);
    }

    public boolean getBoolean(int columnIndex)
        throws SQLException
    {
        return original.getBoolean(columnIndex);
    }

    public byte getByte(int columnIndex)
        throws SQLException
    {
        return original.getByte(columnIndex);
    }

    public short getShort(int columnIndex)
        throws SQLException
    {
        return original.getShort(columnIndex);
    }

    public int getInt(int columnIndex)
        throws SQLException
    {
        return original.getInt(columnIndex);
    }

    public long getLong(int columnIndex)
        throws SQLException
    {
        return original.getLong(columnIndex);
    }

    public float getFloat(int columnIndex)
        throws SQLException
    {
        return original.getFloat(columnIndex);
    }

    public double getDouble(int columnIndex)
        throws SQLException
    {
        return original.getDouble(columnIndex);
    }

    @Deprecated
    public BigDecimal getBigDecimal(int columnIndex, int scale)
        throws SQLException
    {
        return original.getBigDecimal(columnIndex, scale);
    }

    public byte[] getBytes(int columnIndex)
        throws SQLException
    {
        return original.getBytes(columnIndex);
    }

    public Date getDate(int columnIndex)
        throws SQLException
    {
        return original.getDate(columnIndex);
    }

    public Time getTime(int columnIndex)
        throws SQLException
    {
        return original.getTime(columnIndex);
    }

    public Timestamp getTimestamp(int columnIndex)
        throws SQLException
    {
        return original.getTimestamp(columnIndex);
    }

    public InputStream getAsciiStream(int columnIndex)
        throws SQLException
    {
        return original.getAsciiStream(columnIndex);
    }

    @Deprecated
    public InputStream getUnicodeStream(int columnIndex)
        throws SQLException
    {
        return original.getUnicodeStream(columnIndex);
    }

    public InputStream getBinaryStream(int columnIndex)
        throws SQLException
    {
        return original.getBinaryStream(columnIndex);
    }

    public String getString(String columnLabel)
        throws SQLException
    {
        return original.getString(columnLabel);
    }

    public boolean getBoolean(String columnLabel)
        throws SQLException
    {
        return original.getBoolean(columnLabel);
    }

    public byte getByte(String columnLabel)
        throws SQLException
    {
        return original.getByte(columnLabel);
    }

    public short getShort(String columnLabel)
        throws SQLException
    {
        return original.getShort(columnLabel);
    }

    public int getInt(String columnLabel)
        throws SQLException
    {
        return original.getInt(columnLabel);
    }

    public long getLong(String columnLabel)
        throws SQLException
    {
        return original.getLong(columnLabel);
    }

    public float getFloat(String columnLabel)
        throws SQLException
    {
        return original.getFloat(columnLabel);
    }

    public double getDouble(String columnLabel)
        throws SQLException
    {
        return original.getDouble(columnLabel);
    }

    @Deprecated
    public BigDecimal getBigDecimal(String columnLabel, int scale)
        throws SQLException
    {
        return original.getBigDecimal(columnLabel, scale);
    }

    public byte[] getBytes(String columnLabel)
        throws SQLException
    {
        return original.getBytes(columnLabel);
    }

    public Date getDate(String columnLabel)
        throws SQLException
    {
        return original.getDate(columnLabel);
    }

    public Time getTime(String columnLabel)
        throws SQLException
    {
        return original.getTime(columnLabel);
    }

    public Timestamp getTimestamp(String columnLabel)
        throws SQLException
    {
        return original.getTimestamp(columnLabel);
    }

    public InputStream getAsciiStream(String columnLabel)
        throws SQLException
    {
        return original.getAsciiStream(columnLabel);
    }

    @Deprecated
    public InputStream getUnicodeStream(String columnLabel)
        throws SQLException
    {
        return original.getUnicodeStream(columnLabel);
    }

    public InputStream getBinaryStream(String columnLabel)
        throws SQLException
    {
        return original.getBinaryStream(columnLabel);
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

    public String getCursorName()
        throws SQLException
    {
        return original.getCursorName();
    }

    public ResultSetMetaData getMetaData()
        throws SQLException
    {
        return new JDBCResultSetMetaData(this, original.getMetaData());
    }

    public Object getObject(int columnIndex)
        throws SQLException
    {
        return original.getObject(columnIndex);
    }

    public Object getObject(String columnLabel)
        throws SQLException
    {
        return original.getObject(columnLabel);
    }

    public int findColumn(String columnLabel)
        throws SQLException
    {
        return original.findColumn(columnLabel);
    }

    public Reader getCharacterStream(int columnIndex)
        throws SQLException
    {
        return original.getCharacterStream(columnIndex);
    }

    public Reader getCharacterStream(String columnLabel)
        throws SQLException
    {
        return original.getCharacterStream(columnLabel);
    }

    public BigDecimal getBigDecimal(int columnIndex)
        throws SQLException
    {
        return original.getBigDecimal(columnIndex);
    }

    public BigDecimal getBigDecimal(String columnLabel)
        throws SQLException
    {
        return original.getBigDecimal(columnLabel);
    }

    public boolean isBeforeFirst()
        throws SQLException
    {
        return original.isBeforeFirst();
    }

    public boolean isAfterLast()
        throws SQLException
    {
        return original.isAfterLast();
    }

    public boolean isFirst()
        throws SQLException
    {
        return original.isFirst();
    }

    public boolean isLast()
        throws SQLException
    {
        return original.isLast();
    }

    public void beforeFirst()
        throws SQLException
    {
        original.beforeFirst();
    }

    public void afterLast()
        throws SQLException
    {
        original.afterLast();
    }

    public boolean first()
        throws SQLException
    {
        return original.first();
    }

    public boolean last()
        throws SQLException
    {
        return original.last();
    }

    public int getRow()
        throws SQLException
    {
        return original.getRow();
    }

    public boolean absolute(int row)
        throws SQLException
    {
        return original.absolute(row);
    }

    public boolean relative(int rows)
        throws SQLException
    {
        return original.relative(rows);
    }

    public boolean previous()
        throws SQLException
    {
        return original.previous();
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

    public int getType()
        throws SQLException
    {
        return original.getType();
    }

    public int getConcurrency()
        throws SQLException
    {
        return original.getConcurrency();
    }

    public boolean rowUpdated()
        throws SQLException
    {
        return original.rowUpdated();
    }

    public boolean rowInserted()
        throws SQLException
    {
        return original.rowInserted();
    }

    public boolean rowDeleted()
        throws SQLException
    {
        return original.rowDeleted();
    }

    public void updateNull(int columnIndex)
        throws SQLException
    {
        original.updateNull(columnIndex);
    }

    public void updateBoolean(int columnIndex, boolean x)
        throws SQLException
    {
        original.updateBoolean(columnIndex, x);
    }

    public void updateByte(int columnIndex, byte x)
        throws SQLException
    {
        original.updateByte(columnIndex, x);
    }

    public void updateShort(int columnIndex, short x)
        throws SQLException
    {
        original.updateShort(columnIndex, x);
    }

    public void updateInt(int columnIndex, int x)
        throws SQLException
    {
        original.updateInt(columnIndex, x);
    }

    public void updateLong(int columnIndex, long x)
        throws SQLException
    {
        original.updateLong(columnIndex, x);
    }

    public void updateFloat(int columnIndex, float x)
        throws SQLException
    {
        original.updateFloat(columnIndex, x);
    }

    public void updateDouble(int columnIndex, double x)
        throws SQLException
    {
        original.updateDouble(columnIndex, x);
    }

    public void updateBigDecimal(int columnIndex, BigDecimal x)
        throws SQLException
    {
        original.updateBigDecimal(columnIndex, x);
    }

    public void updateString(int columnIndex, String x)
        throws SQLException
    {
        original.updateString(columnIndex, x);
    }

    public void updateBytes(int columnIndex, byte[] x)
        throws SQLException
    {
        original.updateBytes(columnIndex, x);
    }

    public void updateDate(int columnIndex, Date x)
        throws SQLException
    {
        original.updateDate(columnIndex, x);
    }

    public void updateTime(int columnIndex, Time x)
        throws SQLException
    {
        original.updateTime(columnIndex, x);
    }

    public void updateTimestamp(int columnIndex, Timestamp x)
        throws SQLException
    {
        original.updateTimestamp(columnIndex, x);
    }

    public void updateAsciiStream(int columnIndex, InputStream x, int length)
        throws SQLException
    {
        original.updateAsciiStream(columnIndex, x, length);
    }

    public void updateBinaryStream(int columnIndex, InputStream x, int length)
        throws SQLException
    {
        original.updateBinaryStream(columnIndex, x, length);
    }

    public void updateCharacterStream(int columnIndex, Reader x, int length)
        throws SQLException
    {
        original.updateCharacterStream(columnIndex, x, length);
    }

    public void updateObject(int columnIndex, Object x, int scaleOrLength)
        throws SQLException
    {
        original.updateObject(columnIndex, x, scaleOrLength);
    }

    public void updateObject(int columnIndex, Object x)
        throws SQLException
    {
        original.updateObject(columnIndex, x);
    }

    public void updateNull(String columnLabel)
        throws SQLException
    {
        original.updateNull(columnLabel);
    }

    public void updateBoolean(String columnLabel, boolean x)
        throws SQLException
    {
        original.updateBoolean(columnLabel, x);
    }

    public void updateByte(String columnLabel, byte x)
        throws SQLException
    {
        original.updateByte(columnLabel, x);
    }

    public void updateShort(String columnLabel, short x)
        throws SQLException
    {
        original.updateShort(columnLabel, x);
    }

    public void updateInt(String columnLabel, int x)
        throws SQLException
    {
        original.updateInt(columnLabel, x);
    }

    public void updateLong(String columnLabel, long x)
        throws SQLException
    {
        original.updateLong(columnLabel, x);
    }

    public void updateFloat(String columnLabel, float x)
        throws SQLException
    {
        original.updateFloat(columnLabel, x);
    }

    public void updateDouble(String columnLabel, double x)
        throws SQLException
    {
        original.updateDouble(columnLabel, x);
    }

    public void updateBigDecimal(String columnLabel, BigDecimal x)
        throws SQLException
    {
        original.updateBigDecimal(columnLabel, x);
    }

    public void updateString(String columnLabel, String x)
        throws SQLException
    {
        original.updateString(columnLabel, x);
    }

    public void updateBytes(String columnLabel, byte[] x)
        throws SQLException
    {
        original.updateBytes(columnLabel, x);
    }

    public void updateDate(String columnLabel, Date x)
        throws SQLException
    {
        original.updateDate(columnLabel, x);
    }

    public void updateTime(String columnLabel, Time x)
        throws SQLException
    {
        original.updateTime(columnLabel, x);
    }

    public void updateTimestamp(String columnLabel, Timestamp x)
        throws SQLException
    {
        original.updateTimestamp(columnLabel, x);
    }

    public void updateAsciiStream(String columnLabel, InputStream x, int length)
        throws SQLException
    {
        original.updateAsciiStream(columnLabel, x, length);
    }

    public void updateBinaryStream(String columnLabel, InputStream x, int length)
        throws SQLException
    {
        original.updateBinaryStream(columnLabel, x, length);
    }

    public void updateCharacterStream(String columnLabel, Reader reader, int length)
        throws SQLException
    {
        original.updateCharacterStream(columnLabel, reader, length);
    }

    public void updateObject(String columnLabel, Object x, int scaleOrLength)
        throws SQLException
    {
        original.updateObject(columnLabel, x, scaleOrLength);
    }

    public void updateObject(String columnLabel, Object x)
        throws SQLException
    {
        original.updateObject(columnLabel, x);
    }

    public void insertRow()
        throws SQLException
    {
        original.insertRow();
    }

    public void updateRow()
        throws SQLException
    {
        original.updateRow();
    }

    public void deleteRow()
        throws SQLException
    {
        original.deleteRow();
    }

    public void refreshRow()
        throws SQLException
    {
        original.refreshRow();
    }

    public void cancelRowUpdates()
        throws SQLException
    {
        original.cancelRowUpdates();
    }

    public void moveToInsertRow()
        throws SQLException
    {
        original.moveToInsertRow();
    }

    public void moveToCurrentRow()
        throws SQLException
    {
        original.moveToCurrentRow();
    }

    public Object getObject(int columnIndex, Map<String, Class<?>> map)
        throws SQLException
    {
        return original.getObject(columnIndex, map);
    }

    public Ref getRef(int columnIndex)
        throws SQLException
    {
        return original.getRef(columnIndex);
    }

    public Blob getBlob(int columnIndex)
        throws SQLException
    {
        return original.getBlob(columnIndex);
    }

    public Clob getClob(int columnIndex)
        throws SQLException
    {
        return original.getClob(columnIndex);
    }

    public Array getArray(int columnIndex)
        throws SQLException
    {
        return original.getArray(columnIndex);
    }

    public Object getObject(String columnLabel, Map<String, Class<?>> map)
        throws SQLException
    {
        return original.getObject(columnLabel, map);
    }

    public Ref getRef(String columnLabel)
        throws SQLException
    {
        return original.getRef(columnLabel);
    }

    public Blob getBlob(String columnLabel)
        throws SQLException
    {
        return original.getBlob(columnLabel);
    }

    public Clob getClob(String columnLabel)
        throws SQLException
    {
        return original.getClob(columnLabel);
    }

    public Array getArray(String columnLabel)
        throws SQLException
    {
        return original.getArray(columnLabel);
    }

    public Date getDate(int columnIndex, Calendar cal)
        throws SQLException
    {
        return original.getDate(columnIndex, cal);
    }

    public Date getDate(String columnLabel, Calendar cal)
        throws SQLException
    {
        return original.getDate(columnLabel, cal);
    }

    public Time getTime(int columnIndex, Calendar cal)
        throws SQLException
    {
        return original.getTime(columnIndex, cal);
    }

    public Time getTime(String columnLabel, Calendar cal)
        throws SQLException
    {
        return original.getTime(columnLabel, cal);
    }

    public Timestamp getTimestamp(int columnIndex, Calendar cal)
        throws SQLException
    {
        return original.getTimestamp(columnIndex, cal);
    }

    public Timestamp getTimestamp(String columnLabel, Calendar cal)
        throws SQLException
    {
        return original.getTimestamp(columnLabel, cal);
    }

    public URL getURL(int columnIndex)
        throws SQLException
    {
        return original.getURL(columnIndex);
    }

    public URL getURL(String columnLabel)
        throws SQLException
    {
        return original.getURL(columnLabel);
    }

    public void updateRef(int columnIndex, Ref x)
        throws SQLException
    {
        original.updateRef(columnIndex, x);
    }

    public void updateRef(String columnLabel, Ref x)
        throws SQLException
    {
        original.updateRef(columnLabel, x);
    }

    public void updateBlob(int columnIndex, Blob x)
        throws SQLException
    {
        original.updateBlob(columnIndex, x);
    }

    public void updateBlob(String columnLabel, Blob x)
        throws SQLException
    {
        original.updateBlob(columnLabel, x);
    }

    public void updateClob(int columnIndex, Clob x)
        throws SQLException
    {
        original.updateClob(columnIndex, x);
    }

    public void updateClob(String columnLabel, Clob x)
        throws SQLException
    {
        original.updateClob(columnLabel, x);
    }

    public void updateArray(int columnIndex, Array x)
        throws SQLException
    {
        original.updateArray(columnIndex, x);
    }

    public void updateArray(String columnLabel, Array x)
        throws SQLException
    {
        original.updateArray(columnLabel, x);
    }

    public RowId getRowId(int columnIndex)
        throws SQLException
    {
        return original.getRowId(columnIndex);
    }

    public RowId getRowId(String columnLabel)
        throws SQLException
    {
        return original.getRowId(columnLabel);
    }

    public void updateRowId(int columnIndex, RowId x)
        throws SQLException
    {
        original.updateRowId(columnIndex, x);
    }

    public void updateRowId(String columnLabel, RowId x)
        throws SQLException
    {
        original.updateRowId(columnLabel, x);
    }

    public int getHoldability()
        throws SQLException
    {
        return original.getHoldability();
    }

    public boolean isClosed()
        throws SQLException
    {
        return original.isClosed();
    }

    public void updateNString(int columnIndex, String nString)
        throws SQLException
    {
        original.updateNString(columnIndex, nString);
    }

    public void updateNString(String columnLabel, String nString)
        throws SQLException
    {
        original.updateNString(columnLabel, nString);
    }

    public void updateNClob(int columnIndex, NClob nClob)
        throws SQLException
    {
        original.updateNClob(columnIndex, nClob);
    }

    public void updateNClob(String columnLabel, NClob nClob)
        throws SQLException
    {
        original.updateNClob(columnLabel, nClob);
    }

    public NClob getNClob(int columnIndex)
        throws SQLException
    {
        return original.getNClob(columnIndex);
    }

    public NClob getNClob(String columnLabel)
        throws SQLException
    {
        return original.getNClob(columnLabel);
    }

    public SQLXML getSQLXML(int columnIndex)
        throws SQLException
    {
        return original.getSQLXML(columnIndex);
    }

    public SQLXML getSQLXML(String columnLabel)
        throws SQLException
    {
        return original.getSQLXML(columnLabel);
    }

    public void updateSQLXML(int columnIndex, SQLXML xmlObject)
        throws SQLException
    {
        original.updateSQLXML(columnIndex, xmlObject);
    }

    public void updateSQLXML(String columnLabel, SQLXML xmlObject)
        throws SQLException
    {
        original.updateSQLXML(columnLabel, xmlObject);
    }

    public String getNString(int columnIndex)
        throws SQLException
    {
        return original.getNString(columnIndex);
    }

    public String getNString(String columnLabel)
        throws SQLException
    {
        return original.getNString(columnLabel);
    }

    public Reader getNCharacterStream(int columnIndex)
        throws SQLException
    {
        return original.getNCharacterStream(columnIndex);
    }

    public Reader getNCharacterStream(String columnLabel)
        throws SQLException
    {
        return original.getNCharacterStream(columnLabel);
    }

    public void updateNCharacterStream(int columnIndex, Reader x, long length)
        throws SQLException
    {
        original.updateNCharacterStream(columnIndex, x, length);
    }

    public void updateNCharacterStream(String columnLabel, Reader reader, long length)
        throws SQLException
    {
        original.updateNCharacterStream(columnLabel, reader, length);
    }

    public void updateAsciiStream(int columnIndex, InputStream x, long length)
        throws SQLException
    {
        original.updateAsciiStream(columnIndex, x, length);
    }

    public void updateBinaryStream(int columnIndex, InputStream x, long length)
        throws SQLException
    {
        original.updateBinaryStream(columnIndex, x, length);
    }

    public void updateCharacterStream(int columnIndex, Reader x, long length)
        throws SQLException
    {
        original.updateCharacterStream(columnIndex, x, length);
    }

    public void updateAsciiStream(String columnLabel, InputStream x, long length)
        throws SQLException
    {
        original.updateAsciiStream(columnLabel, x, length);
    }

    public void updateBinaryStream(String columnLabel, InputStream x, long length)
        throws SQLException
    {
        original.updateBinaryStream(columnLabel, x, length);
    }

    public void updateCharacterStream(String columnLabel, Reader reader, long length)
        throws SQLException
    {
        original.updateCharacterStream(columnLabel, reader, length);
    }

    public void updateBlob(int columnIndex, InputStream inputStream, long length)
        throws SQLException
    {
        original.updateBlob(columnIndex, inputStream, length);
    }

    public void updateBlob(String columnLabel, InputStream inputStream, long length)
        throws SQLException
    {
        original.updateBlob(columnLabel, inputStream, length);
    }

    public void updateClob(int columnIndex, Reader reader, long length)
        throws SQLException
    {
        original.updateClob(columnIndex, reader, length);
    }

    public void updateClob(String columnLabel, Reader reader, long length)
        throws SQLException
    {
        original.updateClob(columnLabel, reader, length);
    }

    public void updateNClob(int columnIndex, Reader reader, long length)
        throws SQLException
    {
        original.updateNClob(columnIndex, reader, length);
    }

    public void updateNClob(String columnLabel, Reader reader, long length)
        throws SQLException
    {
        original.updateNClob(columnLabel, reader, length);
    }

    public void updateNCharacterStream(int columnIndex, Reader x)
        throws SQLException
    {
        original.updateNCharacterStream(columnIndex, x);
    }

    public void updateNCharacterStream(String columnLabel, Reader reader)
        throws SQLException
    {
        original.updateNCharacterStream(columnLabel, reader);
    }

    public void updateAsciiStream(int columnIndex, InputStream x)
        throws SQLException
    {
        original.updateAsciiStream(columnIndex, x);
    }

    public void updateBinaryStream(int columnIndex, InputStream x)
        throws SQLException
    {
        original.updateBinaryStream(columnIndex, x);
    }

    public void updateCharacterStream(int columnIndex, Reader x)
        throws SQLException
    {
        original.updateCharacterStream(columnIndex, x);
    }

    public void updateAsciiStream(String columnLabel, InputStream x)
        throws SQLException
    {
        original.updateAsciiStream(columnLabel, x);
    }

    public void updateBinaryStream(String columnLabel, InputStream x)
        throws SQLException
    {
        original.updateBinaryStream(columnLabel, x);
    }

    public void updateCharacterStream(String columnLabel, Reader reader)
        throws SQLException
    {
        original.updateCharacterStream(columnLabel, reader);
    }

    public void updateBlob(int columnIndex, InputStream inputStream)
        throws SQLException
    {
        original.updateBlob(columnIndex, inputStream);
    }

    public void updateBlob(String columnLabel, InputStream inputStream)
        throws SQLException
    {
        original.updateBlob(columnLabel, inputStream);
    }

    public void updateClob(int columnIndex, Reader reader)
        throws SQLException
    {
        original.updateClob(columnIndex, reader);
    }

    public void updateClob(String columnLabel, Reader reader)
        throws SQLException
    {
        original.updateClob(columnLabel, reader);
    }

    public void updateNClob(int columnIndex, Reader reader)
        throws SQLException
    {
        original.updateNClob(columnIndex, reader);
    }

    public void updateNClob(String columnLabel, Reader reader)
        throws SQLException
    {
        original.updateNClob(columnLabel, reader);
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

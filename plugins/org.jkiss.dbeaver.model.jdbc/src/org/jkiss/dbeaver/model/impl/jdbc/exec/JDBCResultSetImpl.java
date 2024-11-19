/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.impl.jdbc.exec;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSetMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.AbstractResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCTrace;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
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
public class JDBCResultSetImpl extends AbstractResultSet<JDBCSession, JDBCStatement> implements JDBCResultSet {

    private static final Log log = Log.getLog(JDBCResultSetImpl.class);

    private final ResultSet original;
    private final String description;
    private JDBCResultSetMetaData metaData;
    private long rowsFetched;
    private long maxRows = -1;
    private final boolean fake;
    private final boolean disableLogging;

    public static JDBCResultSet makeResultSet(@NotNull JDBCSession session, @Nullable JDBCStatement statement, @NotNull ResultSet original, String description, boolean disableLogging)
        throws SQLException
    {
        return session.getDataSource().getJdbcFactory().createResultSet(session, statement, original, description, disableLogging);
    }

    protected JDBCResultSetImpl(@NotNull JDBCSession session, @Nullable JDBCStatement statement, @NotNull ResultSet original, String description, boolean disableLogging)
    {
        super(session, statement);
        this.original = original;
        this.disableLogging = disableLogging;
        this.description = description;
        this.fake = statement == null;

        if (!disableLogging) {
            // Notify handler
            QMUtils.getDefaultHandler().handleResultSetOpen(this);
        }
        if (JDBCTrace.isApiTraceEnabled()) {
            JDBCTrace.dumpResultSetOpen(this.original);
        }
    }
/*

    protected JDBCResultSetImpl(JDBCStatementImpl statement, ResultSet original)
    {
        this.session = statement.getSession();
        this.statement = statement;
        this.original = original;
        this.fake = false;

        if (this.statement.isQMLoggingEnabled()) {
            // Notify handler
            QMUtils.getDefaultHandler().handleResultSetOpen(this);
        }
    }
*/

    protected void beforeFetch()
    {
        // FIXME: starte/end block. Do we need them here?
        //this.session.getProgressMonitor().startBlock(statement, null);
        //QMUtils.getDefaultHandler().handleResultSetFetch(this);
        this.session.getExecutionContext().lockQueryExecution();
    }

    protected void afterFetch()
    {
        this.session.getExecutionContext().unlockQueryExecution();
        if (JDBCUtils.LOG_JDBC_WARNINGS) {
            try {
                JDBCUtils.reportWarnings(getSession(), this.getWarnings());
            } catch (Throwable e) {
                log.debug("Error reading JDBC warnings: " + e.getMessage());
            }
        }
        //this.session.getProgressMonitor().endBlock();
    }

    @Override
    public ResultSet getOriginal()
    {
        return original;
    }

    @Override
    public JDBCSession getSession()
    {
        return session;
    }

    @Override
    public JDBCStatement getSourceStatement()
    {
        if (fake && statement == null) {
            // Make fake statement
            JDBCFakeStatementImpl fakeStatement = new JDBCFakeStatementImpl(
                session,
                this,
                "-- " + description, // Set description as commented SQL
                disableLogging);
            this.statement = fakeStatement;

            fakeStatement.beforeExecute();
            fakeStatement.afterExecute();
        }
        return statement;
    }

    @Override
    public JDBCStatement getStatement()
    {
        return getSourceStatement();
    }

    @Override
    public Object getAttributeValue(int index)
        throws DBCException
    {
        checkNotEmpty();
        try {
            // JDBC uses 1-based indexes
            return original.getObject(index + 1);
        }
        catch (SQLException e) {
            throw new DBCException(e, session.getExecutionContext());
        }
    }

    @Override
    public Object getAttributeValue(String name) throws DBCException {
        checkNotEmpty();
        try {
            return original.getObject(name);
        }
        catch (SQLException e) {
            throw new DBCException(e, session.getExecutionContext());
        }
    }

    private void checkNotEmpty()
    {
        if (original == null) {
            throw new IllegalStateException();
        }
    }

    @Override
    public boolean nextRow()
        throws DBCException
    {
        if (this.original == null) {
            return false;
        }
        try {
            return this.next();
        }
        catch (SQLException e) {
            throw new DBCException(e, session.getExecutionContext());
        }
    }

    @Override
    public boolean moveTo(int position) throws DBCException
    {
        if (this.original == null) {
            return false;
        }
        try {
            return this.absolute(position);
        }
        catch (SQLException e) {
            throw new DBCException(e, session.getExecutionContext());
        }
    }

    @NotNull
    @Override
    public JDBCResultSetMetaData getMeta()
        throws DBCException
    {
        if (metaData == null) {
            try {
                metaData = createMetaDataImpl();
            } catch (SQLException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
        }
        return metaData;
    }

    @Nullable
    @Override
    public String getResultSetName() throws DBCException {
        if (this.original == null) {
            return null;
        }
        try {
            return original.getCursorName();
        }
        catch (SQLException e) {
            throw new DBCException(e, session.getExecutionContext());
        }
    }

    @Override
    public Object getFeature(String name) {
        if (FEATURE_NAME_JDBC.equals(name)) {
            return true;
        }
        return super.getFeature(name);
    }

    @Override
    public ResultSetMetaData getMetaData()
        throws SQLException
    {
        try {
            return getMeta();
        } catch (DBCException e) {
            if (e.getCause() instanceof SQLException) {
                throw (SQLException)e.getCause();
            } else {
                throw new SQLException(e);
            }
        }
    }

    public void setMaxRows(long maxRows) {
        this.maxRows = maxRows;
    }

    @Override
    public boolean next()
        throws SQLException
    {
        if (this.original == null) {
            return false;
        }
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
            if (fetched && JDBCTrace.isApiTraceEnabled()) {
                JDBCTrace.dumpResultSetRow(this.original);
            }

            return fetched;
        }
        finally {
            this.afterFetch();
        }
    }

    @Override
    public void close()
    {
        if (original != null) {
/*
            // Check for warnings
            try {
                JDBCUtils.reportWarnings(session, getOriginal().getWarnings());
                getOriginal().clearWarnings();
            } catch (Throwable e) {
                log.debug("Can't check for resultset warnings", e);
            }
*/
            if (!disableLogging) {
                // Handle close
                QMUtils.getDefaultHandler().handleResultSetClose(this, rowsFetched);
            }

            // Close result set
            try {
                original.close();
            }
            catch (SQLException e) {
                log.error("Can't close result set", e);
            }
        }
        if (fake && statement != null) {
            statement.close();
        }
        if (JDBCTrace.isApiTraceEnabled()) {
            JDBCTrace.dumpResultSetClose();
        }
    }

    @Override
    public boolean wasNull()
        throws SQLException
    {
        checkNotEmpty();
        return original.wasNull();
    }

    @Override
    public String getString(int columnIndex)
        throws SQLException
    {
        checkNotEmpty();
        return original.getString(columnIndex);
    }

    private static void traceGetValue(int columnIndex, String value) {

    }

    @Override
    public boolean getBoolean(int columnIndex)
        throws SQLException
    {
        checkNotEmpty();
        return original.getBoolean(columnIndex);
    }

    @Override
    public byte getByte(int columnIndex)
        throws SQLException
    {
        checkNotEmpty();
        return original.getByte(columnIndex);
    }

    @Override
    public short getShort(int columnIndex)
        throws SQLException
    {
        checkNotEmpty();
        return original.getShort(columnIndex);
    }

    @Override
    public int getInt(int columnIndex)
        throws SQLException
    {
        checkNotEmpty();
        return original.getInt(columnIndex);
    }

    @Override
    public long getLong(int columnIndex)
        throws SQLException
    {
        checkNotEmpty();
        return original.getLong(columnIndex);
    }

    @Override
    public float getFloat(int columnIndex)
        throws SQLException
    {
        checkNotEmpty();
        return original.getFloat(columnIndex);
    }

    @Override
    public double getDouble(int columnIndex)
        throws SQLException
    {
        checkNotEmpty();
        return original.getDouble(columnIndex);
    }

    @Override
    @Deprecated
    public BigDecimal getBigDecimal(int columnIndex, int scale)
        throws SQLException
    {
        checkNotEmpty();
        return original.getBigDecimal(columnIndex, scale);
    }

    @Override
    public byte[] getBytes(int columnIndex)
        throws SQLException
    {
        checkNotEmpty();
        return original.getBytes(columnIndex);
    }

    @Override
    public Date getDate(int columnIndex)
        throws SQLException
    {
        checkNotEmpty();
        return original.getDate(columnIndex);
    }

    @Override
    public Time getTime(int columnIndex)
        throws SQLException
    {
        checkNotEmpty();
        return original.getTime(columnIndex);
    }

    @Override
    public Timestamp getTimestamp(int columnIndex)
        throws SQLException
    {
        checkNotEmpty();
        return original.getTimestamp(columnIndex);
    }

    @Override
    public InputStream getAsciiStream(int columnIndex)
        throws SQLException
    {
        checkNotEmpty();
        return original.getAsciiStream(columnIndex);
    }

    @Override
    @Deprecated
    public InputStream getUnicodeStream(int columnIndex)
        throws SQLException
    {
        checkNotEmpty();
        return original.getUnicodeStream(columnIndex);
    }

    @Override
    public InputStream getBinaryStream(int columnIndex)
        throws SQLException
    {
        checkNotEmpty();
        return original.getBinaryStream(columnIndex);
    }

    @Override
    public String getString(String columnLabel)
        throws SQLException
    {
        checkNotEmpty();
        return original.getString(columnLabel);
    }

    @Override
    public boolean getBoolean(String columnLabel)
        throws SQLException
    {
        checkNotEmpty();
        return original.getBoolean(columnLabel);
    }

    @Override
    public byte getByte(String columnLabel)
        throws SQLException
    {
        checkNotEmpty();
        return original.getByte(columnLabel);
    }

    @Override
    public short getShort(String columnLabel)
        throws SQLException
    {
        checkNotEmpty();
        return original.getShort(columnLabel);
    }

    @Override
    public int getInt(String columnLabel)
        throws SQLException
    {
        checkNotEmpty();
        return original.getInt(columnLabel);
    }

    @Override
    public long getLong(String columnLabel)
        throws SQLException
    {
        checkNotEmpty();
        return original.getLong(columnLabel);
    }

    @Override
    public float getFloat(String columnLabel)
        throws SQLException
    {
        checkNotEmpty();
        return original.getFloat(columnLabel);
    }

    @Override
    public double getDouble(String columnLabel)
        throws SQLException
    {
        checkNotEmpty();
        return original.getDouble(columnLabel);
    }

    @Override
    @Deprecated
    public BigDecimal getBigDecimal(String columnLabel, int scale)
        throws SQLException
    {
        checkNotEmpty();
        return original.getBigDecimal(columnLabel, scale);
    }

    @Override
    public byte[] getBytes(String columnLabel)
        throws SQLException
    {
        checkNotEmpty();
        return original.getBytes(columnLabel);
    }

    @Override
    public Date getDate(String columnLabel)
        throws SQLException
    {
        checkNotEmpty();
        return original.getDate(columnLabel);
    }

    @Override
    public Time getTime(String columnLabel)
        throws SQLException
    {
        checkNotEmpty();
        return original.getTime(columnLabel);
    }

    @Override
    public Timestamp getTimestamp(String columnLabel)
        throws SQLException
    {
        checkNotEmpty();
        return original.getTimestamp(columnLabel);
    }

    @Override
    public InputStream getAsciiStream(String columnLabel)
        throws SQLException
    {
        checkNotEmpty();
        return original.getAsciiStream(columnLabel);
    }

    @Override
    @Deprecated
    public InputStream getUnicodeStream(String columnLabel)
        throws SQLException
    {
        checkNotEmpty();
        return original.getUnicodeStream(columnLabel);
    }

    @Override
    public InputStream getBinaryStream(String columnLabel)
        throws SQLException
    {
        checkNotEmpty();
        return original.getBinaryStream(columnLabel);
    }

    @Override
    public SQLWarning getWarnings()
        throws SQLException
    {
        checkNotEmpty();
        return original.getWarnings();
    }

    @Override
    public void clearWarnings()
        throws SQLException
    {
        if (original == null) {
            return;
        }
        original.clearWarnings();
    }

    @Nullable
    @Override
    public String getCursorName()
        throws SQLException
    {
        if (original == null) {
            return null;
        }
        return original.getCursorName();
    }

    @Override
    public Object getObject(int columnIndex)
        throws SQLException
    {
        checkNotEmpty();
        return original.getObject(columnIndex);
    }

    @Override
    public Object getObject(String columnLabel)
        throws SQLException
    {
        checkNotEmpty();
        return original.getObject(columnLabel);
    }

    @Override
    public int findColumn(String columnLabel)
        throws SQLException
    {
        return original.findColumn(columnLabel);
    }

    @Override
    public Reader getCharacterStream(int columnIndex)
        throws SQLException
    {
        return original.getCharacterStream(columnIndex);
    }

    @Override
    public Reader getCharacterStream(String columnLabel)
        throws SQLException
    {
        return original.getCharacterStream(columnLabel);
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex)
        throws SQLException
    {
        return original.getBigDecimal(columnIndex);
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel)
        throws SQLException
    {
        return original.getBigDecimal(columnLabel);
    }

    @Override
    public boolean isBeforeFirst()
        throws SQLException
    {
        return original.isBeforeFirst();
    }

    @Override
    public boolean isAfterLast()
        throws SQLException
    {
        return original.isAfterLast();
    }

    @Override
    public boolean isFirst()
        throws SQLException
    {
        return original.isFirst();
    }

    @Override
    public boolean isLast()
        throws SQLException
    {
        return original.isLast();
    }

    @Override
    public void beforeFirst()
        throws SQLException
    {
        original.beforeFirst();
    }

    @Override
    public void afterLast()
        throws SQLException
    {
        original.afterLast();
    }

    @Override
    public boolean first()
        throws SQLException
    {
        return original.first();
    }

    @Override
    public boolean last()
        throws SQLException
    {
        return original.last();
    }

    @Override
    public int getRow()
        throws SQLException
    {
        return original.getRow();
    }

    @Override
    public boolean absolute(int row)
        throws SQLException
    {
        return original.absolute(row);
    }

    @Override
    public boolean relative(int rows)
        throws SQLException
    {
        return original.relative(rows);
    }

    @Override
    public boolean previous()
        throws SQLException
    {
        return original.previous();
    }

    @Override
    public void setFetchDirection(int direction)
        throws SQLException
    {
        original.setFetchDirection(direction);
    }

    @Override
    public int getFetchDirection()
        throws SQLException
    {
        return original.getFetchDirection();
    }

    @Override
    public void setFetchSize(int rows)
        throws SQLException
    {
        original.setFetchSize(rows);
    }

    @Override
    public int getFetchSize()
        throws SQLException
    {
        return original.getFetchSize();
    }

    @Override
    public int getType()
        throws SQLException
    {
        return original.getType();
    }

    @Override
    public int getConcurrency()
        throws SQLException
    {
        return original.getConcurrency();
    }

    @Override
    public boolean rowUpdated()
        throws SQLException
    {
        return original.rowUpdated();
    }

    @Override
    public boolean rowInserted()
        throws SQLException
    {
        return original.rowInserted();
    }

    @Override
    public boolean rowDeleted()
        throws SQLException
    {
        return original.rowDeleted();
    }

    @Override
    public void updateNull(int columnIndex)
        throws SQLException
    {
        original.updateNull(columnIndex);
    }

    @Override
    public void updateBoolean(int columnIndex, boolean x)
        throws SQLException
    {
        original.updateBoolean(columnIndex, x);
    }

    @Override
    public void updateByte(int columnIndex, byte x)
        throws SQLException
    {
        original.updateByte(columnIndex, x);
    }

    @Override
    public void updateShort(int columnIndex, short x)
        throws SQLException
    {
        original.updateShort(columnIndex, x);
    }

    @Override
    public void updateInt(int columnIndex, int x)
        throws SQLException
    {
        original.updateInt(columnIndex, x);
    }

    @Override
    public void updateLong(int columnIndex, long x)
        throws SQLException
    {
        original.updateLong(columnIndex, x);
    }

    @Override
    public void updateFloat(int columnIndex, float x)
        throws SQLException
    {
        original.updateFloat(columnIndex, x);
    }

    @Override
    public void updateDouble(int columnIndex, double x)
        throws SQLException
    {
        original.updateDouble(columnIndex, x);
    }

    @Override
    public void updateBigDecimal(int columnIndex, BigDecimal x)
        throws SQLException
    {
        original.updateBigDecimal(columnIndex, x);
    }

    @Override
    public void updateString(int columnIndex, String x)
        throws SQLException
    {
        original.updateString(columnIndex, x);
    }

    @Override
    public void updateBytes(int columnIndex, byte[] x)
        throws SQLException
    {
        original.updateBytes(columnIndex, x);
    }

    @Override
    public void updateDate(int columnIndex, Date x)
        throws SQLException
    {
        original.updateDate(columnIndex, x);
    }

    @Override
    public void updateTime(int columnIndex, Time x)
        throws SQLException
    {
        original.updateTime(columnIndex, x);
    }

    @Override
    public void updateTimestamp(int columnIndex, Timestamp x)
        throws SQLException
    {
        original.updateTimestamp(columnIndex, x);
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, int length)
        throws SQLException
    {
        original.updateAsciiStream(columnIndex, x, length);
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, int length)
        throws SQLException
    {
        original.updateBinaryStream(columnIndex, x, length);
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, int length)
        throws SQLException
    {
        original.updateCharacterStream(columnIndex, x, length);
    }

    @Override
    public void updateObject(int columnIndex, Object x, int scaleOrLength)
        throws SQLException
    {
        original.updateObject(columnIndex, x, scaleOrLength);
    }

    @Override
    public void updateObject(int columnIndex, Object x)
        throws SQLException
    {
        original.updateObject(columnIndex, x);
    }

    @Override
    public void updateNull(String columnLabel)
        throws SQLException
    {
        original.updateNull(columnLabel);
    }

    @Override
    public void updateBoolean(String columnLabel, boolean x)
        throws SQLException
    {
        original.updateBoolean(columnLabel, x);
    }

    @Override
    public void updateByte(String columnLabel, byte x)
        throws SQLException
    {
        original.updateByte(columnLabel, x);
    }

    @Override
    public void updateShort(String columnLabel, short x)
        throws SQLException
    {
        original.updateShort(columnLabel, x);
    }

    @Override
    public void updateInt(String columnLabel, int x)
        throws SQLException
    {
        original.updateInt(columnLabel, x);
    }

    @Override
    public void updateLong(String columnLabel, long x)
        throws SQLException
    {
        original.updateLong(columnLabel, x);
    }

    @Override
    public void updateFloat(String columnLabel, float x)
        throws SQLException
    {
        original.updateFloat(columnLabel, x);
    }

    @Override
    public void updateDouble(String columnLabel, double x)
        throws SQLException
    {
        original.updateDouble(columnLabel, x);
    }

    @Override
    public void updateBigDecimal(String columnLabel, BigDecimal x)
        throws SQLException
    {
        original.updateBigDecimal(columnLabel, x);
    }

    @Override
    public void updateString(String columnLabel, String x)
        throws SQLException
    {
        original.updateString(columnLabel, x);
    }

    @Override
    public void updateBytes(String columnLabel, byte[] x)
        throws SQLException
    {
        original.updateBytes(columnLabel, x);
    }

    @Override
    public void updateDate(String columnLabel, Date x)
        throws SQLException
    {
        original.updateDate(columnLabel, x);
    }

    @Override
    public void updateTime(String columnLabel, Time x)
        throws SQLException
    {
        original.updateTime(columnLabel, x);
    }

    @Override
    public void updateTimestamp(String columnLabel, Timestamp x)
        throws SQLException
    {
        original.updateTimestamp(columnLabel, x);
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, int length)
        throws SQLException
    {
        original.updateAsciiStream(columnLabel, x, length);
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, int length)
        throws SQLException
    {
        original.updateBinaryStream(columnLabel, x, length);
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, int length)
        throws SQLException
    {
        original.updateCharacterStream(columnLabel, reader, length);
    }

    @Override
    public void updateObject(String columnLabel, Object x, int scaleOrLength)
        throws SQLException
    {
        original.updateObject(columnLabel, x, scaleOrLength);
    }

    @Override
    public void updateObject(String columnLabel, Object x)
        throws SQLException
    {
        original.updateObject(columnLabel, x);
    }

    @Override
    public void insertRow()
        throws SQLException
    {
        original.insertRow();
    }

    @Override
    public void updateRow()
        throws SQLException
    {
        original.updateRow();
    }

    @Override
    public void deleteRow()
        throws SQLException
    {
        original.deleteRow();
    }

    @Override
    public void refreshRow()
        throws SQLException
    {
        original.refreshRow();
    }

    @Override
    public void cancelRowUpdates()
        throws SQLException
    {
        original.cancelRowUpdates();
    }

    @Override
    public void moveToInsertRow()
        throws SQLException
    {
        original.moveToInsertRow();
    }

    @Override
    public void moveToCurrentRow()
        throws SQLException
    {
        original.moveToCurrentRow();
    }

    @Override
    public Object getObject(int columnIndex, Map<String, Class<?>> map)
        throws SQLException
    {
        return original.getObject(columnIndex, map);
    }

    @Override
    public Ref getRef(int columnIndex)
        throws SQLException
    {
        return original.getRef(columnIndex);
    }

    @Override
    public Blob getBlob(int columnIndex)
        throws SQLException
    {
        return original.getBlob(columnIndex);
    }

    @Override
    public Clob getClob(int columnIndex)
        throws SQLException
    {
        return original.getClob(columnIndex);
    }

    @Override
    public Array getArray(int columnIndex)
        throws SQLException
    {
        return original.getArray(columnIndex);
    }

    @Override
    public Object getObject(String columnLabel, Map<String, Class<?>> map)
        throws SQLException
    {
        return original.getObject(columnLabel, map);
    }

    @Override
    public Ref getRef(String columnLabel)
        throws SQLException
    {
        return original.getRef(columnLabel);
    }

    @Override
    public Blob getBlob(String columnLabel)
        throws SQLException
    {
        return original.getBlob(columnLabel);
    }

    @Override
    public Clob getClob(String columnLabel)
        throws SQLException
    {
        return original.getClob(columnLabel);
    }

    @Override
    public Array getArray(String columnLabel)
        throws SQLException
    {
        return original.getArray(columnLabel);
    }

    @Override
    public Date getDate(int columnIndex, Calendar cal)
        throws SQLException
    {
        return original.getDate(columnIndex, cal);
    }

    @Override
    public Date getDate(String columnLabel, Calendar cal)
        throws SQLException
    {
        return original.getDate(columnLabel, cal);
    }

    @Override
    public Time getTime(int columnIndex, Calendar cal)
        throws SQLException
    {
        return original.getTime(columnIndex, cal);
    }

    @Override
    public Time getTime(String columnLabel, Calendar cal)
        throws SQLException
    {
        return original.getTime(columnLabel, cal);
    }

    @Override
    public Timestamp getTimestamp(int columnIndex, Calendar cal)
        throws SQLException
    {
        return original.getTimestamp(columnIndex, cal);
    }

    @Override
    public Timestamp getTimestamp(String columnLabel, Calendar cal)
        throws SQLException
    {
        return original.getTimestamp(columnLabel, cal);
    }

    @Override
    public URL getURL(int columnIndex)
        throws SQLException
    {
        return original.getURL(columnIndex);
    }

    @Override
    public URL getURL(String columnLabel)
        throws SQLException
    {
        return original.getURL(columnLabel);
    }

    @Override
    public void updateRef(int columnIndex, Ref x)
        throws SQLException
    {
        original.updateRef(columnIndex, x);
    }

    @Override
    public void updateRef(String columnLabel, Ref x)
        throws SQLException
    {
        original.updateRef(columnLabel, x);
    }

    @Override
    public void updateBlob(int columnIndex, Blob x)
        throws SQLException
    {
        original.updateBlob(columnIndex, x);
    }

    @Override
    public void updateBlob(String columnLabel, Blob x)
        throws SQLException
    {
        original.updateBlob(columnLabel, x);
    }

    @Override
    public void updateClob(int columnIndex, Clob x)
        throws SQLException
    {
        original.updateClob(columnIndex, x);
    }

    @Override
    public void updateClob(String columnLabel, Clob x)
        throws SQLException
    {
        original.updateClob(columnLabel, x);
    }

    @Override
    public void updateArray(int columnIndex, Array x)
        throws SQLException
    {
        original.updateArray(columnIndex, x);
    }

    @Override
    public void updateArray(String columnLabel, Array x)
        throws SQLException
    {
        original.updateArray(columnLabel, x);
    }

    @Override
    public RowId getRowId(int columnIndex)
        throws SQLException
    {
        return original.getRowId(columnIndex);
    }

    @Override
    public RowId getRowId(String columnLabel)
        throws SQLException
    {
        return original.getRowId(columnLabel);
    }

    @Override
    public void updateRowId(int columnIndex, RowId x)
        throws SQLException
    {
        original.updateRowId(columnIndex, x);
    }

    @Override
    public void updateRowId(String columnLabel, RowId x)
        throws SQLException
    {
        original.updateRowId(columnLabel, x);
    }

    @Override
    public int getHoldability()
        throws SQLException
    {
        return original.getHoldability();
    }

    @Override
    public boolean isClosed()
        throws SQLException
    {
        return original.isClosed();
    }

    @Override
    public void updateNString(int columnIndex, String nString)
        throws SQLException
    {
        original.updateNString(columnIndex, nString);
    }

    @Override
    public void updateNString(String columnLabel, String nString)
        throws SQLException
    {
        original.updateNString(columnLabel, nString);
    }

    @Override
    public void updateNClob(int columnIndex, NClob nClob)
        throws SQLException
    {
        original.updateNClob(columnIndex, nClob);
    }

    @Override
    public void updateNClob(String columnLabel, NClob nClob)
        throws SQLException
    {
        original.updateNClob(columnLabel, nClob);
    }

    @Override
    public NClob getNClob(int columnIndex)
        throws SQLException
    {
        return original.getNClob(columnIndex);
    }

    @Override
    public NClob getNClob(String columnLabel)
        throws SQLException
    {
        return original.getNClob(columnLabel);
    }

    @Override
    public SQLXML getSQLXML(int columnIndex)
        throws SQLException
    {
        return original.getSQLXML(columnIndex);
    }

    @Override
    public SQLXML getSQLXML(String columnLabel)
        throws SQLException
    {
        return original.getSQLXML(columnLabel);
    }

    @Override
    public void updateSQLXML(int columnIndex, SQLXML xmlObject)
        throws SQLException
    {
        original.updateSQLXML(columnIndex, xmlObject);
    }

    @Override
    public void updateSQLXML(String columnLabel, SQLXML xmlObject)
        throws SQLException
    {
        original.updateSQLXML(columnLabel, xmlObject);
    }

    @Override
    public String getNString(int columnIndex)
        throws SQLException
    {
        return original.getNString(columnIndex);
    }

    @Override
    public String getNString(String columnLabel)
        throws SQLException
    {
        return original.getNString(columnLabel);
    }

    @Override
    public Reader getNCharacterStream(int columnIndex)
        throws SQLException
    {
        return original.getNCharacterStream(columnIndex);
    }

    @Override
    public Reader getNCharacterStream(String columnLabel)
        throws SQLException
    {
        return original.getNCharacterStream(columnLabel);
    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x, long length)
        throws SQLException
    {
        original.updateNCharacterStream(columnIndex, x, length);
    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader, long length)
        throws SQLException
    {
        original.updateNCharacterStream(columnLabel, reader, length);
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, long length)
        throws SQLException
    {
        original.updateAsciiStream(columnIndex, x, length);
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, long length)
        throws SQLException
    {
        original.updateBinaryStream(columnIndex, x, length);
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, long length)
        throws SQLException
    {
        original.updateCharacterStream(columnIndex, x, length);
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, long length)
        throws SQLException
    {
        original.updateAsciiStream(columnLabel, x, length);
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, long length)
        throws SQLException
    {
        original.updateBinaryStream(columnLabel, x, length);
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, long length)
        throws SQLException
    {
        original.updateCharacterStream(columnLabel, reader, length);
    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream, long length)
        throws SQLException
    {
        original.updateBlob(columnIndex, inputStream, length);
    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream, long length)
        throws SQLException
    {
        original.updateBlob(columnLabel, inputStream, length);
    }

    @Override
    public void updateClob(int columnIndex, Reader reader, long length)
        throws SQLException
    {
        original.updateClob(columnIndex, reader, length);
    }

    @Override
    public void updateClob(String columnLabel, Reader reader, long length)
        throws SQLException
    {
        original.updateClob(columnLabel, reader, length);
    }

    @Override
    public void updateNClob(int columnIndex, Reader reader, long length)
        throws SQLException
    {
        original.updateNClob(columnIndex, reader, length);
    }

    @Override
    public void updateNClob(String columnLabel, Reader reader, long length)
        throws SQLException
    {
        original.updateNClob(columnLabel, reader, length);
    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x)
        throws SQLException
    {
        original.updateNCharacterStream(columnIndex, x);
    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader)
        throws SQLException
    {
        original.updateNCharacterStream(columnLabel, reader);
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x)
        throws SQLException
    {
        original.updateAsciiStream(columnIndex, x);
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x)
        throws SQLException
    {
        original.updateBinaryStream(columnIndex, x);
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x)
        throws SQLException
    {
        original.updateCharacterStream(columnIndex, x);
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x)
        throws SQLException
    {
        original.updateAsciiStream(columnLabel, x);
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x)
        throws SQLException
    {
        original.updateBinaryStream(columnLabel, x);
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader)
        throws SQLException
    {
        original.updateCharacterStream(columnLabel, reader);
    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream)
        throws SQLException
    {
        original.updateBlob(columnIndex, inputStream);
    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream)
        throws SQLException
    {
        original.updateBlob(columnLabel, inputStream);
    }

    @Override
    public void updateClob(int columnIndex, Reader reader)
        throws SQLException
    {
        original.updateClob(columnIndex, reader);
    }

    @Override
    public void updateClob(String columnLabel, Reader reader)
        throws SQLException
    {
        original.updateClob(columnLabel, reader);
    }

    @Override
    public void updateNClob(int columnIndex, Reader reader)
        throws SQLException
    {
        original.updateNClob(columnIndex, reader);
    }

    @Override
    public void updateNClob(String columnLabel, Reader reader)
        throws SQLException
    {
        original.updateNClob(columnLabel, reader);
    }

    @Nullable
    @Override
    public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
        return original.getObject(columnIndex, type);
    }

    @Nullable
    @Override
    public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
        return original.getObject(columnLabel, type);
    }

    @Override
    public <T> T unwrap(Class<T> iface)
        throws SQLException
    {
        return original.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface)
        throws SQLException
    {
        return original.isWrapperFor(iface);
    }

    protected JDBCResultSetMetaData createMetaDataImpl() throws SQLException
    {
        return session.getDataSource().getJdbcFactory().createResultSetMetaData(this);
    }

}

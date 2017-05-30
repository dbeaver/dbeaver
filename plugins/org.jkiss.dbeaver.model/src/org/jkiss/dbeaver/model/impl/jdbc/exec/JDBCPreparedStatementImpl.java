/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.sql.SQLUtils;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manageable prepared statement.
 * Stores information about execution in query manager and operated progress monitor.
 */
public class JDBCPreparedStatementImpl extends JDBCStatementImpl<PreparedStatement> implements JDBCPreparedStatement {

    private static final Log log = Log.getLog(JDBCPreparedStatementImpl.class);

    private static final Object NULL_VALUE = new Object();

    private Map<Object, Object> paramMap;

    protected static class ContentParameter {
        String displayString;
        ContentParameter(JDBCSession session, Object value) {
            if (value instanceof RowId) {
                displayString = SQLUtils.quoteString(session.getDataSource(), new String(((RowId) value).getBytes()));
            } else if (value instanceof byte[]) {
                byte[] bytes = (byte[])value;
                displayString = DBValueFormatting.formatBinaryString(session.getDataSource(), bytes, DBDDisplayFormat.NATIVE, true);
            } else {
                displayString = "DATA(" + (value == null ? DBConstants.NULL_VALUE_LABEL : value.getClass().getSimpleName()) + ")";
            }
        }

        @Override
        public String toString() {
            return displayString;
        }
    }

    public JDBCPreparedStatementImpl(
        @NotNull JDBCSession connection,
        @NotNull PreparedStatement original,
        String query,
        boolean disableLogging)
    {
        super(connection, original, disableLogging);
        setQueryString(query);
    }

    @Override
    public PreparedStatement getOriginal()
    {
        return original;
    }

    @Override
    public void close() {
        if (paramMap != null) {
            paramMap.clear();
            paramMap = null;
        }
        super.close();
    }

    public String getFormattedQuery() {
        if (paramMap == null) {
            return getQueryString();
        } else {
            String q = getQueryString();
            if (q == null) {
                return "";
            }
            int length = q.length();
            StringBuilder formatted = new StringBuilder(length * 2);
            int paramIndex = 0;
            for (int i = 0; i < length; i++) {
                char c = q.charAt(i);
                switch (c) {
                    case '?': {
                        paramIndex++;
                        Object paramValue = paramMap.get(paramIndex);
                        if (paramValue != null) {
                            formatted.append(formatParameterValue(paramValue));
                            continue;
                        }
                        break;
                    }
                    case ':': {
                        // FIXME: process named parameters
                        break;
                    }
                    case '\'':
                    case '"': {
                        formatted.append(c);
                        for (int k = i + 1; k < length; k++) {
                            char c2 = q.charAt(k);
                            if (c2 == c && q.charAt(k - 1) != '\\') {
                                i = k;
                                c = c2;
                                break;
                            } else {
                                formatted.append(c2);
                            }
                        }
                        break;
                    }
                }
                formatted.append(c);
            }

            return formatted.toString();
        }
    }

    @NotNull
    private String formatParameterValue(Object value) {
        if (value instanceof CharSequence) {
            return SQLUtils.quoteString(connection.getDataSource(), value.toString());
        } else if (value instanceof Number) {
            return DBValueFormatting.convertNumberToNativeString((Number) value);
        } else if (value instanceof java.util.Date) {
            try {
                DBDDataFormatterProfile formatterProfile = getSession().getDataSource().getDataFormatterProfile();
                if (value instanceof Date) {
                    return SQLUtils.quoteString(connection.getDataSource(), formatterProfile.createFormatter(DBDDataFormatter.TYPE_NAME_DATE).formatValue(value));
                } else if (value instanceof Time) {
                    return SQLUtils.quoteString(connection.getDataSource(), formatterProfile.createFormatter(DBDDataFormatter.TYPE_NAME_TIME).formatValue(value));
                } else {
                    return SQLUtils.quoteString(connection.getDataSource(), formatterProfile.createFormatter(DBDDataFormatter.TYPE_NAME_TIMESTAMP).formatValue(value));
                }
            } catch (Exception e) {
                log.debug("Error formatting date [" + value + "]", e);
            }
        } else if (value == NULL_VALUE) {
            return "NULL";
        }
        return value.toString();
    }

    protected void handleStatementBind(Object parameter, @Nullable Object o)
    {
        if (isQMLoggingEnabled()) {
            // Save parameters
            if (o == null) {
                o = NULL_VALUE;
            } else if (!DBUtils.isAtomicParameter(o)) {
                // Wrap complex things
                o = new ContentParameter(connection, o);
            }
            if (paramMap == null) {
                paramMap = new LinkedHashMap<>();
            }
            paramMap.put(parameter, o);
        }
        QMUtils.getDefaultHandler().handleStatementBind(this, parameter, o);
    }

    ////////////////////////////////////////////////////////////////////
    // DBC Statement overrides
    ////////////////////////////////////////////////////////////////////

    @Override
    public boolean executeStatement()
        throws DBCException
    {
        try {
            return execute();
        }
        catch (SQLException e) {
            throw new DBCException(e, connection.getDataSource());
        }
    }

    @Override
    public void addToBatch() throws DBCException
    {
        try {
            addBatch();
        }
        catch (SQLException e) {
            throw new DBCException(e, connection.getDataSource());
        }
    }

    ////////////////////////////////////////////////////////////////////
    // Statement overrides
    ////////////////////////////////////////////////////////////////////

    @Override
    public JDBCResultSet executeQuery()
        throws SQLException
    {
        this.beforeExecute();
        try {
            return createResultSetImpl(getOriginal().executeQuery());
        } catch (Throwable e) {
            throw super.handleExecuteError(e);
        } finally {
            super.afterExecute();
        }
    }

    @Override
    public int executeUpdate()
        throws SQLException
    {
        this.beforeExecute();
        try {
            return getOriginal().executeUpdate();
        } catch (Throwable e) {
            throw super.handleExecuteError(e);
        } finally {
            super.afterExecute();
        }
    }

    @Override
    public boolean execute()
        throws SQLException
    {
        this.beforeExecute();
        try {
            return getOriginal().execute();
        } catch (Throwable e) {
            throw super.handleExecuteError(e);
        } finally {
            super.afterExecute();
        }
    }

    @Override
    public void setNull(int parameterIndex, int sqlType)
        throws SQLException
    {
        getOriginal().setNull(parameterIndex, sqlType);

        handleStatementBind(parameterIndex, null);
    }

    @Override
    public void setBoolean(int parameterIndex, boolean x)
        throws SQLException
    {
        getOriginal().setBoolean(parameterIndex, x);

        handleStatementBind(parameterIndex, x);
    }

    @Override
    public void setByte(int parameterIndex, byte x)
        throws SQLException
    {
        getOriginal().setByte(parameterIndex, x);

        handleStatementBind(parameterIndex, x);
    }

    @Override
    public void setShort(int parameterIndex, short x)
        throws SQLException
    {
        getOriginal().setShort(parameterIndex, x);

        handleStatementBind(parameterIndex, x);
    }

    @Override
    public void setInt(int parameterIndex, int x)
        throws SQLException
    {
        getOriginal().setInt(parameterIndex, x);

        handleStatementBind(parameterIndex, x);
    }

    @Override
    public void setLong(int parameterIndex, long x)
        throws SQLException
    {
        getOriginal().setLong(parameterIndex, x);

        handleStatementBind(parameterIndex, x);
    }

    @Override
    public void setFloat(int parameterIndex, float x)
        throws SQLException
    {
        getOriginal().setFloat(parameterIndex, x);

        handleStatementBind(parameterIndex, x);
    }

    @Override
    public void setDouble(int parameterIndex, double x)
        throws SQLException
    {
        getOriginal().setDouble(parameterIndex, x);

        handleStatementBind(parameterIndex, x);
    }

    @Override
    public void setBigDecimal(int parameterIndex, BigDecimal x)
        throws SQLException
    {
        getOriginal().setBigDecimal(parameterIndex, x);

        handleStatementBind(parameterIndex, x);
    }

    @Override
    public void setString(int parameterIndex, String x)
        throws SQLException
    {
        getOriginal().setString(parameterIndex, x);

        handleStatementBind(parameterIndex, x);
    }

    @Override
    public void setBytes(int parameterIndex, byte[] x)
        throws SQLException
    {
        getOriginal().setBytes(parameterIndex, x);

        handleStatementBind(parameterIndex, x);
    }

    @Override
    public void setDate(int parameterIndex, Date x)
        throws SQLException
    {
        getOriginal().setDate(parameterIndex, x);
        
        handleStatementBind(parameterIndex, x);
    }

    @Override
    public void setTime(int parameterIndex, Time x)
        throws SQLException
    {
        getOriginal().setTime(parameterIndex, x);

        handleStatementBind(parameterIndex, x);
    }

    @Override
    public void setTimestamp(
        int parameterIndex, Timestamp x)
        throws SQLException
    {
        getOriginal().setTimestamp(parameterIndex, x);

        handleStatementBind(parameterIndex, x);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, int length)
        throws SQLException
    {
        getOriginal().setAsciiStream(parameterIndex, x, length);

        handleStatementBind(parameterIndex, x);
    }

    @Override
    @Deprecated
    public void setUnicodeStream(int parameterIndex, InputStream x, int length)
        throws SQLException
    {
        getOriginal().setUnicodeStream(parameterIndex, x, length);

        handleStatementBind(parameterIndex, x);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, int length)
        throws SQLException
    {
        getOriginal().setBinaryStream(parameterIndex, x, length);

        handleStatementBind(parameterIndex, x);
    }

    @Override
    public void clearParameters()
        throws SQLException
    {
        getOriginal().clearParameters();
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType)
        throws SQLException
    {
        getOriginal().setObject(parameterIndex, x, targetSqlType);

        handleStatementBind(parameterIndex, x);
    }

    @Override
    public void setObject(int parameterIndex, Object x)
        throws SQLException
    {
        getOriginal().setObject(parameterIndex, x);

        handleStatementBind(parameterIndex, x);
    }

    @Override
    public void addBatch()
        throws SQLException
    {
        getOriginal().addBatch();
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader reader, int length)
        throws SQLException
    {
        getOriginal().setCharacterStream(parameterIndex, reader, length);

        handleStatementBind(parameterIndex, reader);
    }

    @Override
    public void setRef(int parameterIndex, Ref x)
        throws SQLException
    {
        getOriginal().setRef(parameterIndex, x);

        handleStatementBind(parameterIndex, x);
    }

    @Override
    public void setBlob(int parameterIndex, Blob x)
        throws SQLException
    {
        getOriginal().setBlob(parameterIndex, x);

        handleStatementBind(parameterIndex, x);
    }

    @Override
    public void setClob(int parameterIndex, Clob x)
        throws SQLException
    {
        getOriginal().setClob(parameterIndex, x);

        handleStatementBind(parameterIndex, x);
    }

    @Override
    public void setArray(int parameterIndex, Array x)
        throws SQLException
    {
        getOriginal().setArray(parameterIndex, x);

        handleStatementBind(parameterIndex, x);
    }

    @Override
    public ResultSetMetaData getMetaData()
        throws SQLException
    {
        return getOriginal().getMetaData();
    }

    @Override
    public void setDate(int parameterIndex, Date x, Calendar cal)
        throws SQLException
    {
        getOriginal().setDate(parameterIndex, x, cal);

        handleStatementBind(parameterIndex, x);
    }

    @Override
    public void setTime(int parameterIndex, Time x, Calendar cal)
        throws SQLException
    {
        getOriginal().setTime(parameterIndex, x, cal);

        handleStatementBind(parameterIndex, x);
    }

    @Override
    public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal)
        throws SQLException
    {
        getOriginal().setTimestamp(parameterIndex, x, cal);

        handleStatementBind(parameterIndex, x);
    }

    @Override
    public void setNull(int parameterIndex, int sqlType, String typeName)
        throws SQLException
    {
        getOriginal().setNull(parameterIndex, sqlType, typeName);

        handleStatementBind(parameterIndex, null);
    }

    @Override
    public void setURL(int parameterIndex, URL x)
        throws SQLException
    {
        getOriginal().setURL(parameterIndex, x);

        handleStatementBind(parameterIndex, x);
    }

    @Override
    public ParameterMetaData getParameterMetaData()
        throws SQLException
    {
        return getOriginal().getParameterMetaData();
    }

    @Override
    public void setRowId(int parameterIndex, RowId x)
        throws SQLException
    {
        getOriginal().setRowId(parameterIndex, x);

        handleStatementBind(parameterIndex, x);
    }

    @Override
    public void setNString(int parameterIndex, String x)
        throws SQLException
    {
        getOriginal().setNString(parameterIndex, x);

        handleStatementBind(parameterIndex, x);
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader x, long length)
        throws SQLException
    {
        getOriginal().setNCharacterStream(parameterIndex, x, length);

        handleStatementBind(parameterIndex, x);
    }

    @Override
    public void setNClob(int parameterIndex, NClob x)
        throws SQLException
    {
        getOriginal().setNClob(parameterIndex, x);

        handleStatementBind(parameterIndex, x);
    }

    @Override
    public void setClob(int parameterIndex, Reader x, long length)
        throws SQLException
    {
        getOriginal().setClob(parameterIndex, x, length);

        handleStatementBind(parameterIndex, x);
    }

    @Override
    public void setBlob(int parameterIndex, InputStream x, long length)
        throws SQLException
    {
        getOriginal().setBlob(parameterIndex, x, length);

        handleStatementBind(parameterIndex, x);
    }

    @Override
    public void setNClob(int parameterIndex, Reader x, long length)
        throws SQLException
    {
        getOriginal().setNClob(parameterIndex, x, length);

        handleStatementBind(parameterIndex, x);
    }

    @Override
    public void setSQLXML(int parameterIndex, SQLXML xmlObject)
        throws SQLException
    {
        getOriginal().setSQLXML(parameterIndex, xmlObject);

        handleStatementBind(parameterIndex, xmlObject);
    }

    @Override
    public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength)
        throws SQLException
    {
        getOriginal().setObject(parameterIndex, x, targetSqlType, scaleOrLength);

        handleStatementBind(parameterIndex, x);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x, long length)
        throws SQLException
    {
        getOriginal().setAsciiStream(parameterIndex, x, length);

        handleStatementBind(parameterIndex, x);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x, long length)
        throws SQLException
    {
        getOriginal().setBinaryStream(parameterIndex, x, length);

        handleStatementBind(parameterIndex, x);
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader x, long length)
        throws SQLException
    {
        getOriginal().setCharacterStream(parameterIndex, x, length);

        handleStatementBind(parameterIndex, x);
    }

    @Override
    public void setAsciiStream(int parameterIndex, InputStream x)
        throws SQLException
    {
        getOriginal().setAsciiStream(parameterIndex, x);

        handleStatementBind(parameterIndex, x);
    }

    @Override
    public void setBinaryStream(int parameterIndex, InputStream x)
        throws SQLException
    {
        getOriginal().setBinaryStream(parameterIndex, x);

        handleStatementBind(parameterIndex, x);
    }

    @Override
    public void setCharacterStream(int parameterIndex, Reader x)
        throws SQLException
    {
        getOriginal().setCharacterStream(parameterIndex, x);

        handleStatementBind(parameterIndex, x);
    }

    @Override
    public void setNCharacterStream(int parameterIndex, Reader x)
        throws SQLException
    {
        getOriginal().setNCharacterStream(parameterIndex, x);

        handleStatementBind(parameterIndex, x);
    }

    @Override
    public void setClob(int parameterIndex, Reader x)
        throws SQLException
    {
        getOriginal().setClob(parameterIndex, x);

        handleStatementBind(parameterIndex, x);
    }

    @Override
    public void setBlob(int parameterIndex, InputStream x)
        throws SQLException
    {
        getOriginal().setBlob(parameterIndex, x);

        handleStatementBind(parameterIndex, x);
    }

    @Override
    public void setNClob(int parameterIndex, Reader x)
        throws SQLException
    {
        getOriginal().setNClob(parameterIndex, x);

        handleStatementBind(parameterIndex, x);
    }

}

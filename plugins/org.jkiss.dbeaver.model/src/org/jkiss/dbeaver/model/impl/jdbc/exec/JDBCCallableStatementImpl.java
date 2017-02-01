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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCCallableStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.sql.SQLDataSource;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameter;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;
import org.jkiss.utils.CommonUtils;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Managable base statement.
 * Stores information about execution in query manager and operated progress monitor.
 */
public class JDBCCallableStatementImpl extends JDBCPreparedStatementImpl implements JDBCCallableStatement {

    private static final Log log = Log.getLog(JDBCCallableStatementImpl.class);
    private static final Pattern EXEC_PATTERN = Pattern.compile("[a-z]+\\s+([^(]+)\\s*\\(");

    private DBSProcedure procedure;

    public JDBCCallableStatementImpl(
        @NotNull JDBCSession connection,
        @NotNull CallableStatement original,
        @Nullable String query,
        boolean disableLogging)
    {
        super(connection, original, query, disableLogging);

        // Find procedure definition
        try {
            procedure = findProcedure(connection, query);
        } catch (Throwable e) {
            log.debug(e);
        }

        // Bind procedure parameters
        try {
            ParameterMetaData paramsMeta = original.getParameterMetaData();
            if (paramsMeta != null) {
                bindProcedureFromJDBC(paramsMeta);
            }
        } catch (Throwable e) {
            log.debug(e.getMessage());
            // Try to bind parameters from procedure meta info
            try {
                if (procedure != null) {
                    bindProcedureFromMeta();
                }
            } catch (Throwable e1) {
                log.debug(e1);
            }
        }
    }

    private static DBSProcedure findProcedure(DBCSession session, String queryString) throws DBException {
        DBPDataSource dataSource = session.getDataSource();
        if (!CommonUtils.isEmpty(queryString)) {
            Matcher matcher = EXEC_PATTERN.matcher(queryString);
            if (matcher.find()) {
                String procName = matcher.group(1);
                char divChar = 0;
                if (dataSource instanceof SQLDataSource) {
                    divChar = ((SQLDataSource) dataSource).getSQLDialect().getStructSeparator();
                }
                if (procName.indexOf(divChar) != -1) {
                    return findProcedureByNames(session, procName.split("\\" + divChar));
                } else {
                    return findProcedureByNames(session, procName);
                }
            }
        }

        return null;
    }

    private static DBSProcedure findProcedureByNames(@NotNull DBCSession session, @NotNull String... names) throws DBException {
        if (!(session.getDataSource() instanceof DBSObjectContainer)) {
            return null;
        }
        DBSObjectContainer container = (DBSObjectContainer) session.getDataSource();
        for (int i = 0; i < names.length - 1; i++) {
            DBSObject child = container.getChild(session.getProgressMonitor(), DBObjectNameCaseTransformer.transformName(session.getDataSource(), names[i]));
            if (child instanceof DBSObjectContainer) {
                container = (DBSObjectContainer) child;
            } else {
                return null;
            }
        }
        if (container instanceof DBSProcedureContainer) {
            return ((DBSProcedureContainer) container).getProcedure(session.getProgressMonitor(), DBObjectNameCaseTransformer.transformName(session.getDataSource(), names[names.length - 1]));
        }
        return null;
    }

    @Override
    public CallableStatement getOriginal()
    {
        return (CallableStatement)original;
    }

    ////////////////////////////////////////////////////////////////////
    // Procedure bindings
    ////////////////////////////////////////////////////////////////////

    void bindProcedureFromJDBC(@NotNull ParameterMetaData paramsMeta) throws DBException {
        try {
            int parameterCount = paramsMeta.getParameterCount();
            if (parameterCount > 0) {
                for (int index = 0; index < parameterCount; index++) {
                    int parameterMode = paramsMeta.getParameterMode(index + 1);
                    if (parameterMode == ParameterMetaData.parameterModeOut || parameterMode == ParameterMetaData.parameterModeInOut) {
                        registerOutParameter(index + 1, paramsMeta.getParameterType(index + 1));
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBException("Error binding callable statement parameters from metadata: " + e.getMessage(), e);
        }
    }

    void bindProcedureFromMeta() throws DBException {
        if (procedure == null) {
            return;
        }
        try {
            Collection<? extends DBSProcedureParameter> params = procedure.getParameters(getConnection().getProgressMonitor());
            if (!CommonUtils.isEmpty(params)) {
                int index = 0;
                for (DBSProcedureParameter param : params) {
                    if (param.getParameterKind() == DBSProcedureParameterKind.OUT || param.getParameterKind() == DBSProcedureParameterKind.INOUT) {
                        index++;
                        registerOutParameter(index, param.getParameterType().getTypeID());
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBException("Error binding callable statement parameters", e);
        }
    }

    @Override
    public boolean executeStatement() throws DBCException {
        boolean hasResults = super.executeStatement();
        if (!hasResults && procedure != null) {
            return true;
        }
        return hasResults;
    }

    @Nullable
    @Override
    public JDBCResultSet getResultSet()
        throws SQLException
    {
        JDBCResultSet resultSet = makeResultSet(getOriginal().getResultSet());
        if (resultSet == null && procedure != null) {
            JDBCResultSetCallable procResults = new JDBCResultSetCallable(getConnection(), this);
            try {
                Collection<? extends DBSProcedureParameter> params = procedure.getParameters(getConnection().getProgressMonitor());
                if (!CommonUtils.isEmpty(params)) {
                    for (DBSProcedureParameter param : params) {
                        if (param.getParameterKind() == DBSProcedureParameterKind.OUT || param.getParameterKind() == DBSProcedureParameterKind.INOUT) {
                            procResults.addColumn(param.getName(), param.getParameterType());
                        }
                    }
                }
            } catch (DBException e) {
                log.warn("Error extracting callable results", e);
            }
            procResults.addRow();
            return procResults;
        }
        return resultSet;
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

    ////////////////////////////////////////////////////////
    // Output parameters mapping

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

    ////////////////////////////////////////////////////////
    // Output parameters mapping

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
        handleStatementBind(parameterName, val);
    }

    @Override
    public void setNull(String parameterName, int sqlType)
        throws SQLException
    {
        getOriginal().setNull(parameterName, sqlType);
        handleStatementBind(parameterName, null);
    }

    @Override
    public void setBoolean(String parameterName, boolean x)
        throws SQLException
    {
        getOriginal().setBoolean(parameterName, x);
        handleStatementBind(parameterName, x);
    }

    @Override
    public void setByte(String parameterName, byte x)
        throws SQLException
    {
        getOriginal().setByte(parameterName, x);
        handleStatementBind(parameterName, x);
    }

    @Override
    public void setShort(String parameterName, short x)
        throws SQLException
    {
        getOriginal().setShort(parameterName, x);
        handleStatementBind(parameterName, x);
    }

    @Override
    public void setInt(String parameterName, int x)
        throws SQLException
    {
        getOriginal().setInt(parameterName, x);
        handleStatementBind(parameterName, x);
    }

    @Override
    public void setLong(String parameterName, long x)
        throws SQLException
    {
        getOriginal().setLong(parameterName, x);
        handleStatementBind(parameterName, x);
    }

    @Override
    public void setFloat(String parameterName, float x)
        throws SQLException
    {
        getOriginal().setFloat(parameterName, x);
        handleStatementBind(parameterName, x);
    }

    @Override
    public void setDouble(String parameterName, double x)
        throws SQLException
    {
        getOriginal().setDouble(parameterName, x);
        handleStatementBind(parameterName, x);
    }

    @Override
    public void setBigDecimal(String parameterName, BigDecimal x)
        throws SQLException
    {
        getOriginal().setBigDecimal(parameterName, x);
        handleStatementBind(parameterName, x);
    }

    @Override
    public void setString(String parameterName, String x)
        throws SQLException
    {
        getOriginal().setString(parameterName, x);
        handleStatementBind(parameterName, x);
    }

    @Override
    public void setBytes(String parameterName, byte[] x)
        throws SQLException
    {
        getOriginal().setBytes(parameterName, x);
        handleStatementBind(parameterName, x);
    }

    @Override
    public void setDate(String parameterName, Date x)
        throws SQLException
    {
        getOriginal().setDate(parameterName, x);
        handleStatementBind(parameterName, x);
    }

    @Override
    public void setTime(String parameterName, Time x)
        throws SQLException
    {
        getOriginal().setTime(parameterName, x);
        handleStatementBind(parameterName, x);
    }

    @Override
    public void setTimestamp(String parameterName, Timestamp x)
        throws SQLException
    {
        getOriginal().setTimestamp(parameterName, x);
        handleStatementBind(parameterName, x);
    }

    @Override
    public void setAsciiStream(String parameterName, InputStream x, int length)
        throws SQLException
    {
        getOriginal().setAsciiStream(parameterName, x, length);
        handleStatementBind(parameterName, x);
    }

    @Override
    public void setBinaryStream(String parameterName, InputStream x, int length)
        throws SQLException
    {
        getOriginal().setBinaryStream(parameterName, x, length);
        handleStatementBind(parameterName, x);
    }

    @Override
    public void setObject(String parameterName, Object x, int targetSqlType, int scale)
        throws SQLException
    {
        getOriginal().setObject(parameterName, x, targetSqlType, scale);
        handleStatementBind(parameterName, x);
    }

    @Override
    public void setObject(String parameterName, Object x, int targetSqlType)
        throws SQLException
    {
        getOriginal().setObject(parameterName, x, targetSqlType);
        handleStatementBind(parameterName, x);
    }

    @Override
    public void setObject(String parameterName, Object x)
        throws SQLException
    {
        getOriginal().setObject(parameterName, x);
        handleStatementBind(parameterName, x);
    }

    @Override
    public void setCharacterStream(String parameterName, Reader reader, int length)
        throws SQLException
    {
        getOriginal().setCharacterStream(parameterName, reader, length);
        handleStatementBind(parameterName, reader);
    }

    @Override
    public void setDate(String parameterName, Date x, Calendar cal)
        throws SQLException
    {
        getOriginal().setDate(parameterName, x, cal);
        handleStatementBind(parameterName, x);
    }

    @Override
    public void setTime(String parameterName, Time x, Calendar cal)
        throws SQLException
    {
        getOriginal().setTime(parameterName, x, cal);
        handleStatementBind(parameterName, x);
    }

    @Override
    public void setTimestamp(String parameterName, Timestamp x, Calendar cal)
        throws SQLException
    {
        getOriginal().setTimestamp(parameterName, x, cal);
        handleStatementBind(parameterName, x);
    }

    @Override
    public void setNull(String parameterName, int sqlType, String typeName)
        throws SQLException
    {
        getOriginal().setNull(parameterName, sqlType, typeName);
        handleStatementBind(parameterName, null);
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
        handleStatementBind(parameterName, x);
    }

    @Override
    public void setNString(String parameterName, String value)
        throws SQLException
    {
        getOriginal().setNString(parameterName, value);
        handleStatementBind(parameterName, value);
    }

    @Override
    public void setNCharacterStream(String parameterName, Reader value, long length)
        throws SQLException
    {
        getOriginal().setNCharacterStream(parameterName, value, length);
        handleStatementBind(parameterName, value);
    }

    @Override
    public void setNClob(String parameterName, NClob value)
        throws SQLException
    {
        getOriginal().setNClob(parameterName, value);
        handleStatementBind(parameterName, value);
    }

    @Override
    public void setClob(String parameterName, Reader reader, long length)
        throws SQLException
    {
        getOriginal().setClob(parameterName, reader, length);
        handleStatementBind(parameterName, reader);
    }

    @Override
    public void setBlob(String parameterName, InputStream inputStream, long length)
        throws SQLException
    {
        getOriginal().setBlob(parameterName, inputStream, length);
        handleStatementBind(parameterName, inputStream);
    }

    @Override
    public void setNClob(String parameterName, Reader reader, long length)
        throws SQLException
    {
        getOriginal().setNClob(parameterName, reader, length);
        handleStatementBind(parameterName, reader);
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
        handleStatementBind(parameterName, xmlObject);
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
        handleStatementBind(parameterName, x);
    }

    @Override
    public void setClob(String parameterName, Clob x)
        throws SQLException
    {
        getOriginal().setClob(parameterName, x);
        handleStatementBind(parameterName, x);
    }

    @Override
    public void setAsciiStream(String parameterName, InputStream x, long length)
        throws SQLException
    {
        getOriginal().setAsciiStream(parameterName, x, length);
        handleStatementBind(parameterName, x);
    }

    @Override
    public void setBinaryStream(String parameterName, InputStream x, long length)
        throws SQLException
    {
        getOriginal().setBinaryStream(parameterName, x, length);
        handleStatementBind(parameterName, x);
    }

    @Override
    public void setCharacterStream(String parameterName, Reader reader, long length)
        throws SQLException
    {
        getOriginal().setCharacterStream(parameterName, reader, length);
        handleStatementBind(parameterName, reader);
    }

    @Override
    public void setAsciiStream(String parameterName, InputStream x)
        throws SQLException
    {
        getOriginal().setAsciiStream(parameterName, x);
        handleStatementBind(parameterName, x);
    }

    @Override
    public void setBinaryStream(String parameterName, InputStream x)
        throws SQLException
    {
        getOriginal().setBinaryStream(parameterName, x);
        handleStatementBind(parameterName, x);
    }

    @Override
    public void setCharacterStream(String parameterName, Reader reader)
        throws SQLException
    {
        getOriginal().setCharacterStream(parameterName, reader);
        handleStatementBind(parameterName, reader);
    }

    @Override
    public void setNCharacterStream(String parameterName, Reader value)
        throws SQLException
    {
        getOriginal().setNCharacterStream(parameterName, value);
        handleStatementBind(parameterName, value);
    }

    @Override
    public void setClob(String parameterName, Reader reader)
        throws SQLException
    {
        getOriginal().setClob(parameterName, reader);
        handleStatementBind(parameterName, reader);
    }

    @Override
    public void setBlob(String parameterName, InputStream inputStream)
        throws SQLException
    {
        getOriginal().setBlob(parameterName, inputStream);
        handleStatementBind(parameterName, inputStream);
    }

    @Override
    public void setNClob(String parameterName, Reader reader)
        throws SQLException
    {
        getOriginal().setNClob(parameterName, reader);
        handleStatementBind(parameterName, reader);
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
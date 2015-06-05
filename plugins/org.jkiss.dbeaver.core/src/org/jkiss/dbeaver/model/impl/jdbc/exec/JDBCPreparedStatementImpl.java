/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.model.impl.jdbc.exec;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.qm.QMUtils;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;

/**
 * Managable prepared statement.
 * Stores information about execution in query manager and operated progress monitor.
 */
public class JDBCPreparedStatementImpl extends JDBCStatementImpl<PreparedStatement> implements JDBCPreparedStatement {

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

    private void handleStatementBind(int parameterIndex, @Nullable Object o)
    {
        QMUtils.getDefaultHandler().handleStatementBind(this, parameterIndex, o);
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

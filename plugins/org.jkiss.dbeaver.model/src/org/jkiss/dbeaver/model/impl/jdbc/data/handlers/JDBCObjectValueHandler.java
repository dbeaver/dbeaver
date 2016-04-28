/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.impl.jdbc.data.handlers;

import org.jkiss.dbeaver.Log;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCCursor;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCRowId;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.ResultSet;
import java.sql.RowId;
import java.sql.SQLException;

/**
 * JDBC Object value handler.
 * Handle STRUCT types.
 *
 * @author Serge Rider
 */
public class JDBCObjectValueHandler extends JDBCAbstractValueHandler {

    private static final Log log = Log.getLog(JDBCObjectValueHandler.class);

    public static final JDBCObjectValueHandler INSTANCE = new JDBCObjectValueHandler();

    @Override
    protected Object fetchColumnValue(
        DBCSession session,
        JDBCResultSet resultSet,
        DBSTypedObject type,
        int index)
        throws DBCException, SQLException
    {
        Object value = resultSet.getObject(index);
        if (value instanceof ResultSet) {
            value = new JDBCCursor(
                (JDBCSession) session,
                (ResultSet) value,
                type.getTypeName());
        } else if (value instanceof RowId) {
            value = new JDBCRowId((RowId) value);
        }
        return value;
    }

    @Override
    protected void bindParameter(
        JDBCSession session,
        JDBCPreparedStatement statement,
        DBSTypedObject paramType,
        int paramIndex,
        Object value)
        throws DBCException, SQLException
    {
        if (value == null) {
            statement.setNull(paramIndex, paramType.getTypeID());
        } else if (value instanceof JDBCRowId) {
            statement.setRowId(paramIndex, ((JDBCRowId) value).getValue());
        } else {
            try {
                statement.setObject(paramIndex, value, paramType.getTypeID());
            } catch (SQLException e) {
                statement.setObject(paramIndex, value);
            }
        }
    }

    @NotNull
    @Override
    public Class<Object> getValueObjectType(@NotNull DBSTypedObject attribute)
    {
        return Object.class;
    }

    @Override
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy) throws DBCException
    {
        if (copy && object != null) {
            if (object instanceof DBDObject) {
                if (object instanceof DBDValueCloneable) {
                    return ((DBDValueCloneable) object).cloneValue(session.getProgressMonitor());
                }
                throw new DBCException("Can't copy object value " + object);
            }
        }
        return object;
    }

    @NotNull
    @Override
    public String getValueDisplayString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format)
    {
        if (value instanceof DBDValue) {
            return value.toString();
        }
        return DBUtils.getDefaultValueDisplayString(value, format);
    }

}
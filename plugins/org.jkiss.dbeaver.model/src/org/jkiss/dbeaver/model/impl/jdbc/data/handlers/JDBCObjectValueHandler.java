/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.impl.jdbc.data.handlers;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDObject;
import org.jkiss.dbeaver.model.data.DBDValue;
import org.jkiss.dbeaver.model.data.DBDValueCloneable;
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
    public Class<?> getValueObjectType(@NotNull DBSTypedObject attribute)
    {
        return Object.class;
    }

    @Override
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy, boolean validateValue) throws DBCException
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

        if (format == DBDDisplayFormat.NATIVE) {
            String typeName = column.getTypeName();
            if (value instanceof String && !((String) value).startsWith("'") && (typeName.equals(DBConstants.TYPE_NAME_UUID) || typeName.equals(DBConstants.TYPE_NAME_UUID2))) {
                return "'" + value + "'";
            }
        }

        return DBValueFormatting.getDefaultValueDisplayString(value, format);
    }

}
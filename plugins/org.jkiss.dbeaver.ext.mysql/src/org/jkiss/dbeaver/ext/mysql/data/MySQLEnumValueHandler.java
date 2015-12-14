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
package org.jkiss.dbeaver.ext.mysql.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableColumn;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCAbstractValueHandler;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableColumn;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;

/**
 * MySQL ENUM value handler
 */
public class MySQLEnumValueHandler extends JDBCAbstractValueHandler {

    public static final MySQLEnumValueHandler INSTANCE = new MySQLEnumValueHandler();

    @Override
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy) throws DBCException
    {
        if (object == null) {
            return new MySQLEnumValue((MySQLTableColumn) type, null);
        } else if (object instanceof MySQLEnumValue) {
            return copy ? new MySQLEnumValue((MySQLEnumValue) object) : object;
        } else if (object instanceof String && type instanceof MySQLTableColumn) {
            return new MySQLEnumValue((MySQLTableColumn) type, (String) object);
        } else {
            throw new DBCException("Unsupported ");
        }
    }

    @NotNull
    @Override
    public String getValueDisplayString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format)
    {
        if (!(value instanceof MySQLEnumValue)) {
            return super.getValueDisplayString(column, value, format);
        }
        String strValue = ((MySQLEnumValue) value).getValue();
        return DBUtils.getDefaultValueDisplayString(strValue, format);
    }

    @Override
    protected Object fetchColumnValue(
        DBCSession session,
        JDBCResultSet resultSet,
        DBSTypedObject type,
        int index)
        throws SQLException
    {
        DBSEntityAttribute attribute = null;
        if (type instanceof DBSTableColumn) {
            attribute = (DBSTableColumn) type;
        } else if (type instanceof DBDAttributeBinding) {
            attribute = ((DBDAttributeBinding) type).getEntityAttribute();
        }
        if (attribute == null) {
            throw new SQLException("Enum column wasn't resolved for '" + type + "'");
        }
        MySQLTableColumn enumColumn;
        if (attribute instanceof MySQLTableColumn) {
            enumColumn = (MySQLTableColumn) attribute;
        } else {
            throw new SQLException("Bad column type: " + attribute.getClass().getName());
        }
        return new MySQLEnumValue(enumColumn, resultSet.getString(index));
    }

    @Override
    public void bindParameter(JDBCSession session, JDBCPreparedStatement statement, DBSTypedObject paramType, int paramIndex, Object value)
        throws SQLException
    {
        // Sometimes we have String in value instead of MySQLTypeEnum
        // It happens when we edit result sets as MySQL reports RS column type as CHAR for enum/set types
        String strValue;
        if (value instanceof MySQLEnumValue) {
            strValue = ((MySQLEnumValue) value).getValue();
        } else {
            strValue = CommonUtils.toString(value);
        }
        if (strValue == null) {
            statement.setNull(paramIndex, paramType.getTypeID());
        } else {
            statement.setString(paramIndex, strValue);
        }
    }

    @NotNull
    @Override
    public Class<MySQLEnumValue> getValueObjectType(@NotNull DBSTypedObject attribute)
    {
        return MySQLEnumValue.class;
    }


}
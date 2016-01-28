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
package org.jkiss.dbeaver.ext.postgresql.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreAttribute;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataType;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCArray;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCArrayValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * PostgreArrayValueHandler
 */
public class PostgreArrayValueHandler extends JDBCArrayValueHandler {
    public static final PostgreArrayValueHandler INSTANCE = new PostgreArrayValueHandler();

/*
    protected Object fetchColumnValue(
        DBCSession session,
        JDBCResultSet resultSet,
        DBSTypedObject type,
        int index)
        throws DBCException, SQLException
    {
        try {
            Object value = resultSet.getArray(index);
            return getValueFromObject(session, type, value, false);
        } catch (Throwable e) {
            return super.fetchColumnValue(session, resultSet, type, index);
        }
    }
*/

    @Override
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy) throws DBCException
    {
        if (object != null && object.getClass().getName().equals(PostgreConstants.PG_OBJECT_CLASS)) {
            PostgreDataType itemType = null;
            if (type instanceof PostgreAttribute) {
                final PostgreDataType arrayType = ((PostgreAttribute) type).getTable().getDatabase().dataTypeCache.getCachedObject(type.getTypeName());
                if (arrayType != null) {
                    itemType = arrayType.getElementType();
                }
            }

            final Object value = PostgreUtils.extractValue(object);
            if (value == null) {
                return null;
            } else if (value instanceof String && itemType != null) {
                return convertStringToArray(session, itemType, (String)value);
            } else {
                // Can't parse
                return new JDBCArray(itemType, DBUtils.findValueHandler(session, itemType), new Object[] { value } );
            }
        }
        return super.getValueFromObject(session, type, object, copy);
    }

    private JDBCArray convertStringToArray(@NotNull DBCSession session, @NotNull PostgreDataType itemType, @NotNull String value) {
        List<String> strings = new ArrayList<>(10);
        StringTokenizer st = new StringTokenizer(value, " ");
        while (st.hasMoreTokens()) {
            strings.add(st.nextToken());
        }
        Object[] contents = new Object[strings.size()];
        for (int i = 0; i < strings.size(); i++) {
            switch (itemType.getTypeID()) {
                case Types.BOOLEAN: contents[i] = Boolean.valueOf(strings.get(i)); break;
                case Types.TINYINT: contents[i] = Byte.parseByte(strings.get(i)); break;
                case Types.SMALLINT: contents[i] = Short.parseShort(strings.get(i)); break;
                case Types.INTEGER: contents[i] = Integer.parseInt(strings.get(i)); break;
                case Types.BIGINT: contents[i] = Long.parseLong(strings.get(i)); break;
                case Types.FLOAT: contents[i] = Float.parseFloat(strings.get(i)); break;
                case Types.REAL:
                case Types.DOUBLE: contents[i] = Double.parseDouble(strings.get(i)); break;
                default:
                    contents[i] = strings.get(i); break;
            }
        }
        return new JDBCArray(itemType, DBUtils.findValueHandler(session, itemType), contents);
    }

}

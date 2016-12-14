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
package org.jkiss.dbeaver.ext.postgresql.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataType;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDCollection;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCCollection;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCArrayValueHandler;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * PostgreArrayValueHandler
 */
public class PostgreArrayValueHandler extends JDBCArrayValueHandler {
    public static final PostgreArrayValueHandler INSTANCE = new PostgreArrayValueHandler();

    @Override
    public JDBCCollection getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy) throws DBCException
    {
        if (object != null) {
            String className = object.getClass().getName();
            if (object instanceof String || className.equals(PostgreConstants.PG_OBJECT_CLASS)) {
                PostgreDataType itemType = null;
                final PostgreDataType arrayType = PostgreUtils.findDataType((PostgreDataSource) session.getDataSource(), type);
                if (arrayType != null) {
                    itemType = arrayType.getElementType();
                }
                if (itemType != null) {
                    if (className.equals(PostgreConstants.PG_OBJECT_CLASS)) {
                        final Object value = PostgreUtils.extractPGObjectValue(object);
                        if (value == null) {
                            return null;
                        } else if (value instanceof String) {
                            return convertStringToArray(session, itemType, (String) value);
                        } else {
                            // Can't parse
                            return new JDBCCollection(itemType, DBUtils.findValueHandler(session, itemType), new Object[]{value});
                        }
                    } else if (object instanceof String) {
                        return convertStringToArray(session, itemType, (String) object);
                    }
                }
            }
        }
        JDBCCollection collection = super.getValueFromObject(session, type, object, copy);
        return collection;
    }

    private JDBCCollection convertStringToArray(@NotNull DBCSession session, @NotNull PostgreDataType itemType, @NotNull String value) {
        List<String> strings = new ArrayList<>(10);
        StringTokenizer st = new StringTokenizer(value, " ");
        while (st.hasMoreTokens()) {
            strings.add(st.nextToken());
        }
        Object[] contents = new Object[strings.size()];
        for (int i = 0; i < strings.size(); i++) {
            contents[i] = PostgreUtils.convertStringToValue(itemType, strings.get(i), false);
        }
        return new JDBCCollection(itemType, DBUtils.findValueHandler(session, itemType), contents);
    }

    @NotNull
    @Override
    public String getValueDisplayString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format) {
        DBDCollection collection = (DBDCollection) value;
        if (!DBUtils.isNullValue(value)) {
            DBDValueHandler valueHandler = collection.getComponentValueHandler();
            StringBuilder str = new StringBuilder();
            str.append("{");
            for (int i = 0; i < collection.getItemCount(); i++) {
                if (i > 0) {
                    str.append(','); //$NON-NLS-1$
                }
                final Object item = collection.getItem(i);
                String itemString;
                if (item instanceof JDBCCollection) {
                    // Multi-dimensional arrays case
                    itemString = getValueDisplayString(column, item, format);
                } else {
                    itemString = valueHandler.getValueDisplayString(collection.getComponentType(), item, DBDDisplayFormat.NATIVE);
                }
                str.append(itemString);
            }
            str.append("}");

            return str.toString();
        }
        return super.getValueDisplayString(column, value, format);
    }
}

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
    public DBDCollection getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, Object object, boolean copy) throws DBCException
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
        return super.getValueFromObject(session, type, object, copy);
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
            if (format == DBDDisplayFormat.NATIVE) {
                str.append("'");
            }
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
            if (format == DBDDisplayFormat.NATIVE) {
                str.append("'");
            }

            return str.toString();
        }
        return super.getValueDisplayString(column, value, format);
    }
}

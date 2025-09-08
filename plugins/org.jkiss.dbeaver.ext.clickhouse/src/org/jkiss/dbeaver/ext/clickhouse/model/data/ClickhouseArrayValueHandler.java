/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.clickhouse.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.clickhouse.ClickhouseTypeParser;
import org.jkiss.dbeaver.ext.clickhouse.model.ClickhouseArrayType;
import org.jkiss.dbeaver.ext.clickhouse.model.ClickhouseDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDCollection;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCCollection;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCArrayValueHandler;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.Array;
import java.sql.SQLException;
import java.util.*;

public class ClickhouseArrayValueHandler extends JDBCArrayValueHandler {
    public static final ClickhouseArrayValueHandler INSTANCE = new ClickhouseArrayValueHandler();
    public static final String ARRAY_DELIMITER = ",";
    public static final Set<Character> QUOTED_CHARS = Set.of('[', ']', '"', ' ', '\\');

    @Override
    protected boolean convertSingleValueToArray() {
        return false;
    }

    @Override
    protected boolean useGetArray(@NotNull DBCSession session, @NotNull DBSTypedObject type) {
        return true;
    }

    @Override
    protected boolean useSetArray(@NotNull DBCSession session, @NotNull DBSTypedObject type) {
        return true;
    }

    @Override
    public Object getValueFromObject(
        @NotNull DBCSession session,
        @NotNull DBSTypedObject type,
        Object object,
        boolean copy,
        boolean validateValue
    ) throws DBCException {
        if (object == null) {
            return super.getValueFromObject(session, type, object, copy, validateValue);
        }

        ClickhouseArrayType arrayType = getArrayType(session, type);
        DBSDataType itemType = arrayType.getComponentType(session.getProgressMonitor());
        if (itemType == null) {
            throw new DBCException("Array type " + arrayType.getFullTypeName() + " doesn't have a component type");
        }

        if (object instanceof List<?> list) {
            return makeCollectionFromNestedJavaCollection((JDBCSession) session, itemType, list);
        } else if (object instanceof Array array && itemType.getName().startsWith("Tuple")) {
            // Tuples are represented as Object[] and need to be handled separately to avoid confusion with nested arrays
            return makeCollectionFromTupleArray(session, itemType, array);
        }

        return super.getValueFromObject(session, type, object, copy, validateValue);
    }

    @NotNull
    private Object makeCollectionFromNestedJavaCollection(
        @NotNull JDBCSession session,
        @NotNull DBSDataType itemType,
        Collection<?> collection
    ) throws DBCException {
        try {
            if (itemType instanceof ClickhouseArrayType arrayItemType) {
                List<Object> convertedItems = new ArrayList<>(collection.size());
                for (Object item : collection) {
                    if (item instanceof Collection<?> collectionItem) {
                        convertedItems.add(makeCollectionFromNestedJavaCollection(session, arrayItemType, collectionItem));
                    } else {
                        DBDValueHandler valueHandler = DBUtils.findValueHandler(session, arrayItemType);
                        convertedItems.add(
                            valueHandler.getValueFromObject(session, arrayItemType, item, false, true)
                        );
                    }
                }

                DBDValueHandler valueHandler = DBUtils.findValueHandler(session, arrayItemType);
                return new JDBCCollection(
                    session.getProgressMonitor(),
                    arrayItemType.getComponentType(session.getProgressMonitor()),
                    valueHandler,
                    convertedItems.toArray()
                );
            } else {
                DBDValueHandler valueHandler = DBUtils.findValueHandler(session, itemType);
                return new JDBCCollection(
                    session.getProgressMonitor(),
                    itemType,
                    valueHandler,
                    collection.toArray()
                );
            }
        } catch (DBException e) {
            throw new DBCException("Can't extract array data from Java array", e);
        }
    }

    @NotNull
    @Override
    public String getValueDisplayString(
        @NotNull DBSTypedObject column,
        Object value,
        @NotNull DBDDisplayFormat format
    ) {
        if (!DBUtils.isNullValue(value) && value instanceof JDBCCollection collection) {
            final StringJoiner output = new StringJoiner(ARRAY_DELIMITER, "[", "]");

            for (int i = 0; i < collection.getItemCount(); i++) {
                final Object item = collection.getItem(i);
                final String member;

                if (item instanceof DBDCollection) {
                    member = getArrayMemberDisplayString(column, this, item, format);
                } else {
                    final DBSDataType componentType = collection.getComponentType();
                    final DBDValueHandler componentHandler = collection.getComponentValueHandler();
                    member = getArrayMemberDisplayString(componentType, componentHandler, item, format);
                }

                output.add(member);
            }

            return output.toString();
        }

        return super.getValueDisplayString(column, value, format);
    }

    @NotNull
    private static String getArrayMemberDisplayString(
        @NotNull DBSTypedObject type,
        @NotNull DBDValueHandler handler,
        @Nullable Object value,
        @NotNull DBDDisplayFormat format
    ) {
        if (DBUtils.isNullValue(value)) {
            return SQLConstants.NULL_VALUE;
        }

        final String string = handler.getValueDisplayString(type, value, format);

        if (isQuotingRequired(type, string)) {
            return '\'' + string.replaceAll("[\\\\\"]", "\\\\$0") + '\'';
        }

        return string;
    }

    private static boolean isQuotingRequired(@NotNull DBSTypedObject type, @NotNull String value) {
        switch (type.getDataKind()) {
            case ARRAY:
            case NUMERIC:
                return false;
            case STRING:
            case DATETIME:
            case UNKNOWN:
                return true;
            default:
                break;
        }

        if (value.isEmpty() || value.equalsIgnoreCase(SQLConstants.NULL_VALUE)) {
            return true;
        }

        for (int index = 0; index < value.length(); index++) {
            if (QUOTED_CHARS.contains(value.charAt(index))) {
                return true;
            }
        }

        return value.contains(ARRAY_DELIMITER);
    }

    @NotNull
    private Object makeCollectionFromTupleArray(
        @NotNull DBCSession session,
        @NotNull DBSDataType itemType,
        @NotNull Array array
    ) {
        DBDValueHandler valueHandler = DBUtils.findValueHandler(session, itemType);
        try {
            ArrayList<Object> tuples = new ArrayList<>();
            for (Object tuple : (Object[]) array.getArray()) {
                Object value = valueHandler.getValueFromObject(session, itemType, tuple, false, false);
                tuples.add(value);
            }
            return new JDBCCollection(
                session.getProgressMonitor(),
                itemType,
                valueHandler,
                tuples.toArray()
            );
        } catch (DBCException | SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @NotNull
    private ClickhouseArrayType getArrayType(
        @NotNull DBCSession session,
        @NotNull DBSTypedObject type
    ) throws DBCException {
        ClickhouseArrayType arrayType;
        try {
            arrayType = (ClickhouseArrayType) ClickhouseTypeParser.getType(
                session.getProgressMonitor(),
                (ClickhouseDataSource) session.getDataSource(),
                type.getFullTypeName()
            );
        } catch (DBException e) {
            throw new DBCException("Can't resolve array data type " + type.getFullTypeName());
        }
        if (arrayType == null) {
            throw new DBCException("Can't resolve array data type " + type.getFullTypeName());
        }
        return arrayType;
    }
}

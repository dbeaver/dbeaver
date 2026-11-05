/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.clickhouse.ClickhouseTypeParser;
import org.jkiss.dbeaver.ext.clickhouse.model.ClickhouseArrayType;
import org.jkiss.dbeaver.ext.clickhouse.model.ClickhouseDataSource;
import org.jkiss.dbeaver.ext.clickhouse.model.ClickhouseMapType;
import org.jkiss.dbeaver.ext.clickhouse.model.ClickhouseTupleType;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDCollection;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDValueBinder;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCCollection;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCArrayValueHandler;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.sql.Array;
import java.sql.SQLException;
import java.util.*;

public class ClickhouseArrayValueHandler extends JDBCArrayValueHandler implements DBDValueBinder {
    private static final Log log = Log.getLog(ClickhouseArrayValueHandler.class);

    public static final ClickhouseArrayValueHandler INSTANCE = new ClickhouseArrayValueHandler();
    public static final String ARRAY_DELIMITER = ",";
    public static final String ARRAY_PREFIX = "[";
    public static final String ARRAY_SUFFIX = "]";
    public static final Set<Character> QUOTED_CHARS = Set.of('[', ']', '"', ' ', '\\');

    @NotNull
    @Override
    public String makeQueryBind(@NotNull DBSAttributeBase attribute, @NotNull Object value) {
        return ClickhouseComplexValueSerializer.shouldInline(value)
            ? getValueDisplayString(attribute, value, DBDDisplayFormat.NATIVE)
            : "?";
    }

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
    protected void bindParameter(
        @NotNull JDBCSession session,
        @NotNull JDBCPreparedStatement statement,
        @NotNull DBSTypedObject paramType,
        int paramIndex,
        @Nullable Object value
    ) throws DBCException, SQLException {
        if (value instanceof ClickhouseMapValue map && !map.isNull()) {
            statement.setObject(paramIndex, map.toJdbcMap());
        } else if (value instanceof JDBCCollection collection && !collection.isNull()
            && containsCompositeType(collection.getComponentType())) {
            // getArrayValue() unwraps nested DBDValues to Object[], which the driver treats as arrays, not tuples.
            statement.setObject(paramIndex, ClickhouseStructValueHandler.toJdbcValue(collection, null));
        } else {
            super.bindParameter(session, statement, paramType, paramIndex, value);
        }
    }

    private static boolean containsCompositeType(@NotNull DBSDataType type) {
        if (type instanceof ClickhouseMapType || type instanceof ClickhouseTupleType) {
            return true;
        }
        return type instanceof ClickhouseArrayType arrayType && containsCompositeType(arrayType.getComponentType());
    }

    @Override
    protected Object fetchColumnValue(
        @NotNull DBCSession session,
        @NotNull JDBCResultSet resultSet,
        @NotNull DBSTypedObject type,
        int index
    ) throws DBCException, SQLException {
        final String typeName = type.getTypeName();
        if (typeName.startsWith("Map")) {
            return ClickhouseStructValueHandler.INSTANCE.getValueFromObject(session, type, resultSet.getObject(index), false, false);
        }

        // Remove after https://github.com/ClickHouse/clickhouse-java/issues/2711 is fixed
        try {
            return super.fetchColumnValue(session, resultSet, type, index);
        } catch (SQLException exception) {
            String lowerCaseTypename = type.getTypeName().toLowerCase();
            if (lowerCaseTypename.contains("ipv4")
                || lowerCaseTypename.contains("ipv6")
                || lowerCaseTypename.contains("uuid")
                || lowerCaseTypename.contains("map")
            ) {
                log.warn("Falling back to getString() for type " + type.getTypeName(), exception);
                return getValueFromObject(session, type, resultSet.getString(index), false, false);
            } else {
                throw exception;
            }
        }

    }

    @Override
    public Object getValueFromObject(
        @NotNull DBCSession session,
        @NotNull DBSTypedObject type,
        @Nullable Object object,
        boolean copy,
        boolean validateValue
    ) throws DBCException {
        final String typeName = type.getTypeName();
        if (typeName.startsWith("Map")) {
            return ClickhouseStructValueHandler.INSTANCE.getValueFromObject(session, type, object, copy, validateValue);
        }
        if (object == null) {
            return super.getValueFromObject(session, type, object, copy, validateValue);
        }
        if (object instanceof JDBCCollection collection && collection.isNull()) {
            return copy ? collection.cloneValue(session.getProgressMonitor()) : collection;
        }

        ClickhouseArrayType arrayType = getArrayType(session, type);
        DBSDataType itemType = arrayType.getComponentType(session.getProgressMonitor());
        if (itemType == null) {
            throw new DBCException("Array type " + arrayType.getFullTypeName() + " doesn't have a component type");
        }

        if (object instanceof String string && string.startsWith(ARRAY_PREFIX) && string.endsWith(ARRAY_SUFFIX)) {
            return JDBCCollection.makeCollectionFromString((JDBCSession) session, string);
        }
        if (object instanceof Array array) {
            try {
                return makeCollectionFromArray(session, itemType, array.getArray());
            } catch (SQLException e) {
                throw new DBCException("Can't extract array data from JDBC array", e);
            }
        }
        if (object instanceof Collection<?> collection) {
            return makeCollectionFromArray(session, itemType, collection.toArray());
        }
        if (object.getClass().isArray()) {
            return makeCollectionFromArray(session, itemType, object);
        }

        return super.getValueFromObject(session, type, object, copy, validateValue);
    }

    @NotNull
    private Object makeCollectionFromArray(
        @NotNull DBCSession session,
        @NotNull DBSDataType itemType,
        @NotNull Object array
    ) throws DBCException {
        int length = java.lang.reflect.Array.getLength(array);
        Object[] items = new Object[length];
        try {
            for (int i = 0; i < length; i++) {
                items[i] = ClickhouseTypeParser.makeValue(session, itemType, java.lang.reflect.Array.get(array, i));
            }
        } catch (DBException e) {
            throw new DBCException("Can't extract array data from Java array", e);
        }
        return new JDBCCollection(session.getProgressMonitor(), itemType, DBUtils.findValueHandler(session, itemType), items);
    }

    @NotNull
    @Override
    public String getValueDisplayString(
        @NotNull DBSTypedObject column,
        Object value,
        @NotNull DBDDisplayFormat format
    ) {
        if (format == DBDDisplayFormat.NATIVE && value instanceof ClickhouseMapValue map) {
            return ClickhouseComplexValueSerializer.toSQL(map.getMapType().getDataSource(), column, map);
        }
        if (format == DBDDisplayFormat.NATIVE && value instanceof JDBCCollection collection) {
            return ClickhouseComplexValueSerializer.toSQL(collection.getComponentType().getDataSource(), column, collection);
        }
        if (!DBUtils.isNullValue(value) && value instanceof JDBCCollection collection) {
            final StringJoiner output = new StringJoiner(ARRAY_DELIMITER, ARRAY_PREFIX, ARRAY_SUFFIX);

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

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
import org.jkiss.dbeaver.ext.clickhouse.model.ClickhouseMapType;
import org.jkiss.dbeaver.ext.clickhouse.model.ClickhouseTupleType;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDValue;
import org.jkiss.dbeaver.model.data.DBDValueBinder;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCStructImpl;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCCollection;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCComposite;
import org.jkiss.dbeaver.model.impl.jdbc.data.handlers.JDBCStructValueHandler;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.utils.ArrayUtils;

import java.sql.Array;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class ClickhouseStructValueHandler extends JDBCStructValueHandler implements DBDValueBinder {
    
    public static final ClickhouseStructValueHandler INSTANCE = new ClickhouseStructValueHandler();

    private static final Log log = Log.getLog(ClickhouseStructValueHandler.class);

    @NotNull
    @Override
    public String makeQueryBind(@NotNull DBSAttributeBase attribute, @NotNull Object value) {
        return value instanceof ClickhouseTupleValue
            ? getValueDisplayString(attribute, value, DBDDisplayFormat.NATIVE)
            : "?";
    }

    @Override
    protected void bindParameter(
        @NotNull JDBCSession session,
        @NotNull JDBCPreparedStatement statement,
        @NotNull DBSTypedObject paramType,
        int paramIndex,
        @Nullable Object value
    ) throws DBCException, SQLException {
        if (value instanceof ClickhouseTupleValue tupleValue && !tupleValue.isNull()) {
            statement.setObject(paramIndex, toJdbcValue(tupleValue, tupleValue.getDataType()));
        } else {
            super.bindParameter(session, statement, paramType, paramIndex, value);
        }
    }

    // The ClickHouse JDBC driver encodes Struct as a tuple, Map as a map, and Object[] as an array.
    // Convert nested DBeaver values before passing them to the driver so their SQL types are preserved.
    @Nullable
    static Object toJdbcValue(@Nullable Object value, @Nullable DBSDataType type) throws DBCException, SQLException {
        if (value == null) {
            return null;
        }
        if (value instanceof ClickhouseTupleValue tuple) {
            return toJdbcValue(tuple.getRawValue(), tuple.getDataType());
        }
        if (value instanceof ClickhouseMapValue map) {
            return map.isNull() ? null : map.toJdbcMap();
        }
        if (value instanceof JDBCCollection collection) {
            Object[] contents = (Object[]) collection.getRawValue();
            return contents == null ? null : toJdbcArray(contents, collection.getComponentType());
        }
        if (value instanceof Array array) {
            return toJdbcValue(array.getArray(), type);
        }
        if (value instanceof Map<?, ?> map) {
            Map<Object, Object> converted = new LinkedHashMap<>();
            DBSDataType keyType = type instanceof ClickhouseMapType mapType ? mapType.getKeyType() : null;
            DBSDataType valueType = type instanceof ClickhouseMapType mapType ? mapType.getValueType() : null;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                converted.put(toJdbcValue(entry.getKey(), keyType), toJdbcValue(entry.getValue(), valueType));
            }
            return converted;
        }
        if (value instanceof Collection<?> collection) {
            return toJdbcValue(collection.toArray(), type);
        }
        if (value instanceof Object[] array) {
            if (type instanceof ClickhouseTupleType tupleType) {
                if (array.length != tupleType.getAttributes().size()) {
                    throw new DBCException("Tuple value does not match its declared number of attributes");
                }
                Object[] members = new Object[array.length];
                for (int i = 0; i < members.length; i++) {
                    members[i] = toJdbcValue(array[i], tupleType.getAttributes().get(i).getDataType());
                }
                return new JDBCStructImpl(tupleType.getFullTypeName(), members, "");
            }
            DBSDataType componentType = type instanceof ClickhouseArrayType arrayType
                ? arrayType.getComponentType() : null;
            return toJdbcArray(array, componentType);
        }
        return value;
    }

    @NotNull
    private static Object[] toJdbcArray(@NotNull Object[] values, @Nullable DBSDataType componentType) throws DBCException, SQLException {
        Object[] converted = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            converted[i] = toJdbcValue(values[i], componentType);
        }
        return converted;
    }

    @Override
    public Object getValueFromObject(@NotNull DBCSession session, @NotNull DBSTypedObject type, @Nullable Object object, boolean copy, boolean validateValue) throws DBCException {
        if (object instanceof DBDValue) {
            return object;
        }

        final String typeName = type.getFullTypeName();

        if (typeName.startsWith("Tuple") || typeName.startsWith("Map")) {
            try {
                return ClickhouseTypeParser.makeValue(session, typeName, object);
            } catch (DBException e) {
                log.debug("Can't parse data type: " + typeName, e);
            }
        }

        return super.getValueFromObject(session, type, object, copy, validateValue);
    }

    @NotNull
    @Override
    public String getValueDisplayString(@NotNull DBSTypedObject column, Object value, @NotNull DBDDisplayFormat format) {
        if (format == DBDDisplayFormat.NATIVE && value instanceof ClickhouseTupleValue tuple) {
            return ClickhouseComplexValueSerializer.toSQL(tuple.getDataType().getDataSource(), column, tuple);
        }
        if (value instanceof JDBCComposite) {
            Object[] values = ((JDBCComposite) value).getValues();
            if (!ArrayUtils.isEmpty(values)) {
                return DBValueFormatting.getDefaultValueDisplayString(values, format);
            }
        }
        return super.getValueDisplayString(column, value, format);
    }
}

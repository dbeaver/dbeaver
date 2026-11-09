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
import org.jkiss.dbeaver.ext.clickhouse.model.ClickhouseArrayType;
import org.jkiss.dbeaver.ext.clickhouse.model.ClickhouseMapType;
import org.jkiss.dbeaver.ext.clickhouse.model.ClickhouseTupleType;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCCollection;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;

import java.lang.reflect.Array;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Formats ClickHouse complex values as SQL expressions for native display and inline updates.
 * The JDBC driver's Map literal cannot be used as an expression in ALTER TABLE UPDATE.
 */
public final class ClickhouseComplexValueSerializer {
    private ClickhouseComplexValueSerializer() {
    }

    public static boolean shouldInline(@Nullable Object value) {
        return !DBUtils.isNullValue(value) && (value instanceof ClickhouseMapValue
            || value instanceof ClickhouseTupleValue || value instanceof JDBCCollection);
    }

    @NotNull
    static String toSQL(@NotNull DBPDataSource dataSource, @NotNull DBSTypedObject type, @Nullable Object value) {
        if (DBUtils.isNullValue(value)) {
            return SQLConstants.NULL_VALUE;
        }
        if (value instanceof ClickhouseMapValue map) {
            return formatMap(dataSource, map.getMapType(), map.getEntries());
        }
        if (value instanceof ClickhouseTupleValue tuple) {
            Object[] members = (Object[]) tuple.getRawValue();
            return members == null ? SQLConstants.NULL_VALUE : formatTuple(dataSource, (ClickhouseTupleType) tuple.getDataType(), members);
        }
        if (value instanceof JDBCCollection collection) {
            if (collection.getItemCount() == 0) {
                return "[]";
            }
            StringJoiner elements = new StringJoiner(",", "array(", ")");
            for (int i = 0; i < collection.getItemCount(); i++) {
                elements.add(toSQL(dataSource, collection.getComponentType(), collection.getItem(i)));
            }
            return elements.toString();
        }
        if (type instanceof ClickhouseMapType mapType && value instanceof Map<?, ?> map) {
            return formatMap(dataSource, mapType, map.entrySet());
        }
        if (type instanceof ClickhouseTupleType tupleType && value instanceof Object[] values) {
            return formatTuple(dataSource, tupleType, values);
        }
        if (type instanceof ClickhouseArrayType arrayType) {
            if (value instanceof java.sql.Array sqlArray) {
                try {
                    return formatArray(dataSource, arrayType, sqlArray.getArray());
                } catch (SQLException e) {
                    throw new IllegalStateException("Cannot read ClickHouse array for SQL serialization", e);
                }
            }
            if (value instanceof Collection<?> collection) {
                return formatArray(dataSource, arrayType, collection.toArray());
            }
            if (value.getClass().isArray()) {
                return formatArray(dataSource, arrayType, value);
            }
        }
        // Use the scalar handler and dialect to quote strings, escape values and format nulls.
        return SQLUtils.convertValueToSQL(
            dataSource,
            type,
            DBUtils.findValueHandler(dataSource, type),
            value,
            DBDDisplayFormat.NATIVE,
            false
        );
    }

    @NotNull
    private static String formatMap(
        @NotNull DBPDataSource dataSource,
        @NotNull ClickhouseMapType type,
        @NotNull Iterable<? extends Map.Entry<?, ?>> entries
    ) {
        StringJoiner items = new StringJoiner(",", "map(", ")");
        boolean hasEntries = false;
        for (Map.Entry<?, ?> entry : entries) {
            hasEntries = true;
            items.add(toSQL(dataSource, type.getKeyType(), entry.getKey()));
            items.add(toSQL(dataSource, type.getValueType(), entry.getValue()));
        }
        return hasEntries ? items.toString()
            : "CAST(([],[]), " + dataSource.getSQLDialect().getQuotedString(type.getFullTypeName()) + ")";
    }

    @NotNull
    private static String formatTuple(@NotNull DBPDataSource dataSource, @NotNull ClickhouseTupleType type, @NotNull Object[] values) {
        if (values.length != type.getAttributes().size()) {
            throw new IllegalArgumentException("Tuple value does not match its declared number of attributes");
        }
        StringJoiner members = new StringJoiner(",", "tuple(", ")");
        for (int i = 0; i < values.length; i++) {
            members.add(toSQL(dataSource, type.getAttributes().get(i).getDataType(), values[i]));
        }
        return members.toString();
    }

    @NotNull
    private static String formatArray(@NotNull DBPDataSource dataSource, @NotNull ClickhouseArrayType type, @NotNull Object values) {
        int length = Array.getLength(values);
        if (length == 0) {
            return "[]";
        }
        StringJoiner items = new StringJoiner(",", "array(", ")");
        DBSDataType componentType = type.getComponentType();
        for (int i = 0; i < length; i++) {
            items.add(toSQL(dataSource, componentType, Array.get(values, i)));
        }
        return items.toString();
    }
}

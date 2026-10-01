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
package org.jkiss.dbeaver.ext.duckdb.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.GenericDataType;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCBasicDataTypeCache;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Locale;

public class DuckDataTypeCache extends JDBCBasicDataTypeCache<GenericStructContainer, GenericDataType> {
    public DuckDataTypeCache(@NotNull GenericStructContainer container) {
        super(container);
    }

    @NotNull
    @Override
    protected JDBCStatement prepareObjectsStatement(
        @NotNull JDBCSession session,
        @NotNull GenericStructContainer container
    ) throws SQLException {
        return session.prepareStatement(
            "select distinct(type_name), type_name, type_category " +
            "from duckdb_types() " +
            "where schema_name = 'main'"
        );
    }

    @Override
    protected GenericDataType fetchObject(
        @NotNull JDBCSession session,
        @NotNull GenericStructContainer container,
        @NotNull JDBCResultSet dbResult
    ) {
        final String name = JDBCUtils.safeGetString(dbResult, "type_name");
        final int kind = getTypeKind(name, JDBCUtils.safeGetString(dbResult, "type_category"));
        return new GenericDataType(container, kind, name, null, false, false, -1, -1, -1);
    }

    private static int getTypeKind(@Nullable String typeName, @Nullable String category) {
        if (typeName != null) {
            int type = switch (typeName.toLowerCase(Locale.ROOT)) {
                case "date" -> Types.DATE;
                case "time" -> Types.TIME;
                case "timetz", "time with time zone" -> Types.TIME_WITH_TIMEZONE;
                case "timestamp", "timestamp_s", "timestamp_ms", "timestamp_ns", "timestamp_us" -> Types.TIMESTAMP;
                case "timestamptz", "timestamp with time zone" -> Types.TIMESTAMP_WITH_TIMEZONE;
                default -> Types.OTHER;
            };
            if (type != Types.OTHER) {
                return type;
            }
        }
        if (category == null) {
            return Types.OTHER;
        }

        return switch (category.toLowerCase(Locale.ROOT)) {
            case "boolean", "bool", "logical" -> Types.BOOLEAN;
            case "composite", "point_2d", "point_3d", "point_4d", "linestring_2d", "polygon_2d", "box_2d" ->
                Types.STRUCT;
            case "wkb_blob", "blob", "bytea", "varbinary", "binary" -> Types.BINARY;
            case "date" -> Types.DATE;
            case "datetime", "timestamp_us" -> Types.TIMESTAMP;
            case "timestamptz" -> Types.TIMESTAMP_WITH_TIMEZONE;
            case "time" -> Types.TIME;
            case "timetz" -> Types.TIME_WITH_TIMEZONE;
            case "numeric" -> Types.NUMERIC;
            case "string", "varchar", "bpchar", "nvarchar", "text" -> Types.VARCHAR;
            default -> Types.OTHER;
        };
    }
}

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
package org.jkiss.dbeaver.ext.starrocks.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;

import java.sql.Types;

/**
 * StarRocks Table Column - represents a column in a StarRocks table or view.
 * Parses column metadata from SHOW FULL COLUMNS result set.
 */
public class StarRocksTableColumn extends GenericTableColumn {

    private static final Log log = Log.getLog(StarRocksTableColumn.class);

    private static final String COL_COLUMN_NAME = "COLUMN_NAME"; //$NON-NLS-1$
    private static final String COL_COLUMN_TYPE = "COLUMN_TYPE"; //$NON-NLS-1$
    private static final String COL_IS_NULLABLE = "IS_NULLABLE"; //$NON-NLS-1$
    private static final String COL_COLUMN_DEFAULT = "COLUMN_DEFAULT"; //$NON-NLS-1$
    private static final String COL_COLUMN_COMMENT = "COLUMN_COMMENT"; //$NON-NLS-1$

    public StarRocksTableColumn(@NotNull GenericTableBase table) {
        super(table);
    }

    /**
     * Constructs a column by parsing information_schema.columns result set.
     */
    public StarRocksTableColumn(
        @NotNull GenericTableBase table,
        @NotNull JDBCResultSet dbResult,
        int ordinal
    ) {
        super(table,
            JDBCUtils.safeGetString(dbResult, COL_COLUMN_NAME),
            JDBCUtils.safeGetString(dbResult, COL_COLUMN_TYPE),
            mapSqlType(JDBCUtils.safeGetString(dbResult, COL_COLUMN_TYPE)),
            Types.OTHER,
            ordinal,
            extractColumnSize(JDBCUtils.safeGetString(dbResult, COL_COLUMN_TYPE)),
            extractColumnSize(JDBCUtils.safeGetString(dbResult, COL_COLUMN_TYPE)),
            extractScale(JDBCUtils.safeGetString(dbResult, COL_COLUMN_TYPE)),
            null,
            10,
            "YES".equalsIgnoreCase(JDBCUtils.safeGetString(dbResult, COL_IS_NULLABLE)),
            JDBCUtils.safeGetString(dbResult, COL_COLUMN_COMMENT),
            JDBCUtils.safeGetString(dbResult, COL_COLUMN_DEFAULT),
            false,
            false
        );
    }

    /**
     * Constructs a column with basic properties (for manual creation).
     */
    public StarRocksTableColumn(
        @NotNull GenericTableBase table,
        @NotNull String columnName,
        @NotNull String typeName,
        boolean notNull,
        int ordinal
    ) {
        super(table,
            columnName,
            typeName,
            mapSqlType(typeName),
            Types.OTHER,
            ordinal,
            extractColumnSize(typeName),
            extractColumnSize(typeName),
            extractScale(typeName),
            null,
            10,
            notNull,
            null,
            null,
            false,
            false
        );
    }

    @NotNull
    @Override
    public StarRocksDataSource getDataSource() {
        return (StarRocksDataSource) super.getDataSource();
    }

    private static int mapSqlType(@Nullable String typeName) {
        if (typeName == null) {
            return Types.OTHER;
        }
        String upperType = typeName.toUpperCase();
        int parenIndex = upperType.indexOf('(');
        if (parenIndex > 0) {
            upperType = upperType.substring(0, parenIndex).trim();
        }

        return switch (upperType) {
            case "BOOLEAN", "BOOL" -> Types.BOOLEAN;
            case "TINYINT" -> Types.TINYINT;
            case "SMALLINT" -> Types.SMALLINT;
            case "INT", "INTEGER" -> Types.INTEGER;
            case "BIGINT", "LARGEINT" -> Types.BIGINT;
            case "FLOAT" -> Types.FLOAT;
            case "DOUBLE" -> Types.DOUBLE;
            case "DECIMAL", "DECIMALV2", "DECIMAL32", "DECIMAL64", "DECIMAL128" -> Types.DECIMAL;
            case "CHAR" -> Types.CHAR;
            case "VARCHAR", "STRING", "TEXT" -> Types.VARCHAR;
            case "BINARY", "VARBINARY" -> Types.VARBINARY;
            case "DATE" -> Types.DATE;
            case "DATETIME", "TIMESTAMP" -> Types.TIMESTAMP;
            case "TIME" -> Types.TIME;
            case "ARRAY" -> Types.ARRAY;
            case "MAP", "STRUCT" -> Types.STRUCT;
            default -> Types.OTHER;
        };
    }

    private static long extractColumnSize(@Nullable String typeName) {
        if (typeName == null) {
            return 0;
        }
        int parenStart = typeName.indexOf('(');
        int parenEnd = typeName.indexOf(')');
        if (parenStart > 0 && parenEnd > parenStart) {
            String sizeStr = typeName.substring(parenStart + 1, parenEnd);
            int commaIndex = sizeStr.indexOf(',');
            if (commaIndex > 0) {
                sizeStr = sizeStr.substring(0, commaIndex);
            }
            try {
                return Long.parseLong(sizeStr.trim());
            } catch (NumberFormatException e) {
                log.debug("Failed to parse column size from type: " + typeName, e); //$NON-NLS-1$
                return 0;
            }
        }
        return 0;
    }

    @Nullable
    private static Integer extractScale(@Nullable String typeName) {
        if (typeName == null) {
            return null;
        }
        int parenStart = typeName.indexOf('(');
        int parenEnd = typeName.indexOf(')');
        if (parenStart > 0 && parenEnd > parenStart) {
            String sizeStr = typeName.substring(parenStart + 1, parenEnd);
            int commaIndex = sizeStr.indexOf(',');
            if (commaIndex > 0) {
                try {
                    return Integer.parseInt(sizeStr.substring(commaIndex + 1).trim());
                } catch (NumberFormatException e) {
                    log.debug("Failed to parse column scale from type: " + typeName, e); //$NON-NLS-1$
                    return null;
                }
            }
        }
        return null;
    }
}

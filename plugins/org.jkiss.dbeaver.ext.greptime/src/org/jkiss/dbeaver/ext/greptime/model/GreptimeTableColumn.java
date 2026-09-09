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
package org.jkiss.dbeaver.ext.greptime.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.ext.greptime.GreptimeUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

import java.sql.Types;
import java.util.Objects;

public class GreptimeTableColumn extends GenericTableColumn {

    private static final String COLUMN_NAME = "COLUMN_NAME"; //$NON-NLS-1$
    private static final String COLUMN_TYPE = "COLUMN_TYPE"; //$NON-NLS-1$
    private static final String IS_NULLABLE = "IS_NULLABLE"; //$NON-NLS-1$
    private static final String COLUMN_DEFAULT = "COLUMN_DEFAULT"; //$NON-NLS-1$
    private static final String COLUMN_COMMENT = "COLUMN_COMMENT"; //$NON-NLS-1$
    private static final String CHARACTER_MAXIMUM_LENGTH = "CHARACTER_MAXIMUM_LENGTH"; //$NON-NLS-1$
    private static final String CHARACTER_OCTET_LENGTH = "CHARACTER_OCTET_LENGTH"; //$NON-NLS-1$
    private static final String NUMERIC_PRECISION = "NUMERIC_PRECISION"; //$NON-NLS-1$
    private static final String NUMERIC_SCALE = "NUMERIC_SCALE"; //$NON-NLS-1$
    private static final String DATETIME_PRECISION = "DATETIME_PRECISION"; //$NON-NLS-1$
    private static final String SEMANTIC_TYPE = "SEMANTIC_TYPE"; //$NON-NLS-1$

    private final String semanticType;

    public GreptimeTableColumn(
        @NotNull GenericTableBase table,
        @NotNull JDBCResultSet dbResult,
        int ordinalPosition
    ) {
        this(
            table,
            dbResult,
            ordinalPosition,
            JDBCUtils.safeGetString(dbResult, COLUMN_TYPE)
        );
    }

    private GreptimeTableColumn(
        @NotNull GenericTableBase table,
        @NotNull JDBCResultSet dbResult,
        int ordinalPosition,
        @Nullable String columnType
    ) {
        super(
            table,
            JDBCUtils.safeGetString(dbResult, COLUMN_NAME),
            columnType,
            GreptimeUtils.typeNameToValueType(columnType),
            Types.OTHER,
            ordinalPosition,
            getColumnSize(dbResult),
            JDBCUtils.safeGetLong(dbResult, CHARACTER_OCTET_LENGTH),
            getScale(dbResult),
            JDBCUtils.safeGetInteger(dbResult, NUMERIC_PRECISION),
            10,
            "NO".equalsIgnoreCase(JDBCUtils.safeGetString(dbResult, IS_NULLABLE)),
            JDBCUtils.safeGetString(dbResult, COLUMN_COMMENT),
            JDBCUtils.safeGetString(dbResult, COLUMN_DEFAULT),
            false,
            false
        );
        this.semanticType = JDBCUtils.safeGetString(dbResult, SEMANTIC_TYPE);
    }

    @Nullable
    @Property(viewable = true, order = 75)
    public String getSemanticType() {
        return semanticType;
    }

    private static long getColumnSize(@NotNull JDBCResultSet dbResult) {
        long characterLength = JDBCUtils.safeGetLong(dbResult, CHARACTER_MAXIMUM_LENGTH);
        if (characterLength > 0) {
            return characterLength;
        }

        Integer numericPrecision = JDBCUtils.safeGetInteger(dbResult, NUMERIC_PRECISION);
        return Objects.requireNonNullElse(numericPrecision, 0);
    }

    @Nullable
    private static Integer getScale(@NotNull JDBCResultSet dbResult) {
        Integer numericScale = JDBCUtils.safeGetInteger(dbResult, NUMERIC_SCALE);
        if (numericScale != null) {
            return numericScale;
        }
        return JDBCUtils.safeGetInteger(dbResult, DATETIME_PRECISION);
    }
}

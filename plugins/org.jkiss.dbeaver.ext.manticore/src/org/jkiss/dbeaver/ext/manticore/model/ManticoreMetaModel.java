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
package org.jkiss.dbeaver.ext.manticore.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericDataType;
import org.jkiss.dbeaver.ext.generic.model.GenericDataTypeCache;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaObject;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCBasicDataTypeCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCDataType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Locale;

public class ManticoreMetaModel extends GenericMetaModel {

    @NotNull
    @Override
    public GenericDataSource createDataSourceImpl(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSourceContainer container
    ) throws DBException {
        return new ManticoreDataSource(monitor, container, this);
    }

    @NotNull
    @Override
    public JDBCBasicDataTypeCache<GenericStructContainer, ? extends JDBCDataType> createDataTypeCache(
        @NotNull GenericStructContainer container
    ) {
        return new GenericDataTypeCache(container) {
            @Override
            protected boolean handleCacheReadError(@NotNull Exception error) {
                return true;
            }

            @Override
            protected void addCustomObjects(
                @NotNull DBRProgressMonitor monitor,
                @NotNull GenericStructContainer owner,
                @NotNull List<GenericDataType> genericDataTypes
            ) throws DBException {
                addType(genericDataTypes, "bigint", Types.BIGINT);
                addType(genericDataTypes, "integer", Types.INTEGER);
                addType(genericDataTypes, "int", Types.INTEGER);
                addType(genericDataTypes, "uint", Types.INTEGER);
                addType(genericDataTypes, "float", Types.FLOAT);
                addType(genericDataTypes, "timestamp", Types.TIMESTAMP);
                addType(genericDataTypes, "bool", Types.BOOLEAN);
                addType(genericDataTypes, "string", Types.VARCHAR);
                addType(genericDataTypes, "text", Types.LONGVARCHAR);
                addType(genericDataTypes, "field", Types.LONGVARCHAR);
                addType(genericDataTypes, "json", Types.LONGVARCHAR);
                addType(genericDataTypes, "mva", Types.ARRAY);
                addType(genericDataTypes, "bit", Types.BIT);
            }

            private void addType(@NotNull List<GenericDataType> types, @NotNull String name, int jdbcType) {
                if (DBUtils.findObject(types, name) == null) {
                    types.add(new GenericDataType(owner, jdbcType, name, name, false, true, 0, 0, 0));
                }
            }
        };
    }

    @Override
    public JDBCStatement prepareTableLoadStatement(
        @NotNull JDBCSession session,
        @NotNull GenericStructContainer owner,
        @Nullable GenericTableBase object,
        @Nullable String objectName
    ) throws SQLException {
        String sql = "SHOW TABLES";
        String name = object != null ? object.getName() : objectName;
        if (!CommonUtils.isEmpty(name)) {
            sql += " LIKE '" + escapeLikeLiteral(name) + "'";
        }
        return session.prepareStatement(sql);
    }

    @Override
    public GenericTableBase createTableImpl(
        @NotNull JDBCSession session,
        @NotNull GenericStructContainer owner,
        @NotNull GenericMetaObject tableObject,
        @NotNull JDBCResultSet dbResult
    ) {
        String tableName = firstNonEmpty(
            JDBCUtils.safeGetString(dbResult, "Table"),
            JDBCUtils.safeGetString(dbResult, "Index"),
            JDBCUtils.safeGetString(dbResult, "TABLE_NAME")
        );
        if (CommonUtils.isEmpty(tableName)) {
            return null;
        }
        String tableType = firstNonEmpty(
            JDBCUtils.safeGetString(dbResult, "Type"),
            JDBCUtils.safeGetString(dbResult, "TABLE_TYPE")
        );
        return createTableOrViewImpl(owner, tableName, tableType, dbResult);
    }

    @Override
    public JDBCStatement prepareTableColumnLoadStatement(
        @NotNull JDBCSession session,
        @NotNull GenericStructContainer owner,
        @Nullable GenericTableBase forTable
    ) throws SQLException {
        if (forTable == null) {
            return session.prepareStatement("SHOW TABLES LIKE ''");
        }
        return session.prepareStatement("DESCRIBE " + quoteIdentifier(forTable.getName()));
    }

    @Override
    public GenericTableColumn fetchTableColumn(
        @NotNull JDBCSession session,
        @NotNull GenericStructContainer owner,
        @NotNull GenericTableBase table,
        @NotNull JDBCResultSet dbResult
    ) throws DBException {
        String columnName = firstNonEmpty(
            JDBCUtils.safeGetString(dbResult, "Field"),
            JDBCUtils.safeGetString(dbResult, "field"),
            JDBCUtils.safeGetString(dbResult, "COLUMN_NAME")
        );
        if (CommonUtils.isEmpty(columnName)) {
            return null;
        }
        String typeName = firstNonEmpty(
            JDBCUtils.safeGetString(dbResult, "Type"),
            JDBCUtils.safeGetString(dbResult, "type"),
            JDBCUtils.safeGetString(dbResult, "TYPE_NAME")
        );
        if (CommonUtils.isEmpty(typeName)) {
            typeName = "string";
        }
        int valueType = getJdbcType(typeName);
        int ordinalPos = 0;
        try {
            ordinalPos = dbResult.getRow();
        } catch (SQLException ignored) {
        }
        return createTableColumnImpl(
            session.getProgressMonitor(),
            dbResult,
            table,
            columnName,
            typeName,
            valueType,
            0,
            ordinalPos,
            0,
            0,
            null,
            null,
            10,
            false,
            JDBCUtils.safeGetString(dbResult, "Properties"),
            null,
            false,
            false
        );
    }

    @Override
    public boolean hasProcedureSupport() {
        return false;
    }

    @Override
    public boolean hasFunctionSupport() {
        return false;
    }

    @Override
    public boolean supportsViews(@NotNull GenericDataSource dataSource) {
        return false;
    }

    @Override
    public boolean supportsTriggers(@NotNull GenericDataSource dataSource) {
        return false;
    }

    @Override
    public boolean supportsSequences(@NotNull GenericDataSource dataSource) {
        return false;
    }

    @Override
    public boolean supportsSynonyms(@NotNull GenericDataSource dataSource) {
        return false;
    }

    @Override
    public boolean supportsNotNullColumnModifiers(DBSObject object) {
        return false;
    }

    @NotNull
    private static String quoteIdentifier(@NotNull String name) {
        return "`" + name.replace("`", "``") + "`";
    }

    @NotNull
    private static String escapeLikeLiteral(@NotNull String name) {
        return name.replace("\\", "\\\\").replace("'", "''");
    }

    @Nullable
    private static String firstNonEmpty(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!CommonUtils.isEmpty(value)) {
                return value;
            }
        }
        return null;
    }

    private static int getJdbcType(@NotNull String typeName) {
        String type = typeName.toLowerCase(Locale.ROOT);
        int space = type.indexOf(' ');
        if (space > 0) {
            type = type.substring(0, space);
        }
        return switch (type) {
            case "text", "field", "json" -> Types.LONGVARCHAR;
            case "string" -> Types.VARCHAR;
            case "integer", "int", "uint", "uint32", "ordinal" -> Types.INTEGER;
            case "bigint", "uint64" -> Types.BIGINT;
            case "float" -> Types.FLOAT;
            case "double" -> Types.DOUBLE;
            case "timestamp" -> Types.TIMESTAMP;
            case "bool", "boolean" -> Types.BOOLEAN;
            case "bit" -> Types.BIT;
            case "mva", "multi", "multi64" -> Types.ARRAY;
            default -> Types.OTHER;
        };
    }
}

/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.clickhouse.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformProvider;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformType;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformer;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCBasicDataTypeCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCDataType;
import org.jkiss.dbeaver.model.impl.sql.QueryTransformerLimit;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.Map;

/**
 * ClickhouseMetaModel
 */
public class ClickhouseMetaModel extends GenericMetaModel implements DBCQueryTransformProvider
{
    public ClickhouseMetaModel() {
        super();
    }

    @Override
    public GenericDataSource createDataSourceImpl(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
        return new ClickhouseDataSource(monitor, container, this);
    }

    @Override
    public JDBCBasicDataTypeCache<GenericStructContainer, ? extends JDBCDataType> createDataTypeCache(@NotNull GenericStructContainer container) {
        return new ClickhouseDataTypeCache(container);
    }

    @Override
    public GenericSchema createSchemaImpl(@NotNull GenericDataSource dataSource, @Nullable GenericCatalog catalog, @NotNull String schemaName) throws DBException {
        return new ClickhouseSchema(dataSource, catalog, schemaName);
    }

    @Override
    public GenericCatalog createCatalogImpl(@NotNull GenericDataSource dataSource, @NotNull String catalogName) {
        return new ClickhouseCatalog(dataSource, catalogName);
    }

    @Override
    public boolean isSystemSchema(GenericSchema schema) {
        return schema.getName().equalsIgnoreCase("INFORMATION_SCHEMA") || schema.getName().equals("system");
    }

    @Override
    public JDBCStatement prepareTableLoadStatement(
        @NotNull JDBCSession session,
        @NotNull GenericStructContainer owner,
        @Nullable GenericTableBase table,
        @Nullable String tableName) throws SQLException {
        // engine can be View or MaterializedView, we can read this field instead table_type
        String sql =
            "SELECT name as TABLE_NAME, engine as TABLE_TYPE, database as TABLE_SCHEM," +
                (((ClickhouseDataSource) owner.getDataSource()).isSupportTableComments() ? "comment as REMARKS," : "") + " * " +
            "FROM system.tables\n" +
            "WHERE database = ?" + (table != null || CommonUtils.isNotEmpty(tableName) ? " and name=?" : "");
        JDBCPreparedStatement dbStat = session.prepareStatement(sql);
        dbStat.setString(1, owner.getName());
        if (table != null || CommonUtils.isNotEmpty(tableName)) {
            dbStat.setString(2, table != null ? table.getName() : tableName);
        }
        return dbStat;
    }

    @Override
    public GenericTableBase createTableOrViewImpl(GenericStructContainer container, @Nullable String tableName, @Nullable String tableType, @Nullable JDBCResultSet dbResult) {
        if (tableType != null && isView(tableType)) {
            return new ClickhouseView(container, tableName, tableType, dbResult);
        } else {
            return new ClickhouseTable(
                container,
                tableName,
                tableType,
                dbResult);
        }
    }

    @Override
    public GenericTableColumn createTableColumnImpl(@NotNull DBRProgressMonitor monitor, @Nullable JDBCResultSet dbResult, @NotNull GenericTableBase table, String columnName, String typeName, int valueType, int sourceType, int ordinalPos, long columnSize, long charLength, Integer scale, Integer precision, int radix, boolean notNull, String remarks, String defaultValue, boolean autoIncrement, boolean autoGenerated) throws DBException {
        return new ClickhouseTableColumn(
            table, columnName, typeName, valueType, sourceType, ordinalPos,
            columnSize, charLength, scale, precision, radix, notNull,
            remarks, defaultValue, autoIncrement, autoGenerated
        );
    }

    @Override
    public String getTableDDL(@NotNull DBRProgressMonitor monitor, @NotNull GenericTableBase sourceObject, @NotNull Map<String, Object> options) throws DBException {
        GenericSchema schema =  sourceObject.getSchema();
        GenericCatalog catalog =  sourceObject.getCatalog();

        if (
            (schema != null && schema.getName().equals("system"))
            || (catalog != null && catalog.getName().equals("system"))
        ) {
            return super.getTableDDL(monitor, sourceObject, options);
        }
        GenericDataSource dataSource = sourceObject.getDataSource();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, sourceObject, "Read Clickhouse view/table source")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SHOW CREATE TABLE " + sourceObject.getFullyQualifiedName(DBPEvaluationContext.DDL)))
            {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    StringBuilder sql = new StringBuilder();
                    while (dbResult.next()) {
                        String line = dbResult.getString(1);
                        if (line == null) {
                            continue;
                        }
                        sql.append(line).append("\n");
                    }
                    String ddl = sql.toString();

                    return normalizeDDL(ddl);
                }
            }
        } catch (SQLException e) {
            throw new DBDatabaseException(e, dataSource);
        }
    }

    @Override
    public boolean supportsTableDDLSplit(GenericTableBase sourceObject) {
        return false;
    }

    private String normalizeDDL(String ddl) {
        int declStart = ddl.indexOf("(");
        int declEnd = ddl.indexOf(") ENGINE");
        if (declEnd == -1) {
            declEnd = ddl.length() - 1;
        }
        return
            ddl.substring(0, declStart) + "(\n" +
            ddl.substring(declStart + 1, declEnd).replace(",", ",\n") + "\n" +
            ddl.substring(declEnd);
    }

    @Override
    public String getViewDDL(@NotNull DBRProgressMonitor monitor, @NotNull GenericView sourceObject, @NotNull Map<String, Object> options) throws DBException {
        return getTableDDL(monitor, sourceObject, options);
    }

    @Override
    public boolean supportsNotNullColumnModifiers(DBSObject object) {
        return false;
    }

    @Override
    @Nullable
    public DBCQueryTransformer createQueryTransformer(@NotNull DBCQueryTransformType type) {
        if (type == DBCQueryTransformType.RESULT_SET_LIMIT) {
            return new QueryTransformerLimit(true, true);
        }
        return null;
    }

}

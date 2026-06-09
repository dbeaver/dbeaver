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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.ext.generic.model.GenericView;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaObject;
import org.jkiss.dbeaver.ext.starrocks.StarRocksUtils;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * StarRocks meta model - provides StarRocks-specific metadata loading
 */
public class StarRocksMetaModel extends GenericMetaModel {

    private static final String TYPE_MATERIALIZED_VIEW = "MATERIALIZED VIEW"; //$NON-NLS-1$
    private static final String TYPE_VIEW = "VIEW"; //$NON-NLS-1$
    private static final String ORDINAL_POSITION = "ORDINAL_POSITION"; //$NON-NLS-1$

    public StarRocksMetaModel() {
        super();
    }

    @NotNull
    @Override
    public GenericDataSource createDataSourceImpl(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSourceContainer container
    ) throws DBException {
        return new StarRocksDataSource(monitor, container, this);
    }

    @NotNull
    @Override
    public GenericCatalog createCatalogImpl(
        @NotNull GenericDataSource dataSource,
        @NotNull String catalogName
    ) {
        StarRocksDataSource starRocksDataSource = (StarRocksDataSource) dataSource;
        StarRocksCatalog catalog = new StarRocksCatalog(starRocksDataSource, catalogName);

        // Populate type and comment from cached metadata
        StarRocksDataSource.CatalogMetadata metadata = starRocksDataSource.getCatalogMetadata(catalogName);
        if (metadata != null) {
            catalog.setType(metadata.type());
            catalog.setComment(metadata.comment());
        }

        return catalog;
    }

    @NotNull
    @Override
    public GenericSchema createSchemaImpl(
        @NotNull GenericDataSource dataSource,
        @Nullable GenericCatalog catalog,
        @NotNull String schemaName
    ) throws DBException {
        return new StarRocksDatabase((StarRocksDataSource) dataSource, (StarRocksCatalog) catalog, schemaName);
    }

    @Nullable
    @Override
    public List<GenericSchema> loadSchemas(
        @NotNull JDBCSession session,
        @NotNull GenericDataSource dataSource,
        @Nullable GenericCatalog catalog
    ) throws DBException {
        if (catalog == null) {
            return null;
        }

        List<GenericSchema> schemas = new ArrayList<>();
        try {
            // Switch to the catalog context
            StarRocksUtils.setCatalogContext(session, (StarRocksDataSource) dataSource, catalog.getName());

            // Load databases using SHOW DATABASES
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SHOW DATABASES")) { //$NON-NLS-1$
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String schemaName = JDBCUtils.safeGetString(dbResult, 1);
                        if (schemaName != null) {
                            schemas.add(createSchemaImpl(dataSource, catalog, schemaName));
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBException("Error loading StarRocks databases", e); //$NON-NLS-1$
        }
        return schemas;
    }

    @Override
    @NotNull
    public JDBCStatement prepareTableLoadStatement(
        @NotNull JDBCSession session,
        @NotNull GenericStructContainer owner,
        @Nullable GenericTableBase object,
        @Nullable String objectName
    ) throws SQLException {
        GenericCatalog catalog = owner.getCatalog();
        if (catalog != null) {
            StarRocksUtils.setCatalogContext(session, (StarRocksDataSource) owner.getDataSource(), catalog.getName());
        }

        GenericSchema schema = owner.getSchema();
        String schemaName = schema != null ? schema.getName() : owner.getName();

        // StarRocks returns Table_type="VIEW" for both views and materialized views.
        // Only internal catalogs support materialized views - external catalogs don't.
        // For internal catalogs, we join with information_schema.materialized_views to distinguish them.
        // Result columns: table_name, table_type, is_materialized
        boolean isInternalCatalog = catalog instanceof StarRocksCatalog starRocksCatalog && starRocksCatalog.isInternal();

        String query;
        if (isInternalCatalog) {
            query =
                "SELECT t.table_name, " + //$NON-NLS-1$
                "       CASE WHEN t.table_type = 'BASE TABLE' THEN 'BASE TABLE' ELSE 'VIEW' END as table_type, " + //$NON-NLS-1$
                "       (mv.table_name IS NOT NULL) as is_materialized, " + //$NON-NLS-1$
                "       t.table_comment as REMARKS " + //$NON-NLS-1$
                "FROM information_schema.tables t " + //$NON-NLS-1$
                "LEFT JOIN information_schema.materialized_views mv " + //$NON-NLS-1$
                "  ON t.table_schema = mv.table_schema AND t.table_name = mv.table_name " + //$NON-NLS-1$
                "WHERE t.table_schema = ?"; //$NON-NLS-1$
        } else {
            query =
                "SELECT t.table_name, " + //$NON-NLS-1$
                "       CASE WHEN t.table_type = 'BASE TABLE' THEN 'BASE TABLE' ELSE 'VIEW' END as table_type, " + //$NON-NLS-1$
                "       FALSE as is_materialized, " + //$NON-NLS-1$
                "       t.table_comment as REMARKS " + //$NON-NLS-1$
                "FROM information_schema.tables t " + //$NON-NLS-1$
                "WHERE t.table_schema = ?"; //$NON-NLS-1$
        }

        JDBCPreparedStatement stmt = session.prepareStatement(query);
        stmt.setString(1, schemaName);
        return stmt;
    }

    @Nullable
    @Override
    public GenericTableBase createTableImpl(
        @NotNull JDBCSession session,
        @NotNull GenericStructContainer owner,
        @NotNull GenericMetaObject tableObject,
        @NotNull JDBCResultSet dbResult
    ) {
        String tableName = JDBCUtils.safeGetString(dbResult, 1);
        String tableType = JDBCUtils.safeGetString(dbResult, 2);

        return createTableOrViewImpl(owner, tableName, tableType, dbResult);
    }

    @Nullable
    @Override
    public GenericTableBase createTableOrViewImpl(
        @NotNull GenericStructContainer container,
        @Nullable String tableName,
        @Nullable String tableType,
        @Nullable JDBCResultSet dbResult
    ) {
        if (tableName == null) {
            return null;
        }

        String tableTypeUpper = tableType != null ? tableType.toUpperCase() : ""; //$NON-NLS-1$

        // Check if this view is actually a materialized view
        if (tableTypeUpper.contains(TYPE_VIEW)) {
            Boolean isMaterialized = JDBCUtils.safeGetBoolean(dbResult, 3);
            if (Boolean.TRUE.equals(isMaterialized)) {
                return new StarRocksMaterializedView(container, tableName, tableType, dbResult);
            } else {
                return new StarRocksView(container, tableName, tableType, dbResult);
            }
        } else {
            return new StarRocksTable(container, tableName, tableType, dbResult);
        }
    }

    @NotNull
    @Override
    public JDBCStatement prepareTableColumnLoadStatement(
        @NotNull JDBCSession session,
        @NotNull GenericStructContainer owner,
        @Nullable GenericTableBase forTable
    ) throws SQLException {
        if (forTable == null) {
            throw new SQLException("Cannot load columns without specifying a table"); //$NON-NLS-1$
        }

        // Switch to the catalog context
        GenericCatalog catalog = owner.getCatalog();
        if (catalog != null) {
            StarRocksUtils.setCatalogContext(session, (StarRocksDataSource) owner.getDataSource(), catalog.getName());
        }

        GenericSchema schema = owner.getSchema();
        String schemaName = schema != null ? schema.getName() : owner.getName();

        // Use information_schema.columns to get ORDINAL_POSITION
        String sql = "SELECT " + //$NON-NLS-1$
            "COLUMN_NAME, " + //$NON-NLS-1$
            "DATA_TYPE, " + //$NON-NLS-1$
            "COLUMN_TYPE, " + //$NON-NLS-1$
            "IS_NULLABLE, " + //$NON-NLS-1$
            "COLUMN_KEY, " + //$NON-NLS-1$
            "COLUMN_DEFAULT, " + //$NON-NLS-1$
            "COLUMN_COMMENT, " + //$NON-NLS-1$
            "ORDINAL_POSITION " + //$NON-NLS-1$
            "FROM information_schema.columns " + //$NON-NLS-1$
            "WHERE table_schema = ? AND table_name = ? " + //$NON-NLS-1$
            "ORDER BY ORDINAL_POSITION"; //$NON-NLS-1$

        JDBCPreparedStatement stmt = session.prepareStatement(sql);
        stmt.setString(1, schemaName);
        stmt.setString(2, forTable.getName());
        return stmt;
    }

    @Override
    public GenericTableColumn fetchTableColumn(
        @NotNull JDBCSession session,
        @NotNull GenericStructContainer owner,
        @NotNull GenericTableBase table,
        @NotNull JDBCResultSet dbResult
    ) throws DBException {
        int ordinal = JDBCUtils.safeGetInt(dbResult, ORDINAL_POSITION);
        return new StarRocksTableColumn(table, dbResult, ordinal);
    }

    @Nullable
    @Override
    public String getViewDDL(
        @NotNull DBRProgressMonitor monitor,
        @NotNull GenericView sourceObject,
        @NotNull Map<String, Object> options
    ) throws DBException {
        return sourceObject.getDDL();
    }

    @Nullable
    @Override
    public String getTableDDL(
        @NotNull DBRProgressMonitor monitor,
        @NotNull GenericTableBase sourceObject,
        @NotNull Map<String, Object> options
    ) throws DBException {
        if (sourceObject instanceof StarRocksTable table) {
            return table.getObjectDefinitionText(monitor, options);
        }
        if (sourceObject instanceof StarRocksView view) {
            return view.getObjectDefinitionText(monitor, options);
        }
        if (sourceObject instanceof StarRocksMaterializedView materializedView) {
            return materializedView.getObjectDefinitionText(monitor, options);
        }
        return super.getTableDDL(monitor, sourceObject, options);
    }

    @Override
    public boolean isView(@NotNull String tableType) {
        String upperType = tableType.toUpperCase();
        return upperType.contains(TYPE_VIEW) || upperType.contains(TYPE_MATERIALIZED_VIEW);
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
}

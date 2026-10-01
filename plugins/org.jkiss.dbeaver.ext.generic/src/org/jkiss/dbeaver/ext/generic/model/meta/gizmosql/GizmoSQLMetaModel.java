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
package org.jkiss.dbeaver.ext.generic.model.meta.gizmosql;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericView;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.Map;

/**
 * GizmoSQL-specific metadata overrides. GizmoSQL is backed by Flight SQL, which has no standard
 * way to expose view DDL (Flight SQL defines metadata commands for tables/columns/primary keys
 * but not views). The server publishes the DDL through a reserved catalog view
 * {@code _gizmosql_system.main.gizmosql_view_definition} on recent versions; older servers don't
 * register it, so we fall back to querying {@code duckdb_views()} directly — the source the
 * catalog view wraps. If neither is available (typically the SQLite backend), we return an
 * informative stub rather than failing the view-DDL tab.
 */
public class GizmoSQLMetaModel extends GenericMetaModel {

    private static final String DDL_UNAVAILABLE = "-- View definition not available";
    private static final String DDL_UNSUPPORTED_SQLITE =
        "-- View DDL not supported on this GizmoSQL server: "
            + "duckdb_views() is unavailable (typically means the SQLite backend).";

    @Override
    public String getViewDDL(
        @NotNull DBRProgressMonitor monitor,
        @NotNull GenericView sourceObject,
        @NotNull Map<String, Object> options
    ) throws DBException {
        GenericCatalog catalog = sourceObject.getCatalog();
        String viewName = sourceObject.getName();
        String schemaName = sourceObject.getContainer().getName();
        String catalogName = catalog == null ? null : catalog.getName();

        try (JDBCSession session = DBUtils.openMetaSession(
            monitor, sourceObject, "Read GizmoSQL view definition")
        ) {
            String ddl = fetchFromSystemCatalog(session, catalogName, schemaName, viewName, sourceObject);
            if (ddl != null) {
                return ddl;
            }
            return fetchFromDuckdbViews(session, catalogName, schemaName, viewName, sourceObject);
        }
    }

    /**
     * Query the preferred {@code _gizmosql_system.main.gizmosql_view_definition} catalog view.
     *
     * @return the DDL text, {@link #DDL_UNAVAILABLE} when the row exists but is empty, or
     *     {@code null} when the server predates the catalog view (caller should fall through to
     *     the inline {@code duckdb_views()} path).
     */
    @Nullable
    private static String fetchFromSystemCatalog(
        @NotNull JDBCSession session,
        @Nullable String catalogName,
        @NotNull String schemaName,
        @NotNull String viewName,
        @NotNull GenericView sourceObject
    ) throws DBException {
        String sql = "SELECT \"VIEW_DEFINITION\" FROM _gizmosql_system.main.gizmosql_view_definition "
            + "WHERE \"TABLE_NAME\" = ? AND \"TABLE_SCHEM\" = ?"
            + (catalogName == null ? "" : " AND \"TABLE_CAT\" = ?");
        try {
            String ddl = catalogName == null
                ? JDBCUtils.queryString(session, sql, viewName, schemaName)
                : JDBCUtils.queryString(session, sql, viewName, schemaName, catalogName);
            return CommonUtils.isEmpty(ddl) ? DDL_UNAVAILABLE : ddl;
        } catch (SQLException e) {
            if (looksLikeMissingSystemCatalog(e)) {
                return null;
            }
            throw new DBDatabaseException(e, sourceObject.getDataSource());
        }
    }

    /**
     * Fall-back path: query {@code duckdb_views()} directly. Mirrors the catalog view definition
     * created at server startup — keep the two in sync if either changes.
     */
    @NotNull
    private static String fetchFromDuckdbViews(
        @NotNull JDBCSession session,
        @Nullable String catalogName,
        @NotNull String schemaName,
        @NotNull String viewName,
        @NotNull GenericView sourceObject
    ) throws DBException {
        String sql = "SELECT sql FROM duckdb_views() "
            + "WHERE view_name = ? AND schema_name = ?"
            + (catalogName == null ? "" : " AND database_name = ?");
        try {
            String ddl = catalogName == null
                ? JDBCUtils.queryString(session, sql, viewName, schemaName)
                : JDBCUtils.queryString(session, sql, viewName, schemaName, catalogName);
            return CommonUtils.isEmpty(ddl) ? DDL_UNAVAILABLE : ddl;
        } catch (SQLException e) {
            if (looksLikeDuckdbViewsMissing(e)) {
                return DDL_UNSUPPORTED_SQLITE;
            }
            throw new DBDatabaseException(e, sourceObject.getDataSource());
        }
    }

    private static boolean looksLikeMissingSystemCatalog(@NotNull SQLException e) {
        String msg = e.getMessage() == null ? "" : e.getMessage();
        return msg.contains("_gizmosql_system")
            || msg.contains("gizmosql_view_definition")
            || msg.contains("Catalog Error");
    }

    private static boolean looksLikeDuckdbViewsMissing(@NotNull SQLException e) {
        String msg = e.getMessage() == null ? "" : e.getMessage();
        return msg.contains("duckdb_views") || msg.contains("Catalog Error");
    }
}

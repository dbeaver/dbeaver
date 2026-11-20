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
package org.jkiss.dbeaver.ext.duckdb.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericExecutionContext;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionBootstrap;
import org.jkiss.dbeaver.model.exec.DBCCachedContextDefaults;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;

public class DuckDBExecutionContext extends GenericExecutionContext {

    private static final Log log = Log.getLog(DuckDBExecutionContext.class);
    private String activeCatalogName;
    private String activeSchemaName;
    private GenericSchema activeSchema;

    public DuckDBExecutionContext(@NotNull JDBCRemoteInstance instance, @NotNull String purpose) {
        super(instance, purpose);
    }

    @Override
    public boolean supportsCatalogChange() {
        return true;
    }

    @Override
    public boolean supportsSchemaChange() {
        return true;
    }

    @Nullable
    @Override
    public GenericCatalog getDefaultCatalog() {
        return activeCatalogName == null ? null : getDataSource().getCatalog(activeCatalogName);
    }

    @Nullable
    @Override
    public GenericSchema getDefaultSchema() {
        return activeSchema;
    }

    @Override
    public boolean refreshDefaults(@NotNull DBRProgressMonitor monitor, boolean useBootstrapSettings) throws DBException {
        // Bootstrap defaults are applied below, because the catalog must be switched before the schema
        boolean changed = super.refreshDefaults(monitor, false);

        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.META, "Query active catalog and schema")) {
            if (useBootstrapSettings) {
                DBPConnectionBootstrap bootstrap = getBootstrapSettings();
                try {
                    if (!CommonUtils.isEmpty(bootstrap.getDefaultCatalogName())) {
                        session.setCatalog(bootstrap.getDefaultCatalogName());
                    }
                    if (!CommonUtils.isEmpty(bootstrap.getDefaultSchemaName())) {
                        session.setSchema(bootstrap.getDefaultSchemaName());
                    }
                } catch (SQLException e) {
                    log.warn("Error applying default catalog and schema", e);
                }
            }
            // Read both every time: a USE statement in the SQL editor can change either of them
            String currentCatalog = session.getCatalog();
            String currentSchema = session.getSchema();
            if (!CommonUtils.equalObjects(currentCatalog, activeCatalogName)
                || !CommonUtils.equalObjects(currentSchema, activeSchemaName)
            ) {
                activeCatalogName = currentCatalog;
                activeSchemaName = currentSchema;
                changed = true;
            }
        } catch (SQLException e) {
            log.warn("Error reading active catalog and schema", e);
        }
        resolveDefaultSchema(monitor);

        return changed;
    }

    @Override
    public void setDefaultCatalog(@NotNull DBRProgressMonitor monitor, @NotNull GenericCatalog catalog, @Nullable GenericSchema schema) throws DBCException {
        if (catalog == null) {
            return;
        }

        String newCatalogName = catalog.getName();
        if (CommonUtils.equalObjects(activeCatalogName, newCatalogName)) {
            setActiveSchema(monitor, schema);
            return;
        }

        final GenericSchema oldSchema = getDefaultSchema();
        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.UTIL, "Set active catalog")) {
            JDBCUtils.executeSQL(session, "USE " + quoteIdentifier(newCatalogName));
            activeCatalogName = newCatalogName;
        } catch (Exception e) {
            try {
                throw new DBException("Failed to set active catalog to " + newCatalogName, e);
            } catch (DBException ex) {
                log.warn("Error setting active catalog", ex);
            }
        }

        super.setDefaultCatalog(monitor, catalog, schema);
        // Read the schema only now: the JDBC catalog switch above resets it to the catalog's default schema
        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.UTIL, TASK_TITLE_SET_SCHEMA)) {
            activeSchemaName = session.getSchema();
        } catch (SQLException e) {
            throw new DBCException(e, this);
        }
        resolveDefaultSchema(monitor);
        DBUtils.fireObjectSelectionChange(oldSchema, activeSchema, this);
        setActiveSchema(monitor, schema);
    }

    @Override
    public void setDefaultSchema(@NotNull DBRProgressMonitor monitor, @Nullable GenericSchema schema) throws DBCException {
        GenericCatalog catalog = schema == null ? null : schema.getCatalog();
        if (catalog != null && !catalog.getName().equals(activeCatalogName)) {
            // The schema belongs to another attached database, so switch the database first
            setDefaultCatalog(monitor, catalog, schema);
        } else {
            setActiveSchema(monitor, schema);
        }
    }

    private void setActiveSchema(@NotNull DBRProgressMonitor monitor, @Nullable GenericSchema schema) throws DBCException {
        if (schema == null) {
            return;
        }
        final GenericSchema oldSchema = activeSchema;
        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.UTIL, TASK_TITLE_SET_SCHEMA)) {
            // Apply the database too, because a USE in the SQL editor may have changed both without refreshing
            // the defaults. A single USE leaves the session as it was if the schema no longer exists.
            GenericCatalog catalog = schema.getCatalog();
            String catalogPrefix = catalog == null ? "" : quoteIdentifier(catalog.getName()) + ".";
            JDBCUtils.executeSQL(session, "USE " + catalogPrefix + quoteIdentifier(schema.getName()));
        } catch (SQLException e) {
            throw new DBCException(e, this);
        }
        activeSchemaName = schema.getName();
        activeSchema = schema;
        DBUtils.fireObjectSelectionChange(oldSchema, schema, this);
    }

    /**
     * Always quotes, unlike {@link DBUtils#getQuotedIdentifier}, which keeps names that already look quoted as is.
     */
    @NotNull
    private static String quoteIdentifier(@NotNull String name) {
        return '"' + name.replace("\"", "\"\"") + '"';
    }

    /**
     * Resolves the active schema object here, where a progress monitor is available, for {@link #getDefaultSchema()}.
     */
    void resolveDefaultSchema(@NotNull DBRProgressMonitor monitor) {
        GenericSchema schema = null;
        if (activeSchemaName != null) {
            GenericCatalog catalog = getDefaultCatalog();
            try {
                // No catalogs, e.g. with the omit-catalog driver setting: schemas live at the data source root
                schema = catalog == null
                    ? getDataSource().getSchema(activeSchemaName)
                    : catalog.getSchema(monitor, activeSchemaName);
            } catch (DBException e) {
                log.debug("Error reading active schema", e);
            }
        }
        activeSchema = schema;
    }

    @Override
    public void initDefaultsFrom(@NotNull DBRProgressMonitor monitor, @NotNull GenericExecutionContext context) {
        try {
            super.initDefaultsFrom(monitor, context);
        } catch (DBCException e) {
            log.warn("Error initializing defaults from context", e);
        }

        if (context instanceof DuckDBExecutionContext duckDBContext) {
            String schemaName = duckDBContext.activeSchemaName;
            this.activeCatalogName = duckDBContext.activeCatalogName;
            // The new connection starts in the catalog's default schema, even on reconnect of this same context
            this.activeSchemaName = null;
            try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.UTIL, TASK_TITLE_SET_SCHEMA)) {
                if (schemaName != null) {
                    session.setSchema(schemaName);
                }
                this.activeSchemaName = session.getSchema();
            } catch (SQLException e) {
                log.warn("Error restoring default schema", e);
            }
            resolveDefaultSchema(monitor);
        }
    }

    @NotNull
    @Override
    public DBCCachedContextDefaults getCachedDefault() {
        return new DBCCachedContextDefaults(activeCatalogName, activeSchemaName);
    }
}
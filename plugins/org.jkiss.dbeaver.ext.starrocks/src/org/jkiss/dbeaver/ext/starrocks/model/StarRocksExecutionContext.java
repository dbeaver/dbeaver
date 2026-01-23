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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.connection.DBPConnectionBootstrap;
import org.jkiss.dbeaver.model.exec.DBCCachedContextDefaults;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;

/**
 * StarRocks Execution Context - manages active catalog and database context.
 * Supports 2-level context: Catalog and Database (schema).
 */
public class StarRocksExecutionContext extends JDBCExecutionContext
        implements DBCExecutionContextDefaults<StarRocksCatalog, StarRocksDatabase> {

    private static final Log log = Log.getLog(StarRocksExecutionContext.class);

    private String activeCatalogName;
    private String activeDatabaseName;

    StarRocksExecutionContext(@NotNull JDBCRemoteInstance instance, @NotNull String purpose) {
        super(instance, purpose);
    }

    @NotNull
    @Override
    public StarRocksDataSource getDataSource() {
        return (StarRocksDataSource) super.getDataSource();
    }

    @NotNull
    @Override
    public StarRocksExecutionContext getContextDefaults() {
        return this;
    }

    @Nullable
    public String getActiveCatalogName() {
        return activeCatalogName;
    }

    public void setActiveCatalogName(@Nullable String activeCatalogName) {
        this.activeCatalogName = activeCatalogName;
    }

    @Nullable
    public String getActiveDatabaseName() {
        return activeDatabaseName;
    }

    public void setActiveDatabaseName(@Nullable String activeDatabaseName) {
        this.activeDatabaseName = activeDatabaseName;
    }

    @Nullable
    @Override
    public StarRocksCatalog getDefaultCatalog() {
        return CommonUtils.isEmpty(activeCatalogName) ? null : getDataSource().getCatalog(activeCatalogName); 
    }

    @Nullable
    @Override
    public StarRocksDatabase getDefaultSchema() {
        StarRocksCatalog catalog = getDefaultCatalog();
        if (catalog == null) {
            return null;
        }
        return CommonUtils.isEmpty(activeDatabaseName) ? null : catalog.getCachedDatabase(activeDatabaseName);
    }

    @Override
    public boolean supportsCatalogChange() {
        return true;
    }

    @Override
    public boolean supportsSchemaChange() {
        return true;
    }

    @Override
    public void setDefaultCatalog(
        @NotNull DBRProgressMonitor monitor,
        @Nullable StarRocksCatalog catalog,
        @Nullable StarRocksDatabase schema
    ) throws DBCException {
        if (catalog == null || isAlreadyActive(catalog, schema)) {
            return;
        }

        final StarRocksCatalog oldCatalog = getDefaultCatalog();
        final StarRocksDatabase oldSchema = getDefaultSchema();

        setCurrentCatalog(monitor, catalog.getName());
        activeCatalogName = catalog.getName();

        if (schema != null) {
            setCurrentDatabase(monitor, schema.getName());
            activeDatabaseName = schema.getName();
        }

        fireSelectionChangeEvents(oldCatalog, catalog, oldSchema, schema);
    }

    private boolean isAlreadyActive(@NotNull StarRocksCatalog catalog, @Nullable StarRocksDatabase schema) {
        String newCatalogName = catalog.getName();
        boolean catalogMatch = newCatalogName.equals(activeCatalogName);
        boolean schemaMatch = schema == null || schema.getName().equals(activeDatabaseName);
        return catalogMatch && schemaMatch;
    }

    private void fireSelectionChangeEvents(
        @Nullable StarRocksCatalog oldCatalog,
        @NotNull StarRocksCatalog newCatalog,
        @Nullable StarRocksDatabase oldSchema,
        @Nullable StarRocksDatabase newSchema
    ) {
        if (oldCatalog != null) {
            DBUtils.fireObjectSelectionChange(oldCatalog, newCatalog, this);
        }
        if (oldSchema != null && newSchema != null) {
            DBUtils.fireObjectSelectionChange(oldSchema, newSchema, this);
        }
    }

    @Override
    public void setDefaultSchema(@NotNull DBRProgressMonitor monitor, @Nullable StarRocksDatabase schema) throws DBCException {
        if (schema == null) {
            return;
        }

        // Ensure we're in the correct catalog first
        StarRocksCatalog catalog = schema.getCatalog();
        if (catalog != null && !catalog.getName().equals(activeCatalogName)) {
            setCurrentCatalog(monitor, catalog.getName());
            activeCatalogName = catalog.getName();
        }

        final StarRocksDatabase oldSchema = getDefaultSchema();

        if (!setCurrentDatabase(monitor, schema.getName())) {
            return;
        }
        activeDatabaseName = schema.getName();

        // Send notifications
        if (oldSchema != null) {
            DBUtils.fireObjectSelectionChange(oldSchema, schema, this);
        }
    }

    @Override
    public boolean refreshDefaults(@NotNull DBRProgressMonitor monitor, boolean useBootstrapSettings) throws DBException {
        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.META, "Query active catalog and database")) { //$NON-NLS-1$
            if (useBootstrapSettings) {
                DBPConnectionBootstrap bootstrap = getBootstrapSettings();
                if (!CommonUtils.isEmpty(bootstrap.getDefaultCatalogName())) {
                    setCurrentCatalog(monitor, bootstrap.getDefaultCatalogName());
                    activeCatalogName = bootstrap.getDefaultCatalogName();
                }
                if (!CommonUtils.isEmpty(bootstrap.getDefaultSchemaName())) {
                    setCurrentDatabase(monitor, bootstrap.getDefaultSchemaName());
                    activeDatabaseName = bootstrap.getDefaultSchemaName();
                }
            }

            // Get current catalog
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT CATALOG()")) { //$NON-NLS-1$
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        activeCatalogName = JDBCUtils.safeGetString(dbResult, 1);
                    }
                }
            } catch (SQLException e) {
                log.debug("Error getting current catalog", e); //$NON-NLS-1$
                // Default to default_catalog if we can't determine
                activeCatalogName = StarRocksDataSource.DEFAULT_CATALOG_NAME;
            }

            // Get current database
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT DATABASE()")) { //$NON-NLS-1$
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        activeDatabaseName = JDBCUtils.safeGetString(dbResult, 1);
                    }
                }
            } catch (SQLException e) {
                log.debug("Error getting current database", e); //$NON-NLS-1$
            }
        }

        return true;
    }

    private boolean setCurrentCatalog(@NotNull DBRProgressMonitor monitor, @NotNull String catalogName) throws DBCException {
        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.UTIL, "Set active catalog")) { //$NON-NLS-1$
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SET CATALOG " + DBUtils.getQuotedIdentifier(getDataSource(), catalogName))) { //$NON-NLS-1$
                dbStat.execute();
            } catch (SQLException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
            return true;
        }
    }

    private boolean setCurrentDatabase(@NotNull DBRProgressMonitor monitor, @NotNull String databaseName) throws DBCException {
        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.UTIL, "Set active database")) { //$NON-NLS-1$
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                    "USE " + DBUtils.getQuotedIdentifier(getDataSource(), databaseName))) { //$NON-NLS-1$
                dbStat.execute();
            } catch (SQLException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
            this.activeDatabaseName = databaseName;
            return true;
        }
    }

    @NotNull
    @Override
    public DBCCachedContextDefaults getCachedDefault() {
        return new DBCCachedContextDefaults(activeCatalogName, activeDatabaseName);
    }
}

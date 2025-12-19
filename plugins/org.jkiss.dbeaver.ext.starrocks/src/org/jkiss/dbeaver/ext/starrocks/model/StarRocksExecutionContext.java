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

    StarRocksExecutionContext(@NotNull JDBCRemoteInstance instance, String purpose) {
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

    public String getActiveCatalogName() {
        return activeCatalogName;
    }

    public void setActiveCatalogName(String activeCatalogName) {
        this.activeCatalogName = activeCatalogName;
    }

    public String getActiveDatabaseName() {
        return activeDatabaseName;
    }

    public void setActiveDatabaseName(String activeDatabaseName) {
        this.activeDatabaseName = activeDatabaseName;
    }

    @Override
    public StarRocksCatalog getDefaultCatalog() {
        return CommonUtils.isEmpty(activeCatalogName) ? null : getDataSource().getCatalog(activeCatalogName); 
    }

    @Override
    public StarRocksDatabase getDefaultSchema() {
        StarRocksCatalog catalog = getDefaultCatalog(); 
        if (catalog == null) {
            return null; 
        }
        return CommonUtils.isEmpty(activeDatabaseName) 
            ? null : 
            catalog.getDatabase(new org.jkiss.dbeaver.model.VoidProgressMonitor(), activeDatabaseName); 
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
    public void setDefaultCatalog(DBRProgressMonitor monitor, StarRocksCatalog catalog, StarRocksDatabase schema) throws DBCException {
        if (catalog == null) {
            return;
        }

        String newCatalogName = catalog.getName();
        if (newCatalogName.equals(activeCatalogName) && (schema == null || schema.getName().equals(activeDatabaseName))) {
            return;
        }

        final StarRocksCatalog oldCatalog = getDefaultCatalog();
        final StarRocksDatabase oldSchema = getDefaultSchema();

        // Set catalog
        if (!setCurrentCatalog(monitor, newCatalogName)) {
            return;
        }
        activeCatalogName = newCatalogName;

        // Set database/schema if provided
        if (schema != null) {
            if (!setCurrentDatabase(monitor, schema.getName())) {
                return;
            }
            activeDatabaseName = schema.getName();
        }

        // Send notifications
        if (oldCatalog != null) {
            DBUtils.fireObjectSelectionChange(oldCatalog, catalog, this);
        }
        if (oldSchema != null && schema != null) {
            DBUtils.fireObjectSelectionChange(oldSchema, schema, this);
        }
    }

    @Override
    public void setDefaultSchema(DBRProgressMonitor monitor, StarRocksDatabase schema) throws DBCException {
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
    public boolean refreshDefaults(DBRProgressMonitor monitor, boolean useBootstrapSettings) throws DBException {
        JDBCSession session = openSession(monitor, DBCExecutionPurpose.META, "Query active catalog and database");
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
        try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT CATALOG()")) {
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                if (dbResult.next()) {
                    activeCatalogName = JDBCUtils.safeGetString(dbResult, 1);
                }
            }
        } catch (SQLException e) {
            log.debug("Error getting current catalog", e);
            // Default to default_catalog if we can't determine
            activeCatalogName = StarRocksDataSource.DEFAULT_CATALOG_NAME;
        }

        // Get current database
        try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT DATABASE()")) {
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                if (dbResult.next()) {
                    activeDatabaseName = JDBCUtils.safeGetString(dbResult, 1);
                }
            }
        } catch (SQLException e) {
            log.debug("Error getting current database", e);
        }

        return true;
    }

    private boolean setCurrentCatalog(DBRProgressMonitor monitor, String catalogName) throws DBCException {
        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.UTIL, "Set active catalog")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SET CATALOG " + DBUtils.getQuotedIdentifier(getDataSource(), catalogName))) {
                dbStat.execute();
            } catch (SQLException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
            return true;
        }
    }

    private boolean setCurrentDatabase(DBRProgressMonitor monitor, String databaseName) throws DBCException {
        try (JDBCSession session = openSession(monitor, DBCExecutionPurpose.UTIL, "Set active database")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                    "USE " + DBUtils.getQuotedIdentifier(getDataSource(), databaseName))) {
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

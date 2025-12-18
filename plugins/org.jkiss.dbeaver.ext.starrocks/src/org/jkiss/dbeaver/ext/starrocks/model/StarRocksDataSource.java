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
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;

/**
 * StarRocks DataSource - extends JDBCDataSource directly to support 3-level hierarchy:
 * Catalog -> Database -> Table
 */
public class StarRocksDataSource extends JDBCDataSource implements DBPRefreshableObject {

    /**
     * Default catalog name in StarRocks (internal catalog).
     */
    public static final String DEFAULT_CATALOG_NAME = "default_catalog";

    private final CatalogCache catalogCache = new CatalogCache();

    public StarRocksDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container)
            throws DBException {
        super(monitor, container, new StarRocksDialect());
    }

    @Override
    protected JDBCExecutionContext createExecutionContext(JDBCRemoteInstance instance, String type) {
        return new StarRocksExecutionContext(instance, type);
    }

    @Override
    protected void initializeContextState(
            @NotNull DBRProgressMonitor monitor,
            @NotNull JDBCExecutionContext context,
            JDBCExecutionContext initFrom
    ) throws DBException {
        if (initFrom != null) {
            StarRocksExecutionContext starRocksContext = (StarRocksExecutionContext) context;
            StarRocksExecutionContext starRocksInitFrom = (StarRocksExecutionContext) initFrom;
            String activeCatalog = starRocksInitFrom.getActiveCatalogName();
            String activeDatabase = starRocksInitFrom.getActiveDatabaseName();
            if (!CommonUtils.isEmpty(activeCatalog)) {
                starRocksContext.setActiveCatalogName(activeCatalog);
            }
            if (!CommonUtils.isEmpty(activeDatabase)) {
                starRocksContext.setActiveDatabaseName(activeDatabase);
            }
        } else {
            ((StarRocksExecutionContext) context).refreshDefaults(monitor, true);
        }
    }

    // ======== DBPDataTypeProvider (required abstract methods) ========

    @Override
    public DBSDataType getLocalDataType(String typeName) {
        return null; // Uses JDBC metadata
    }

    @Override
    public Collection<? extends DBSDataType> getLocalDataTypes() {
        return Collections.emptyList();
    }

    // ======== Catalog/Database Navigation - 3-Level Hierarchy ========

    @Association
    public Collection<StarRocksCatalog> getCatalogs(DBRProgressMonitor monitor) throws DBException {
        return catalogCache.getAllObjects(monitor, this);
    }

    public StarRocksCatalog getCatalog(DBRProgressMonitor monitor, String name) throws DBException {
        return catalogCache.getObject(monitor, this, name);
    }

    public StarRocksCatalog getDefaultCatalog(DBRProgressMonitor monitor) throws DBException {
        return getCatalog(monitor, DEFAULT_CATALOG_NAME);
    }

    public boolean isDefaultCatalog(StarRocksCatalog catalog) {
        return DEFAULT_CATALOG_NAME.equalsIgnoreCase(catalog.getName());
    }

    // ======== DBSObjectContainer Implementation ========

    @Override
    public Collection<? extends DBSObject> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getCatalogs(monitor);
    }

    @Override
    public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException {
        // First try to find a catalog with this name
        StarRocksCatalog catalog = getCatalog(monitor, childName);
        if (catalog != null) {
            return catalog;
        }

        // If not found as catalog, search for database in default catalog
        StarRocksCatalog defaultCatalog = getDefaultCatalog(monitor);
        if (defaultCatalog != null) {
            StarRocksDatabase database = defaultCatalog.getDatabase(monitor, childName);
            if (database != null) {
                return database;
            }
        }

        // Search all catalogs for a database with this name
        for (StarRocksCatalog cat : getCatalogs(monitor)) {
            StarRocksDatabase database = cat.getDatabase(monitor, childName);
            if (database != null) {
                return database;
            }
        }

        return null;
    }

    @NotNull
    @Override
    public Class<? extends DBSObject> getPrimaryChildType(@Nullable DBRProgressMonitor monitor) throws DBException {
        return StarRocksCatalog.class;
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException {
        catalogCache.getAllObjects(monitor, this);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        catalogCache.clearCache();
        return this;
    }

    // ======== Catalog Cache ========

    class CatalogCache extends JDBCObjectCache<StarRocksDataSource, StarRocksCatalog> {
        @NotNull
        @Override
        protected JDBCPreparedStatement prepareObjectsStatement(
                @NotNull JDBCSession session,
                @NotNull StarRocksDataSource owner) throws SQLException {
            return session.prepareStatement("SHOW CATALOGS");
        }

        @NotNull
        @Override
        protected StarRocksCatalog fetchObject(
                @NotNull JDBCSession session,
                @NotNull StarRocksDataSource owner,
                @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new StarRocksCatalog(owner, resultSet);
        }
    }
}

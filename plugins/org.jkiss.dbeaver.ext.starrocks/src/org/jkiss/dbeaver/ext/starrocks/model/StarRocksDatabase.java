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
import org.jkiss.dbeaver.ext.starrocks.StarRocksDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;

import java.sql.SQLException;
import java.util.Collection;

/**
 * StarRocks Database - represents a database/schema within a StarRocks catalog.
 * Implements DBSSchema and DBSObjectContainer for proper hierarchy support.
 * Implements DBPQualifiedObject to provide catalog-aware fully qualified names.
 */
public class StarRocksDatabase implements DBSSchema, DBSObjectContainer, DBPQualifiedObject, DBPRefreshableObject {

    private static final Log log = Log.getLog(StarRocksDatabase.class);

    private final StarRocksCatalog catalog;
    private final String name;
    private final TableCache tableCache;

    public StarRocksDatabase(
            @NotNull StarRocksCatalog catalog,
            @NotNull JDBCResultSet resultSet) {
        this.catalog = catalog;
        this.name = JDBCUtils.safeGetString(resultSet, 1); // SHOW DATABASES returns single column
        this.tableCache = new TableCache();
        this.tableCache.setCaseSensitive(!getDataSource().getSQLDialect().useCaseInsensitiveNameLookup());
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public StarRocksCatalog getParentObject() {
        return catalog;
    }

    @NotNull
    @Override
    public StarRocksDataSource getDataSource() {
        return catalog.getDataSource();
    }

    public StarRocksCatalog getCatalog() {
        return catalog;
    }

    /**
     * Get the table cache for this database
     */
    public TableCache getTableCache() {
        return tableCache;
    }

    // ======== DBPQualifiedObject Implementation ========

    /**
     * Provides catalog-aware fully qualified names.
     * Format: catalog.database or `catalog`.`database`
     */
    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        switch (context) {
            case DML:
            case DDL:
                // For SQL contexts, include catalog name
                return DBUtils.getQuotedIdentifier(catalog) + "." + DBUtils.getQuotedIdentifier(this);
            default:
                // For UI contexts, just show database name
                return getName();
        }
    }

    // ======== Table Management ========

    @Association
    public Collection<StarRocksTable> getTables(DBRProgressMonitor monitor) throws DBException {
        return tableCache.getAllObjects(monitor, this);
    }

    public StarRocksTable getTable(DBRProgressMonitor monitor, String name) throws DBException {
        return tableCache.getObject(monitor, this, name);
    }

    // ======== DBSObjectContainer Implementation ========

    @Override
    public Collection<? extends DBSObject> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getTables(monitor);
    }

    @Override
    public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException {
        return getTable(monitor, childName);
    }

    @NotNull
    @Override
    public Class<? extends DBSObject> getPrimaryChildType(@Nullable DBRProgressMonitor monitor) throws DBException {
        return StarRocksTable.class;
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException {
        tableCache.getAllObjects(monitor, this);
        if ((scope & STRUCT_ATTRIBUTES) != 0) {
            tableCache.loadChildren(monitor, this, null);
        }
    }

    // ======== Refresh ========

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        tableCache.clearCache();
        return this;
    }

    // ======== Helper Methods ========

    /**
     * Switch to this database's catalog context before executing queries
     */
    void switchToCatalogContext(JDBCSession session) throws SQLException {
        String catalogName = catalog.getName();
        String useCatalogSQL = "SET CATALOG `" + catalogName + "`";
        try {
            session.getOriginal().createStatement().execute(useCatalogSQL);
        } catch (SQLException e) {
            log.debug("Error switching to catalog " + catalogName, e);
            throw e;
        }
    }

    // ======== Table Cache ========

    /**
     * Cache for tables within this database.
     * Uses JDBCStructCache to support both tables and their columns.
     */
    public class TableCache extends JDBCStructCache<StarRocksDatabase, StarRocksTable, StarRocksTableColumn> {

        protected TableCache() {
            super("Table");
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull StarRocksDatabase owner) throws SQLException {
            // Switch to correct catalog context
            switchToCatalogContext(session);

            // Query tables using SHOW FULL TABLES
            return session.prepareStatement("SHOW FULL TABLES FROM " + DBUtils.getQuotedIdentifier(owner));
        }

        @Override
        protected StarRocksTable fetchObject(@NotNull JDBCSession session, @NotNull StarRocksDatabase owner, @NotNull JDBCResultSet dbResult) throws SQLException, DBException {
            String tableName = JDBCUtils.safeGetString(dbResult, 1);
            String tableType = JDBCUtils.safeGetString(dbResult, 2);
            boolean isView = tableType != null && tableType.toUpperCase().contains("VIEW");
            return new StarRocksTable(owner, tableName, isView);
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull StarRocksDatabase owner, @Nullable StarRocksTable table) throws SQLException {
            // Switch to correct catalog context
            switchToCatalogContext(session);

            StringBuilder sql = new StringBuilder("SHOW FULL COLUMNS FROM ");
            if (table != null) {
                sql.append(DBUtils.getQuotedIdentifier(table));
                sql.append(" FROM ");
            }
            sql.append(DBUtils.getQuotedIdentifier(owner));

            return session.prepareStatement(sql.toString());
        }

        @Override
        protected StarRocksTableColumn fetchChild(@NotNull JDBCSession session, @NotNull StarRocksDatabase owner, @NotNull StarRocksTable table, @NotNull JDBCResultSet dbResult) throws SQLException, DBException {
            return new StarRocksTableColumn(table, dbResult);
        }
    }
}

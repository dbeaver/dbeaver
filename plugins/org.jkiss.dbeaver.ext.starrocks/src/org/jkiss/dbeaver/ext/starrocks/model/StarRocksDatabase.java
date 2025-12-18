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
import java.sql.Statement;
import java.util.Collection;

/**
 * StarRocks Database - represents a database/schema within a StarRocks catalog.
 */
public class StarRocksDatabase implements DBSSchema, DBSObjectContainer, DBPRefreshableObject {

    private final StarRocksCatalog catalog;
    private final String name;
    private final TableCache tableCache;

    public StarRocksDatabase(@NotNull StarRocksCatalog catalog, @NotNull JDBCResultSet resultSet) {
        this.catalog = catalog;
        this.name = JDBCUtils.safeGetString(resultSet, 1);
        this.tableCache = new TableCache();
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

    public TableCache getTableCache() {
        return tableCache;
    }

    @Association
    public Collection<StarRocksTable> getTables(DBRProgressMonitor monitor) throws DBException {
        return tableCache.getTypedObjects(monitor, this, StarRocksTable.class);
    }

    public StarRocksTable getTable(DBRProgressMonitor monitor, String name) throws DBException {
        return tableCache.getObject(monitor, this, name, StarRocksTable.class);
    }

    @Association
    public Collection<StarRocksView> getViews(DBRProgressMonitor monitor) throws DBException {
        return tableCache.getTypedObjects(monitor, this, StarRocksView.class);
    }

    public StarRocksView getView(DBRProgressMonitor monitor, String name) throws DBException {
        return tableCache.getObject(monitor, this, name, StarRocksView.class);
    }

    @Association
    public Collection<StarRocksMaterializedView> getMaterializedViews(DBRProgressMonitor monitor) throws DBException {
        return tableCache.getTypedObjects(monitor, this, StarRocksMaterializedView.class);
    }

    public StarRocksMaterializedView getMaterializedView(DBRProgressMonitor monitor, String name) throws DBException {
        return tableCache.getObject(monitor, this, name, StarRocksMaterializedView.class);
    }

    // DBSObjectContainer implementation
    @Override
    public Collection<? extends DBSObject> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException {
        return tableCache.getAllObjects(monitor, this);
    }

    @Override
    public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException {
        return tableCache.getObject(monitor, this, childName);
    }

    @NotNull
    @Override
    public Class<? extends DBSObject> getPrimaryChildType(@Nullable DBRProgressMonitor monitor) {
        return StarRocksTableBase.class;
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException {
        tableCache.getAllObjects(monitor, this);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        tableCache.clearCache();
        return this;
    }

    void switchToCatalogContext(JDBCSession session) throws SQLException {
        try (Statement stmt = session.getOriginal().createStatement()) {
            stmt.execute("SET CATALOG " + DBUtils.getQuotedIdentifier(getDataSource(), catalog.getName()));
        }
    }

    /**
     * Cache for tables and views within this database.
     * Uses JDBCStructCache to support both tables/views and their columns.
     */
    public class TableCache extends JDBCStructCache<StarRocksDatabase, StarRocksTableBase, StarRocksTableColumn> {

        protected TableCache() {
            super("Table");
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull StarRocksDatabase owner) throws SQLException {
            switchToCatalogContext(session);
            return session.prepareStatement("SHOW FULL TABLES FROM " + DBUtils.getQuotedIdentifier(owner));
        }

        @Override
        protected StarRocksTableBase fetchObject(@NotNull JDBCSession session, @NotNull StarRocksDatabase owner, @NotNull JDBCResultSet dbResult) throws SQLException, DBException {
            String tableName = JDBCUtils.safeGetString(dbResult, 1);
            String tableType = JDBCUtils.safeGetString(dbResult, 2);
            String tableTypeUpper = tableType != null ? tableType.toUpperCase() : "";

            if (tableTypeUpper.contains("MATERIALIZED VIEW")) {
                return new StarRocksMaterializedView(owner, tableName);
            } else if (tableTypeUpper.contains("VIEW")) {
                return new StarRocksView(owner, tableName);
            } else {
                return new StarRocksTable(owner, tableName);
            }
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull StarRocksDatabase owner, @Nullable StarRocksTableBase table) throws SQLException {
            if (table == null) {
                throw new SQLException("Cannot load columns without specifying a table");
            }
            switchToCatalogContext(session);
            String sql = "SHOW FULL COLUMNS FROM " + DBUtils.getQuotedIdentifier(table) +
                         " FROM " + DBUtils.getQuotedIdentifier(owner);
            return session.prepareStatement(sql);
        }

        @Override
        protected StarRocksTableColumn fetchChild(@NotNull JDBCSession session, @NotNull StarRocksDatabase owner, @NotNull StarRocksTableBase table, @NotNull JDBCResultSet dbResult) throws SQLException, DBException {
            return new StarRocksTableColumn(table, dbResult);
        }
    }
}

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
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;

/**
 * StarRocks Catalog - represents a catalog (e.g., default_catalog, hive_catalog)
 * Contains multiple databases (schemas)
 */
public class StarRocksCatalog implements DBSCatalog, DBPRefreshableObject {

    private final StarRocksDataSource dataSource;
    private final String name;
    private final String type;
    private final String comment;
    private final DatabaseCache databaseCache = new DatabaseCache();

    public StarRocksCatalog(
            @NotNull StarRocksDataSource dataSource,
            @NotNull JDBCResultSet resultSet) {
        this.dataSource = dataSource;
        this.name = JDBCUtils.safeGetString(resultSet, "Catalog");
        this.type = JDBCUtils.safeGetString(resultSet, "Type");
        this.comment = JDBCUtils.safeGetString(resultSet, "Comment");
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    @Property(viewable = true, order = 2)
    public String getType() {
        return type;
    }

    @Property(viewable = true, order = 3)
    public String getComment() {
        return comment;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @Nullable
    @Override
    public String getDescription() {
        return comment;
    }

    @Override
    public DBSObject getParentObject() {
        return dataSource;
    }

    @NotNull
    @Override
    public StarRocksDataSource getDataSource() {
        return dataSource;
    }

    @Association
    public Collection<StarRocksDatabase> getDatabases(DBRProgressMonitor monitor) throws DBException {
        return databaseCache.getAllObjects(monitor, this);
    }

    public StarRocksDatabase getDatabase(DBRProgressMonitor monitor, String name) throws DBException {
        return databaseCache.getObject(monitor, this, name);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        databaseCache.clearCache();
        return this;
    }

    @Override
    public Collection<? extends DBSObject> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getDatabases(monitor);
    }

    @Override
    public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException {
        return getDatabase(monitor, childName);
    }

    @NotNull
    @Override
    public Class<? extends DBSObject> getPrimaryChildType(@Nullable DBRProgressMonitor monitor) throws DBException {
        return StarRocksDatabase.class;
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException {
        databaseCache.getAllObjects(monitor, this);
    }

    /**
     * Cache for databases within this catalog
     */
    class DatabaseCache extends JDBCObjectCache<StarRocksCatalog, StarRocksDatabase> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(
                @NotNull JDBCSession session,
                @NotNull StarRocksCatalog owner) throws SQLException {
            // Switch to this catalog context
            try (Statement stmt = session.getOriginal().createStatement()) {
                stmt.execute("SET CATALOG " + DBUtils.getQuotedIdentifier(dataSource, name));
            }
            return session.prepareStatement("SHOW DATABASES");
        }

        @NotNull
        @Override
        protected StarRocksDatabase fetchObject(
                @NotNull JDBCSession session,
                @NotNull StarRocksCatalog owner,
                @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new StarRocksDatabase(owner, resultSet);
        }
    }
}

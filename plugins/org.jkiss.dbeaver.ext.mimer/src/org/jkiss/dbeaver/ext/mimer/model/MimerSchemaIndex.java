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
package org.jkiss.dbeaver.ext.mimer.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.SQLException;
import java.util.Collection;

/**
 * One row of the schema-wide "Indexes" summary - every index in the schema, across every table
 * in one flat list, read from {@code INFORMATION_SCHEMA.EXT_INDEXES WHERE INDEX_SCHEMA = ? AND
 * TABLE_NAME LIKE '%'} (a wildcard table pattern instead of one specific table). A read-only
 * summary for browsing every index in a schema without expanding each table individually - full
 * create/drop/alter support already exists on the
 * richer, per-table {@link MimerTableIndex} (via each table's own Indexes folder); this class is
 * deliberately a separate, lightweight, purpose-built leaf rather than resolving to those real
 * objects, since doing that here would need one extra per-table lookup per index found instead of
 * the single schema-wide query this uses.
 *
 * @author Mimer Information Technology
 */
public class MimerSchemaIndex implements DBSObject {

    private final MimerSchema schema;
    private final String indexName;
    private final String tableName;
    private final boolean unique;
    private final ColumnCache columnCache = new ColumnCache();
    private MimerAccessPath accessPath;
    private boolean accessPathResolved;

    public MimerSchemaIndex(@NotNull MimerSchema schema, @NotNull JDBCResultSet dbResult) {
        this.schema = schema;
        this.indexName = JDBCUtils.safeGetString(dbResult, "INDEX_NAME");
        this.tableName = JDBCUtils.safeGetString(dbResult, "TABLE_NAME");
        this.unique = "YES".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "IS_UNIQUE"));
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return indexName;
    }

    @Property(viewable = true, order = 2)
    public String getTableName() {
        return tableName;
    }

    @Property(viewable = true, order = 3)
    public boolean isUnique() {
        return unique;
    }

    /**
     * 11.1+ only (reads {@code false} pre-11.1, same as {@link MimerTableIndex#isClustered}) -
     * resolved via the owning table's own {@link MimerAccessPath} rather than a second, separate
     * {@code EXT_ACCESS_PATHS} query, to avoid re-deriving that view's tricky version-gated
     * column history (see {@link MimerTable#getAccessPaths}) a second time.
     */
    @Property(viewable = true, order = 4)
    public boolean isClustered(@NotNull DBRProgressMonitor monitor) throws DBException {
        MimerAccessPath path = resolveAccessPath(monitor);
        return path != null && path.isClustered();
    }

    /**
     * 11.1+ only - reuses {@link MimerSchema#getIndexExtInfo}, the same schema-wide bulk-loaded
     * lookup {@link MimerTableIndex#isIgnoreNulls} already relies on.
     */
    @Property(viewable = true, order = 5)
    public boolean isIgnoreNulls(@NotNull DBRProgressMonitor monitor) throws DBException {
        return schema.getIndexExtInfo(monitor, tableName, indexName).ignoreNulls();
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    /**
     * Resolves this index's real {@link MimerAccessPath} (found by name among the owning table's
     * own access paths) - the source of {@link #isClustered} and each {@link
     * MimerSchemaIndexColumn}'s key/included status. Lazy and cached: only loaded the first time
     * a caller actually asks for one of those properties (e.g. double-clicking one index in the
     * schema-wide list), not for the whole flat list up front.
     */
    @Nullable
    synchronized MimerAccessPath resolveAccessPath(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (!accessPathResolved) {
            accessPathResolved = true;
            GenericTableBase table = schema.getTable(monitor, tableName);
            if (table instanceof MimerTable mimerTable) {
                for (MimerAccessPath path : mimerTable.getAccessPaths(monitor)) {
                    if (indexName.equalsIgnoreCase(path.getName())) {
                        accessPath = path;
                        break;
                    }
                }
            }
        }
        return accessPath;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @Override
    public DBSObject getParentObject() {
        return schema;
    }

    @NotNull
    @Override
    public MimerDataSource getDataSource() {
        return (MimerDataSource) schema.getDataSource();
    }

    @Association
    public Collection<MimerSchemaIndexColumn> getColumns(@NotNull DBRProgressMonitor monitor) throws DBException {
        return columnCache.getAllObjects(monitor, this);
    }

    static class ColumnCache extends JDBCObjectCache<MimerSchemaIndex, MimerSchemaIndexColumn> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerSchemaIndex index) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT COLUMN_NAME, ORDINAL_POSITION, IS_ASCENDING\n" +
                "FROM INFORMATION_SCHEMA.EXT_INDEX_COLUMN_USAGE\n" +
                "WHERE INDEX_SCHEMA = ? AND INDEX_NAME = ?\n" +
                "ORDER BY ORDINAL_POSITION");
            dbStat.setString(1, index.schema.getName());
            dbStat.setString(2, index.indexName);
            return dbStat;
        }

        @Override
        protected MimerSchemaIndexColumn fetchObject(@NotNull JDBCSession session, @NotNull MimerSchemaIndex index, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerSchemaIndexColumn(index, resultSet);
        }
    }
}

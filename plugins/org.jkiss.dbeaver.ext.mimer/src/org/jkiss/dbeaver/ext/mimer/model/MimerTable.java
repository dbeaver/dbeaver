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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTable;
import org.jkiss.dbeaver.ext.mimer.MimerConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * {@code GenericTable} + a {@link MimerObjectPrivilege.PrivilegeCache} for its "Privileges"
 * folder (SELECT/INSERT/UPDATE/DELETE grants) - see {@link
 * org.jkiss.dbeaver.ext.mimer.model.MimerMetaModel#createTableOrViewImpl} - plus the "Access
 * Path" folder (see {@link MimerAccessPath}) and, alongside it, filtering of the plain
 * "Indexes" folder (see {@link #isStandaloneIndex}).
 *
 * @author Mimer Information Technology
 */
public class MimerTable extends GenericTable {

    private static final Log log = Log.getLog(MimerTable.class);

    private final MimerObjectPrivilege.PrivilegeCache privilegeCache = new MimerObjectPrivilege.PrivilegeCache();
    private final UsedByCache usedByCache = new UsedByCache();
    private final UsesCache usesCache = new UsesCache();
    private List<MimerAccessPath> accessPaths;
    private String pendingDatabank;

    public MimerTable(
        @NotNull GenericStructContainer container,
        @Nullable String tableName,
        @Nullable String tableType,
        @Nullable JDBCResultSet dbResult
    ) {
        super(container, tableName, tableType, dbResult);
    }

    @Association
    public Collection<MimerObjectPrivilege> getPrivileges(DBRProgressMonitor monitor) throws DBException {
        return privilegeCache.getAllObjects(monitor, this);
    }

    /**
     * The databank this table's rows are stored in ({@code CREATE TABLE ... IN <databank>}).
     * Editable while the table is being created, read-only afterwards - Mimer SQL has no
     * {@code ALTER TABLE ... SET DATABANK}. For an existing table it's derived from the same
     * {@code EXT_TABLE_DATABANK_USAGE} rows the "Uses" folder shows ({@link #getUses}), so it
     * rides that folder's own cache rather than a second one; joined with {@code ", "} in the
     * (not seen live) case of more than one. See {@link
     * org.jkiss.dbeaver.ext.mimer.edit.MimerTableManager#appendTableModifiers} for the create side.
     */
    @Property(viewable = true, editable = true, order = 50, listProvider = DatabankListProvider.class)
    @Nullable
    public String getDatabank(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (!isPersisted()) {
            return pendingDatabank;
        }
        List<String> databanks = new ArrayList<>();
        for (MimerObjectUses use : getUses(monitor)) {
            if ("DATABANK".equalsIgnoreCase(use.getObjectType()) && use.getObjectName() != null) {
                databanks.add(use.getObjectName());
            }
        }
        return databanks.isEmpty() ? null : String.join(", ", databanks);
    }

    public void setDatabank(String databank) {
        this.pendingDatabank = databank;
    }

    /**
     * The base class only ever offers {@link DBSIndexType#OTHER}, which leaves the "Create New
     * Index" dialog's Type combo permanently disabled (a single-entry combo is greyed out).
     * Offers {@link DBSIndexType#CLUSTERED} too once {@link
     * MimerDataSource#supportsClusteredIndexes} is true - see {@link
     * org.jkiss.dbeaver.ext.mimer.edit.MimerIndexManager} for where the chosen type becomes
     * {@code CREATE [UNIQUE] CLUSTERED INDEX ...}.
     */
    @Override
    public Collection<DBSIndexType> getTableIndexTypes() {
        if (getDataSource() instanceof MimerDataSource ds && ds.supportsClusteredIndexes()) {
            return List.of(DBSIndexType.OTHER, DBSIndexType.CLUSTERED);
        }
        return super.getTableIndexTypes();
    }

    public MimerObjectPrivilege.PrivilegeCache getPrivilegeCache() {
        return privilegeCache;
    }

    @Association
    public synchronized Collection<MimerAccessPath> getAccessPaths(@NotNull DBRProgressMonitor monitor) {
        loadAccessPaths(monitor);
        return accessPaths;
    }

    /**
     * Whether the driver's {@code getIndexInfo()} entry named {@code indexName} is a real,
     * user-created index rather than the internal structure backing a primary or foreign key
     * (see {@link MimerAccessPath} and {@link MimerMetaModel#createIndexImpl}). Fails open
     * (returns {@code true}) if {@code EXT_ACCESS_PATHS} can't be read or doesn't mention the
     * name, to avoid hiding a genuine index over an unrelated catalog issue.
     */
    synchronized boolean isStandaloneIndex(@NotNull DBRProgressMonitor monitor, @NotNull String indexName) {
        loadAccessPaths(monitor);
        for (MimerAccessPath path : accessPaths) {
            if (path.getName() != null && path.getName().equalsIgnoreCase(indexName)) {
                return path.isStandaloneIndex();
            }
        }
        return true;
    }

    private void loadAccessPaths(@NotNull DBRProgressMonitor monitor) {
        if (accessPaths != null) {
            return;
        }
        List<MimerAccessPath> result = new ArrayList<>();
        // IS_CLUSTERED and IS_KEY_COLUMN are both 11.1+-only columns on EXT_ACCESS_PATHS, even
        // though the view itself is older - so we leave them out of the query pre-11.1.
        // MimerAccessPath/MimerAccessPathColumn check the same two version predicates before
        // reading either column, so a pre-11.1 server never hits a missing-column error for
        // them. IS_KEY_COLUMN is the current name (still evolving in 11.1) - see
        // MimerAccessPathColumn for the history.
        MimerDataSource ds = getDataSource() instanceof MimerDataSource mds ? mds : null;
        boolean clustered = ds == null || ds.supportsClusteredIndexes();
        boolean included = ds == null || ds.supportsIndexInclude();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read Mimer SQL access paths")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT INDEX_NAME, INDEX_TYPE, INDEX_ALGORITHM," + (clustered ? " IS_CLUSTERED,\n" : "\n") +
                "       COLUMN_NAME, ORDINAL_POSITION, COLUMN_SOURCE," + (included ? " IS_KEY_COLUMN,\n" : "\n") +
                "       COLLATION_SCHEMA, COLLATION_NAME\n" +
                "FROM INFORMATION_SCHEMA.EXT_ACCESS_PATHS\n" +
                "WHERE INDEX_SCHEMA = ? AND TABLE_NAME = ?\n" +
                "ORDER BY INDEX_NAME, ORDINAL_POSITION")
            ) {
                dbStat.setString(1, getSchema().getName());
                dbStat.setString(2, getName());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    MimerAccessPath current = null;
                    while (dbResult.next()) {
                        String name = dbResult.getString("INDEX_NAME");
                        if (current == null || !current.getName().equalsIgnoreCase(name)) {
                            current = new MimerAccessPath(this, dbResult);
                            result.add(current);
                        }
                        current.addColumn(dbResult);
                    }
                }
            }
        } catch (SQLException | DBException e) {
            // INFORMATION_SCHEMA.EXT_ACCESS_PATHS not readable on this server (e.g. an
            // unverified older Mimer SQL version) - fail open, see #isStandaloneIndex.
            log.debug("Error loading Mimer SQL access paths for '" + getName() + "'", e);
            result = Collections.emptyList();
        }
        accessPaths = result;
    }

    /**
     * Other database objects that depend on this table - an index counts as using its table, in
     * addition to whatever {@code EXT_OBJECT_OBJECT_USED} itself reports (views, triggers,
     * foreign keys, ...).
     */
    @Association
    public Collection<MimerObjectUsedBy> getUsedBy(@NotNull DBRProgressMonitor monitor) throws DBException {
        return usedByCache.getAllObjects(monitor, this);
    }

    /**
     * Other database objects this table depends on - the databank(s) it's stored in, in addition
     * to whatever {@code EXT_OBJECT_OBJECT_USING} itself reports (domains used by its columns,
     * sequences used by column defaults, ...).
     */
    @Association
    public Collection<MimerObjectUses> getUses(@NotNull DBRProgressMonitor monitor) throws DBException {
        return usesCache.getAllObjects(monitor, this);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        privilegeCache.clearCache();
        usedByCache.clearCache();
        usesCache.clearCache();
        accessPaths = null;
        return super.refreshObject(monitor);
    }

    /**
     * User databanks for the "Databank" property's dropdown - system databanks
     * ({@link MimerConstants#SYSTEM_DATABANKS}) aren't valid table storage. Stays typeable
     * ({@link #allowCustomValue()}) for a name the picker can't see yet.
     */
    public static class DatabankListProvider implements IPropertyValueListProvider<MimerTable> {
        @Override
        public boolean allowCustomValue() {
            return true;
        }

        @Override
        public Object[] getPossibleValues(MimerTable object) {
            if (!(object.getDataSource() instanceof MimerDataSource ds)) {
                return new Object[0];
            }
            try {
                return ds.getDatabanks(new VoidProgressMonitor()).stream()
                    .map(MimerDatabank::getName)
                    .filter(name -> !MimerConstants.SYSTEM_DATABANKS.contains(name))
                    .toArray();
            } catch (DBException e) {
                log.debug("Can't load databank list for the table Databank property", e);
                return new Object[0];
            }
        }
    }

    static class UsedByCache extends JDBCObjectCache<MimerTable, MimerObjectUsedBy> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerTable owner) throws SQLException {
            JDBCPreparedStatement stmt = session.prepareStatement(
                "SELECT USING_OBJECT_SCHEMA, USING_OBJECT_NAME, USING_OBJECT_TYPE\n" +
                "FROM INFORMATION_SCHEMA.EXT_OBJECT_OBJECT_USED\n" +
                "WHERE USED_OBJECT_SCHEMA = ? AND USED_OBJECT_NAME = ? AND USED_OBJECT_TYPE = 'BASE TABLE'\n" +
                "UNION\n" +
                "SELECT INDEX_SCHEMA, INDEX_NAME, 'INDEX'\n" +
                "FROM INFORMATION_SCHEMA.EXT_INDEXES\n" +
                "WHERE INDEX_SCHEMA = ? AND TABLE_NAME = ?\n" +
                "ORDER BY 1, 2");
            stmt.setString(1, owner.getSchema().getName());
            stmt.setString(2, owner.getName());
            stmt.setString(3, owner.getSchema().getName());
            stmt.setString(4, owner.getName());
            return stmt;
        }

        @Override
        protected MimerObjectUsedBy fetchObject(@NotNull JDBCSession session, @NotNull MimerTable owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerObjectUsedBy(owner, resultSet);
        }
    }

    static class UsesCache extends JDBCObjectCache<MimerTable, MimerObjectUses> {
        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MimerTable owner) throws SQLException {
            JDBCPreparedStatement stmt = session.prepareStatement(
                "SELECT USED_OBJECT_SCHEMA, USED_OBJECT_NAME, USED_OBJECT_TYPE\n" +
                "FROM INFORMATION_SCHEMA.EXT_OBJECT_OBJECT_USING\n" +
                "WHERE USING_OBJECT_SCHEMA = ? AND USING_OBJECT_NAME = ? AND USING_OBJECT_TYPE = 'BASE TABLE'\n" +
                "UNION ALL\n" +
                "SELECT DATABANK_CREATOR, DATABANK_NAME, 'DATABANK'\n" +
                "FROM INFORMATION_SCHEMA.EXT_TABLE_DATABANK_USAGE\n" +
                "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?");
            stmt.setString(1, owner.getSchema().getName());
            stmt.setString(2, owner.getName());
            stmt.setString(3, owner.getSchema().getName());
            stmt.setString(4, owner.getName());
            return stmt;
        }

        @Override
        protected MimerObjectUses fetchObject(@NotNull JDBCSession session, @NotNull MimerTable owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new MimerObjectUses(owner, resultSet);
        }
    }
}

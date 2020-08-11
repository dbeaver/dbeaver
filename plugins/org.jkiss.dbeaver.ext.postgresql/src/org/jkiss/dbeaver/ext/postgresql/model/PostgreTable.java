/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDPseudoAttribute;
import org.jkiss.dbeaver.model.data.DBDPseudoAttributeContainer;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBStructUtils;
import org.jkiss.dbeaver.model.struct.cache.SimpleObjectCache;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * PostgreTable
 */
public abstract class PostgreTable extends PostgreTableReal implements PostgreTableContainer, DBDPseudoAttributeContainer
{
    private static final Log log = Log.getLog(PostgreTable.class);

    private SimpleObjectCache<PostgreTable, PostgreTableForeignKey> foreignKeys = new SimpleObjectCache<>();
    //private List<PostgreTablePartition>  partitions  = null;

    private boolean hasOids;
    private long tablespaceId;
    private List<PostgreTableInheritance> superTables;
    private List<PostgreTableInheritance> subTables;
    private boolean hasSubClasses;

    private boolean hasPartitions;
    private String partitionKey;
    private String partitionRange;

    public PostgreTable(PostgreTableContainer container)
    {
        super(container);
    }

    public PostgreTable(
        PostgreTableContainer container,
        ResultSet dbResult)
    {
        super(container, dbResult);

        this.hasOids = JDBCUtils.safeGetBoolean(dbResult, "relhasoids");
        this.tablespaceId = JDBCUtils.safeGetLong(dbResult, "reltablespace");
        this.hasSubClasses = JDBCUtils.safeGetBoolean(dbResult, "relhassubclass");

        this.partitionKey = getDataSource().isServerVersionAtLeast(10, 0) ? JDBCUtils.safeGetString(dbResult, "partition_key")  : null;
        this.hasPartitions = this.partitionKey != null;
    }

    // Copy constructor
    public PostgreTable(DBRProgressMonitor monitor, PostgreTableContainer container, PostgreTable source, boolean persisted) throws DBException {
        super(monitor, container, source, persisted);
        this.hasOids = source.hasOids;
        this.tablespaceId = container == source.getContainer() ? source.tablespaceId : 0;

        this.partitionKey = source.partitionKey;

/*
        // Copy FKs
        List<PostgreTableForeignKey> fkList = new ArrayList<>();
        for (PostgreTableForeignKey srcFK : CommonUtils.safeCollection(source.getForeignKeys(monitor))) {
            PostgreTableForeignKey fk = new PostgreTableForeignKey(monitor, this, srcFK);
            if (fk.getReferencedConstraint() != null) {
                fk.setName(fk.getName() + "_copy"); // Fix FK name - they are unique within schema
                fkList.add(fk);
            } else {
                log.debug("Can't copy association '" + srcFK.getName() + "' - can't find referenced constraint");
            }
        }
        this.foreignKeys.setCache(fkList);
*/
    }

    public SimpleObjectCache<PostgreTable, PostgreTableForeignKey> getForeignKeyCache() {
        return foreignKeys;
    }

    public boolean isTablespaceSpecified() {
        return tablespaceId != 0;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 20, listProvider = TablespaceListProvider.class)
    public PostgreTablespace getTablespace(DBRProgressMonitor monitor) throws DBException {
        if (tablespaceId == 0) {
            return getDatabase().getDefaultTablespace(monitor);
        }
        return PostgreUtils.getObjectById(monitor, getDatabase().tablespaceCache, getDatabase(), tablespaceId);
    }

    public void setTablespace(PostgreTablespace tablespace) {
        this.tablespaceId = tablespace.getObjectId();
    }

    @Override
    public boolean isView()
    {
        return false;
    }

    @Property(editable = true, updatable = true, order = 40)
    public boolean isHasOids() {
        return hasOids;
    }

    public void setHasOids(boolean hasOids) {
        this.hasOids = hasOids;
    }

    @Property(viewable = true, order = 42)
    public boolean hasPartitions() {
        return hasPartitions;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 43)
    public String getPartitionKey() {
        return partitionKey;
    }

    public void setPartitionKey(String partitionKey) {
        this.partitionKey = partitionKey;
    }

    @Override
    protected void fetchStatistics(JDBCResultSet dbResult) throws DBException, SQLException {
        super.fetchStatistics(dbResult);
        if (diskSpace != null && diskSpace == 0 && hasSubClasses) {
            // Prefetch partitions (shouldn't be too expensive, we already have all tables in cache)
            getPartitions(dbResult.getSession().getProgressMonitor());
        }
    }

    @Override
    public long getStatObjectSize() {
        if (diskSpace != null && subTables != null) {
            long partSizeSum = diskSpace;
            for (PostgreTableInheritance ti : subTables) {
                PostgreTableBase partTable = ti.getParentObject();
                if (partTable.isPartition() && partTable instanceof PostgreTableReal) {
                    partSizeSum += ((PostgreTableReal) partTable).getStatObjectSize();
                }
            }
            return partSizeSum;
        }
        return super.getStatObjectSize();
    }

    @Override
    public Collection<PostgreIndex> getIndexes(DBRProgressMonitor monitor) throws DBException {
        return getSchema().indexCache.getObjects(monitor, getSchema(), this);
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        return DBStructUtils.generateTableDDL(monitor, this, options, false);
    }

    @Override
    public DBDPseudoAttribute[] getPseudoAttributes() {
        if (this.hasOids && getDataSource().getServerType().supportsOids()) {
            return new DBDPseudoAttribute[]{PostgreConstants.PSEUDO_ATTR_OID};
        } else {
            return null;
        }
    }

    @Association
    @Override
    public synchronized Collection<? extends DBSEntityAssociation> getAssociations(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        final List<PostgreTableInheritance> superTables = getSuperInheritance(monitor);
        final Collection<PostgreTableForeignKey> foreignKeys = getForeignKeys(monitor);
        if (CommonUtils.isEmpty(superTables)) {
            return foreignKeys;
        } else if (CommonUtils.isEmpty(foreignKeys)) {
            return superTables;
        }
        List<DBSEntityAssociation> agg = new ArrayList<>(superTables.size() + foreignKeys.size());
        agg.addAll(superTables);
        agg.addAll(foreignKeys);
        return agg;
    }

    @Override
    public Collection<? extends DBSEntityAssociation> getReferences(@NotNull DBRProgressMonitor monitor) throws DBException {
        List<DBSEntityAssociation> refs = new ArrayList<>(
            CommonUtils.safeList(getSubInheritance(monitor)));
        // This is dummy implementation
        // Get references from this schema only
        final Collection<PostgreTableForeignKey> allForeignKeys =
            getContainer().getSchema().constraintCache.getTypedObjects(monitor, getContainer(), PostgreTableForeignKey.class);
        for (PostgreTableForeignKey constraint : allForeignKeys) {
            if (constraint.getAssociatedEntity() == this) {
                refs.add(constraint);
            }
        }
        return refs;
    }

    @Association
    public Collection<PostgreTableForeignKey> getForeignKeys(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getSchema().constraintCache.getTypedObjects(monitor, getSchema(), this, PostgreTableForeignKey.class);
    }

    @Nullable
    @Property(viewable = false, optional = true, order = 30)
    public List<PostgreTableBase> getSuperTables(DBRProgressMonitor monitor) throws DBException {
        final List<PostgreTableInheritance> si = getSuperInheritance(monitor);
        if (CommonUtils.isEmpty(si)) {
            return null;
        }
        List<PostgreTableBase> result = new ArrayList<>(si.size());
        for (int i1 = 0; i1 < si.size(); i1++) {
            result.add(si.get(i1).getAssociatedEntity());
        }
        return result;
    }

    /**
     * Sub tables = child tables
     */
    @Nullable
    @Property(viewable = false, optional = true, order = 31)
    public List<PostgreTableBase> getSubTables(DBRProgressMonitor monitor) throws DBException {
        final List<PostgreTableInheritance> si = getSubInheritance(monitor);
        if (CommonUtils.isEmpty(si)) {
            return null;
        }
        List<PostgreTableBase> result = new ArrayList<>(si.size());
        for (PostgreTableInheritance aSi : si) {
            PostgreTableBase table = aSi.getParentObject();
            if (!table.isPartition()) {
                result.add(table);
            }
        }
        return result;
    }

    @Nullable
    public List<PostgreTableInheritance> getSuperInheritance(DBRProgressMonitor monitor) throws DBException {
        if (superTables == null && getDataSource().getServerType().supportsInheritance()) {
            superTables = initSuperTables(monitor);
        }
        return superTables == null || superTables.isEmpty() ? null : superTables;
    }

    private List<PostgreTableInheritance> initSuperTables(DBRProgressMonitor monitor) throws DBException {
        List<PostgreTableInheritance> inheritanceList = new ArrayList<>();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table inheritance info")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT i.*,c.relnamespace " +
                "FROM pg_catalog.pg_inherits i,pg_catalog.pg_class c " +
                "WHERE i.inhrelid=? AND c.oid=i.inhparent " +
                "ORDER BY i.inhseqno")) {
                dbStat.setLong(1, getObjectId());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        final long parentSchemaId = JDBCUtils.safeGetLong(dbResult, "relnamespace");
                        final long parentTableId = JDBCUtils.safeGetLong(dbResult, "inhparent");
                        PostgreSchema schema = getDatabase().getSchema(monitor, parentSchemaId);
                        if (schema == null) {
                            log.warn("Can't find parent table's schema '" + parentSchemaId + "'");
                            continue;
                        }
                        PostgreTableBase parentTable = schema.getTable(monitor, parentTableId);
                        if (parentTable == null) {
                            log.warn("Can't find parent table '" + parentTableId + "' in '" + schema.getName() + "'");
                            continue;
                        }
                        inheritanceList.add(
                            new PostgreTableInheritance(
                                this,
                                parentTable,
                                JDBCUtils.safeGetInt(dbResult, "inhseqno"),
                                true));
                    }
                }
                return inheritanceList;
            } catch (SQLException e) {
                throw new DBCException(e, session.getExecutionContext());
            }
        }
    }

    @Nullable
    public String getPartitionRange(DBRProgressMonitor monitor) throws DBException {
        if (partitionRange == null && getDataSource().getServerType().supportsInheritance()) {
            try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table partition range")) {
                try (JDBCPreparedStatement dbStat = session.prepareStatement(
                        "select pg_get_expr(c.relpartbound, c.oid, true) as partition_range from \"pg_catalog\".pg_class c where relname = ? and relnamespace = ?;")) { //$NON-NLS-1$
                    dbStat.setString(1, getName());
                    dbStat.setLong(2, getSchema().oid);
                    try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                        dbResult.next();
                        partitionRange = JDBCUtils.safeGetString(dbResult, "partition_range"); //$NON-NLS-1$
                    }
                } catch (SQLException e) {
                    throw new DBCException(e, session.getExecutionContext());
                }
            }
        }
        return partitionRange;
    }

    public boolean hasSubClasses() {
        return hasSubClasses;
    }

    @Nullable
    public List<PostgreTableInheritance> getSubInheritance(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (subTables == null && hasSubClasses && getDataSource().getServerType().supportsInheritance()) {
            List<PostgreTableInheritance> tables = new ArrayList<>();
            try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table inheritance info")) {
                String sql = "SELECT i.*,c.relnamespace " +
                    "FROM pg_catalog.pg_inherits i,pg_catalog.pg_class c " +
                    "WHERE i.inhparent=? AND c.oid=i.inhrelid";
//                if (getDataSource().isServerVersionAtLeast(10, 0)) {
//                    sql += " AND c.relispartition=false";
//                }
                try (JDBCPreparedStatement dbStat = session.prepareStatement(sql)) {
                    dbStat.setLong(1, getObjectId());
                    try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                        while (dbResult.next()) {
                            final long subSchemaId = JDBCUtils.safeGetLong(dbResult, "relnamespace"); //$NON-NLS-1$
                            final long subTableId = JDBCUtils.safeGetLong(dbResult, "inhrelid"); //$NON-NLS-1$
                            PostgreSchema schema = getDatabase().getSchema(monitor, subSchemaId);
                            if (schema == null) {
                                log.warn("Can't find sub-table's schema '" + subSchemaId + "'");
                                continue;
                            }
                            PostgreTableBase subTable = schema.getTable(monitor, subTableId);
                            if (subTable == null) {
                                log.warn("Can't find sub-table '" + subTableId + "' in '" + schema.getName() + "'");
                                continue;
                            }
                            tables.add(
                                new PostgreTableInheritance(
                                    subTable,
                                    this,
                                    JDBCUtils.safeGetInt(dbResult, "inhseqno"),//$NON-NLS-1$
                                    true));
                        }
                    }
                } catch (SQLException e) {
                    throw new DBCException(e, session.getExecutionContext());
                }
            }
            DBUtils.orderObjects(tables);
            this.subTables = tables;
        }
        return subTables == null || subTables.isEmpty() ? null : subTables;
    }

    @Nullable
    @Association
    public Collection<PostgreTableBase> getPartitions(DBRProgressMonitor monitor) throws DBException {
        final List<PostgreTableInheritance> si = getSubInheritance(monitor);
        if (CommonUtils.isEmpty(si)) {
            return null;
        }
        List<PostgreTableBase> result = new ArrayList<>(si.size());
        for (int i1 = 0; i1 < si.size(); i1++) {
            PostgreTableBase table = si.get(i1).getParentObject();
            if (table.isPartition()) {
                result.add(table);
            }
        }
        return result;
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        superTables = null;
        subTables = null;
        return super.refreshObject(monitor);
    }
}

/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.kingbase.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.kingbase.KingbaseConstants;
import org.jkiss.dbeaver.ext.kingbase.KingbaseUtils;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDPseudoAttribute;
import org.jkiss.dbeaver.model.data.DBDPseudoAttributeContainer;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableConstraint;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.IPropertyValueValidator;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.DBSEntityConstrainable;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintInfo;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBStructUtils;
import org.jkiss.dbeaver.model.struct.cache.SimpleObjectCache;
import org.jkiss.utils.CommonUtils;

/**
 * KingbaseTable
 */
public abstract class KingbaseTable extends KingbaseTableReal
    implements KingbaseTableContainer, DBDPseudoAttributeContainer, DBSEntityConstrainable
{
    private static final Log log = Log.getLog(KingbaseTable.class);

    private final SimpleObjectCache<KingbaseTable, KingbaseTableForeignKey> foreignKeys = new SimpleObjectCache<>();

    private boolean hasOids;
    private long tablespaceId;
    private List<KingbaseTableInheritance> superTables;
    private List<KingbaseTableInheritance> subTables;
    private boolean hasSubClasses;

    private boolean hasPartitions;
    private boolean hasRowLevelSecurity;
    private String partitionKey;
    private String partitionRange;

    public KingbaseTable(KingbaseTableContainer container)
    {
        super(container);
    }

    public KingbaseTable(
        KingbaseTableContainer container,
        ResultSet dbResult)
    {
        super(container, dbResult);

        if (getDataSource().getServerType().supportsHasOidsColumn()) {
            this.hasOids = JDBCUtils.safeGetBoolean(dbResult, "relhasoids");
        }
        this.tablespaceId = JDBCUtils.safeGetLong(dbResult, "reltablespace");
        this.hasSubClasses = JDBCUtils.safeGetBoolean(dbResult, "relhassubclass");

        this.partitionKey = JDBCUtils.safeGetString(dbResult, "partition_key");
        this.hasPartitions = this.partitionKey != null;
        this.hasRowLevelSecurity = getDataSource().getServerType().supportsRowLevelSecurity()
            && JDBCUtils.safeGetBoolean(dbResult, "relrowsecurity");
    }
    public KingbaseTable(DBRProgressMonitor monitor, KingbaseTableContainer container, KingbaseTable source, boolean persisted) throws DBException {
        super(monitor, container, source, persisted);
        this.hasOids = source.hasOids;
        this.tablespaceId = container == source.getContainer() ? source.tablespaceId : 0;

        this.partitionKey = source.partitionKey;

        KingbaseSchema.IndexCache indexCache = getSchema().getIndexCache();
        if (indexCache != null) {
            for (KingbaseIndex srcIndex : CommonUtils.safeCollection(source.getIndexes(monitor))) {
                if (srcIndex.isPrimaryKeyIndex()) {
                    continue;
                }
                KingbaseIndex constr = new KingbaseIndex(monitor, this, srcIndex);
                indexCache.cacheObject(constr);
            }
        }
    }

    public SimpleObjectCache<KingbaseTable, KingbaseTableForeignKey> getForeignKeyCache() {
        return foreignKeys;
    }

    public boolean isTablespaceSpecified() {
        return tablespaceId != 0;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 20, listProvider = TablespaceListProvider.class)
    public KingbaseTablespace getTablespace(DBRProgressMonitor monitor) throws DBException {
        if (tablespaceId == 0) {
            return getDatabase().getDefaultTablespace(monitor);
        }
        return KingbaseUtils.getObjectById(monitor, getDatabase().tablespaceCache, getDatabase(), tablespaceId);
    }

    public void setTablespace(KingbaseTablespace tablespace) {
        this.tablespaceId = tablespace.getObjectId();
    }

    @Override
    public boolean isView()
    {
        return false;
    }

    @Property(editable = true, updatable = true, order = 40, visibleIf = KingbaseColumnHasOidsValidator.class)
    public boolean isHasOids() {
        return hasOids;
    }

    public void setHasOids(boolean hasOids) {
        this.hasOids = hasOids;
    }

    @Property(viewable = true, updatable = true, order = 41, visibleIf = KingbaseColumnHasRowLevelSecurity.class)
    public boolean isHasRowLevelSecurity() {
        return hasRowLevelSecurity;
    }

    public void setHasRowLevelSecurity(boolean hasRowLevelSecurity) {
        this.hasRowLevelSecurity = hasRowLevelSecurity;
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
            getPartitions(dbResult.getSession().getProgressMonitor());
        }
    }

    @Override
    public long getStatObjectSize() {
        if (diskSpace != null && subTables != null) {
            long partSizeSum = diskSpace;
            for (KingbaseTableInheritance ti : subTables) {
                KingbaseTableBase partTable = ti.getParentObject();
                if (partTable.isPartition() && partTable instanceof KingbaseTableReal) {
                    partSizeSum += ((KingbaseTableReal) partTable).getStatObjectSize();
                }
            }
            return partSizeSum;
        }
        return super.getStatObjectSize();
    }

    @Override
    public Collection<KingbaseIndex> getIndexes(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (!getDataSource().getServerType().supportsIndexes()) {
            return Collections.emptyList();
        }
        return getSchema().getIndexes(monitor, this);
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        return DBStructUtils.generateTableDDL(monitor, this, options, false);
    }

    @Override
    public DBDPseudoAttribute[] getPseudoAttributes() {
        if (this.hasOids && getDataSource().getServerType().supportsOids()) {
            return new DBDPseudoAttribute[]{KingbaseConstants.PSEUDO_ATTR_OID};
        } else {
            return null;
        }
    }

    @Association
    @Override
    public synchronized Collection<? extends DBSEntityAssociation> getAssociations(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {

        final List<KingbaseTableInheritance> superTables = getSuperInheritance(monitor);
        final Collection<KingbaseTableForeignKey> foreignKeys = getForeignKeys(monitor);
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
        if (monitor == null) {
            return null;
        }
        List<DBSEntityAssociation> refs = new ArrayList<>(
            CommonUtils.safeList(getSubInheritance(monitor)));
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read referencing schemas")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement("SELECT DISTINCT connamespace FROM sys_catalog.sys_constraint WHERE confrelid=?")) {
                dbStat.setLong(1, getObjectId());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        final long schemaId = JDBCUtils.safeGetLong(dbResult, 1);
                        final KingbaseSchema schema = getContainer().getDatabase().getSchema(monitor, schemaId);
                        if (schema == null) {
                            continue;
                        }
                        final Collection<KingbaseTableForeignKey> allForeignKeys =
                            schema.getConstraintCache().getTypedObjects(monitor, schema, KingbaseTableForeignKey.class);
                        for (KingbaseTableForeignKey constraint : allForeignKeys) {
                            if (constraint.getAssociatedEntity() == this) {
                                refs.add(constraint);
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                throw new DBDatabaseException(e, getDataSource());
            }
        }
        return refs;
    }

    @Association
    public Collection<KingbaseTableForeignKey> getForeignKeys(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getSchema().getConstraintCache().getTypedObjects(monitor, getSchema(), this, KingbaseTableForeignKey.class);
    }

    @Nullable
    @Property(viewable = false, optional = true, order = 30)
    public List<KingbaseTableBase> getSuperTables(DBRProgressMonitor monitor) throws DBException {
        final List<KingbaseTableInheritance> si = getSuperInheritance(monitor);
        if (CommonUtils.isEmpty(si)) {
            return null;
        }
        List<KingbaseTableBase> result = new ArrayList<>(si.size());
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
    public List<KingbaseTableBase> getSubTables(DBRProgressMonitor monitor) throws DBException {
        final List<KingbaseTableInheritance> si = getSubInheritance(monitor);
        if (CommonUtils.isEmpty(si)) {
            return null;
        }
        List<KingbaseTableBase> result = new ArrayList<>(si.size());
        for (KingbaseTableInheritance aSi : si) {
            KingbaseTableBase table = aSi.getParentObject();
            if (!table.isPartition()) {
                result.add(table);
            }
        }
        return result;
    }

    @Nullable
    public List<KingbaseTableInheritance> getSuperInheritance(DBRProgressMonitor monitor) throws DBException {
        if (superTables == null && getDataSource().getServerType().supportsInheritance() && isPersisted() && monitor != null) {
            superTables = initSuperTables(monitor);
        }
        return superTables == null || superTables.isEmpty() ? null : superTables;
    }

    void addSuperTableInheritance(KingbaseTableBase superTable, int seqNum) {
        KingbaseTableInheritance inheritance = new KingbaseTableInheritance(this, superTable, seqNum, true);
        if (superTables == null) {
            superTables = new ArrayList<>();
        }
        superTables.add(inheritance);
    }

    void nullifyEmptySuperTableInheritance() {
        if (superTables == null) {
            superTables = new ArrayList<>();
        }
    }

    void resetSuperInheritance() {
        superTables = null;
    }

    private List<KingbaseTableInheritance> initSuperTables(DBRProgressMonitor monitor) throws DBException {
        List<KingbaseTableInheritance> inheritanceList = new ArrayList<>();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table inheritance info")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT i.*,c.relnamespace " +
                "FROM sys_catalog.sys_inherits i,sys_catalog.sys_class c " +
                "WHERE i.inhrelid=? AND c.oid=i.inhparent " +
                "ORDER BY i.inhseqno")) {
                dbStat.setLong(1, getObjectId());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        final long parentSchemaId = JDBCUtils.safeGetLong(dbResult, "relnamespace");
                        final long parentTableId = JDBCUtils.safeGetLong(dbResult, "inhparent");
                        KingbaseSchema schema = getDatabase().getSchema(monitor, parentSchemaId);
                        if (schema == null) {
                            log.warn("Can't find parent table's schema '" + parentSchemaId + "'");
                            continue;
                        }
                        KingbaseTableBase parentTable = schema.getTable(monitor, parentTableId);
                        if (parentTable == null) {
                            log.warn("Can't find parent table '" + parentTableId + "' in '" + schema.getName() + "'");
                            continue;
                        }
                        inheritanceList.add(
                            new KingbaseTableInheritance(
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
                        "select sys_get_expr(c.relpartbound, c.oid, true) as partition_range from \"sys_catalog\".sys_class c where relname = ? and relnamespace = ?;")) { //$NON-NLS-1$
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
    public List<KingbaseTableInheritance> getSubInheritance(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (isPersisted() && subTables == null && hasSubClasses && getDataSource().getServerType().supportsInheritance()) {
            List<KingbaseTableInheritance> tables = new ArrayList<>();
            try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table inheritance info")) {
                String sql = "SELECT i.*,c.relnamespace " +
                    "FROM sys_catalog.sys_inherits i,sys_catalog.sys_class c " +
                    "WHERE i.inhparent=? AND c.oid=i.inhrelid";
                try (JDBCPreparedStatement dbStat = session.prepareStatement(sql)) {
                    dbStat.setLong(1, getObjectId());
                    try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                        while (dbResult.next()) {
                            final long subSchemaId = JDBCUtils.safeGetLong(dbResult, "relnamespace"); //$NON-NLS-1$
                            final long subTableId = JDBCUtils.safeGetLong(dbResult, "inhrelid"); //$NON-NLS-1$
                            KingbaseSchema schema = getDatabase().getSchema(monitor, subSchemaId);
                            if (schema == null) {
                                log.warn("Can't find sub-table's schema '" + subSchemaId + "'");
                                continue;
                            }
                            KingbaseTableBase subTable = schema.getTable(monitor, subTableId);
                            if (subTable == null) {
                                log.warn("Can't find sub-table '" + subTableId + "' in '" + schema.getName() + "'");
                                continue;
                            }
                            tables.add(
                                new KingbaseTableInheritance(
                                    subTable,
                                    this,
                                    JDBCUtils.safeGetInt(dbResult, "inhseqno"),
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
    public List<KingbaseTableBase> getPartitions(DBRProgressMonitor monitor) throws DBException {
        final List<KingbaseTableInheritance> si = getSubInheritance(monitor);
        if (CommonUtils.isEmpty(si)) {
            return null;
        }
        return si.stream()
            .map(AbstractTableConstraint::getParentObject)
            .filter(KingbaseTableBase::isPartition)
            .collect(Collectors.toList());
    }

    @Override
    public boolean supportsObjectDefinitionOption(String option) {
        if (hasPartitions && DBPScriptObject.OPTION_INCLUDE_PARTITIONS.equals(option)) {
            return true;
        }
        return super.supportsObjectDefinitionOption(option);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        superTables = null;
        subTables = null;
        return super.refreshObject(monitor);
    }

    @NotNull
    @Override
    public List<DBSEntityConstraintInfo> getSupportedConstraints() {
        return List.of(
            DBSEntityConstraintInfo.of(DBSEntityConstraintType.PRIMARY_KEY, KingbaseTableConstraint.class),
            DBSEntityConstraintInfo.of(DBSEntityConstraintType.UNIQUE_KEY, KingbaseTableConstraint.class),
            DBSEntityConstraintInfo.of(DBSEntityConstraintType.INDEX, KingbaseIndex.class),
            DBSEntityConstraintInfo.of(DBSEntityConstraintType.CHECK, KingbaseTableConstraint.class)
        );
    }

    public static class KingbaseColumnHasOidsValidator implements IPropertyValueValidator<KingbaseTable, Object> {

        @Override
        public boolean isValidValue(KingbaseTable object, Object value) throws IllegalArgumentException {
            return object.getDataSource().getServerType().supportsHasOidsColumn();
        }
    }

    public static class KingbaseColumnHasRowLevelSecurity implements IPropertyValueValidator<KingbaseTable, Object> {
        @Override
        public boolean isValidValue(KingbaseTable object, Object value) throws IllegalArgumentException {
            return object.getDataSource().getServerType().supportsRowLevelSecurity();
        }
    }
}

/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.*;
import org.jkiss.dbeaver.model.impl.DBSObjectCache;
import org.jkiss.dbeaver.model.impl.SimpleObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * MySQLTable
 */
public class MySQLTable extends MySQLTableBase
{
    private static final Log log = Log.getLog(MySQLTable.class);

    private static final String INNODB_COMMENT = "InnoDB free";
    public static final String CATEGORY_STATISTICS = "Statistics";

    public static class AdditionalInfo {
        private volatile boolean loaded = false;
        private long rowCount;
        private long autoIncrement;
        private String description;
        private Date createTime, updateTime, checkTime;
        private MySQLCharset charset;
        private MySQLCollation collation;
        private MySQLEngine engine;
        private long avgRowLength;
        private long dataLength;
        private long maxDataLength;
        private long dataFree;
        private long indexLength;
        private String rowFormat;

        @Property(viewable = true, editable = true, updatable = true, listProvider = EngineListProvider.class, order = 3) public MySQLEngine getEngine() { return engine; }
        @Property(viewable = true, editable = true, updatable = true, order = 4) public long getAutoIncrement() { return autoIncrement; }
        @Property(viewable = false, editable = true, updatable = true, listProvider = CharsetListProvider.class, order = 5) public MySQLCharset getCharset() { return charset; }
        @Property(viewable = false, editable = true, updatable = true, listProvider = CollationListProvider.class, order = 6) public MySQLCollation getCollation() { return collation; }
        @Property(viewable = true, editable = true, updatable = true, order = 100) public String getDescription() { return description; }

        @Property(category = CATEGORY_STATISTICS, viewable = true, order = 10) public long getRowCount() { return rowCount; }
        @Property(category = CATEGORY_STATISTICS, viewable = false, order = 11) public long getAvgRowLength() { return avgRowLength; }
        @Property(category = CATEGORY_STATISTICS, viewable = true, order = 12) public long getDataLength() { return dataLength; }
        @Property(category = CATEGORY_STATISTICS, viewable = false, order = 13) public long getMaxDataLength() { return maxDataLength; }
        @Property(category = CATEGORY_STATISTICS, viewable = false, order = 14) public long getDataFree() { return dataFree; }
        @Property(category = CATEGORY_STATISTICS, viewable = false, order = 15) public long getIndexLength() { return indexLength; }
        @Property(category = CATEGORY_STATISTICS, viewable = false, order = 16) public String getRowFormat() { return rowFormat; }

        @Property(category = CATEGORY_STATISTICS, viewable = false, order = 20) public Date getCreateTime() { return createTime; }
        @Property(category = CATEGORY_STATISTICS, viewable = false, order = 21) public Date getUpdateTime() { return updateTime; }
        @Property(category = CATEGORY_STATISTICS, viewable = false, order = 22) public Date getCheckTime() { return checkTime; }

        public void setEngine(MySQLEngine engine) { this.engine = engine; }
        public void setAutoIncrement(long autoIncrement) { this.autoIncrement = autoIncrement; }
        public void setDescription(String description) { this.description = description; }

        public void setCharset(MySQLCharset charset) { this.charset = charset; this.collation = charset == null ? null : charset.getDefaultCollation(); }
        public void setCollation(MySQLCollation collation) { this.collation = collation; }
    }

    public static class AdditionalInfoValidator implements IPropertyCacheValidator<MySQLTable> {
        @Override
        public boolean isPropertyCached(MySQLTable object, Object propertyId)
        {
            return object.additionalInfo.loaded;
        }
    }

    private final SimpleObjectCache<MySQLTable, MySQLTableForeignKey> foreignKeys = new SimpleObjectCache<>();
    private final PartitionCache partitionCache = new PartitionCache();

    private final AdditionalInfo additionalInfo = new AdditionalInfo();

    public MySQLTable(MySQLCatalog catalog)
    {
        super(catalog);
    }

    // Copy constructor
    public MySQLTable(DBRProgressMonitor monitor, MySQLCatalog catalog, DBSEntity source) throws DBException {
        super(monitor, catalog, source);
        if (source instanceof MySQLTable) {
            AdditionalInfo sourceAI = ((MySQLTable)source).getAdditionalInfo(monitor);
            additionalInfo.loaded = true;
            additionalInfo.description = sourceAI.description;
            additionalInfo.charset = sourceAI.charset;
            additionalInfo.collation = sourceAI.collation;
            additionalInfo.engine = sourceAI.engine;

            // Copy triggers
            for (MySQLTrigger srcTrigger : ((MySQLTable) source).getTriggers(monitor)) {
                MySQLTrigger trigger = new MySQLTrigger(catalog, this, srcTrigger);
                getContainer().triggerCache.cacheObject(trigger);
            }
            // Copy partitions
            for (MySQLPartition partition : ((MySQLTable)source).partitionCache.getCachedObjects()) {
                partitionCache.cacheObject(new MySQLPartition(monitor, this, partition));
            }
        }
        if (source instanceof DBSTable) {
            // Copy indexes
            for (DBSTableIndex srcIndex : CommonUtils.safeCollection(((DBSTable)source).getIndexes(monitor))) {
                if (srcIndex instanceof MySQLTableIndex && srcIndex.getName().equals(MySQLConstants.INDEX_PRIMARY)) {
                    // Skip primary key index (it will be created implicitly)
                    continue;
                }
                MySQLTableIndex index = new MySQLTableIndex(monitor, this, srcIndex);
                this.getContainer().indexCache.cacheObject(index);
            }
        }

        // Copy constraints
        for (DBSEntityConstraint srcConstr : CommonUtils.safeCollection(source.getConstraints(monitor))) {
            MySQLTableConstraint constr = new MySQLTableConstraint(monitor, this, srcConstr);
            this.getContainer().constraintCache.cacheObject(constr);
        }

        // Copy FKs
        List<MySQLTableForeignKey> fkList = new ArrayList<>();
        for (DBSEntityAssociation srcFK : CommonUtils.safeCollection(source.getAssociations(monitor))) {
            MySQLTableForeignKey fk = new MySQLTableForeignKey(monitor, this, srcFK);
            if (fk.getReferencedConstraint() != null) {
                fk.setName(fk.getName() + "_copy"); // Fix FK name - they are unique within schema
                fkList.add(fk);
            } else {
                log.debug("Can't copy association '" + srcFK.getName() + "' - can't find referenced constraint");
            }
        }
        this.foreignKeys.setCache(fkList);
    }

    public MySQLTable(
        MySQLCatalog catalog,
        ResultSet dbResult)
    {
        super(catalog, dbResult);
    }

    @PropertyGroup()
    @LazyProperty(cacheValidator = AdditionalInfoValidator.class)
    public AdditionalInfo getAdditionalInfo(DBRProgressMonitor monitor) throws DBCException
    {
        synchronized (additionalInfo) {
            if (!additionalInfo.loaded) {
                loadAdditionalInfo(monitor);
            }
            return additionalInfo;
        }
    }

    @Override
    public boolean isView()
    {
        return false;
    }

    @Override
    @Association
    public synchronized Collection<MySQLTableIndex> getIndexes(DBRProgressMonitor monitor)
        throws DBException
    {
        // Read indexes using cache
        return this.getContainer().indexCache.getObjects(monitor, getContainer(), this);
    }

    @Nullable
    @Override
    @Association
    public synchronized Collection<MySQLTableConstraint> getConstraints(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return getContainer().constraintCache.getObjects(monitor, getContainer(), this);
    }

    public MySQLTableConstraint getConstraint(DBRProgressMonitor monitor, String ukName)
        throws DBException
    {
        return getContainer().constraintCache.getObject(monitor, getContainer(), this, ukName);
    }

    @Override
    @Association
    public Collection<MySQLTableForeignKey> getReferences(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return loadForeignKeys(monitor, true);
    }

    @Override
    public synchronized Collection<MySQLTableForeignKey> getAssociations(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        if (!foreignKeys.isFullyCached()) {
            List<MySQLTableForeignKey> fkList = loadForeignKeys(monitor, false);
            foreignKeys.setCache(fkList);
        }
        return foreignKeys.getCachedObjects();
    }

    public MySQLTableForeignKey getAssociation(DBRProgressMonitor monitor, String fkName)
        throws DBException
    {
        return DBUtils.findObject(getAssociations(monitor), fkName);
    }

    public DBSObjectCache<MySQLTable, MySQLTableForeignKey> getForeignKeyCache()
    {
        return foreignKeys;
    }

    @Association
    public Collection<MySQLTrigger> getTriggers(DBRProgressMonitor monitor)
        throws DBException
    {
        List<MySQLTrigger> triggers = new ArrayList<>();
        for (MySQLTrigger trigger : getContainer().triggerCache.getAllObjects(monitor, getContainer())) {
            if (trigger.getTable() == this) {
                triggers.add(trigger);
            }
        }
        return triggers;
    }

    @Association
    public Collection<MySQLPartition> getPartitions(DBRProgressMonitor monitor)
        throws DBException
    {
        return partitionCache.getAllObjects(monitor, this);
    }

    private void loadAdditionalInfo(DBRProgressMonitor monitor) throws DBCException
    {
        if (!isPersisted()) {
            additionalInfo.loaded = true;
            return;
        }
        MySQLDataSource dataSource = getDataSource();
        try (JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Load table status")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SHOW TABLE STATUS FROM " + DBUtils.getQuotedIdentifier(getContainer()) + " LIKE '" + getName() + "'")) {
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    if (dbResult.next()) {
                        // filer table description (for INNODB it contains some system information)
                        String desc = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_TABLE_COMMENT);
                        if (desc != null) {
                            if (desc.startsWith(INNODB_COMMENT)) {
                                desc = "";
                            } else if (!CommonUtils.isEmpty(desc)) {
                                int divPos = desc.indexOf("; " + INNODB_COMMENT);
                                if (divPos != -1) {
                                    desc = desc.substring(0, divPos);
                                }
                            }
                            additionalInfo.description = desc;
                        }
                        additionalInfo.engine = dataSource.getEngine(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_ENGINE));
                        additionalInfo.rowCount = JDBCUtils.safeGetLong(dbResult, MySQLConstants.COL_TABLE_ROWS);
                        additionalInfo.autoIncrement = JDBCUtils.safeGetLong(dbResult, MySQLConstants.COL_AUTO_INCREMENT);
                        additionalInfo.createTime = JDBCUtils.safeGetTimestamp(dbResult, MySQLConstants.COL_CREATE_TIME);
                        additionalInfo.updateTime = JDBCUtils.safeGetTimestamp(dbResult, "Update_time");
                        additionalInfo.checkTime = JDBCUtils.safeGetTimestamp(dbResult, "Check_time");
                        additionalInfo.collation = dataSource.getCollation(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_COLLATION));
                        if (additionalInfo.collation != null) {
                            additionalInfo.charset = additionalInfo.collation.getCharset();
                        }
                        additionalInfo.avgRowLength = JDBCUtils.safeGetLong(dbResult, MySQLConstants.COL_AVG_ROW_LENGTH);
                        additionalInfo.dataLength = JDBCUtils.safeGetLong(dbResult, MySQLConstants.COL_DATA_LENGTH);
                        additionalInfo.maxDataLength = JDBCUtils.safeGetLong(dbResult, "Max_data_length");
                        additionalInfo.dataFree = JDBCUtils.safeGetLong(dbResult, "Data_free");
                        additionalInfo.indexLength = JDBCUtils.safeGetLong(dbResult, "Index_length");
                        additionalInfo.rowFormat = JDBCUtils.safeGetString(dbResult, "Row_format");
                    }
                    additionalInfo.loaded = true;
                }
            }
        } catch (SQLException e) {
            throw new DBCException(e, dataSource);
        }
    }

    private List<MySQLTableForeignKey> loadForeignKeys(DBRProgressMonitor monitor, boolean references)
        throws DBException
    {
        List<MySQLTableForeignKey> fkList = new ArrayList<>();
        if (!isPersisted()) {
            return fkList;
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, getDataSource(), "Load table relations")) {
            Map<String, MySQLTableForeignKey> fkMap = new HashMap<>();
            Map<String, MySQLTableConstraint> pkMap = new HashMap<>();
            JDBCDatabaseMetaData metaData = session.getMetaData();
            // Load indexes
            JDBCResultSet dbResult;
            if (references) {
                dbResult = metaData.getExportedKeys(
                    getContainer().getName(),
                    null,
                    getName());
            } else {
                dbResult = metaData.getImportedKeys(
                    getContainer().getName(),
                    null,
                    getName());
            }
            try {
                while (dbResult.next()) {
                    String pkTableCatalog = JDBCUtils.safeGetString(dbResult, JDBCConstants.PKTABLE_CAT);
                    String pkTableName = JDBCUtils.safeGetString(dbResult, JDBCConstants.PKTABLE_NAME);
                    String pkColumnName = JDBCUtils.safeGetString(dbResult, JDBCConstants.PKCOLUMN_NAME);
                    String fkTableCatalog = JDBCUtils.safeGetString(dbResult, JDBCConstants.FKTABLE_CAT);
                    String fkTableName = JDBCUtils.safeGetString(dbResult, JDBCConstants.FKTABLE_NAME);
                    String fkColumnName = JDBCUtils.safeGetString(dbResult, JDBCConstants.FKCOLUMN_NAME);
                    int keySeq = JDBCUtils.safeGetInt(dbResult, JDBCConstants.KEY_SEQ);
                    int updateRuleNum = JDBCUtils.safeGetInt(dbResult, JDBCConstants.UPDATE_RULE);
                    int deleteRuleNum = JDBCUtils.safeGetInt(dbResult, JDBCConstants.DELETE_RULE);
                    String fkName = JDBCUtils.safeGetString(dbResult, JDBCConstants.FK_NAME);
                    String pkName = JDBCUtils.safeGetString(dbResult, JDBCConstants.PK_NAME);

                    DBSForeignKeyModifyRule deleteRule = JDBCUtils.getCascadeFromNum(deleteRuleNum);
                    DBSForeignKeyModifyRule updateRule = JDBCUtils.getCascadeFromNum(updateRuleNum);

                    MySQLTable pkTable = getDataSource().findTable(monitor, pkTableCatalog, pkTableName);
                    if (pkTable == null) {
                        log.debug("Can't find PK table " + pkTableName);
                        if (references) {
                            continue;
                        }
                    }
                    MySQLTable fkTable = getDataSource().findTable(monitor, fkTableCatalog, fkTableName);
                    if (fkTable == null) {
                        log.warn("Can't find FK table " + fkTableName);
                        if (!references) {
                            continue;
                        }
                    }
                    MySQLTableColumn pkColumn = pkTable == null ? null : pkTable.getAttribute(monitor, pkColumnName);
                    if (pkColumn == null) {
                        log.debug("Can't find PK table " + pkTableName + " column " + pkColumnName);
                        if (references) {
                            continue;
                        }
                    }
                    MySQLTableColumn fkColumn = fkTable == null ? null : fkTable.getAttribute(monitor, fkColumnName);
                    if (fkColumn == null) {
                        log.debug("Can't find FK table " + fkTableName + " column " + fkColumnName);
                        if (!references) {
                            continue;
                        }
                    }

                    // Find PK
                    MySQLTableConstraint pk = null;
                    if (pkTable != null && pkName != null) {
                        pk = DBUtils.findObject(pkTable.getConstraints(monitor), pkName);
                        if (pk == null) {
                            log.warn("Unique key '" + pkName + "' not found in table " + pkTable.getFullyQualifiedName(DBPEvaluationContext.DDL));
                        }
                    }
                    if (pk == null && pkTable != null) {
                        Collection<MySQLTableConstraint> constraints = pkTable.getConstraints(monitor);
                        if (constraints != null) {
                            for (MySQLTableConstraint pkConstraint : constraints) {
                                if (pkConstraint.getConstraintType().isUnique() && DBUtils.getConstraintAttribute(monitor, pkConstraint, pkColumn) != null) {
                                    pk = pkConstraint;
                                    break;
                                }
                            }
                        }
                    }
                    if (pk == null && pkTable != null) {
                        log.warn("Can't find primary key for table " + pkTable.getFullyQualifiedName(DBPEvaluationContext.DDL));
                        // Too bad. But we have to create new fake PK for this FK
                        String pkFullName = pkTable.getFullyQualifiedName(DBPEvaluationContext.DDL) + "." + pkName;
                        pk = pkMap.get(pkFullName);
                        if (pk == null) {
                            pk = new MySQLTableConstraint(pkTable, pkName, null, DBSEntityConstraintType.PRIMARY_KEY, true);
                            pk.addColumn(new MySQLTableConstraintColumn(pk, pkColumn, keySeq));
                            pkMap.put(pkFullName, pk);
                        }
                    }

                    // Find (or create) FK
                    MySQLTableForeignKey fk = null;
                    if (references && fkTable != null) {
                        fk = DBUtils.findObject(fkTable.getAssociations(monitor), fkName);
                        if (fk == null) {
                            log.warn("Can't find foreign key '" + fkName + "' for table " + fkTable.getFullyQualifiedName(DBPEvaluationContext.DDL));
                            // No choice, we have to create fake foreign key :(
                        } else {
                            if (!fkList.contains(fk)) {
                                fkList.add(fk);
                            }
                        }
                    }

                    if (fk == null) {
                        fk = fkMap.get(fkName);
                        if (fk == null) {
                            fk = new MySQLTableForeignKey(fkTable, fkName, null, pk, deleteRule, updateRule, true);
                            fkMap.put(fkName, fk);
                            fkList.add(fk);
                        }
                        MySQLTableForeignKeyColumn fkColumnInfo = new MySQLTableForeignKeyColumn(fk, fkColumn, keySeq, pkColumn);
                        fk.addColumn(fkColumnInfo);
                    }
                }
            } finally {
                dbResult.close();
            }
            return fkList;
        } catch (SQLException ex) {
            throw new DBException(ex, getDataSource());
        }
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        return getDDL(monitor, options);
    }

    @Override
    public void setObjectDefinitionText(String sourceText) throws DBException {
        throw new DBException("Table DDL is read-only");
    }

    class PartitionCache extends JDBCObjectCache<MySQLTable, MySQLPartition> {
        Map<String, MySQLPartition> partitionMap = new HashMap<>();
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull MySQLTable mySQLTable) throws SQLException
        {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM " + MySQLConstants.META_TABLE_PARTITIONS +
                " WHERE TABLE_SCHEMA=? AND TABLE_NAME=? " +
                " ORDER BY PARTITION_ORDINAL_POSITION,SUBPARTITION_ORDINAL_POSITION");
            dbStat.setString(1, getContainer().getName());
            dbStat.setString(2, getName());
            return dbStat;
        }

        @Override
        protected MySQLPartition fetchObject(@NotNull JDBCSession session, @NotNull MySQLTable table, @NotNull JDBCResultSet dbResult) throws SQLException, DBException
        {
            String partitionName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_PARTITION_NAME);
            if (partitionName == null) {
                partitionName = "PARTITION";
            }
            String subPartitionName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_SUBPARTITION_NAME);
            if (CommonUtils.isEmpty(subPartitionName)) {
                return new MySQLPartition(table, null, partitionName, dbResult);
            } else {
                MySQLPartition parentPartition = partitionMap.get(partitionName);
                if (parentPartition == null) {
                    parentPartition = new MySQLPartition(table, null, partitionName, dbResult);
                    partitionMap.put(partitionName, parentPartition);
                }
                new MySQLPartition(table, parentPartition, subPartitionName, dbResult);
                return null;
            }
        }

        @Override
        protected void invalidateObjects(DBRProgressMonitor monitor, MySQLTable owner, Iterator<MySQLPartition> objectIter)
        {
            partitionMap = null;
        }
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return additionalInfo.description;
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        getContainer().constraintCache.clearObjectCache(this);
        getContainer().indexCache.clearObjectCache(this);
        getContainer().triggerCache.clearChildrenOf(this);

        return super.refreshObject(monitor);
    }

    public static class EngineListProvider implements IPropertyValueListProvider<MySQLTable> {
        @Override
        public boolean allowCustomValue()
        {
            return false;
        }
        @Override
        public Object[] getPossibleValues(MySQLTable object)
        {
            final List<MySQLEngine> engines = new ArrayList<>();
            for (MySQLEngine engine : object.getDataSource().getEngines()) {
                if (engine.getSupport() == MySQLEngine.Support.YES || engine.getSupport() == MySQLEngine.Support.DEFAULT) {
                    engines.add(engine);
                }
            }
            Collections.sort(engines, DBUtils.<MySQLEngine>nameComparator());
            return engines.toArray(new MySQLEngine[engines.size()]);
        }
    }

    public static class CharsetListProvider implements IPropertyValueListProvider<MySQLTable> {
        @Override
        public boolean allowCustomValue()
        {
            return false;
        }
        @Override
        public Object[] getPossibleValues(MySQLTable object)
        {
            return object.getDataSource().getCharsets().toArray();
        }
    }

    public static class CollationListProvider implements IPropertyValueListProvider<MySQLTable> {
        @Override
        public boolean allowCustomValue()
        {
            return false;
        }
        @Override
        public Object[] getPossibleValues(MySQLTable object)
        {
            if (object.additionalInfo.charset == null) {
                return null;
            } else {
                return object.additionalInfo.charset.getCollations().toArray();
            }
        }
    }

}

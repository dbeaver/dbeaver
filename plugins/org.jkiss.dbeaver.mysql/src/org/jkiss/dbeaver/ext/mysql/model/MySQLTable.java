/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.model;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.IPropertyCacheValidator;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyGroup;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSConstraintModifyRule;
import org.jkiss.dbeaver.model.struct.DBSConstraintType;
import org.jkiss.dbeaver.ui.properties.IPropertyValueListProvider;

import java.io.UnsupportedEncodingException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * MySQLTable
 */
public class MySQLTable extends MySQLTableBase
{

    private static final String INNODB_COMMENT = "InnoDB free";

    public static class AdditionalInfo {
        private volatile boolean loaded = false;
        private long rowCount;
        private long autoIncrement;
        private String description;
        private java.util.Date createTime;
        private MySQLCharset charset;
        private MySQLCollation collation;
        private MySQLEngine engine;
        private long avgRowLength;
        private long dataLength;

        @Property(name = "Engine", viewable = true, editable = true, updatable = true, listProvider = EngineListProvider.class, order = 3) public MySQLEngine getEngine() { return engine; }
        @Property(name = "Auto Increment", viewable = true, editable = true, updatable = true, order = 4) public long getAutoIncrement() { return autoIncrement; }
        @Property(name = "Charset", viewable = false, editable = true, updatable = true, listProvider = CharsetListProvider.class, order = 5) public MySQLCharset getCharset() { return charset; }
        @Property(name = "Collation", viewable = false, editable = true, updatable = true, listProvider = CollationListProvider.class, order = 6) public MySQLCollation getCollation() { return collation; }
        @Property(name = "Description", viewable = true, editable = true, updatable = true, order = 100) public String getDescription() { return description; }

        @Property(name = "Row Count", category = "Statistics", viewable = true, order = 10) public long getRowCount() { return rowCount; }
        @Property(name = "Avg Row Length", category = "Statistics", viewable = true, order = 11) public long getAvgRowLength() { return avgRowLength; }
        @Property(name = "Data Length", category = "Statistics", viewable = true, order = 12) public long getDataLength() { return dataLength; }
        @Property(name = "Create Time", category = "Statistics", viewable = false, order = 13) public java.util.Date getCreateTime() { return createTime; }

        public void setEngine(MySQLEngine engine) { this.engine = engine; }
        public void setAutoIncrement(long autoIncrement) { this.autoIncrement = autoIncrement; }
        public void setDescription(String description) { this.description = description; }

        public void setCharset(MySQLCharset charset) { this.charset = charset; this.collation = charset == null ? null : charset.getDefaultCollation(); }
        public void setCollation(MySQLCollation collation) { this.collation = collation; }
    }

    public static class AdditionalInfoValidator implements IPropertyCacheValidator<MySQLTable> {
        public boolean isPropertyCached(MySQLTable object)
        {
            return object.additionalInfo.loaded;
        }
    }

    private List<MySQLIndex> indexes;
    private List<MySQLConstraint> constraints;
    private List<MySQLForeignKey> foreignKeys;
    private List<MySQLTrigger> triggers;
    private List<MySQLPartition> partitions;

    private final AdditionalInfo additionalInfo = new AdditionalInfo();

    public MySQLTable(MySQLCatalog catalog)
    {
        super(catalog);
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

    public boolean isView()
    {
        return false;
    }

    public List<MySQLIndex> getIndexes(DBRProgressMonitor monitor)
        throws DBException
    {
        if (indexes == null) {
            // Read indexes using cache
            this.getContainer().getIndexCache().getObjects(monitor, this);
        }
        return indexes;
    }

    public MySQLIndex getIndex(DBRProgressMonitor monitor, String indexName)
        throws DBException
    {
        return DBUtils.findObject(getIndexes(monitor), indexName);
    }

    boolean isIndexesCached()
    {
        return indexes != null;
    }

    void setIndexes(List<MySQLIndex> indexes)
    {
        this.indexes = indexes;
    }

    public List<MySQLConstraint> getConstraints(DBRProgressMonitor monitor)
        throws DBException
    {
        if (constraints == null) {
            getContainer().loadConstraints(monitor, this);
        }
        return constraints;
    }

    public MySQLConstraint getConstraint(DBRProgressMonitor monitor, String ukName)
        throws DBException
    {
        return DBUtils.findObject(getConstraints(monitor), ukName);
    }

    public List<MySQLForeignKey> getReferences(DBRProgressMonitor monitor)
        throws DBException
    {
        return loadForeignKeys(monitor, true);
    }

    public List<MySQLForeignKey> getForeignKeys(DBRProgressMonitor monitor)
        throws DBException
    {
        if (foreignKeys == null) {
            foreignKeys = loadForeignKeys(monitor, false);
        }
        return foreignKeys;
    }

    public MySQLForeignKey getForeignKey(DBRProgressMonitor monitor, String fkName)
        throws DBException
    {
        return DBUtils.findObject(getForeignKeys(monitor), fkName);
    }

    public List<MySQLTrigger> getTriggers(DBRProgressMonitor monitor)
        throws DBException
    {
        if (triggers == null) {
            triggers = loadTriggers(monitor);
        }
        return triggers;
    }

    public MySQLTrigger getTrigger(DBRProgressMonitor monitor, String triggerName)
        throws DBException
    {
        return DBUtils.findObject(getTriggers(monitor), triggerName);
    }

    public List<MySQLPartition> getPartitions(DBRProgressMonitor monitor)
        throws DBException
    {
        if (partitions == null) {
            partitions = loadPartitions(monitor);
        }
        return partitions;
    }


    public String getDDL(DBRProgressMonitor monitor)
        throws DBException
    {
        if (!isPersisted()) {
            return "";
        }
        JDBCExecutionContext context = getDataSource().openContext(monitor, DBCExecutionPurpose.META, "Retrieve table DDL");
        try {
            PreparedStatement dbStat = context.prepareStatement(
                "SHOW CREATE " + (isView() ? "VIEW" : "TABLE") + " " + getFullQualifiedName());
            try {
                ResultSet dbResult = dbStat.executeQuery();
                try {
                    if (dbResult.next()) {
                        byte[] ddl;
                        if (isView()) {
                            ddl = dbResult.getBytes("Create View");
                        } else {
                            ddl = dbResult.getBytes("Create Table");
                        }
                        if (ddl == null) {
                            return null;
                        } else {
                            try {
                                return new String(ddl, getContainer().getDefaultCharset().getName());
                            } catch (UnsupportedEncodingException e) {
                                log.debug(e);
                                return new String(ddl);
                            }
                        }
                    } else {
                        return "DDL is not available";
                    }
                }
                finally {
                    dbResult.close();
                }
            }
            finally {
                dbStat.close();
            }
        }
        catch (SQLException ex) {
            throw new DBException(ex);
        }
        finally {
            context.close();
        }
    }

    @Override
    public boolean refreshEntity(DBRProgressMonitor monitor) throws DBException
    {
        super.refreshEntity(monitor);
        indexes = null;
        constraints = null;
        foreignKeys = null;
        triggers = null;
        synchronized (additionalInfo) {
            additionalInfo.loaded = false;
        }
        return true;
    }

    private void loadAdditionalInfo(DBRProgressMonitor monitor) throws DBCException
    {
        if (!isPersisted()) {
            additionalInfo.loaded = true;
            return;
        }
        JDBCExecutionContext context = getDataSource().openContext(monitor, DBCExecutionPurpose.META, "Load table status");
        try {
            JDBCPreparedStatement dbStat = context.prepareStatement(
                "SHOW TABLE STATUS FROM " + DBUtils.getQuotedIdentifier(getContainer()) + " LIKE '" + getName() + "'");
            try {
                JDBCResultSet dbResult = dbStat.executeQuery();
                try {
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
                                } else {
                                    desc = "";
                                }
                            }
                            additionalInfo.description = desc;
                        }
                        additionalInfo.engine = getDataSource().getEngine(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_ENGINE));
                        additionalInfo.rowCount = JDBCUtils.safeGetLong(dbResult, MySQLConstants.COL_TABLE_ROWS);
                        additionalInfo.autoIncrement = JDBCUtils.safeGetLong(dbResult, MySQLConstants.COL_AUTO_INCREMENT);
                        additionalInfo.createTime = JDBCUtils.safeGetTimestamp(dbResult, MySQLConstants.COL_CREATE_TIME);
                        additionalInfo.collation = getDataSource().getCollation(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_COLLATION));
                        if (additionalInfo.collation != null) {
                            additionalInfo.charset = additionalInfo.collation.getCharset();
                        }
                        additionalInfo.avgRowLength = JDBCUtils.safeGetLong(dbResult, MySQLConstants.COL_AVG_ROW_LENGTH);
                        additionalInfo.dataLength = JDBCUtils.safeGetLong(dbResult, MySQLConstants.COL_DATA_LENGTH);
                    }
                    additionalInfo.loaded = true;
                } finally {
                    dbResult.close();
                }
            } finally {
                dbStat.close();
            }
        }
        catch (SQLException e) {
            throw new DBCException(e);
        } finally {
            context.close();
        }
    }

    boolean uniqueKeysCached()
    {
        return this.constraints != null;
    }

    void cacheUniqueKey(MySQLConstraint constraint)
    {
        if (constraints == null) {
            constraints = new ArrayList<MySQLConstraint>();
        }
        
        constraints.add(constraint);
    }

    private List<MySQLForeignKey> loadForeignKeys(DBRProgressMonitor monitor, boolean references)
        throws DBException
    {
        List<MySQLForeignKey> fkList = new ArrayList<MySQLForeignKey>();
        if (!isPersisted()) {
            return fkList;
        }
        JDBCExecutionContext context = getDataSource().openContext(monitor, DBCExecutionPurpose.META, "Load table relations");
        try {
            Map<String, MySQLForeignKey> fkMap = new HashMap<String, MySQLForeignKey>();
            Map<String, MySQLConstraint> pkMap = new HashMap<String, MySQLConstraint>();
            JDBCDatabaseMetaData metaData = context.getMetaData();
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

                    DBSConstraintModifyRule deleteRule = JDBCUtils.getCascadeFromNum(deleteRuleNum);
                    DBSConstraintModifyRule updateRule = JDBCUtils.getCascadeFromNum(updateRuleNum);

                    MySQLTable pkTable = getDataSource().findTable(monitor, pkTableCatalog, pkTableName);
                    if (pkTable == null) {
                        log.warn("Can't find PK table " + pkTableName);
                        continue;
                    }
                    MySQLTable fkTable = getDataSource().findTable(monitor, fkTableCatalog, fkTableName);
                    if (fkTable == null) {
                        log.warn("Can't find FK table " + fkTableName);
                        continue;
                    }
                    MySQLTableColumn pkColumn = pkTable.getColumn(monitor, pkColumnName);
                    if (pkColumn == null) {
                        log.warn("Can't find PK table " + pkTable.getFullQualifiedName() + " column " + pkColumnName);
                        continue;
                    }
                    MySQLTableColumn fkColumn = fkTable.getColumn(monitor, fkColumnName);
                    if (fkColumn == null) {
                        log.warn("Can't find FK table " + fkTable.getFullQualifiedName() + " column " + fkColumnName);
                        continue;
                    }

                    // Find PK
                    MySQLConstraint pk = null;
                    if (pkName != null) {
                        pk = DBUtils.findObject(pkTable.getConstraints(monitor), pkName);
                        if (pk == null) {
                            log.warn("Unique key '" + pkName + "' not found in table " + pkTable.getFullQualifiedName());
                        }
                    }
                    if (pk == null) {
                        List<MySQLConstraint> constraints = pkTable.getConstraints(monitor);
                        if (constraints != null) {
                            for (MySQLConstraint pkConstraint : constraints) {
                                if (pkConstraint.getConstraintType().isUnique() && pkConstraint.getColumn(monitor, pkColumn) != null) {
                                    pk = pkConstraint;
                                    break;
                                }
                            }
                        }
                    }
                    if (pk == null) {
                        log.warn("Could not find primary key for table " + pkTable.getFullQualifiedName());
                        // Too bad. But we have to create new fake PK for this FK
                        String pkFullName = pkTable.getFullQualifiedName() + "." + pkName;
                        pk = pkMap.get(pkFullName);
                        if (pk == null) {
                            pk = new MySQLConstraint(pkTable, pkName, null, DBSConstraintType.PRIMARY_KEY, true);
                            pk.addColumn(new MySQLConstraintColumn(pk, pkColumn, keySeq));
                            pkMap.put(pkFullName, pk);
                        }
                    }

                    // Find (or create) FK
                    MySQLForeignKey fk = null;
                    if (references) {
                        fk = DBUtils.findObject(fkTable.getForeignKeys(monitor), fkName);
                        if (fk == null) {
                            log.warn("Could not find foreign key '" + fkName + "' for table " + fkTable.getFullQualifiedName());
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
                            fk = new MySQLForeignKey(fkTable, fkName, null, pk, deleteRule, updateRule, true);
                            fkMap.put(fkName, fk);
                            fkList.add(fk);
                        }
                        MySQLForeignKeyColumn fkColumnInfo = new MySQLForeignKeyColumn(fk, fkColumn, keySeq, pkColumn);
                        fk.addColumn(fkColumnInfo);
                    }
                }
            }
            finally {
                dbResult.close();
            }
            return fkList;
        } catch (SQLException ex) {
            throw new DBException(ex);
        }
        finally {
            context.close();
        }
    }

    private List<MySQLTrigger> loadTriggers(DBRProgressMonitor monitor)
        throws DBException
    {
        List<MySQLTrigger> tmpTriggers = new ArrayList<MySQLTrigger>();
        if (!isPersisted()) {
            return tmpTriggers;
        }
        // Load only trigger's owner catalog and trigger name
        // Actual triggers are stored in catalog - we just get em from cache
        JDBCExecutionContext context = getDataSource().openContext(monitor, DBCExecutionPurpose.META, "Load table '" + getName() + "' triggers");
        try {
            JDBCPreparedStatement dbStat = context.prepareStatement(
                "SELECT " + MySQLConstants.COL_TRIGGER_SCHEMA + "," + MySQLConstants.COL_TRIGGER_NAME + " FROM " + MySQLConstants.META_TABLE_TRIGGERS +
                " WHERE " + MySQLConstants.COL_TRIGGER_EVENT_OBJECT_SCHEMA + "=? AND " + MySQLConstants.COL_TRIGGER_EVENT_OBJECT_TABLE + "=? " +
                " ORDER BY " + MySQLConstants.COL_TRIGGER_NAME);
            try {
                dbStat.setString(1, getContainer().getName());
                dbStat.setString(2, getName());
                JDBCResultSet dbResult = dbStat.executeQuery();
                try {
                    while (dbResult.next()) {
                        String ownerSchema = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_TRIGGER_SCHEMA);
                        String triggerName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_TRIGGER_NAME);
                        MySQLCatalog triggerCatalog = getDataSource().getCatalog(ownerSchema);
                        if (triggerCatalog == null) {
                            log.warn("Could not find catalog '" + ownerSchema + "'");
                            continue;
                        }
                        MySQLTrigger trigger = triggerCatalog.getTrigger(monitor, triggerName);
                        if (trigger == null) {
                            log.warn("Could not find trigger '" + triggerName + "' catalog '" + ownerSchema + "'");
                            continue;
                        }
                        tmpTriggers.add(trigger);
                    }
                    return tmpTriggers;
                }
                finally {
                    dbResult.close();
                }
            }
            finally {
                dbStat.close();
            }
        }
        catch (SQLException e) {
            throw new DBException(e);
        }
        finally {
            context.close();
        }
    }

    private List<MySQLPartition> loadPartitions(DBRProgressMonitor monitor)
        throws DBException
    {
        List<MySQLPartition> tmpPartitions = new ArrayList<MySQLPartition>();
        if (!isPersisted()) {
            return tmpPartitions;
        }
        Map<String, MySQLPartition> partitionMap = new HashMap<String, MySQLPartition>();
        // Load only partition's owner catalog and partition name
        // Actual partitions are stored in catalog - we just get em from cache
        JDBCExecutionContext context = getDataSource().openContext(monitor, DBCExecutionPurpose.META, "Load table '" + getName() + "' partitions");
        try {
            JDBCPreparedStatement dbStat = context.prepareStatement(
                "SELECT * FROM " + MySQLConstants.META_TABLE_PARTITIONS +
                " WHERE " + MySQLConstants.COL_TABLE_SCHEMA + "=? AND " + MySQLConstants.COL_TABLE_NAME + "=? " +
                " ORDER BY " + MySQLConstants.COL_PARTITION_ORDINAL_POSITION + "," + MySQLConstants.COL_SUBPARTITION_ORDINAL_POSITION);
            try {
                dbStat.setString(1, getContainer().getName());
                dbStat.setString(2, getName());
                JDBCResultSet dbResult = dbStat.executeQuery();
                try {
                    while (dbResult.next()) {
                        String partitionName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_PARTITION_NAME);
                        String subPartitionName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_SUBPARTITION_NAME);
                        if (CommonUtils.isEmpty(subPartitionName)) {
                            MySQLPartition partition = new MySQLPartition(this, null, partitionName, dbResult);
                            tmpPartitions.add(partition);
                        } else {
                            MySQLPartition parentPartition = partitionMap.get(partitionName);
                            if (parentPartition == null) {
                                parentPartition = new MySQLPartition(this, null, partitionName, dbResult);
                                tmpPartitions.add(parentPartition);
                                partitionMap.put(partitionName, parentPartition);
                            }
                            new MySQLPartition(this, parentPartition, subPartitionName, dbResult);
                        }
                    }
                    return tmpPartitions;
                }
                finally {
                    dbResult.close();
                }
            }
            finally {
                dbStat.close();
            }
        }
        catch (SQLException e) {
            throw new DBException(e);
        }
        finally {
            context.close();
        }
    }

    public String getDescription()
    {
        return additionalInfo.description;
    }

    public static class EngineListProvider implements IPropertyValueListProvider<MySQLTable> {
        public boolean allowCustomValue()
        {
            return false;
        }
        public Object[] getPossibleValues(MySQLTable object)
        {
            final List<MySQLEngine> engines = new ArrayList<MySQLEngine>();
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
        public boolean allowCustomValue()
        {
            return false;
        }
        public Object[] getPossibleValues(MySQLTable object)
        {
            return object.getDataSource().getCharsets().toArray();
        }
    }

    public static class CollationListProvider implements IPropertyValueListProvider<MySQLTable> {
        public boolean allowCustomValue()
        {
            return false;
        }
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

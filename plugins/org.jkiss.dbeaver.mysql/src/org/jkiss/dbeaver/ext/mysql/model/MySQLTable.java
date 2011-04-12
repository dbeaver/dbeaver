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
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.meta.IPropertyCacheValidator;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyGroup;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSConstraintCascade;
import org.jkiss.dbeaver.model.struct.DBSConstraintDefferability;
import org.jkiss.dbeaver.model.struct.DBSConstraintType;
import org.jkiss.dbeaver.model.struct.DBSIndexType;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GenericTable
 */
public class MySQLTable extends JDBCTable<MySQLDataSource, MySQLCatalog>
{
    static final Log log = LogFactory.getLog(MySQLTable.class);

    private static final String INNODB_COMMENT = "InnoDB free";

    public static class AdditionalInfo {
        private volatile boolean loaded = false;
        private long rowCount;
        private long autoIncrement;
        private String description;
        private java.util.Date createTime;
        private String collation;
        private MySQLEngine engine;
        private long avgRowLength;
        private long dataLength;

        @Property(name = "Engine", viewable = true, order = 3) public MySQLEngine getEngine() { return engine; }
        @Property(name = "Row Count", viewable = true, order = 5) public long getRowCount() { return rowCount; }
        @Property(name = "Auto Increment", viewable = true, order = 6) public long getAutoIncrement() { return autoIncrement; }
        @Property(name = "Create Time", viewable = true, order = 7) public java.util.Date getCreateTime() { return createTime; }
        @Property(name = "Collation", viewable = true, order = 8) public String getCollation() { return collation; }
        @Property(name = "Avg Row Length", viewable = true, order = 9) public long getAvgRowLength() { return avgRowLength; }
        @Property(name = "Data Length", viewable = true, order = 10) public long getDataLength() { return dataLength; }
        @Property(name = "Description", viewable = true, order = 100) public String getDescription() { return description; }
    }

    public static class AdditionalInfoValidator implements IPropertyCacheValidator<MySQLTable> {
        public boolean isPropertyCached(MySQLTable object)
        {
            return object.additionalInfo.loaded;
        }
    }

    private boolean isView;
    private boolean isSystem;

    private List<MySQLTableColumn> columns;
    private List<MySQLIndex> indexes;
    private List<MySQLConstraint> uniqueKeys;
    private List<MySQLForeignKey> foreignKeys;
    private List<MySQLTrigger> triggers;

    private final AdditionalInfo additionalInfo = new AdditionalInfo();

    public MySQLTable(
        MySQLCatalog catalog,
        ResultSet dbResult)
    {
        super(catalog, true);
        this.loadInfo(dbResult);
    }

    public String getFullQualifiedName()
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getContainer(),
            this);
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
        return this.isView;
    }

    public boolean isSystem()
    {
        return this.isSystem;
    }

    public List<MySQLTableColumn> getColumns(DBRProgressMonitor monitor)
        throws DBException
    {
        if (columns == null) {
            getContainer().getTableCache().loadChildren(monitor, this);
        }
        return columns;
    }

    public MySQLTableColumn getColumn(DBRProgressMonitor monitor, String columnName)
        throws DBException
    {
        return DBUtils.findObject(getColumns(monitor), columnName);
    }

    public List<MySQLIndex> getIndexes(DBRProgressMonitor monitor)
        throws DBException
    {
        if (indexes == null) {
            indexes = loadIndexes(monitor);
        }
        return indexes;
    }

    public MySQLIndex getIndex(DBRProgressMonitor monitor, String indexName)
        throws DBException
    {
        return DBUtils.findObject(getIndexes(monitor), indexName);
    }

    public List<MySQLConstraint> getUniqueKeys(DBRProgressMonitor monitor)
        throws DBException
    {
        if (uniqueKeys == null) {
            getContainer().loadConstraints(monitor, this);
        }
        return uniqueKeys;
    }

    public MySQLConstraint getUniqueKey(DBRProgressMonitor monitor, String ukName)
        throws DBException
    {
        return DBUtils.findObject(getUniqueKeys(monitor), ukName);
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
            loadTriggers(monitor);
        }
        return triggers;
    }

    public MySQLTrigger getTrigger(DBRProgressMonitor monitor, String triggerName)
        throws DBException
    {
        return DBUtils.findObject(getTriggers(monitor), triggerName);
    }

    public String getDDL(DBRProgressMonitor monitor)
        throws DBException
    {
        JDBCExecutionContext context = getDataSource().openContext(monitor, DBCExecutionPurpose.META, "Retrieve table DDL");
        try {
            PreparedStatement dbStat = context.prepareStatement(
                "SHOW CREATE " + (isView() ? "VIEW" : "TABLE") + " " + getFullQualifiedName());
            try {
                ResultSet dbResult = dbStat.executeQuery();
                try {
                    if (dbResult.next()) {
                        if (isView()) {
                            return dbResult.getString("Create View");
                        } else {
                            return dbResult.getString("Create Table");
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

    private void loadInfo(ResultSet dbResult)
    {
        this.setName(JDBCUtils.safeGetString(dbResult, 1));
        this.setTableType(JDBCUtils.safeGetString(dbResult, 2));

        if (!CommonUtils.isEmpty(this.getTableType())) {
            this.isView = (this.getTableType().toUpperCase().indexOf("VIEW") != -1);
            this.isSystem = (this.getTableType().toUpperCase().indexOf("SYSTEM") != -1);
        }
        this.columns = null;
    }

    private void loadAdditionalInfo(DBRProgressMonitor monitor) throws DBCException
    {
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
                        additionalInfo.collation = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_COLLATION);
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

    private List<MySQLIndex> loadIndexes(DBRProgressMonitor monitor)
        throws DBException
    {
        // Load columns first
        getColumns(monitor);
        // Load index columns
        JDBCExecutionContext context = getDataSource().openContext(monitor, DBCExecutionPurpose.META, "Load indexes");
        try {
            List<MySQLIndex> tmpIndexList = new ArrayList<MySQLIndex>();
            Map<String, MySQLIndex> tmpIndexMap = new HashMap<String, MySQLIndex>();
            JDBCDatabaseMetaData metaData = context.getMetaData();
            // Load indexes
            JDBCResultSet dbResult = metaData.getIndexInfo(
                getContainer().getName(),
                null,
                getName(),
                false,
                false);
            try {
                while (dbResult.next()) {
                    String indexName = JDBCUtils.safeGetString(dbResult, JDBCConstants.INDEX_NAME);
                    boolean isNonUnique = JDBCUtils.safeGetBoolean(dbResult, JDBCConstants.NON_UNIQUE);
                    String indexQualifier = JDBCUtils.safeGetString(dbResult, JDBCConstants.INDEX_QUALIFIER);
                    int indexTypeNum = JDBCUtils.safeGetInt(dbResult, JDBCConstants.TYPE);

                    int ordinalPosition = JDBCUtils.safeGetInt(dbResult, JDBCConstants.ORDINAL_POSITION);
                    String columnName = JDBCUtils.safeGetString(dbResult, JDBCConstants.COLUMN_NAME);
                    String ascOrDesc = JDBCUtils.safeGetString(dbResult, JDBCConstants.ASC_OR_DESC);

                    if (CommonUtils.isEmpty(indexName)) {
                        // Bad index - can't evaluate it
                        continue;
                    }
                    DBSIndexType indexType;
                    switch (indexTypeNum) {
                        case DatabaseMetaData.tableIndexStatistic: indexType = DBSIndexType.STATISTIC; break;
                        case DatabaseMetaData.tableIndexClustered: indexType = DBSIndexType.CLUSTERED; break;
                        case DatabaseMetaData.tableIndexHashed: indexType = DBSIndexType.HASHED; break;
                        case DatabaseMetaData.tableIndexOther: indexType = DBSIndexType.OTHER; break;
                        default: indexType = DBSIndexType.UNKNOWN; break;
                    }
                    MySQLIndex index = tmpIndexMap.get(indexName);
                    if (index == null) {
                        index = new MySQLIndex(
                            this,
                            isNonUnique,
                            indexQualifier,
                            indexName,
                            indexType);
                        tmpIndexList.add(index);
                        tmpIndexMap.put(indexName, index);
                    }
                    MySQLTableColumn tableColumn = this.getColumn(monitor, columnName);
                    if (tableColumn == null) {
                        log.warn("Column '" + columnName + "' not found in table '" + this.getName() + "'");
                        continue;
                    }
                    index.addColumn(
                        new MySQLIndexColumn(
                            index,
                            tableColumn,
                            ordinalPosition,
                            !"D".equalsIgnoreCase(ascOrDesc)));
                }
            }
            finally {
                dbResult.close();
            }
            return tmpIndexList;
        } catch (SQLException ex) {
            throw new DBException(ex);
        }
        finally {
            context.close();
        }
    }

    boolean uniqueKeysCached()
    {
        return this.uniqueKeys != null;
    }

    void cacheUniqueKey(MySQLConstraint constraint)
    {
        if (uniqueKeys == null) {
            uniqueKeys = new ArrayList<MySQLConstraint>();
        }
        
        uniqueKeys.add(constraint);
    }

    private List<MySQLForeignKey> loadForeignKeys(DBRProgressMonitor monitor, boolean references)
        throws DBException
    {
        JDBCExecutionContext context = getDataSource().openContext(monitor, DBCExecutionPurpose.META, "Load table relations");
        try {
            List<MySQLForeignKey> fkList = new ArrayList<MySQLForeignKey>();
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
                    int defferabilityNum = JDBCUtils.safeGetInt(dbResult, JDBCConstants.DEFERRABILITY);

                    DBSConstraintCascade deleteRule = getCascadeFromNum(deleteRuleNum);
                    DBSConstraintCascade updateRule = getCascadeFromNum(updateRuleNum);
                    DBSConstraintDefferability defferability;
                    switch (defferabilityNum) {
                        case DatabaseMetaData.importedKeyInitiallyDeferred: defferability = DBSConstraintDefferability.INITIALLY_DEFERRED; break;
                        case DatabaseMetaData.importedKeyInitiallyImmediate: defferability = DBSConstraintDefferability.INITIALLY_IMMEDIATE; break;
                        case DatabaseMetaData.importedKeyNotDeferrable: defferability = DBSConstraintDefferability.NOT_DEFERRABLE; break;
                        default: defferability = DBSConstraintDefferability.UNKNOWN; break;
                    }

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
                        pk = DBUtils.findObject(pkTable.getUniqueKeys(monitor), pkName);
                        if (pk == null) {
                            log.warn("Unique key '" + pkName + "' not found in table " + pkTable.getFullQualifiedName());
                        }
                    }
                    if (pk == null) {
                        List<MySQLConstraint> uniqueKeys = pkTable.getUniqueKeys(monitor);
                        if (uniqueKeys != null) {
                            for (MySQLConstraint pkConstraint : uniqueKeys) {
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
                            pk = new MySQLConstraint(pkTable, pkName, null, DBSConstraintType.PRIMARY_KEY);
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
                            fk = new MySQLForeignKey(fkTable, fkName, null, pk, deleteRule, updateRule, defferability);
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

    private static DBSConstraintCascade getCascadeFromNum(int num)
    {
        switch (num) {
            case DatabaseMetaData.importedKeyNoAction: return DBSConstraintCascade.NO_ACTION;
            case DatabaseMetaData.importedKeyCascade: return DBSConstraintCascade.CASCADE;
            case DatabaseMetaData.importedKeySetNull: return DBSConstraintCascade.SET_NULL;
            case DatabaseMetaData.importedKeySetDefault: return DBSConstraintCascade.SET_DEFAULT;
            case DatabaseMetaData.importedKeyRestrict: return DBSConstraintCascade.RESTRICT;
            default: return DBSConstraintCascade.UNKNOWN;
        }
    }

    private void loadTriggers(DBRProgressMonitor monitor)
        throws DBException
    {
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
                    List<MySQLTrigger> tmpTriggers = new ArrayList<MySQLTrigger>();
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
                    this.triggers = tmpTriggers;
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

    public boolean isColumnsCached()
    {
        return columns != null;
    }

    public void setColumns(List<MySQLTableColumn> columns)
    {
        this.columns = columns;
    }

    public String getDescription()
    {
        return additionalInfo.description;
    }

}

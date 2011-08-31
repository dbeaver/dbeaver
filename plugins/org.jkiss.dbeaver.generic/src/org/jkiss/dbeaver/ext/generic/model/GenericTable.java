/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.*;

/**
 * GenericTable
 */
public class GenericTable extends JDBCTable<GenericDataSource, GenericStructContainer>
{
    static final Log log = LogFactory.getLog(GenericTable.class);

    private String tableType;
    private boolean isView;
    private boolean isSystem;
    private String description;

/*
    private String typeName;
    private GenericCatalog typeCatalog;
    private GenericSchema typeSchema;
*/

    private List<GenericTableColumn> columns;
    private List<GenericIndex> indexes;
    private List<GenericPrimaryKey> uniqueKeys;
    private List<GenericForeignKey> foreignKeys;
    private Long rowCount;

    public GenericTable(
        GenericStructContainer container)
    {
        this(container, null, null, null, false);
    }

    public GenericTable(
        GenericStructContainer container,
        String tableName,
        String tableType,
        String remarks,
        boolean persisted)
    {
        super(container, persisted, tableName);
        this.tableType = tableType;
        this.description = remarks;
        if (!CommonUtils.isEmpty(this.getTableType())) {
            this.isView = (this.getTableType().toUpperCase().indexOf("VIEW") != -1);
            this.isSystem =
                (this.getTableType().toUpperCase().indexOf("SYSTEM") != -1) || // general rule
                tableName.indexOf("RDB$") != -1;    // Firebird
        }
    }

    public DBSObject getParentObject()
    {
        return getContainer().getObject();
    }

    public String getFullQualifiedName()
    {
/*
        String ownerName = null, catalogName = null;
        if (getSchema() != null) {
            ownerName = getSchema().getName();
        } else if (getCatalog() != null) {
            ownerName = getCatalog().getName();
        }
        if (getSchema() != null && getCatalog() != null && getDataSource().getCatalogs().size() > 1) {
            // Use catalog name only if there are multiple catalogs
            catalogName = getCatalog().getName();
        }
*/
        return DBUtils.getFullQualifiedName(getDataSource(), getCatalog(), getSchema(), this);
    }

    public boolean isView()
    {
        return this.isView;
    }

    public boolean isSystem()
    {
        return this.isSystem;
    }

    @Property(name = "Table Type", viewable = true, order = 2)
    private String getTableType()
    {
        return tableType;
    }


    @Property(name = "Catalog", viewable = true, order = 3)
    public GenericCatalog getCatalog()
    {
        return getContainer().getCatalog();
    }

    @Property(name = "Schema", viewable = true, order = 4)
    public GenericSchema getSchema()
    {
        return getContainer().getSchema();
    }

    public List<GenericTableColumn> getColumns(DBRProgressMonitor monitor)
        throws DBException
    {
        if (columns == null) {
            // Read columns using container
            this.getContainer().getTableCache().loadChildren(monitor, getContainer(), this);
            if (columns != null && uniqueKeys == null) {
                // Cache unique keys (they are used by columns to detect key flag)
                getConstraints(monitor);
            }
        }
        return columns;
    }

    public GenericTableColumn getColumn(DBRProgressMonitor monitor, String columnName)
        throws DBException
    {
        return DBUtils.findObject(getColumns(monitor), columnName);
    }

    public List<GenericTableColumn> getColumnsCache()
    {
        return columns;
    }

    boolean isColumnsCached()
    {
        return this.columns != null;
    }

    synchronized void setColumns(List<GenericTableColumn> columns)
    {
        this.columns = columns;
    }

    public List<GenericIndex> getIndexes(DBRProgressMonitor monitor)
        throws DBException
    {
        if (indexes == null && getDataSource().getInfo().supportsIndexes()) {
            // Read indexes using cache
            this.getContainer().getIndexCache().getObjects(monitor, getContainer(), this);
        }
        return indexes;
    }

    public GenericIndex getIndex(DBRProgressMonitor monitor, String indexName)
        throws DBException
    {
        return DBUtils.findObject(getIndexes(monitor), indexName);
    }

    synchronized void setIndexes(List<GenericIndex> indexes)
    {
        this.indexes = indexes;
    }

    Collection<GenericIndex> getIndexesCache()
    {
        return this.indexes;
    }

    public List<GenericPrimaryKey> getConstraints(DBRProgressMonitor monitor)
        throws DBException
    {
        if (uniqueKeys == null) {
            if (columns == null) {
                // ensure all columns are already cached
                getColumns(monitor);
            }
            getContainer().getPrimaryKeysCache().getObjects(monitor, getContainer(), this);
        }
        return uniqueKeys;
    }

    List<GenericPrimaryKey> getUniqueKeysCache()
    {
        return uniqueKeys;
    }

    synchronized void setUniqueKeys(List<GenericPrimaryKey> uniqueKeys)
    {
        this.uniqueKeys = uniqueKeys;
    }

    synchronized void addUniqueKey(GenericPrimaryKey constraint) {
        if (uniqueKeys != null) {
            uniqueKeys.add(constraint);
        }
    }

    public List<GenericForeignKey> getReferences(DBRProgressMonitor monitor)
        throws DBException
    {
        return loadReferences(monitor);
    }

    public List<GenericForeignKey> getForeignKeys(DBRProgressMonitor monitor)
        throws DBException
    {
        if (foreignKeys == null && getDataSource().getInfo().supportsReferentialIntegrity()) {
            getContainer().getForeignKeysCache().getObjects(monitor, getContainer(), this);
        }
        return foreignKeys;
    }

    synchronized void setForeignKeys(List<GenericForeignKey> foreignKeys)
    {
        this.foreignKeys = foreignKeys;
    }

    public List<GenericTable> getSubTables()
    {
        return null;
    }

    @Property(name = "Table Description", viewable = true, order = 100)
    public String getDescription()
    {
        return description;
    }

    public Collection<GenericForeignKey> getForeignKeysCache()
    {
        return foreignKeys;
    }

    public Collection<GenericPrimaryKey> getConstraintsCache()
    {
        return uniqueKeys;
    }

/*
    public String getTypeName()
    {
        return typeName;
    }

    public GenericCatalog getTypeCatalog()
    {
        return typeCatalog;
    }

    public GenericSchema getTypeSchema()
    {
        return typeSchema;
    }

*/

    @Override
    public boolean refreshEntity(DBRProgressMonitor monitor) throws DBException
    {
        columns = null;
        indexes = null;
        uniqueKeys = null;
        foreignKeys = null;
        rowCount = null;
        return true;
    }

    // Comment row count calculation - it works too long and takes a lot of resources without serious reason
    @Property(name = "Row Count", viewable = true, expensive = true, order = 5)
    public Long getRowCount(DBRProgressMonitor monitor)
    {
        if (rowCount != null) {
            return rowCount;
        }
        if (isView() || !isPersisted()) {
            // Do not count rows for views
            return null;
        }

        if (indexes != null) {
            rowCount = getRowCountFromIndexes(monitor);
        }

        if (rowCount == null) {
            // Query row count
            JDBCExecutionContext context = getDataSource().openContext(monitor, DBCExecutionPurpose.META, "Read row count");
            try {
                JDBCPreparedStatement dbStat = context.prepareStatement(
                    "SELECT COUNT(*) FROM " + getFullQualifiedName());
                try {
    //                dbStat.setQueryTimeout(3);
                    JDBCResultSet resultSet = dbStat.executeQuery();
                    try {
                        resultSet.next();
                        rowCount = resultSet.getLong(1);
                    }
                    finally {
                        resultSet.close();
                    }
                }
                finally {
                    dbStat.close();
                }
            }
            catch (SQLException e) {
                //throw new DBCException(e);
                // do not throw this error - row count is optional info and some providers may fail
                log.debug("Can't fetch row count: " + e.getMessage());
                if (indexes != null) {
                    rowCount = getRowCountFromIndexes(monitor);
                }
            }
            finally {
                context.close();
            }
        }
        if (rowCount == null) {
            rowCount = -1L;
        }

        return rowCount;
    }

    private Long getRowCountFromIndexes(DBRProgressMonitor monitor)
    {
        try {
// Try to get cardinality from some unique index
            // Cardinality
            final List<GenericIndex> indexList = getIndexes(monitor);
            if (!CommonUtils.isEmpty(indexList)) {
                for (GenericIndex index : indexList) {
                    if (index.isUnique() || index.getIndexType() == DBSIndexType.STATISTIC) {
                        final long cardinality = index.getCardinality();
                        if (cardinality > 0) {
                            return cardinality;
                        }
                    }
                }
            }
        } catch (DBException e) {
            log.error(e);
        }
        return null;
    }

    private static class ForeignKeyInfo {
        String pkColumnName;
        String fkTableCatalog;
        String fkTableSchema;
        String fkTableName;
        String fkColumnName;
        int keySeq;
        int updateRuleNum;
        int deleteRuleNum;
        String fkName;
        String pkName;
        int defferabilityNum;
    }

    private List<GenericForeignKey> loadReferences(DBRProgressMonitor monitor)
        throws DBException
    {
        if (!isPersisted() || !getDataSource().getInfo().supportsReferentialIntegrity()) {
            return new ArrayList<GenericForeignKey>();
        }
        JDBCExecutionContext context = getDataSource().openContext(monitor, DBCExecutionPurpose.META, "Load table relations");
        try {
            // Read foreign keys in two passes
            // First read entire resultset to prevent recursive metadata requests
            // some drivers don't like it
            List<ForeignKeyInfo> fkInfos = new ArrayList<ForeignKeyInfo>();
            JDBCDatabaseMetaData metaData = context.getMetaData();
            // Load indexes
            JDBCResultSet dbResult = metaData.getExportedKeys(
                    getCatalog() == null ? null : getCatalog().getName(),
                    getSchema() == null ? null : getSchema().getName(),
                    getName());
            try {
                while (dbResult.next()) {
                    ForeignKeyInfo fkInfo = new ForeignKeyInfo();
                    fkInfo.pkColumnName = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.PKCOLUMN_NAME);
                    fkInfo.fkTableCatalog = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.FKTABLE_CAT);
                    fkInfo.fkTableSchema = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.FKTABLE_SCHEM);
                    fkInfo.fkTableName = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.FKTABLE_NAME);
                    fkInfo.fkColumnName = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.FKCOLUMN_NAME);
                    fkInfo.keySeq = JDBCUtils.safeGetInt(dbResult, JDBCConstants.KEY_SEQ);
                    fkInfo.updateRuleNum = JDBCUtils.safeGetInt(dbResult, JDBCConstants.UPDATE_RULE);
                    fkInfo.deleteRuleNum = JDBCUtils.safeGetInt(dbResult, JDBCConstants.DELETE_RULE);
                    fkInfo.fkName = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.FK_NAME);
                    fkInfo.pkName = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.PK_NAME);
                    fkInfo.defferabilityNum = JDBCUtils.safeGetInt(dbResult, JDBCConstants.DEFERRABILITY);
                    fkInfos.add(fkInfo);
                }
            }
            finally {
                dbResult.close();
            }

            List<GenericForeignKey> fkList = new ArrayList<GenericForeignKey>();
            Map<String, GenericForeignKey> fkMap = new HashMap<String, GenericForeignKey>();
            for (ForeignKeyInfo info : fkInfos) {
                DBSConstraintModifyRule deleteRule = JDBCUtils.getCascadeFromNum(info.deleteRuleNum);
                DBSConstraintModifyRule updateRule = JDBCUtils.getCascadeFromNum(info.updateRuleNum);
                DBSConstraintDefferability defferability;
                switch (info.defferabilityNum) {
                    case DatabaseMetaData.importedKeyInitiallyDeferred: defferability = DBSConstraintDefferability.INITIALLY_DEFERRED; break;
                    case DatabaseMetaData.importedKeyInitiallyImmediate: defferability = DBSConstraintDefferability.INITIALLY_IMMEDIATE; break;
                    case DatabaseMetaData.importedKeyNotDeferrable: defferability = DBSConstraintDefferability.NOT_DEFERRABLE; break;
                    default: defferability = DBSConstraintDefferability.UNKNOWN; break;
                }

                if (info.fkTableName == null) {
                    log.debug("Null FK table name");
                    continue;
                }
                //String fkTableFullName = DBUtils.getFullQualifiedName(getDataSource(), info.fkTableCatalog, info.fkTableSchema, info.fkTableName);
                GenericTable fkTable = getDataSource().findTable(monitor, info.fkTableCatalog, info.fkTableSchema, info.fkTableName);
                if (fkTable == null) {
                    log.warn("Can't find FK table " + info.fkTableName);
                    continue;
                }
                GenericTableColumn pkColumn = this.getColumn(monitor, info.pkColumnName);
                if (pkColumn == null) {
                    log.warn("Can't find PK column " + info.pkColumnName);
                    continue;
                }
                GenericTableColumn fkColumn = fkTable.getColumn(monitor, info.fkColumnName);
                if (fkColumn == null) {
                    log.warn("Can't find FK table " + fkTable.getFullQualifiedName() + " column " + info.fkColumnName);
                    continue;
                }

                // Find PK
                GenericPrimaryKey pk = null;
                if (info.pkName != null) {
                    pk = DBUtils.findObject(this.getConstraints(monitor), info.pkName);
                    if (pk == null) {
                        log.warn("Unique key '" + info.pkName + "' not found in table " + this.getFullQualifiedName());
                    }
                }
                if (pk == null) {
                    List<GenericPrimaryKey> uniqueKeys = this.getConstraints(monitor);
                    if (uniqueKeys != null) {
                        for (GenericPrimaryKey pkConstraint : uniqueKeys) {
                            if (pkConstraint.getConstraintType().isUnique() && pkConstraint.getColumn(monitor, pkColumn) != null) {
                                pk = pkConstraint;
                                break;
                            }
                        }
                    }
                }
                if (pk == null) {
                    log.warn("Could not find unique key for table " + this.getFullQualifiedName() + " column " + pkColumn.getName());
                    // Too bad. But we have to create new fake PK for this FK
                    //String pkFullName = getFullQualifiedName() + "." + info.pkName;
                    pk = new GenericPrimaryKey(this, info.pkName, null, DBSConstraintType.PRIMARY_KEY, true);
                    pk.addColumn(new GenericConstraintColumn(pk, pkColumn, info.keySeq));
                    // Add this fake constraint to it's owner
                    this.addUniqueKey(pk);
                }

                // Find (or create) FK
                GenericForeignKey fk = DBUtils.findObject(fkTable.getForeignKeys(monitor), info.fkName);
                if (fk == null) {
                    log.warn("Could not find foreign key '" + info.fkName + "' for table " + fkTable.getFullQualifiedName());
                    // No choice, we have to create fake foreign key :(
                } else {
                    if (!fkList.contains(fk)) {
                        fkList.add(fk);
                    }
                }

                if (fk == null) {
                    fk = fkMap.get(info.fkName);
                    if (fk == null) {
                        fk = new GenericForeignKey(fkTable, info.fkName, null, pk, deleteRule, updateRule, defferability, true);
                        fkMap.put(info.fkName, fk);
                        fkList.add(fk);
                    }
                    GenericForeignKeyColumn fkColumnInfo = new GenericForeignKeyColumn(fk, fkColumn, info.keySeq, pkColumn);
                    fk.addColumn(fkColumnInfo);
                }
            }

            return fkList;
        } catch (SQLException ex) {
            throw new DBException(ex);
        }
        finally {
            context.close();
        }
    }

}

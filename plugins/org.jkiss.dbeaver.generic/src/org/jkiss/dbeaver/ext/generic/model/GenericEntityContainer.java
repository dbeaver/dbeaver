/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.model;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * GenericEntityContainer
 */
public abstract class GenericEntityContainer implements DBSEntityContainer
{
    static final Log log = LogFactory.getLog(GenericEntityContainer.class);

    // Tables types which are not actually a table
    // This is needed for some strange JDBC drivers which returns not a table objects
    // in DatabaseMetaData.getTables method (PostgreSQL especially)
    private static final Set<String> INVALID_TABLE_TYPES = new HashSet<String>();

    static {
        INVALID_TABLE_TYPES.add("INDEX");
        INVALID_TABLE_TYPES.add("SEQUENCE");
        INVALID_TABLE_TYPES.add("SYSTEM INDEX");
        INVALID_TABLE_TYPES.add("SYSTEM SEQUENCE");
    }

    private GenericDataSource dataSource;
    private final TableCache tableCache;
    private final IndexCache indexCache;
    private final ForeignKeysCache foreignKeysCache;
    private final PrimaryKeysCache primaryKeysCache;
    private Map<String, GenericPackage> packageMap;
    private List<GenericProcedure> procedures;

    protected GenericEntityContainer(GenericDataSource dataSource)
    {
        this.dataSource = dataSource;
        this.tableCache = new TableCache(dataSource);
        this.indexCache = new IndexCache();
        this.primaryKeysCache = new PrimaryKeysCache();
        this.foreignKeysCache = new ForeignKeysCache();
    }

    final TableCache getTableCache()
    {
        return tableCache;
    }

    final IndexCache getIndexCache()
    {
        return indexCache;
    }

    final PrimaryKeysCache getPrimaryKeysCache()
    {
        return primaryKeysCache;
    }

    final ForeignKeysCache getForeignKeysCache()
    {
        return foreignKeysCache;
    }

    public GenericDataSource getDataSource()
    {
        return dataSource;
    }


    public boolean isPersisted()
    {
        return true;
    }

    public abstract GenericCatalog getCatalog();

    public abstract GenericSchema getSchema();

    public abstract DBSObject getObject();

    public List<GenericTable> getTables(DBRProgressMonitor monitor)
        throws DBException
    {
        return tableCache.getObjects(monitor);
    }

    public GenericTable getTable(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return tableCache.getObject(monitor, name);
    }

    public List<GenericIndex> getIndexes(DBRProgressMonitor monitor)
        throws DBException
    {
        cacheIndexes(monitor, true);
        return indexCache.getObjects(monitor, null);
    }

    private void cacheIndexes(DBRProgressMonitor monitor, boolean readFromTables)
        throws DBException
    {
        // Cache indexes (read all tables, all columns and all indexes in this container)
        // This doesn't work for generic datasource because metadata facilities
        // allows index query only by certain table name
        //cacheIndexes(monitor, null);
        synchronized (indexCache) {
            if (!indexCache.isCached()) {

                try {
                    // Try to load all indexes with one query
                    indexCache.getObjects(monitor, null);
                } catch (Exception e) {
                    // Failed
                    if (readFromTables) {
                        // Load indexes for all tables and return copy of them
                        List<GenericIndex> tmpIndexList = new ArrayList<GenericIndex>();
                        for (GenericTable table : getTables(monitor)) {
                            for (GenericIndex index : table.getIndexes(monitor)) {
                                tmpIndexList.add(new GenericIndex(index));
                            }
                        }
                        indexCache.setCache(tmpIndexList);
                    }
                }
            }
        }
    }

    public void cacheStructure(DBRProgressMonitor monitor, int scope)
        throws DBException
    {
        // Cache tables
        if ((scope & STRUCT_ENTITIES) != 0) {
            monitor.subTask("Cache tables");
            tableCache.getObjects(monitor);
        }

        // Cache attributes
        if ((scope & STRUCT_ATTRIBUTES) != 0) {
            // Try to cache columns
            // Cannot be sure that all jdbc drivers support reading of all catalog columns
            // So error here is not fatal
            try {
                monitor.subTask("Cache tables' columns");
                tableCache.loadChildren(monitor, null);
            } catch (Exception e) {
                log.debug(e);
            }
        }
        // Cache associations
        if ((scope & STRUCT_ASSOCIATIONS) != 0) {
            // Try to read all PKs
            // Try to read all FKs
            try {
                monitor.subTask("Cache primary keys");
                primaryKeysCache.getObjects(monitor, null);
            } catch (Exception e) {
                // Failed - seems to be unsupported feature
                log.debug(e);
            }

            // Try to read all indexes
            monitor.subTask("Cache indexes");
            cacheIndexes(monitor, false);

            // Try to read all FKs
            try {
                monitor.subTask("Cache foreign keys");
                foreignKeysCache.getObjects(monitor, null);
            } catch (Exception e) {
                // Failed - seems to be unsupported feature
                log.debug(e);
            }
        }
    }

    public Collection<GenericPackage> getPackages(DBRProgressMonitor monitor)
        throws DBException
    {
        if (procedures == null) {
            loadProcedures(monitor);
        }
        return packageMap == null ? null : packageMap.values();
    }

    public GenericPackage getPackage(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return DBUtils.findObject(getPackages(monitor), name);
    }

    public List<GenericProcedure> getProcedures(DBRProgressMonitor monitor)
        throws DBException
    {
        if (procedures == null) {
            loadProcedures(monitor);
        }
        return procedures;
    }

    public List<GenericProcedure> getProcedures(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return DBUtils.findObjects(getProcedures(monitor), name);
    }

    public Collection<? extends DBSEntity> getChildren(DBRProgressMonitor monitor)
        throws DBException
    {
        return getTables(monitor);
    }

    public DBSEntity getChild(DBRProgressMonitor monitor, String childName)
        throws DBException
    {
        return DBUtils.findObject(getChildren(monitor), childName);
    }

    public boolean refreshEntity(DBRProgressMonitor monitor)
        throws DBException
    {
        this.tableCache.clearCache();
        this.indexCache.clearCache();
        this.primaryKeysCache.clearCache();
        this.foreignKeysCache.clearCache();
        this.packageMap = null;
        this.procedures = null;
        return true;
    }

    public String toString()
    {
        return getName() == null ? "<NONE>" : getName();
    }

    private synchronized void loadProcedures(DBRProgressMonitor monitor)
        throws DBException
    {
        JDBCExecutionContext context = getDataSource().openContext(monitor, DBCExecutionPurpose.META, "Load procedures");
        try {
            JDBCResultSet dbResult = context.getMetaData().getProcedures(
                getCatalog() == null ? null : getCatalog().getName(),
                getSchema() == null ? null : getSchema().getName(),
                null);
            try {
                while (dbResult.next()) {
                    String procedureCatalog = JDBCUtils.safeGetString(dbResult, JDBCConstants.PROCEDURE_CAT);
                    String procedureName = JDBCUtils.safeGetString(dbResult, JDBCConstants.PROCEDURE_NAME);
                    String specificName = JDBCUtils.safeGetString(dbResult, JDBCConstants.SPECIFIC_NAME);
                    int procTypeNum = JDBCUtils.safeGetInt(dbResult, JDBCConstants.PROCEDURE_TYPE);
                    String remarks = JDBCUtils.safeGetString(dbResult, JDBCConstants.REMARKS);
                    DBSProcedureType procedureType;
                    switch (procTypeNum) {
                        case DatabaseMetaData.procedureNoResult: procedureType = DBSProcedureType.PROCEDURE; break;
                        case DatabaseMetaData.procedureReturnsResult: procedureType = DBSProcedureType.FUNCTION; break;
                        case DatabaseMetaData.procedureResultUnknown: procedureType = DBSProcedureType.PROCEDURE; break;
                        default: procedureType = DBSProcedureType.UNKNOWN; break;
                    }
                    // Check for packages. Oracle (and may be some other databases) uses catalog name as storage for package name
                    String packageName = null;
                    GenericPackage procedurePackage = null;
                    if (!CommonUtils.isEmpty(procedureCatalog) && CommonUtils.isEmpty(getDataSource().getCatalogs())) {
                        // Catalog name specified while there are no catalogs in data source
                        packageName = procedureCatalog;
                    }

                    if (!CommonUtils.isEmpty(packageName)) {
                        if (packageMap == null) {
                            packageMap = new TreeMap<String, GenericPackage>();
                        }
                        procedurePackage = packageMap.get(packageName);
                        if (procedurePackage == null) {
                            procedurePackage = new GenericPackage(GenericEntityContainer.this, packageName, true);
                            packageMap.put(packageName, procedurePackage);
                        }
                    }

                    final GenericProcedure procedure = new GenericProcedure(
                        procedurePackage != null ? procedurePackage : this,
                        procedureName,
                        specificName,
                        remarks,
                        procedureType);
                    if (procedurePackage != null) {
                        procedurePackage.addProcedure(procedure);
                    } else {
                        if (procedures == null) {
                            procedures = new ArrayList<GenericProcedure>();
                        }
                        procedures.add(procedure);
                    }
                }
                // Order procedures
                if (procedures != null) {
                    DBUtils.orderObjects(procedures);
                }
                if (packageMap != null) {
                    for (GenericPackage pack : packageMap.values()) {
                        pack.orderProcedures();
                    }
                }
            }
            finally {
                dbResult.close();
            }
        } catch (SQLException e) {
            throw new DBException(e);
        } finally {
            context.close();
        }
    }

    /**
     * Tables cache implementation
     */
    class TableCache extends JDBCStructCache<GenericTable, GenericTableColumn> {

        protected TableCache(GenericDataSource dataSource)
        {
            super(dataSource, JDBCConstants.TABLE_NAME);
            setListOrderComparator(DBUtils.<GenericTable>nameComparator());
        }

        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context)
            throws SQLException, DBException
        {
            return context.getMetaData().getTables(
                getCatalog() == null ? null : getCatalog().getName(),
                getSchema() == null ? null : getSchema().getName(),
                null,
                null).getSource();
        }

        protected GenericTable fetchObject(JDBCExecutionContext context, ResultSet dbResult)
            throws SQLException, DBException
        {
            String tableName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_NAME);
            String tableType = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_TYPE);
            String remarks = JDBCUtils.safeGetString(dbResult, JDBCConstants.REMARKS);

            if (tableType != null && INVALID_TABLE_TYPES.contains(tableType)) {
                // Bad table type. Just skip it
                return null;
            }

            boolean isSystemTable = tableType != null && tableType.toUpperCase().indexOf("SYSTEM") != -1;
            if (isSystemTable && !getDataSource().getContainer().isShowSystemObjects()) {
                return null;
            }

            // Skip "recycled" tables (Oracle)
            if (tableName.startsWith("BIN$")) {
                return null;
            }
/*
            // Do not read table type object
            // Actually dunno what to do with it and it often throws stupid warnings in debug

            String typeName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TYPE_NAME);
            String typeCatalogName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TYPE_CAT);
            String typeSchemaName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TYPE_SCHEM);
            GenericCatalog typeCatalog = CommonUtils.isEmpty(typeCatalogName) ?
                null :
                getDataSourceContainer().getCatalog(context.getProgressMonitor(), typeCatalogName);
            GenericSchema typeSchema = CommonUtils.isEmpty(typeSchemaName) ?
                null :
                typeCatalog == null ?
                    getDataSourceContainer().getSchema(context.getProgressMonitor(), typeSchemaName) :
                    typeCatalog.getSchema(typeSchemaName);
*/
            return new GenericTable(
                GenericEntityContainer.this,
                tableName,
                tableType,
                remarks,
                true);
        }

        protected boolean isChildrenCached(GenericTable table)
        {
            return table.isColumnsCached();
        }

        protected void cacheChildren(GenericTable table, List<GenericTableColumn> columns)
        {
            table.setColumns(columns);
        }

        protected JDBCPreparedStatement prepareChildrenStatement(JDBCExecutionContext context, GenericTable forTable)
            throws SQLException, DBException
        {
            return context.getMetaData().getColumns(
                getCatalog() == null ? null : getCatalog().getName(),
                getSchema() == null ? null : getSchema().getName(),
                forTable == null ? null : forTable.getName(),
                null).getSource();
        }

        protected GenericTableColumn fetchChild(JDBCExecutionContext context, GenericTable table, ResultSet dbResult)
            throws SQLException, DBException
        {
            String columnName = JDBCUtils.safeGetString(dbResult, JDBCConstants.COLUMN_NAME);
            int valueType = JDBCUtils.safeGetInt(dbResult, JDBCConstants.DATA_TYPE);
            int sourceType = JDBCUtils.safeGetInt(dbResult, JDBCConstants.SOURCE_DATA_TYPE);
            String typeName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TYPE_NAME);
            long columnSize = JDBCUtils.safeGetLong(dbResult, JDBCConstants.COLUMN_SIZE);
            boolean isNullable = JDBCUtils.safeGetInt(dbResult, JDBCConstants.NULLABLE) != DatabaseMetaData.columnNoNulls;
            int scale = JDBCUtils.safeGetInt(dbResult, JDBCConstants.DECIMAL_DIGITS);
            int precision = 0;//GenericUtils.safeGetInt(dbResult, JDBCConstants.COLUMN_);
            int radix = JDBCUtils.safeGetInt(dbResult, JDBCConstants.NUM_PREC_RADIX);
            String defaultValue = JDBCUtils.safeGetString(dbResult, JDBCConstants.COLUMN_DEF);
            String remarks = JDBCUtils.safeGetString(dbResult, JDBCConstants.REMARKS);
            long charLength = JDBCUtils.safeGetLong(dbResult, JDBCConstants.CHAR_OCTET_LENGTH);
            int ordinalPos = JDBCUtils.safeGetInt(dbResult, JDBCConstants.ORDINAL_POSITION);
            boolean autoIncrement = "YES".equals(JDBCUtils.safeGetString(dbResult, JDBCConstants.IS_AUTOINCREMENT));
            if (valueType == java.sql.Types.OTHER && !CommonUtils.isEmpty(typeName)) {
                // Try to determine value type from type name
                valueType = JDBCUtils.getDataTypeByName(valueType, typeName);
            }
            // Check for identity modifier [DBSPEC: MS SQL]
            if (typeName.toUpperCase().endsWith(GenericConstants.TYPE_MODIFIER_IDENTITY)) {
                autoIncrement = true;
                typeName = typeName.substring(0, typeName.length() - GenericConstants.TYPE_MODIFIER_IDENTITY.length());
            }

            return new GenericTableColumn(
                table,
                columnName,
                typeName, valueType, sourceType, ordinalPos,
                columnSize,
                charLength, scale, precision, radix, isNullable,
                remarks, defaultValue, autoIncrement
            );
        }
    }

    /**
     * Index cache implementation
     */
    class IndexCache extends JDBCCompositeCache<GenericTable, GenericIndex, GenericIndexColumn> {
        protected IndexCache()
        {
            super(tableCache, JDBCConstants.TABLE_NAME, JDBCConstants.INDEX_NAME);
        }

        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context, GenericTable forParent)
            throws SQLException, DBException
        {
            try {
                return context.getMetaData().getIndexInfo(
                        getCatalog() == null ? null : getCatalog().getName(),
                        getSchema() == null ? null : getSchema().getName(),
                        // oracle fails if unquoted complex identifier specified
                        // but other DBs (and logically it's correct) do not want quote chars in this query
                        // so let's fix it in oracle plugin
                        forParent == null ? null : forParent.getName(), //DBUtils.getQuotedIdentifier(getDataSourceContainer(), forTable.getName()),
                        true,
                        true).getSource();
            } catch (SQLException e) {
                throw e;
            } catch (Exception e) {
                if (forParent == null) {
                    throw new DBException("Global indexes read not supported", e);
                } else {
                    throw new DBException(e);
                }
            }
        }

        protected GenericIndex fetchObject(JDBCExecutionContext context, ResultSet dbResult, GenericTable parent, String indexName)
            throws SQLException, DBException
        {
            boolean isNonUnique = JDBCUtils.safeGetBoolean(dbResult, JDBCConstants.NON_UNIQUE);
            String indexQualifier = JDBCUtils.safeGetString(dbResult, JDBCConstants.INDEX_QUALIFIER);
            int indexTypeNum = JDBCUtils.safeGetInt(dbResult, JDBCConstants.TYPE);

            DBSIndexType indexType;
            switch (indexTypeNum) {
                case DatabaseMetaData.tableIndexStatistic: indexType = DBSIndexType.STATISTIC; break;
                case DatabaseMetaData.tableIndexClustered: indexType = DBSIndexType.CLUSTERED; break;
                case DatabaseMetaData.tableIndexHashed: indexType = DBSIndexType.HASHED; break;
                case DatabaseMetaData.tableIndexOther: indexType = DBSIndexType.OTHER; break;
                default: indexType = DBSIndexType.UNKNOWN; break;
            }

            return new GenericIndex(
                parent,
                isNonUnique,
                indexQualifier,
                indexName,
                indexType);
        }

        protected GenericIndexColumn fetchObjectRow(
            JDBCExecutionContext context,
            ResultSet dbResult,
            GenericTable parent,
            GenericIndex object)
            throws SQLException, DBException
        {
            int ordinalPosition = JDBCUtils.safeGetInt(dbResult, JDBCConstants.ORDINAL_POSITION);
            String columnName = JDBCUtils.safeGetString(dbResult, JDBCConstants.COLUMN_NAME);
            String ascOrDesc = JDBCUtils.safeGetString(dbResult, JDBCConstants.ASC_OR_DESC);

            GenericTableColumn tableColumn = parent.getColumn(context.getProgressMonitor(), columnName);
            if (tableColumn == null) {
                log.debug("Column '" + columnName + "' not found in table '" + parent.getName() + "' for index '" + object.getName() + "'");
                return null;
            }

            return new GenericIndexColumn(
                object,
                tableColumn,
                ordinalPosition,
                !"D".equalsIgnoreCase(ascOrDesc));
        }

        protected boolean isObjectsCached(GenericTable parent)
        {
            return parent.isIndexesCached();
        }

        protected void cacheObjects(GenericTable parent, List<GenericIndex> indexes)
        {
            parent.setIndexes(indexes);
        }

        protected void cacheRows(GenericIndex index, List<GenericIndexColumn> rows)
        {
            index.setColumns(rows);
        }
    }

    /**
     * Index cache implementation
     */
    class PrimaryKeysCache extends JDBCCompositeCache<GenericTable, GenericPrimaryKey, GenericConstraintColumn> {
        protected PrimaryKeysCache()
        {
            super(tableCache, JDBCConstants.TABLE_NAME, JDBCConstants.PK_NAME);
        }

        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context, GenericTable forParent)
            throws SQLException, DBException
        {
            try {
                return context.getMetaData().getPrimaryKeys(
                        getCatalog() == null ? null : getCatalog().getName(),
                        getSchema() == null ? null : getSchema().getName(),
                        forParent == null ? null : forParent.getName())
                    .getSource();
            } catch (SQLException e) {
                throw e;
            } catch (Exception e) {
                if (forParent == null) {
                    throw new DBException("Global primary keys read not supported", e);
                } else {
                    throw new DBException(e);
                }
            }
        }

        protected GenericPrimaryKey fetchObject(JDBCExecutionContext context, ResultSet dbResult, GenericTable parent, String pkName)
            throws SQLException, DBException
        {
            return new GenericPrimaryKey(
                parent,
                pkName,
                null,
                DBSConstraintType.PRIMARY_KEY);
        }

        protected GenericConstraintColumn fetchObjectRow(
            JDBCExecutionContext context,
            ResultSet dbResult,
            GenericTable parent,
            GenericPrimaryKey object)
            throws SQLException, DBException
        {
            String columnName = JDBCUtils.safeGetString(dbResult, JDBCConstants.COLUMN_NAME);
            int keySeq = JDBCUtils.safeGetInt(dbResult, JDBCConstants.KEY_SEQ);

            GenericTableColumn tableColumn = parent.getColumn(context.getProgressMonitor(), columnName);
            if (tableColumn == null) {
                log.warn("Column '" + columnName + "' not found in table '" + parent.getFullQualifiedName() + "' for PK '" + object.getFullQualifiedName() + "'");
                return null;
            }

            return new GenericConstraintColumn(object, tableColumn, keySeq);
        }

        protected boolean isObjectsCached(GenericTable parent)
        {
            return parent.isConstraintsCached();
        }

        protected void cacheObjects(GenericTable parent, List<GenericPrimaryKey> primaryKeys)
        {
            parent.setConstraints(primaryKeys);
        }

        protected void cacheRows(GenericPrimaryKey primaryKey, List<GenericConstraintColumn> rows)
        {
            primaryKey.setColumns(rows);
        }
    }

    class ForeignKeysCache extends JDBCCompositeCache<GenericTable, GenericForeignKey, GenericForeignKeyColumn> {

        Map<String, GenericPrimaryKey> pkMap = new HashMap<String, GenericPrimaryKey>();

        protected ForeignKeysCache()
        {
            super(tableCache, JDBCConstants.FKTABLE_NAME, JDBCConstants.FK_NAME);
        }

        @Override
        public void clearCache()
        {
            pkMap.clear();
            super.clearCache();
        }

        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context, GenericTable forParent)
            throws SQLException, DBException
        {
            return context.getMetaData().getImportedKeys(
                getCatalog() == null ? null : getCatalog().getName(),
                getSchema() == null ? null : getSchema().getName(),
                forParent == null ? null : forParent.getName())
                .getSource();
        }

        protected GenericForeignKey fetchObject(JDBCExecutionContext context, ResultSet dbResult, GenericTable parent, String fkName)
            throws SQLException, DBException
        {
            String pkTableCatalog = JDBCUtils.safeGetString(dbResult, JDBCConstants.PKTABLE_CAT);
            String pkTableSchema = JDBCUtils.safeGetString(dbResult, JDBCConstants.PKTABLE_SCHEM);
            String pkTableName = JDBCUtils.safeGetString(dbResult, JDBCConstants.PKTABLE_NAME);

            int keySeq = JDBCUtils.safeGetInt(dbResult, JDBCConstants.KEY_SEQ);
            int updateRuleNum = JDBCUtils.safeGetInt(dbResult, JDBCConstants.UPDATE_RULE);
            int deleteRuleNum = JDBCUtils.safeGetInt(dbResult, JDBCConstants.DELETE_RULE);
            String pkName = JDBCUtils.safeGetString(dbResult, JDBCConstants.PK_NAME);
            int defferabilityNum = JDBCUtils.safeGetInt(dbResult, JDBCConstants.DEFERRABILITY);

            DBSConstraintCascade deleteRule = JDBCUtils.getCascadeFromNum(deleteRuleNum);
            DBSConstraintCascade updateRule = JDBCUtils.getCascadeFromNum(updateRuleNum);
            DBSConstraintDefferability defferability;
            switch (defferabilityNum) {
                case DatabaseMetaData.importedKeyInitiallyDeferred: defferability = DBSConstraintDefferability.INITIALLY_DEFERRED; break;
                case DatabaseMetaData.importedKeyInitiallyImmediate: defferability = DBSConstraintDefferability.INITIALLY_IMMEDIATE; break;
                case DatabaseMetaData.importedKeyNotDeferrable: defferability = DBSConstraintDefferability.NOT_DEFERRABLE; break;
                default: defferability = DBSConstraintDefferability.UNKNOWN; break;
            }

            if (pkTableName == null) {
                log.debug("Null PK table name");
                return null;
            }
            //String pkTableFullName = DBUtils.getFullQualifiedName(getDataSource(), pkTableCatalog, pkTableSchema, pkTableName);
            GenericTable pkTable = getDataSource().findTable(context.getProgressMonitor(), pkTableCatalog, pkTableSchema, pkTableName);
            if (pkTable == null) {
                log.warn("Can't find PK table " + pkTableName);
                return null;
            }

            // Find PK
            GenericPrimaryKey pk = null;
            if (pkName != null) {
                pk = DBUtils.findObject(pkTable.getUniqueKeys(context.getProgressMonitor()), pkName);
                if (pk == null) {
                    log.warn("Unique key '" + pkName + "' not found in table " + pkTable.getFullQualifiedName());
                }
            }
            if (pk == null) {
                String pkColumnName = JDBCUtils.safeGetString(dbResult, JDBCConstants.PKCOLUMN_NAME);
                GenericTableColumn pkColumn = pkTable.getColumn(context.getProgressMonitor(), pkColumnName);
                if (pkColumn == null) {
                    log.warn("Can't find PK table " + pkTable.getFullQualifiedName() + " column " + pkColumnName);
                    return null;
                }

                List<GenericPrimaryKey> uniqueKeys = pkTable.getUniqueKeys(context.getProgressMonitor());
                if (uniqueKeys != null) {
                    for (GenericPrimaryKey pkConstraint : uniqueKeys) {
                        if (pkConstraint.getConstraintType().isUnique() && pkConstraint.getColumn(context.getProgressMonitor(), pkColumn) != null) {
                            pk = pkConstraint;
                            break;
                        }
                    }
                }
                if (pk == null) {
                    log.warn("Could not find unique key for table " + pkTable.getFullQualifiedName() + " column " + pkColumn.getName());
                    // Too bad. But we have to create new fake PK for this FK
                    String pkFullName = pkTable.getFullQualifiedName() + "." + pkName;
                    pk = pkMap.get(pkFullName);
                    if (pk == null) {
                        pk = new GenericPrimaryKey(pkTable, pkName, null, DBSConstraintType.PRIMARY_KEY);
                        pkMap.put(pkFullName, pk);
                        // Add this fake constraint to it's owner
                        pk.getTable().addConstraint(pk);
                    }
                    pk.addColumn(new GenericConstraintColumn(pk, pkColumn, keySeq));
                }
            }

            return new GenericForeignKey(parent, fkName, null, pk, deleteRule, updateRule, defferability);
        }

        protected GenericForeignKeyColumn fetchObjectRow(
            JDBCExecutionContext context,
            ResultSet dbResult,
            GenericTable parent,
            GenericForeignKey foreignKey)
            throws SQLException, DBException
        {
            String pkColumnName = JDBCUtils.safeGetString(dbResult, JDBCConstants.PKCOLUMN_NAME);
            GenericConstraintColumn pkColumn = (GenericConstraintColumn)foreignKey.getReferencedKey().getColumn(context.getProgressMonitor(), pkColumnName);
            if (pkColumn == null) {
                log.warn("Can't find PK table " + foreignKey.getReferencedKey().getTable().getFullQualifiedName() + " column " + pkColumnName);
                return null;
            }
            int keySeq = JDBCUtils.safeGetInt(dbResult, JDBCConstants.KEY_SEQ);

            String fkColumnName = JDBCUtils.safeGetString(dbResult, JDBCConstants.FKCOLUMN_NAME);
            GenericTableColumn fkColumn = foreignKey.getTable().getColumn(context.getProgressMonitor(), fkColumnName);
            if (fkColumn == null) {
                log.warn("Can't find FK table " + foreignKey.getTable().getFullQualifiedName() + " column " + fkColumnName);
                return null;
            }

            return new GenericForeignKeyColumn(foreignKey, fkColumn, keySeq, pkColumn.getTableColumn());
        }

        protected boolean isObjectsCached(GenericTable parent)
        {
            return parent.isForeignKeysCached();
        }

        protected void cacheObjects(GenericTable parent, List<GenericForeignKey> foreignKeys)
        {
            parent.setForeignKeys(foreignKeys);
        }

        protected void cacheRows(GenericForeignKey foreignKey, List<GenericForeignKeyColumn> rows)
        {
            foreignKey.setColumns(rows);
        }

    }

}

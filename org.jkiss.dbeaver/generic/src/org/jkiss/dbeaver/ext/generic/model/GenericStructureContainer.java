package org.jkiss.dbeaver.ext.generic.model;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.TreeMap;

/**
 * GenericStructureContainer
 */
public abstract class GenericStructureContainer implements DBSStructureContainer, DBSStructureAssistant
{
    static Log log = LogFactory.getLog(GenericStructureContainer.class);

    private List<GenericTable> tableList;
    private Map<String, GenericTable> tableMap;
    private List<GenericIndex> indexList;
    private List<GenericConstraint> constraintList;
    private List<GenericProcedure> procedures;
    private boolean columnsCached = false;
    private boolean indexesCached = false;

    protected GenericStructureContainer()
    {
    }

    public abstract GenericDataSource getDataSource();

    public abstract GenericCatalog getCatalog();

    public abstract GenericSchema getSchema();

    public abstract DBSObject getObject();

    public List<GenericTable> getTables(DBRProgressMonitor monitor)
        throws DBException
    {
        this.cacheTables(monitor);
        return tableList;
    }

    public GenericTable getTable(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        this.cacheTables(monitor);
        return tableMap.get(name);
    }

    public List<GenericIndex> getIndexes(DBRProgressMonitor monitor)
        throws DBException
    {
        // Cache indexes (read all tables, all columns and all indexes in this container)
        // This doesn't work for generic datasource because metadata facilities
        // allows index query only by certain table name
        //cacheIndexes(monitor, null);

        if (indexList == null) {
            // Load indexes for all tables and return copy of them
            List<GenericIndex> tmpIndexList = new ArrayList<GenericIndex>();
            for (GenericTable table : getTables(monitor)) {
                for (GenericIndex index : table.getIndexes(monitor)) {
                    tmpIndexList.add(new GenericIndex(index));
                }
            }
            indexList = tmpIndexList;
        }
        return indexList;
    }

    public List<GenericConstraint> getConstraints(DBRProgressMonitor monitor)
        throws DBException
    {
        if (constraintList == null) {
            List<GenericConstraint> tmpConstrList = new ArrayList<GenericConstraint>();
            for (GenericTable table : getTables(monitor)) {
                for (GenericConstraint constraint : table.getConstraints(monitor)) {
                    tmpConstrList.add(new GenericConstraint(constraint));
                }
            }
            constraintList = tmpConstrList;
        }
        return constraintList;
    }

    public void cacheStructure(DBRProgressMonitor monitor, int scope)
    {
        System.out.println("CACHE STRUCTURE!");
    }

    public List<GenericProcedure> getProcedures(DBRProgressMonitor monitor)
        throws DBException
    {
        cacheProcedures(monitor);
        return procedures;
    }

    private void cacheTables(DBRProgressMonitor monitor)
        throws DBException
    {
        if (this.tableList != null) {
            return;
        }
        JDBCUtils.startBlockingOperation(monitor, getDataSource(), "Loading '" + getName() + "' tables");

        List<GenericTable> tmpTableList = new ArrayList<GenericTable>();
        Map<String, GenericTable> tmpTableMap = new HashMap<String, GenericTable>();
        try {
            DatabaseMetaData metaData = getDataSource().getConnection().getMetaData();
            String catalogName = getCatalog() == null ? null : getCatalog().getName();
            String schemaName = getSchema() == null ? null : getSchema().getName();

            // Load tables
            ResultSet dbResult = metaData.getTables(
                catalogName,
                schemaName,
                null,
                null);//getDataSource().getTableTypes());
            try {
                while (dbResult.next()) {

                    String tableName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_NAME);
                    String tableType = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_TYPE);
                    String remarks = JDBCUtils.safeGetString(dbResult, JDBCConstants.REMARKS);

                    boolean isSystemTable = tableType != null && tableType.toUpperCase().indexOf("SYSTEM") != -1;
                    if (isSystemTable && !getDataSource().getContainer().isShowSystemObjects()) {
                        continue;
                    }
                    String typeName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TYPE_NAME);
                    String typeCatalogName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TYPE_CAT);
                    String typeSchemaName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TYPE_SCHEM);
                    GenericCatalog typeCatalog = CommonUtils.isEmpty(typeCatalogName) ?
                        null :
                        getDataSource().getCatalog(monitor, typeCatalogName);
                    GenericSchema typeSchema = CommonUtils.isEmpty(typeSchemaName) ?
                        null :
                        typeCatalog == null ?
                            getDataSource().getSchema(monitor, typeSchemaName) :
                            typeCatalog.getSchema(typeSchemaName);
                    GenericTable table = new GenericTable(
                        this,
                        tableName,
                        tableType,
                        remarks,
                        typeName,
                        typeCatalog,
                        typeSchema);
                    tmpTableList.add(table);
                    tmpTableMap.put(tableName, table);

                    monitor.subTask(tableName);
                    if (monitor.isCanceled()) {
                        break;
                    }
                }
            }
            finally {
                JDBCUtils.safeClose(dbResult);
            }
        }
        catch (SQLException ex) {
            throw new DBException(ex);
        }
        finally {
            JDBCUtils.endBlockingOperation(monitor);
        }

        this.tableList = tmpTableList;
        this.tableMap = tmpTableMap;
    }

    private void cacheProcedures(DBRProgressMonitor monitor)
        throws DBException
    {
        if (procedures != null) {
            return;
        }
        JDBCUtils.startBlockingOperation(monitor, getDataSource(), "Loading '" + getName() + "' procedures");
        List<GenericProcedure> tmpProcedureList = new ArrayList<GenericProcedure>();

        try {
            DatabaseMetaData metaData = getDataSource().getConnection().getMetaData();
            String catalogName = getCatalog() == null ? null : getCatalog().getName();
            String schemaName = getSchema() == null ? null : getSchema().getName();

            // Load procedures
            ResultSet dbResult = metaData.getProcedures(
                catalogName,
                schemaName,
                null);
            try {
                while (dbResult.next()) {
                    String procedureName = JDBCUtils.safeGetString(dbResult, JDBCConstants.PROCEDURE_NAME);
                    int procTypeNum = JDBCUtils.safeGetInt(dbResult, JDBCConstants.PROCEDURE_TYPE);
                    String remarks = JDBCUtils.safeGetString(dbResult, JDBCConstants.REMARKS);
                    DBSProcedureType procedureType;
                    switch (procTypeNum) {
                        case DatabaseMetaData.procedureNoResult: procedureType = DBSProcedureType.PROCEDURE; break;
                        case DatabaseMetaData.procedureReturnsResult: procedureType = DBSProcedureType.FUNCTION; break;
                        case DatabaseMetaData.procedureResultUnknown: procedureType = DBSProcedureType.PROCEDURE; break;
                        default: procedureType = DBSProcedureType.UNKNOWN; break;
                    }
                    GenericProcedure procedure = new GenericProcedure(
                        this,
                        procedureName,
                        remarks, procedureType
                    );
                    tmpProcedureList.add(procedure);
                }
            }
            finally {
                JDBCUtils.safeClose(dbResult);
            }
        }
        catch (SQLException ex) {
            throw new DBException(ex);
        }
        finally {
            JDBCUtils.endBlockingOperation(monitor);
        }
        this.procedures = tmpProcedureList;
    }

    public List<DBSTablePath> findTableNames(DBRProgressMonitor monitor, String tableMask, int maxResults) throws DBException
    {
        JDBCUtils.startBlockingOperation(monitor, getDataSource(), "Looking for tables in '" + getName() + "'");

        List<DBSTablePath> pathList = new ArrayList<DBSTablePath>();
        try {
            DatabaseMetaData metaData = getDataSource().getConnection().getMetaData();
            String catalogName = getCatalog() == null ? null : getCatalog().getName();
            String schemaName = getSchema() == null ? null : getSchema().getName();

            // Make table mask uppercase
            tableMask = tableMask.toUpperCase();

            // Load tables
            ResultSet dbResult = metaData.getTables(
                catalogName,
                schemaName,
                tableMask,
                null);
            try {
                int tableNum = maxResults;
                while (dbResult.next() && tableNum-- > 0) {

                    catalogName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_CAT);
                    schemaName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_SCHEM);
                    String tableName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_NAME);
                    String tableType = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_TYPE);
                    String remarks = JDBCUtils.safeGetString(dbResult, JDBCConstants.REMARKS);

                    pathList.add(
                        new DBSTablePath(
                            catalogName,
                            schemaName,
                            tableName,
                            tableType,
                            remarks));
                }
            }
            finally {
                JDBCUtils.safeClose(dbResult);
            }
            return pathList;
        }
        catch (SQLException ex) {
            throw new DBException(ex);
        }
        finally {
            JDBCUtils.endBlockingOperation(monitor);
        }
    }

    /**
     * Reads table columns from database
     * @param monitor monitor
     * @param forTable table for which to read columns. If null then reads columns for all tables in this container.
     * @throws DBException on error
     */
    void cacheColumns(DBRProgressMonitor monitor, final GenericTable forTable)
        throws DBException
    {
        if (this.columnsCached) {
            return;
        }
        if (forTable == null) {
            cacheTables(monitor);
        } else if (forTable.isColumnsCached()) {
            return;
        }

        monitor.beginTask("Loading table columns", 1);
        try {
            Map<GenericTable, List<GenericTableColumn>> columnMap = new HashMap<GenericTable, List<GenericTableColumn>>();

            DatabaseMetaData metaData = getDataSource().getConnection().getMetaData();
            String catalogName = getCatalog() == null ? null : getCatalog().getName();
            String schemaName = getSchema() == null ? null : getSchema().getName();

            // Load columns
            ResultSet dbResult = metaData.getColumns(
                catalogName,
                schemaName,
                forTable == null ? null : forTable.getName(),
                null);
            try {
                while (dbResult.next()) {
                    String tableName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_NAME);
                    String columnName = JDBCUtils.safeGetString(dbResult, JDBCConstants.COLUMN_NAME);
                    int valueType = JDBCUtils.safeGetInt(dbResult, JDBCConstants.DATA_TYPE);
                    int sourceType = JDBCUtils.safeGetInt(dbResult, JDBCConstants.SOURCE_DATA_TYPE);
                    String typeName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TYPE_NAME);
                    int columnSize = JDBCUtils.safeGetInt(dbResult, JDBCConstants.COLUMN_SIZE);
                    boolean isNullable = JDBCUtils.safeGetInt(dbResult, JDBCConstants.NULLABLE) != DatabaseMetaData.columnNoNulls;
                    int scale = JDBCUtils.safeGetInt(dbResult, JDBCConstants.DECIMAL_DIGITS);
                    int precision = 0;//GenericUtils.safeGetInt(dbResult, JDBCConstants.COLUMN_);
                    int radix = JDBCUtils.safeGetInt(dbResult, JDBCConstants.NUM_PREC_RADIX);
                    String defaultValue = JDBCUtils.safeGetString(dbResult, JDBCConstants.COLUMN_DEF);
                    String remarks = JDBCUtils.safeGetString(dbResult, JDBCConstants.REMARKS);
                    int charLength = JDBCUtils.safeGetInt(dbResult, JDBCConstants.CHAR_OCTET_LENGTH);
                    int ordinalPos = JDBCUtils.safeGetInt(dbResult, JDBCConstants.ORDINAL_POSITION);

                    GenericTable table = forTable;
                    if (table == null) {
                        table = tableMap.get(tableName);
                        if (table == null) {
                            log.warn("Column '" + columnName + "' owner table '" + tableName + "' not found");
                            continue;
                        }
                    }
                    if (table.isColumnsCached()) {
                        // Already read
                        continue;
                    }
                    GenericTableColumn tableColumn = new GenericTableColumn(
                        table,
                        columnName,
                        typeName, valueType, sourceType, ordinalPos,
                        columnSize,
                        charLength, scale, precision, radix, isNullable,
                        remarks, defaultValue
                    );

                    // Add to map
                    List<GenericTableColumn> columns = columnMap.get(table);
                    if (columns == null) {
                        columns = new ArrayList<GenericTableColumn>();
                        columnMap.put(table, columns);
                    }
                    columns.add(tableColumn);
                }

                // All columns are read. Now assign them to tables
                for (Map.Entry<GenericTable, List<GenericTableColumn>> colEntry : columnMap.entrySet()) {
                    colEntry.getKey().setColumns(colEntry.getValue());
                }
                // Now set empty column list for other tables
                if (forTable == null) {
                    for (GenericTable tmpTable : tableList) {
                        if (!columnMap.containsKey(tmpTable)) {
                            tmpTable.setColumns(new ArrayList<GenericTableColumn>());
                        }
                    }
                } else if (!columnMap.containsKey(forTable)) {
                    forTable.setColumns(new ArrayList<GenericTableColumn>());
                }

                if (forTable == null) {
                    this.columnsCached = true;
                }
            }
            finally {
                JDBCUtils.safeClose(dbResult);
            }
        }
        catch (SQLException ex) {
            throw new DBException(ex);
        }
        finally {
            monitor.done();
        }
    }

    void cacheIndexes(DBRProgressMonitor monitor, GenericTable forTable)
        throws DBException
    {
        if (this.indexesCached) {
            return;
        }

        // Load tables and columns first
        cacheColumns(monitor, forTable);
        if (forTable != null && forTable.isIndexesCached()) {
            return;
        }

        // Load index columns
        try {
            Map<GenericTable, Map<String, GenericIndex>> tableIndexMap = new HashMap<GenericTable, Map<String, GenericIndex>>();

            DatabaseMetaData metaData = getDataSource().getConnection().getMetaData();
            // Load indexes
            ResultSet dbResult = metaData.getIndexInfo(
                getCatalog() == null ? null : getCatalog().getName(),
                getSchema() == null ? null : getSchema().getName(),
                // oracle fails if unquoted complex identifier specified
                // but other DBs (and logically it's correct) do not want quote chars in this query
                // so let's fix it in oracle plugin
                forTable == null ? null : forTable.getName(), //DBSUtils.getQuotedIdentifier(getDataSource(), forTable.getName()),
                false,
                false);
            try {
                while (dbResult.next()) {
                    String tableName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_NAME);
                    String indexName = JDBCUtils.safeGetString(dbResult, JDBCConstants.INDEX_NAME);
                    boolean isNonUnique = JDBCUtils.safeGetBoolean(dbResult, JDBCConstants.NON_UNIQUE);
                    String indexQualifier = JDBCUtils.safeGetString(dbResult, JDBCConstants.INDEX_QUALIFIER);
                    int indexTypeNum = JDBCUtils.safeGetInt(dbResult, JDBCConstants.TYPE);

                    int ordinalPosition = JDBCUtils.safeGetInt(dbResult, JDBCConstants.ORDINAL_POSITION);
                    String columnName = JDBCUtils.safeGetString(dbResult, JDBCConstants.COLUMN_NAME);
                    String ascOrDesc = JDBCUtils.safeGetString(dbResult, JDBCConstants.ASC_OR_DESC);

                    if (CommonUtils.isEmpty(indexName) || CommonUtils.isEmpty(tableName)) {
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
                    GenericTable table = forTable;
                    if (table == null) {
                        table = tableMap.get(tableName);
                        if (table == null) {
                            log.warn("Index '" + indexName + "' owner table '" + tableName + "' not found");
                            continue;
                        }
                    }
                    if (table.isIndexesCached()) {
                        // Already read
                        continue;
                    }
                    // Add to map
                    Map<String, GenericIndex> indexMap = tableIndexMap.get(table);
                    if (indexMap == null) {
                        indexMap = new TreeMap<String, GenericIndex>();
                        tableIndexMap.put(table, indexMap);
                    }

                    GenericIndex index = indexMap.get(indexName);
                    if (index == null) {
                        index = new GenericIndex(
                            table,
                            isNonUnique,
                            indexQualifier,
                            indexName,
                            indexType);
                        indexMap.put(indexName, index);
                    }
                    GenericTableColumn tableColumn = table.getColumn(monitor, columnName);
                    if (tableColumn == null) {
                        log.warn("Column '" + columnName + "' not found in table '" + this.getName() + "'");
                        continue;
                    }
                    index.addColumn(
                        new GenericIndexColumn(
                            index,
                            tableColumn,
                            ordinalPosition,
                            !"D".equalsIgnoreCase(ascOrDesc)));
                }

                // All indexes are read. Now assign them to tables
                for (Map.Entry<GenericTable,Map<String,GenericIndex>> colEntry : tableIndexMap.entrySet()) {
                    colEntry.getKey().setIndexes(new ArrayList<GenericIndex>(colEntry.getValue().values()));
                }
                // Now set empty index list for other tables
                if (forTable == null) {
                    for (GenericTable tmpTable : tableList) {
                        if (!tableIndexMap.containsKey(tmpTable)) {
                            tmpTable.setIndexes(new ArrayList<GenericIndex>());
                        }
                    }
                } else if (!tableIndexMap.containsKey(forTable)) {
                    forTable.setIndexes(new ArrayList<GenericIndex>());
                }

                if (forTable == null) {
                    this.indexesCached = true;
                }
            }
            finally {
                JDBCUtils.safeClose(dbResult);
            }
        } catch (SQLException ex) {
            throw new DBException(ex);
        }
    }

    public Collection<? extends DBSObject> getChildren(DBRProgressMonitor monitor)
        throws DBException
    {
        return getTables(monitor);
    }

    public DBSObject getChild(DBRProgressMonitor monitor, String childName)
        throws DBException
    {
        return DBSUtils.findObject(getChildren(monitor), childName);
    }

    public boolean refreshObject(DBRProgressMonitor monitor)
        throws DBException
    {
        this.tableList = null;
        this.tableMap = null;
        this.indexList = null;
        this.constraintList = null;
        this.procedures = null;
        columnsCached = false;
        indexesCached = false;
        return true;
    }

    public String toString()
    {
        return getName() == null ? "<NONE>" : getName();
    }
}

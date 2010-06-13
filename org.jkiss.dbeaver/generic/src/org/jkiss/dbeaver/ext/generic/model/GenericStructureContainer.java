package org.jkiss.dbeaver.ext.generic.model;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.api.ResultSetStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCTableCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.PreparedStatement;
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

    private TableCache tableCache;
    private List<GenericIndex> indexList;
    private List<GenericConstraint> constraintList;
    private ProceduresCache procedureCache;
    private boolean indexesCached = false;

    protected GenericStructureContainer()
    {
        this.tableCache = new TableCache();
        this.procedureCache = new ProceduresCache();
    }

    final TableCache getTableCache()
    {
        return tableCache;
    }

    public abstract GenericDataSource getDataSource();

    public abstract GenericCatalog getCatalog();

    public abstract GenericSchema getSchema();

    public abstract DBSObject getObject();

    public List<GenericTable> getTables(DBRProgressMonitor monitor)
        throws DBException
    {
        return tableCache.getTables(monitor);
    }

    public GenericTable getTable(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return tableCache.getTable(monitor, name);
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
        throws DBException
    {
        tableCache.getTables(monitor);
        if ((scope & STRUCT_ATTRIBUTES) != 0) {
            // Do not use columns cache
            // Cannot be sure that all jdbc drivers support reading of all catalog columns
            //tableCache.cacheColumns(monitor, null);
        }
    }

    public List<GenericProcedure> getProcedures(DBRProgressMonitor monitor)
        throws DBException
    {
        return procedureCache.getObjects(monitor);
    }

    public List<DBSTablePath> findTableNames(DBRProgressMonitor monitor, String tableMask, int maxResults) throws DBException
    {
        JDBCUtils.startConnectionBlock(monitor, getDataSource(), "Looking for tables in '" + getName() + "'");

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
            JDBCUtils.endConnectionBlock(monitor);
        }
    }

    void cacheIndexes(DBRProgressMonitor monitor, GenericTable forTable)
        throws DBException
    {
        if (this.indexesCached) {
            return;
        }

        // Load tables and columns first
        tableCache.cacheColumns(monitor, forTable);
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
                        table = tableCache.getTable(monitor, tableName);
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
                    for (GenericTable tmpTable : tableCache.getTables(monitor)) {
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
        this.tableCache.clearCache();
        this.indexList = null;
        this.constraintList = null;
        this.procedureCache.clearCache();
        this.indexesCached = false;
        return true;
    }

    public String toString()
    {
        return getName() == null ? "<NONE>" : getName();
    }
    
    class TableCache extends JDBCTableCache<GenericTable, GenericTableColumn> {
        
        protected TableCache()
        {
            super(JDBCConstants.TABLE_NAME);
        }

        protected PreparedStatement prepareTablesStatement(DBRProgressMonitor monitor)
            throws SQLException, DBException
        {
            return new ResultSetStatement(
                getDataSource().getConnection().getMetaData().getTables(
                    getCatalog() == null ? null : getCatalog().getName(),
                    getSchema() == null ? null : getSchema().getName(),
                    null,
                    null));
        }

        protected GenericTable fetchTable(DBRProgressMonitor monitor, ResultSet dbResult)
            throws SQLException, DBException
        {
            String tableName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_NAME);
            String tableType = JDBCUtils.safeGetString(dbResult, JDBCConstants.TABLE_TYPE);
            String remarks = JDBCUtils.safeGetString(dbResult, JDBCConstants.REMARKS);

            boolean isSystemTable = tableType != null && tableType.toUpperCase().indexOf("SYSTEM") != -1;
            if (isSystemTable && !getDataSource().getContainer().isShowSystemObjects()) {
                return null;
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
            return new GenericTable(
                GenericStructureContainer.this,
                tableName,
                tableType,
                remarks,
                typeName,
                typeCatalog,
                typeSchema);
        }

        protected boolean isTableColumnsCached(GenericTable table)
        {
            return table.isColumnsCached();
        }

        protected void cacheTableColumns(GenericTable table, List<GenericTableColumn> columns)
        {
            table.setColumns(columns);
        }

        protected PreparedStatement prepareColumnsStatement(DBRProgressMonitor monitor, GenericTable forTable)
            throws SQLException, DBException
        {
            return new ResultSetStatement(
                getDataSource().getConnection().getMetaData().getColumns(
                    getCatalog() == null ? null : getCatalog().getName(),
                    getSchema() == null ? null : getSchema().getName(),
                    forTable == null ? null : forTable.getName(),
                    null));
        }

        protected GenericTableColumn fetchColumn(DBRProgressMonitor monitor, GenericTable table, ResultSet dbResult)
            throws SQLException, DBException
        {
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

            return new GenericTableColumn(
                table,
                columnName,
                typeName, valueType, sourceType, ordinalPos,
                columnSize,
                charLength, scale, precision, radix, isNullable,
                remarks, defaultValue
            );
        }
    }

    class ProceduresCache extends JDBCObjectCache<GenericProcedure> {

        ProceduresCache()
        {
            super("procedures");
        }

        protected PreparedStatement prepareObjectsStatement(DBRProgressMonitor monitor)
            throws SQLException, DBException
        {
            return new ResultSetStatement(
                getDataSource().getConnection().getMetaData().getProcedures(
                    getCatalog() == null ? null : getCatalog().getName(),
                    getSchema() == null ? null : getSchema().getName(),
                    null));
        }

        protected GenericProcedure fetchObject(DBRProgressMonitor monitor, ResultSet dbResult)
            throws SQLException, DBException
        {
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
            return new GenericProcedure(
                GenericStructureContainer.this,
                procedureName,
                remarks, procedureType
            );
        }
    }
}

package org.jkiss.dbeaver.ext.generic.model;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.api.ResultSetStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSIndexType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSProcedureColumnType;
import org.jkiss.dbeaver.model.struct.DBSProcedureType;
import org.jkiss.dbeaver.model.struct.DBSStructureAssistant;
import org.jkiss.dbeaver.model.struct.DBSStructureContainer;
import org.jkiss.dbeaver.model.struct.DBSTablePath;
import org.jkiss.dbeaver.model.struct.DBSUtils;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * GenericStructureContainer
 */
public abstract class GenericStructureContainer implements DBSStructureContainer, DBSStructureAssistant
{
    static Log log = LogFactory.getLog(GenericStructureContainer.class);

    private TableCache tableCache;
    private IndexCache indexCache;
    private ProceduresCache procedureCache;

    protected GenericStructureContainer()
    {
        this.tableCache = new TableCache();
        this.indexCache = new IndexCache();
        this.procedureCache = new ProceduresCache();
    }

    final TableCache getTableCache()
    {
        return tableCache;
    }

    final IndexCache getIndexCache()
    {
        return indexCache;
    }

    final ProceduresCache getProcedureCache()
    {
        return procedureCache;
    }

    public abstract GenericDataSource getDataSource();

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
        // Cache indexes (read all tables, all columns and all indexes in this container)
        // This doesn't work for generic datasource because metadata facilities
        // allows index query only by certain table name
        //cacheIndexes(monitor, null);

        if (!indexCache.isCached()) {
            // Load indexes for all tables and return copy of them
            List<GenericIndex> tmpIndexList = new ArrayList<GenericIndex>();
            for (GenericTable table : getTables(monitor)) {
                for (GenericIndex index : table.getIndexes(monitor)) {
                    tmpIndexList.add(new GenericIndex(index));
                }
            }
            indexCache.setCache(tmpIndexList);
        }
        return indexCache.getObjects(monitor, null);
    }

    public void cacheStructure(DBRProgressMonitor monitor, int scope)
        throws DBException
    {
        tableCache.getObjects(monitor);
        if ((scope & STRUCT_ATTRIBUTES) != 0) {
            // Do not use columns cache
            // Cannot be sure that all jdbc drivers support reading of all catalog columns
            //tableCache.cacheChildren(monitor, null);
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
        this.indexCache.clearCache();
        this.procedureCache.clearCache();
        return true;
    }

    public String toString()
    {
        return getName() == null ? "<NONE>" : getName();
    }

    /**
     * Tables cache implementation
     */
    class TableCache extends JDBCStructCache<GenericTable, GenericTableColumn> {
        
        protected TableCache()
        {
            super("tables", "columns", JDBCConstants.TABLE_NAME);
        }

        protected PreparedStatement prepareObjectsStatement(DBRProgressMonitor monitor)
            throws SQLException, DBException
        {
            return new ResultSetStatement(
                getDataSource().getConnection().getMetaData().getTables(
                    getCatalog() == null ? null : getCatalog().getName(),
                    getSchema() == null ? null : getSchema().getName(),
                    null,
                    null));
        }

        protected GenericTable fetchObject(DBRProgressMonitor monitor, ResultSet dbResult)
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

        protected boolean isChildrenCached(GenericTable table)
        {
            return table.isColumnsCached();
        }

        protected void cacheChildren(GenericTable table, List<GenericTableColumn> columns)
        {
            table.setColumns(columns);
        }

        protected PreparedStatement prepareChildrenStatement(DBRProgressMonitor monitor, GenericTable forTable)
            throws SQLException, DBException
        {
            return new ResultSetStatement(
                getDataSource().getConnection().getMetaData().getColumns(
                    getCatalog() == null ? null : getCatalog().getName(),
                    getSchema() == null ? null : getSchema().getName(),
                    forTable == null ? null : forTable.getName(),
                    null));
        }

        protected GenericTableColumn fetchChild(DBRProgressMonitor monitor, GenericTable table, ResultSet dbResult)
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

    /**
     * Index cache implementation
     */

    class IndexCache extends JDBCCompositeCache<GenericTable, GenericIndex, GenericIndexColumn> {
        protected IndexCache()
        {
            super(tableCache, "indexes", JDBCConstants.TABLE_NAME, JDBCConstants.INDEX_NAME);
        }

/*
        void cacheIndexes(DBRProgressMonitor monitor, GenericTable forTable)
            throws DBException
        {
            if (this.indexesCached) {
                return;
            }

            // Load tables and columns first
            tableCache.loadChildren(monitor, forTable);
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
                            table = tableCache.getObject(monitor, tableName);
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
                        for (GenericTable tmpTable : tableCache.getObjects(monitor)) {
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
*/

        protected PreparedStatement prepareObjectsStatement(DBRProgressMonitor monitor, GenericTable forParent)
            throws SQLException, DBException
        {
            return new ResultSetStatement(
                getDataSource().getConnection().getMetaData().getIndexInfo(
                    getCatalog() == null ? null : getCatalog().getName(),
                    getSchema() == null ? null : getSchema().getName(),
                    // oracle fails if unquoted complex identifier specified
                    // but other DBs (and logically it's correct) do not want quote chars in this query
                    // so let's fix it in oracle plugin
                    forParent == null ? null : forParent.getName(), //DBSUtils.getQuotedIdentifier(getDataSource(), forTable.getName()),
                    false,
                    false));
        }

        protected GenericIndex fetchObject(DBRProgressMonitor monitor, ResultSet dbResult, GenericTable parent)
            throws SQLException, DBException
        {
            String indexName = JDBCUtils.safeGetString(dbResult, JDBCConstants.INDEX_NAME);
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
            DBRProgressMonitor monitor, ResultSet dbResult,
            GenericTable parent, GenericIndex object)
            throws SQLException, DBException
        {
            int ordinalPosition = JDBCUtils.safeGetInt(dbResult, JDBCConstants.ORDINAL_POSITION);
            String columnName = JDBCUtils.safeGetString(dbResult, JDBCConstants.COLUMN_NAME);
            String ascOrDesc = JDBCUtils.safeGetString(dbResult, JDBCConstants.ASC_OR_DESC);

            GenericTableColumn tableColumn = parent.getColumn(monitor, columnName);
            if (tableColumn == null) {
                log.warn("Column '" + columnName + "' not found in table '" + parent.getName() + "'");
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
     * Procedures cache implementation
     */
    class ProceduresCache extends JDBCStructCache<GenericProcedure, GenericProcedureColumn> {

        ProceduresCache()
        {
            super("procedures", "procedure columns", JDBCConstants.PROCEDURE_NAME);
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

        protected boolean isChildrenCached(GenericProcedure parent)
        {
            return parent.isColumnsCached();
        }

        protected void cacheChildren(GenericProcedure parent, List<GenericProcedureColumn> columns)
        {
            parent.cacheColumns(columns);
        }

        protected PreparedStatement prepareChildrenStatement(DBRProgressMonitor monitor, GenericProcedure forObject)
            throws SQLException, DBException
        {
            return new ResultSetStatement(
                getDataSource().getConnection().getMetaData().getProcedureColumns(
                    getCatalog() == null ? null : getCatalog().getName(),
                    getSchema() == null ? null : getSchema().getName(),
                    forObject == null ? null : forObject.getName(),
                    null));
        }

        protected GenericProcedureColumn fetchChild(DBRProgressMonitor monitor, GenericProcedure parent, ResultSet dbResult)
            throws SQLException, DBException
        {
            String columnName = JDBCUtils.safeGetString(dbResult, JDBCConstants.COLUMN_NAME);
            int columnTypeNum = JDBCUtils.safeGetInt(dbResult, JDBCConstants.COLUMN_TYPE);
            int valueType = JDBCUtils.safeGetInt(dbResult, JDBCConstants.DATA_TYPE);
            String typeName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TYPE_NAME);
            int columnSize = JDBCUtils.safeGetInt(dbResult, JDBCConstants.LENGTH);
            boolean isNullable = JDBCUtils.safeGetInt(dbResult, JDBCConstants.NULLABLE) != DatabaseMetaData.procedureNoNulls;
            int scale = JDBCUtils.safeGetInt(dbResult, JDBCConstants.SCALE);
            int precision = JDBCUtils.safeGetInt(dbResult, JDBCConstants.PRECISION);
            int radix = JDBCUtils.safeGetInt(dbResult, JDBCConstants.RADIX);
            String remarks = JDBCUtils.safeGetString(dbResult, JDBCConstants.REMARKS);
            int position = JDBCUtils.safeGetInt(dbResult, JDBCConstants.ORDINAL_POSITION);
            //DBSDataType dataType = getDataSource().getInfo().getSupportedDataType(typeName);
            DBSProcedureColumnType columnType;
            switch (columnTypeNum) {
                case DatabaseMetaData.procedureColumnIn: columnType = DBSProcedureColumnType.IN; break;
                case DatabaseMetaData.procedureColumnInOut: columnType = DBSProcedureColumnType.INOUT; break;
                case DatabaseMetaData.procedureColumnOut: columnType = DBSProcedureColumnType.OUT; break;
                case DatabaseMetaData.procedureColumnReturn: columnType = DBSProcedureColumnType.RETURN; break;
                case DatabaseMetaData.procedureColumnResult: columnType = DBSProcedureColumnType.RESULTSET; break;
                default: columnType = DBSProcedureColumnType.UNKNOWN; break;
            }
            if (CommonUtils.isEmpty(columnName) && columnType == DBSProcedureColumnType.RETURN) {
                columnName = "RETURN";
            }
            return new GenericProcedureColumn(
                parent,
                columnName,
                typeName,
                valueType,
                position,
                columnSize,
                scale, precision, radix, isNullable,
                remarks,
                columnType);
        }
    }
}

/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.model;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.jdbc.JDBCPreparedStatement;
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

    private TableCache tableCache;
    private IndexCache indexCache;
    private ProceduresCache procedureCache;

    protected GenericEntityContainer()
    {

    }

    protected void initCache()
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

    public String getObjectId() {
        return getParentObject() + "." + getName();
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

    public Collection<? extends DBSObject> getChildren(DBRProgressMonitor monitor)
        throws DBException
    {
        return getTables(monitor);
    }

    public DBSObject getChild(DBRProgressMonitor monitor, String childName)
        throws DBException
    {
        return DBUtils.findObject(getChildren(monitor), childName);
    }

    public boolean refreshEntity(DBRProgressMonitor monitor)
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
            super(getDataSource(), JDBCConstants.TABLE_NAME);
        }

        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context)
            throws SQLException, DBException
        {
            return context.getMetaData().getTables(
                getCatalog() == null ? null : getCatalog().getName(),
                getSchema() == null ? null : getSchema().getName(),
                null,
                null).getStatement();
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
/*
            // Do not read table type object
            // Actually dunno what to do with it and it often throws stupid warnings in debug

            String typeName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TYPE_NAME);
            String typeCatalogName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TYPE_CAT);
            String typeSchemaName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TYPE_SCHEM);
            GenericCatalog typeCatalog = CommonUtils.isEmpty(typeCatalogName) ?
                null :
                getDataSource().getCatalog(context.getProgressMonitor(), typeCatalogName);
            GenericSchema typeSchema = CommonUtils.isEmpty(typeSchemaName) ?
                null :
                typeCatalog == null ?
                    getDataSource().getSchema(context.getProgressMonitor(), typeSchemaName) :
                    typeCatalog.getSchema(typeSchemaName);
*/
            return new GenericTable(
                GenericEntityContainer.this,
                tableName,
                tableType,
                remarks/*,
                typeName,
                typeCatalog,
                typeSchema*/);
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
                null).getStatement();
        }

        protected GenericTableColumn fetchChild(JDBCExecutionContext context, GenericTable table, ResultSet dbResult)
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
            boolean autoIncrement = "YES".equals(JDBCUtils.safeGetString(dbResult, JDBCConstants.IS_AUTOINCREMENT));

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
            return context.getMetaData().getIndexInfo(
                    getCatalog() == null ? null : getCatalog().getName(),
                    getSchema() == null ? null : getSchema().getName(),
                    // oracle fails if unquoted complex identifier specified
                    // but other DBs (and logically it's correct) do not want quote chars in this query
                    // so let's fix it in oracle plugin
                    forParent == null ? null : forParent.getName(), //DBUtils.getQuotedIdentifier(getDataSource(), forTable.getName()),
                    false,
                    false).getStatement();
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
            JDBCExecutionContext context, ResultSet dbResult,
            GenericTable parent, GenericIndex object)
            throws SQLException, DBException
        {
            int ordinalPosition = JDBCUtils.safeGetInt(dbResult, JDBCConstants.ORDINAL_POSITION);
            String columnName = JDBCUtils.safeGetString(dbResult, JDBCConstants.COLUMN_NAME);
            String ascOrDesc = JDBCUtils.safeGetString(dbResult, JDBCConstants.ASC_OR_DESC);

            GenericTableColumn tableColumn = parent.getColumn(context.getProgressMonitor(), columnName);
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
            super(getDataSource(), JDBCConstants.PROCEDURE_NAME);
        }

        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context)
            throws SQLException, DBException
        {
            return context.getMetaData().getProcedures(
                getCatalog() == null ? null : getCatalog().getName(),
                getSchema() == null ? null : getSchema().getName(),
                null).getStatement();
        }

        protected GenericProcedure fetchObject(JDBCExecutionContext context, ResultSet dbResult)
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
                GenericEntityContainer.this,
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

        protected JDBCPreparedStatement prepareChildrenStatement(JDBCExecutionContext context, GenericProcedure forObject)
            throws SQLException, DBException
        {
            return context.getMetaData().getProcedureColumns(
                getCatalog() == null ? null : getCatalog().getName(),
                getSchema() == null ? null : getSchema().getName(),
                forObject == null ? null : forObject.getName(),
                null).getStatement();
        }

        protected GenericProcedureColumn fetchChild(JDBCExecutionContext context, GenericProcedure parent, ResultSet dbResult)
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

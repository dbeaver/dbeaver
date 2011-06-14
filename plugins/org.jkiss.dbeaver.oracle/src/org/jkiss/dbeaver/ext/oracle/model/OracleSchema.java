/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.OracleConstants;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.impl.struct.AbstractSchema;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * OracleSchema
 */
public class OracleSchema extends AbstractSchema<OracleDataSource> implements DBPSaveableObject
{
    static final Log log = LogFactory.getLog(OracleSchema.class);

    private long id;
    private final TableCache tableCache = new TableCache();
    private final ConstraintCache constraintCache = new ConstraintCache();
    private final ProceduresCache proceduresCache = new ProceduresCache();
    private final TriggerCache triggerCache = new TriggerCache();
    private final IndexCache indexCache = new IndexCache();
    private final DataTypeCache dataTypeCache = new DataTypeCache();
    private boolean persisted;

    public OracleSchema(OracleDataSource dataSource, ResultSet dbResult)
    {
        super(dataSource, null);
        if (dbResult != null) {
            this.id = JDBCUtils.safeGetLong(dbResult, OracleConstants.COL_USER_ID);
            setName(JDBCUtils.safeGetString(dbResult, OracleConstants.COL_USER_NAME));
            persisted = true;
        } else {
            persisted = false;
        }
    }

    TableCache getTableCache()
    {
        return tableCache;
    }

    ConstraintCache getConstraintCache()
    {
        return constraintCache;
    }

    IndexCache getIndexCache()
    {
        return indexCache;
    }

    ProceduresCache getProceduresCache()
    {
        return proceduresCache;
    }

    TriggerCache getTriggerCache()
    {
        return triggerCache;
    }

    DataTypeCache getDataTypeCache()
    {
        return dataTypeCache;
    }

    @Override
    @Property(name = "Name", viewable = true, editable = true, order = 1)
    public String getName()
    {
        return super.getName();
    }

    public boolean isPersisted()
    {
        return persisted;
    }

    public void setPersisted(boolean persisted)
    {
        this.persisted = persisted;
    }

    public String getDescription()
    {
        return null;
    }

    @Property(name = "User ID", viewable = false, order = 200)
    public long getId()
    {
        return id;
    }


    public Collection<OracleIndex> getIndexes(DBRProgressMonitor monitor)
        throws DBException
    {
        return indexCache.getObjects(monitor, null);
    }

    public Collection<OracleTable> getTables(DBRProgressMonitor monitor)
        throws DBException
    {
        return tableCache.getObjects(monitor, OracleTable.class);
    }

    public OracleTable getTable(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return tableCache.getObject(monitor, name, OracleTable.class);
    }

    public Collection<OracleView> getViews(DBRProgressMonitor monitor)
        throws DBException
    {
        return tableCache.getObjects(monitor, OracleView.class);
    }

    public OracleView getView(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return tableCache.getObject(monitor, name, OracleView.class);
    }

    public Collection<OracleDataType> getDataTypes(DBRProgressMonitor monitor)
        throws DBException
    {
        return dataTypeCache.getObjects(monitor);
    }

    public OracleDataType getDataType(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return dataTypeCache.getObject(monitor, name);
    }

    public Collection<OracleProcedure> getProcedures(DBRProgressMonitor monitor)
        throws DBException
    {
        return proceduresCache.getObjects(monitor);
    }

    public OracleProcedure getProcedure(DBRProgressMonitor monitor, String procName)
        throws DBException
    {
        return proceduresCache.getObject(monitor, procName);
    }

    public Collection<OracleTrigger> getTriggers(DBRProgressMonitor monitor)
        throws DBException
    {
        return triggerCache.getObjects(monitor);
    }

    public OracleTrigger getTrigger(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return triggerCache.getObject(monitor, name);
    }

    public Collection<OracleTableBase> getChildren(DBRProgressMonitor monitor)
        throws DBException
    {
        return tableCache.getObjects(monitor);
    }

    public OracleTableBase getChild(DBRProgressMonitor monitor, String childName)
        throws DBException
    {
        return tableCache.getObject(monitor, childName);
    }

    public Class<? extends DBSEntity> getChildType(DBRProgressMonitor monitor)
        throws DBException
    {
        return OracleTable.class;
    }

    public void cacheStructure(DBRProgressMonitor monitor, int scope)
        throws DBException
    {
        monitor.subTask("Cache tables");
        tableCache.loadObjects(monitor);
        if ((scope & STRUCT_ATTRIBUTES) != 0) {
            monitor.subTask("Cache table columns");
            tableCache.loadChildren(monitor, null);
        }
        if ((scope & STRUCT_ASSOCIATIONS) != 0) {
            monitor.subTask("Cache table indexes");
            indexCache.getObjects(monitor, null);
            monitor.subTask("Cache table constraints");
            constraintCache.getObjects(monitor, null);
        }
    }

    @Override
    public boolean refreshEntity(DBRProgressMonitor monitor)
        throws DBException
    {
        super.refreshEntity(monitor);
        tableCache.clearCache();
        constraintCache.clearCache();
        indexCache.clearCache();
        proceduresCache.clearCache();
        triggerCache.clearCache();
        return true;
    }

    public boolean isSystem()
    {
        return CommonUtils.contains(OracleConstants.SYSTEM_SCHEMAS, getName());
    }

    public DBSCatalog getCatalog()
    {
        return null;
    }

    class TableCache extends JDBCStructCache<OracleTableBase, OracleTableColumn> {
        
        protected TableCache()
        {
            super(getDataSource(), OracleConstants.COL_TABLE_NAME);
        }

        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context)
            throws SQLException, DBException
        {
            final JDBCPreparedStatement dbStat = context.prepareStatement(
                "SELECT tab.*,tc.COMMENTS FROM (\n" +
                    "SELECT t.OWNER,t.TABLE_NAME as TABLE_NAME,'TABLE' as TABLE_TYPE FROM SYS.ALL_ALL_TABLES t\n" +
                    "UNION ALL\n" +
                    "SELECT v.OWNER,v.VIEW_NAME as TABLE_NAME,'VIEW' as TABLE_TYPE FROM SYS.ALL_VIEWS v) tab\n" +
                "LEFT OUTER JOIN SYS.ALL_TAB_COMMENTS tc ON tc.OWNER=tab.OWNER AND tc.TABLE_NAME=tab.TABLE_NAME\n" +
                "WHERE tab.OWNER=?\n" +
                "ORDER BY tab.TABLE_NAME");
            dbStat.setString(1, getName());
            return dbStat;
        }

        protected OracleTableBase fetchObject(JDBCExecutionContext context, ResultSet dbResult)
            throws SQLException, DBException
        {
            final String tableType = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_TABLE_TYPE);
            if ("TABLE".equals(tableType)) {
                return new OracleTable(OracleSchema.this, dbResult);
            } else {
                return new OracleView(OracleSchema.this, dbResult);
            }
        }

        protected boolean isChildrenCached(OracleTableBase table)
        {
            return table.isColumnsCached();
        }

        protected void cacheChildren(OracleTableBase table, List<OracleTableColumn> columns)
        {
            table.setColumns(columns);
        }

        protected JDBCPreparedStatement prepareChildrenStatement(JDBCExecutionContext context, OracleTableBase forTable)
            throws SQLException, DBException
        {
            StringBuilder sql = new StringBuilder();
            sql
                .append("SELECT c.*,cc.COMMENTS\n" +
                    "FROM SYS.ALL_TAB_COLS c\n" +
                    "LEFT OUTER JOIN SYS.ALL_COL_COMMENTS cc ON CC.OWNER=c.OWNER AND cc.TABLE_NAME=c.TABLE_NAME AND cc.COLUMN_NAME=c.COLUMN_NAME\n" +
                    "WHERE c.OWNER=?");
            if (forTable != null) {
                sql.append(" AND c.TABLE_NAME=?");
            }
            sql.append("\nORDER BY ");
            if (forTable != null) {
                sql.append("c.TABLE_NAME,");
            }
            sql.append("c.COLUMN_ID");

            JDBCPreparedStatement dbStat = context.prepareStatement(sql.toString());
            dbStat.setString(1, OracleSchema.this.getName());
            if (forTable != null) {
                dbStat.setString(2, forTable.getName());
            }
            return dbStat;
        }

        protected OracleTableColumn fetchChild(JDBCExecutionContext context, OracleTableBase table, ResultSet dbResult)
            throws SQLException, DBException
        {
            return new OracleTableColumn(context.getProgressMonitor(), table, dbResult);
        }
    }

    /**
     * Index cache implementation
     */
    class ConstraintCache extends JDBCCompositeCache<OracleTable, OracleConstraint, OracleConstraintColumn> {
        protected ConstraintCache()
        {
            super(tableCache, OracleTable.class, OracleConstants.COL_TABLE_NAME, OracleConstants.COL_CONSTRAINT_NAME);
        }

        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context, OracleTable forTable)
            throws SQLException, DBException
        {
            StringBuilder sql = new StringBuilder();
            sql
                .append("SELECT c.TABLE_NAME, c.CONSTRAINT_NAME,c.CONSTRAINT_TYPE,c.SEARCH_CONDITION,c.STATUS," +
                    "c.R_OWNER,c.R_CONSTRAINT_NAME,c.DELETE_RULE," +
                    "COLUMN_NAME,POSITION\n" +
                    "FROM SYS.ALL_CONSTRAINTS c\n" +
                    "JOIN SYS.ALL_CONS_COLUMNS ссol ON c.OWNER=ссol.OWNER AND c.CONSTRAINT_NAME=ссol.CONSTRAINT_NAME\n" +
                    "WHERE c.OWNER=?");
            if (forTable != null) {
                sql.append(" AND c.TABLE_NAME=?");
            }
            sql.append("\nORDER BY");
            if (forTable == null) {
                // Fetch foreign keys after all
                sql.append(" (CASE WHEN c.CONSTRAINT_TYPE='R' THEN 1 ELSE 0 END),");
            }
            sql.append(" c.CONSTRAINT_NAME,POSITION");

            JDBCPreparedStatement dbStat = context.prepareStatement(sql.toString());
            dbStat.setString(1, OracleSchema.this.getName());
            if (forTable != null) {
                dbStat.setString(2, forTable.getName());
            }
            return dbStat;
        }

        protected OracleConstraint fetchObject(JDBCExecutionContext context, ResultSet dbResult, OracleTable parent, String indexName)
            throws SQLException, DBException
        {
            String constraintName = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_CONSTRAINT_NAME);
            String constraintTypeName = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_CONSTRAINT_TYPE);
            DBSConstraintType constraintType;
            if ("C".equals(constraintTypeName)) {
                constraintType = DBSConstraintType.CHECK;
            } else if ("P".equals(constraintTypeName)) {
                constraintType = DBSConstraintType.PRIMARY_KEY;
            } else if ("U".equals(constraintTypeName)) {
                constraintType = DBSConstraintType.UNIQUE_KEY;
            } else if ("R".equals(constraintTypeName)) {
                constraintType = DBSConstraintType.FOREIGN_KEY;
            } else {
                log.debug("Unsupported constraint type: " + constraintTypeName);
                return null;
            }
            final String constraintStatus = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_STATUS);
            if (constraintType == DBSConstraintType.FOREIGN_KEY) {
                // Foreign key is not a regular constraint
                String refOwner = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_R_OWNER);
                String refName = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_R_CONSTRAINT_NAME);
                String deleteRuleName = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_DELETE_RULE);
                return new OracleForeignKey(
                    parent,
                    constraintName,
                    CommonUtils.isEmpty(constraintStatus) ? null : OracleConstants.ObjectStatus.valueOf(constraintStatus),
                    new OracleLazyReference(refOwner, refName),
                    "CASCADE".equals(deleteRuleName) ? DBSConstraintModifyRule.CASCADE : DBSConstraintModifyRule.NO_ACTION,
                    true);
            } else {
                // Make table constraint
                final String searchCondition = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_SEARCH_CONDITION);
                return new OracleConstraint(
                    parent,
                    constraintName,
                    constraintType,
                    searchCondition,
                    CommonUtils.isEmpty(constraintStatus) ? null : OracleConstants.ObjectStatus.valueOf(constraintStatus),
                    true);
            }
        }

        protected OracleConstraintColumn fetchObjectRow(
            JDBCExecutionContext context,
            ResultSet dbResult,
            OracleTable parent,
            OracleConstraint object)
            throws SQLException, DBException
        {
            String columnName = JDBCUtils.safeGetStringTrimmed(dbResult, OracleConstants.COL_COLUMN_NAME);
            int ordinalPosition = JDBCUtils.safeGetInt(dbResult, OracleConstants.COL_POSITION);

            OracleTableColumn tableColumn = parent.getColumn(context.getProgressMonitor(), columnName);
            if (tableColumn == null) {
                log.debug("Column '" + columnName + "' not found in table '" + parent.getName() + "' for constraint '" + object.getName() + "'");
                return null;
            }

            if (object instanceof OracleForeignKey) {
                return new OracleForeignKeyColumn(
                    (OracleForeignKey) object,
                    tableColumn,
                    ordinalPosition);
            } else {
                return new OracleConstraintColumn(
                    object,
                    tableColumn,
                    ordinalPosition);
            }
        }

        protected boolean isObjectsCached(OracleTable parent)
        {
            return parent.isConstraintsCached();
        }

        protected void cacheObjects(DBRProgressMonitor monitor, OracleTable parent, List<OracleConstraint> constraints)
        {
            parent.setConstraints(monitor, constraints);
        }

        protected void cacheRows(DBRProgressMonitor monitor, OracleConstraint constraint, List<OracleConstraintColumn> rows)
        {
            constraint.setColumns(rows);
        }
    }

    /**
     * Index cache implementation
     */
    class IndexCache extends JDBCCompositeCache<OracleTable, OracleIndex, OracleIndexColumn> {
        protected IndexCache()
        {
            super(tableCache, OracleTable.class, OracleConstants.COL_TABLE_NAME, OracleConstants.COL_INDEX_NAME);
        }

        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context, OracleTable forTable)
            throws SQLException, DBException
        {
            StringBuilder sql = new StringBuilder();
            sql.append(
                "SELECT i.INDEX_NAME,i.INDEX_TYPE,i.TABLE_OWNER,i.TABLE_NAME,i.UNIQUENESS,i.TABLESPACE_NAME,i.STATUS,i.NUM_ROWS,i.SAMPLE_SIZE,\n" +
                    "ic.COLUMN_NAME,ic.COLUMN_POSITION,ic.COLUMN_LENGTH,ic.DESCEND\n" +
                    "FROM SYS.ALL_INDEXES i \n" +
                    "JOIN SYS.ALL_IND_COLUMNS ic ON ic.INDEX_OWNER=i.OWNER AND ic.INDEX_NAME=i.INDEX_NAME\n" +
                    "WHERE i.OWNER=? AND i.TABLE_OWNER=?\n");
            if (forTable != null) {
                sql.append(" AND i.TABLE_NAME=?");
            }
            sql.append(" ORDER BY i.INDEX_NAME,ic.COLUMN_POSITION");

            JDBCPreparedStatement dbStat = context.prepareStatement(sql.toString());
            dbStat.setString(1, OracleSchema.this.getName());
            dbStat.setString(2, OracleSchema.this.getName());
            if (forTable != null) {
                dbStat.setString(3, forTable.getName());
            }
            return dbStat;
        }

        protected OracleIndex fetchObject(JDBCExecutionContext context, ResultSet dbResult, OracleTable parent, String indexName)
            throws SQLException, DBException
        {
            String indexTypeName = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_INDEX_TYPE);
            boolean isNonUnique = JDBCUtils.safeGetInt(dbResult, OracleConstants.COL_UNIQUENESS) != 0;
            DBSIndexType indexType;
            if (OracleConstants.INDEX_TYPE_NORMAL.getId().equals(indexTypeName)) {
                indexType = OracleConstants.INDEX_TYPE_NORMAL;
            } else if (OracleConstants.INDEX_TYPE_BITMAP.getId().equals(indexTypeName)) {
                indexType = OracleConstants.INDEX_TYPE_BITMAP;
            } else if (OracleConstants.INDEX_TYPE_FUNCTION_BASED_NORMAL.getId().equals(indexTypeName)) {
                indexType = OracleConstants.INDEX_TYPE_FUNCTION_BASED_NORMAL;
            } else if (OracleConstants.INDEX_TYPE_FUNCTION_BASED_BITMAP.getId().equals(indexTypeName)) {
                indexType = OracleConstants.INDEX_TYPE_FUNCTION_BASED_BITMAP;
            } else if (OracleConstants.INDEX_TYPE_DOMAIN.getId().equals(indexTypeName)) {
                indexType = OracleConstants.INDEX_TYPE_DOMAIN;
            } else {
                indexType = DBSIndexType.OTHER;
            }

            return new OracleIndex(
                parent,
                isNonUnique,
                indexName,
                indexType);
        }

        protected OracleIndexColumn fetchObjectRow(
            JDBCExecutionContext context,
            ResultSet dbResult,
            OracleTable parent,
            OracleIndex object)
            throws SQLException, DBException
        {
            String columnName = JDBCUtils.safeGetStringTrimmed(dbResult, OracleConstants.COL_COLUMN_NAME);
            int ordinalPosition = JDBCUtils.safeGetInt(dbResult, OracleConstants.COL_COLUMN_POSITION);
            boolean isAscending = "ASC".equals(JDBCUtils.safeGetStringTrimmed(dbResult, OracleConstants.COL_DESCEND));

            OracleTableColumn tableColumn = parent.getColumn(context.getProgressMonitor(), columnName);
            if (tableColumn == null) {
                log.debug("Column '" + columnName + "' not found in table '" + parent.getName() + "' for index '" + object.getName() + "'");
                return null;
            }

            return new OracleIndexColumn(
                object,
                tableColumn,
                ordinalPosition,
                isAscending);
        }

        protected boolean isObjectsCached(OracleTable parent)
        {
            return parent.isIndexesCached();
        }

        protected void cacheObjects(DBRProgressMonitor monitor, OracleTable parent, List<OracleIndex> indexes)
        {
            parent.setIndexes(indexes);
        }

        protected void cacheRows(DBRProgressMonitor monitor, OracleIndex index, List<OracleIndexColumn> rows)
        {
            index.setColumns(rows);
        }
    }

    /**
     * DataType cache implementation
     */
    class DataTypeCache extends JDBCObjectCache<OracleDataType> {
        DataTypeCache()
        {
            super(getDataSource());
        }

        @Override
        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context) throws SQLException, DBException
        {
            JDBCPreparedStatement dbStat = context.prepareStatement(
                "SELECT * FROM SYS.ALL_TYPES WHERE OWNER=? ORDER BY TYPE_NAME");
            dbStat.setString(1, getName());
            return dbStat;
        }

        @Override
        protected OracleDataType fetchObject(JDBCExecutionContext context, ResultSet resultSet) throws SQLException, DBException
        {
            return new OracleDataType(OracleSchema.this, resultSet);
        }

        @Override
        protected void invalidateObjects(DBRProgressMonitor monitor, Collection<OracleDataType> oracleDataTypes)
        {
            // Resolve super types
            for (Iterator<OracleDataType> iter = oracleDataTypes.iterator(); iter.hasNext(); ) {
                OracleDataType type = iter.next();
                if (!type.resolveLazyReference(monitor)) {
                    iter.remove();
                }
            }
        }
    }


    /**
     * Procedures cache implementation
     */
    class ProceduresCache extends JDBCStructCache<OracleProcedure, OracleProcedureColumn> {

        ProceduresCache()
        {
            super(getDataSource(), JDBCConstants.PROCEDURE_NAME);
        }

        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context)
            throws SQLException, DBException
        {
            JDBCPreparedStatement dbStat = context.prepareStatement(
                "SELECT * FROM " + OracleConstants.META_TABLE_ROUTINES +
                " WHERE " + OracleConstants.COL_ROUTINE_SCHEMA + "=?" +
                " ORDER BY " + OracleConstants.COL_ROUTINE_NAME
            );
            dbStat.setString(1, getName());
            return dbStat;
        }

        protected OracleProcedure fetchObject(JDBCExecutionContext context, ResultSet dbResult)
            throws SQLException, DBException
        {
            return new OracleProcedure(OracleSchema.this, dbResult);
        }

        protected boolean isChildrenCached(OracleProcedure parent)
        {
            return parent.isColumnsCached();
        }

        protected void cacheChildren(OracleProcedure parent, List<OracleProcedureColumn> columns)
        {
            parent.cacheColumns(columns);
        }

        protected JDBCPreparedStatement prepareChildrenStatement(JDBCExecutionContext context, OracleProcedure procedure)
            throws SQLException, DBException
        {
            // Load procedure columns thru Oracle metadata
            // There is no metadata table about proc/func columns -
            // it should be parsed from SHOW CREATE PROCEDURE/FUNCTION query
            // Lets driver do it instead of me
            return context.getMetaData().getProcedureColumns(
                getName(),
                null,
                procedure.getName(),
                null).getSource();
        }

        protected OracleProcedureColumn fetchChild(JDBCExecutionContext context, OracleProcedure parent, ResultSet dbResult)
            throws SQLException, DBException
        {
            String columnName = JDBCUtils.safeGetString(dbResult, JDBCConstants.COLUMN_NAME);
            int columnTypeNum = JDBCUtils.safeGetInt(dbResult, JDBCConstants.COLUMN_TYPE);
            int valueType = JDBCUtils.safeGetInt(dbResult, JDBCConstants.DATA_TYPE);
            String typeName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TYPE_NAME);
            int position = JDBCUtils.safeGetInt(dbResult, JDBCConstants.ORDINAL_POSITION);
            long columnSize = JDBCUtils.safeGetLong(dbResult, JDBCConstants.LENGTH);
            boolean notNull = JDBCUtils.safeGetInt(dbResult, JDBCConstants.NULLABLE) == DatabaseMetaData.procedureNoNulls;
            int scale = JDBCUtils.safeGetInt(dbResult, JDBCConstants.SCALE);
            int precision = JDBCUtils.safeGetInt(dbResult, JDBCConstants.PRECISION);
            int radix = JDBCUtils.safeGetInt(dbResult, JDBCConstants.RADIX);
            //DBSDataType dataType = getDataSourceContainer().getInfo().getSupportedDataType(typeName);
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
            return new OracleProcedureColumn(
                parent,
                columnName,
                typeName,
                valueType,
                position,
                columnSize,
                scale, precision, radix, notNull,
                columnType);
        }
    }

    class TriggerCache extends JDBCObjectCache<OracleTrigger> {
        
        protected TriggerCache()
        {
            super(getDataSource());
        }

        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context)
            throws SQLException, DBException
        {
            JDBCPreparedStatement dbStat = context.prepareStatement(        
                "SELECT * FROM " + OracleConstants.META_TABLE_TRIGGERS +
                " WHERE " + OracleConstants.COL_TRIGGER_SCHEMA + "=?" +
                " ORDER BY " + OracleConstants.COL_TRIGGER_NAME);
            dbStat.setString(1, getName());
            return dbStat;
        }

        protected OracleTrigger fetchObject(JDBCExecutionContext context, ResultSet dbResult)
            throws SQLException, DBException
        {
            String tableSchema = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_TRIGGER_EVENT_OBJECT_SCHEMA);
            String tableName = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_TRIGGER_EVENT_OBJECT_TABLE);
            OracleTable triggerTable = getDataSource().findTable(context.getProgressMonitor(), tableSchema, tableName);
            return new OracleTrigger(OracleSchema.this, triggerTable, dbResult);
        }

    }

}

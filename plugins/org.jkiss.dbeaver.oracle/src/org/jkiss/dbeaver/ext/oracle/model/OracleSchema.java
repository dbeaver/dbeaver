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
    private boolean constraintsCached = false;
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


    public List<OracleIndex> getIndexes(DBRProgressMonitor monitor)
        throws DBException
    {
        return indexCache.getObjects(monitor, null);
    }

    public List<OracleTable> getTables(DBRProgressMonitor monitor)
        throws DBException
    {
        return tableCache.getObjects(monitor, OracleTable.class);
    }

    public OracleTable getTable(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return tableCache.getObject(monitor, name, OracleTable.class);
    }

    public List<OracleView> getViews(DBRProgressMonitor monitor)
        throws DBException
    {
        return tableCache.getObjects(monitor, OracleView.class);
    }

    public List<OracleProcedure> getProcedures(DBRProgressMonitor monitor)
        throws DBException
    {
        return proceduresCache.getObjects(monitor);
    }

    public OracleProcedure getProcedure(DBRProgressMonitor monitor, String procName)
        throws DBException
    {
        return proceduresCache.getObject(monitor, procName);
    }

    public List<OracleTrigger> getTriggers(DBRProgressMonitor monitor)
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
        monitor.subTask("Cache table constraints");
        constraintCache.getObjects(monitor, null);
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
            super(getDataSource(), OracleConstants.COL_OWNER);
        }

        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context)
            throws SQLException, DBException
        {
            return context.prepareStatement(
                "SELECT * FROM " + OracleConstants.META_TABLE_TABLES +
                " WHERE " + OracleConstants.COL_OWNER + "='" + getName() + "'");
        }

        protected OracleTableBase fetchObject(JDBCExecutionContext context, ResultSet dbResult)
            throws SQLException, DBException
        {
            return new OracleTable(OracleSchema.this, dbResult);
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
                .append("SELECT * FROM ").append(OracleConstants.META_TABLE_COLUMNS)
                .append(" WHERE ").append(OracleConstants.COL_OWNER).append("=?");
            if (forTable != null) {
                sql.append(" AND ").append(OracleConstants.COL_TABLE_NAME).append("=?");
            }
            sql.append(" ORDER BY ");
            if (forTable != null) {
                sql.append(OracleConstants.COL_TABLE_NAME).append(",");
            }
            sql.append(OracleConstants.COL_COLUMN_ID);

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
            return new OracleTableColumn(table, dbResult);
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
                .append("SELECT c.TABLE_NAME, c.CONSTRAINT_NAME,c.CONSTRAINT_TYPE,c.SEARCH_CONDITION,c.STATUS,COLUMN_NAME,POSITION\nFROM ")
                .append(OracleConstants.META_TABLE_CONSTRAINTS).append(" c\n")
                .append("JOIN ").append(OracleConstants.META_TABLE_CONSTRAINT_COLUMNS).append(" ссol ON c.OWNER=ссol.OWNER AND c.CONSTRAINT_NAME=ссol.CONSTRAINT_NAME\n")
                .append("WHERE c.").append(OracleConstants.COL_OWNER).append("=?").append(" AND c.").append(OracleConstants.COL_CONSTRAINT_TYPE).append(" IN ('C','P','U')");
            if (forTable != null) {
                sql.append(" AND c.").append(OracleConstants.COL_TABLE_NAME).append("=?");
            }
            sql.append(" ORDER BY c.").append(OracleConstants.COL_TABLE_NAME).append(",").append(OracleConstants.COL_POSITION);

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
            } else {
                log.debug("Unknown constraint type: " + constraintTypeName);
                return null;
            }

            final String constraintStatus = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_STATUS);
            return new OracleConstraint(
                parent,
                constraintName,
                constraintType,
                JDBCUtils.safeGetString(dbResult, OracleConstants.COL_SEARCH_CONDITION),
                CommonUtils.isEmpty(constraintStatus) ? null : OracleConstants.ObjectStatus.valueOf(constraintStatus),
                true);
        }

        protected OracleConstraintColumn fetchObjectRow(
            JDBCExecutionContext context,
            ResultSet dbResult,
            OracleTable parent,
            OracleConstraint object)
            throws SQLException, DBException
        {
            int ordinalPosition = JDBCUtils.safeGetInt(dbResult, OracleConstants.COL_POSITION);
            String columnName = JDBCUtils.safeGetStringTrimmed(dbResult, OracleConstants.COL_COLUMN_NAME);

            OracleTableColumn tableColumn = parent.getColumn(context.getProgressMonitor(), columnName);
            if (tableColumn == null) {
                log.debug("Column '" + columnName + "' not found in table '" + parent.getName() + "' for constraint '" + object.getName() + "'");
                return null;
            }

            return new OracleConstraintColumn(
                object,
                tableColumn,
                ordinalPosition);
        }

        protected boolean isObjectsCached(OracleTable parent)
        {
            return parent.isIndexesCached();
        }

        protected void cacheObjects(OracleTable parent, List<OracleConstraint> indexes)
        {
            parent.setConstraints(indexes);
        }

        protected void cacheRows(OracleConstraint constraint, List<OracleConstraintColumn> rows)
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
            sql
                .append("SELECT * FROM ").append(OracleConstants.META_TABLE_STATISTICS)
                .append(" WHERE ").append(OracleConstants.COL_TABLE_SCHEMA).append("=?");
            if (forTable != null) {
                sql.append(" AND ").append(OracleConstants.COL_TABLE_NAME).append("=?");
            }
            sql.append(" ORDER BY ").append(OracleConstants.COL_INDEX_NAME).append(",").append(OracleConstants.COL_SEQ_IN_INDEX);

            JDBCPreparedStatement dbStat = context.prepareStatement(sql.toString());
            dbStat.setString(1, OracleSchema.this.getName());
            if (forTable != null) {
                dbStat.setString(2, forTable.getName());
            }
            return dbStat;
//            return context.getMetaData().getIndexInfo(
//                    getName(),
//                    null,
//                    forParent == null ? null : DBUtils.getQuotedIdentifier(getDataSource(), forParent.getName()),
//                    true,
//                    true).getSource();
        }

        protected OracleIndex fetchObject(JDBCExecutionContext context, ResultSet dbResult, OracleTable parent, String indexName)
            throws SQLException, DBException
        {
            boolean isNonUnique = JDBCUtils.safeGetInt(dbResult, OracleConstants.COL_NON_UNIQUE) != 0;
            String indexTypeName = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_INDEX_TYPE);
            DBSIndexType indexType;
            if (OracleConstants.INDEX_TYPE_BTREE.getId().equals(indexTypeName)) {
                indexType = OracleConstants.INDEX_TYPE_BTREE;
            } else if (OracleConstants.INDEX_TYPE_FULLTEXT.getId().equals(indexTypeName)) {
                indexType = OracleConstants.INDEX_TYPE_FULLTEXT;
            } else if (OracleConstants.INDEX_TYPE_HASH.getId().equals(indexTypeName)) {
                indexType = OracleConstants.INDEX_TYPE_HASH;
            } else if (OracleConstants.INDEX_TYPE_RTREE.getId().equals(indexTypeName)) {
                indexType = OracleConstants.INDEX_TYPE_RTREE;
            } else {
                indexType = DBSIndexType.OTHER;
            }
            final String comment = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_COMMENT);

            return new OracleIndex(
                parent,
                isNonUnique,
                indexName,
                indexType,
                comment);
        }

        protected OracleIndexColumn fetchObjectRow(
            JDBCExecutionContext context,
            ResultSet dbResult,
            OracleTable parent,
            OracleIndex object)
            throws SQLException, DBException
        {
            int ordinalPosition = JDBCUtils.safeGetInt(dbResult, OracleConstants.COL_SEQ_IN_INDEX);
            String columnName = JDBCUtils.safeGetStringTrimmed(dbResult, OracleConstants.COL_COLUMN_NAME);
            String ascOrDesc = JDBCUtils.safeGetStringTrimmed(dbResult, OracleConstants.COL_COLLATION);
            boolean nullable = "YES".equals(JDBCUtils.safeGetStringTrimmed(dbResult, OracleConstants.COL_NULLABLE));

            OracleTableColumn tableColumn = parent.getColumn(context.getProgressMonitor(), columnName);
            if (tableColumn == null) {
                log.debug("Column '" + columnName + "' not found in table '" + parent.getName() + "' for index '" + object.getName() + "'");
                return null;
            }

            return new OracleIndexColumn(
                object,
                tableColumn,
                ordinalPosition,
                "A".equalsIgnoreCase(ascOrDesc),
                nullable);
        }

        protected boolean isObjectsCached(OracleTable parent)
        {
            return parent.isIndexesCached();
        }

        protected void cacheObjects(OracleTable parent, List<OracleIndex> indexes)
        {
            parent.setIndexes(indexes);
        }

        protected void cacheRows(OracleIndex index, List<OracleIndexColumn> rows)
        {
            index.setColumns(rows);
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

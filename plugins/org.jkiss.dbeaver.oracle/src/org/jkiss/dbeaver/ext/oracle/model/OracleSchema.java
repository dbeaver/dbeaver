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
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSCatalog;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSIndexType;

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
    private final ForeignKeyCache foreignKeyCache = new ForeignKeyCache();
    private final TriggerCache triggerCache = new TriggerCache();
    private final IndexCache indexCache = new IndexCache();
    private final DataTypeCache dataTypeCache = new DataTypeCache();
    private final SequenceCache sequenceCache = new SequenceCache(); 
    private final PackageCache packageCache = new PackageCache();
    private final ProceduresCache proceduresCache = new ProceduresCache();
    private boolean persisted;

    public OracleSchema(OracleDataSource dataSource, ResultSet dbResult)
    {
        super(dataSource, null);
        if (dbResult != null) {
            this.id = JDBCUtils.safeGetLong(dbResult, "USER_ID");
            setName(JDBCUtils.safeGetString(dbResult, "USERNAME"));
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

    ForeignKeyCache getForeignKeyCache()
    {
        return foreignKeyCache;
    }

    IndexCache getIndexCache()
    {
        return indexCache;
    }

    PackageCache getPackageCache()
    {
        return packageCache;
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

    SequenceCache getSequenceCache()
    {
        return sequenceCache;
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


    @Association
    public Collection<OracleIndex> getIndexes(DBRProgressMonitor monitor)
        throws DBException
    {
        return getIndexCache().getObjects(monitor, this, null);
    }

    @Association
    public Collection<OracleTable> getTables(DBRProgressMonitor monitor)
        throws DBException
    {
        return getTableCache().getObjects(monitor, this, OracleTable.class);
    }

    public OracleTable getTable(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return getTableCache().getObject(monitor, this, name, OracleTable.class);
    }

    @Association
    public Collection<OracleView> getViews(DBRProgressMonitor monitor)
        throws DBException
    {
        return getTableCache().getObjects(monitor, this, OracleView.class);
    }

    public OracleView getView(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return getTableCache().getObject(monitor, this, name, OracleView.class);
    }

    @Association
    public Collection<OracleDataType> getDataTypes(DBRProgressMonitor monitor)
        throws DBException
    {
        return getDataTypeCache().getObjects(monitor, this);
    }

    public OracleDataType getDataType(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return getDataTypeCache().getObject(monitor, this, name);
    }

    @Association
    public Collection<OracleSequence> getSequences(DBRProgressMonitor monitor)
        throws DBException
    {
        return getSequenceCache().getObjects(monitor, this);
    }

    public OracleSequence getSequence(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return getSequenceCache().getObject(monitor, this, name);
    }

    @Association
    public Collection<OraclePackage> getPackages(DBRProgressMonitor monitor)
        throws DBException
    {
        return getPackageCache().getObjects(monitor, this);
    }

    public OraclePackage getPackage(DBRProgressMonitor monitor, String procName)
        throws DBException
    {
        return getPackageCache().getObject(monitor, this, procName);
    }

    @Association
    public Collection<OracleProcedure> getProcedures(DBRProgressMonitor monitor)
        throws DBException
    {
        return getProceduresCache().getObjects(monitor, this);
    }

    public OracleProcedure getProcedure(DBRProgressMonitor monitor, String procName)
        throws DBException
    {
        return getProceduresCache().getObject(monitor, this, procName);
    }

    @Association
    public Collection<OracleTrigger> getTriggers(DBRProgressMonitor monitor)
        throws DBException
    {
        return getTriggerCache().getObjects(monitor, this);
    }

    public OracleTrigger getTrigger(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return getTriggerCache().getObject(monitor, this, name);
    }

    public Collection<OracleTableBase> getChildren(DBRProgressMonitor monitor)
        throws DBException
    {
        return getTableCache().getObjects(monitor, this);
    }

    public OracleTableBase getChild(DBRProgressMonitor monitor, String childName)
        throws DBException
    {
        return getTableCache().getObject(monitor, this, childName);
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
        tableCache.loadObjects(monitor, this);
        if ((scope & STRUCT_ATTRIBUTES) != 0) {
            monitor.subTask("Cache table columns");
            tableCache.getChildren(monitor, this, null);
        }
        if ((scope & STRUCT_ASSOCIATIONS) != 0) {
            monitor.subTask("Cache table indexes");
            indexCache.getObjects(monitor, this, null);
            monitor.subTask("Cache table constraints");
            constraintCache.getObjects(monitor, this, null);
            foreignKeyCache.getObjects(monitor, this, null);
        }
    }

    @Override
    public boolean refreshEntity(DBRProgressMonitor monitor)
        throws DBException
    {
        super.refreshEntity(monitor);
        tableCache.clearCache();
        foreignKeyCache.clearCache();
        constraintCache.clearCache();
        indexCache.clearCache();
        packageCache.clearCache();
        proceduresCache.clearCache();
        triggerCache.clearCache();
        dataTypeCache.clearCache();
        sequenceCache.clearCache();
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

    protected static OracleTableColumn getTableColumn(JDBCExecutionContext context, OracleTable parent, ResultSet dbResult) throws DBException
    {
        String columnName = JDBCUtils.safeGetStringTrimmed(dbResult, "COLUMN_NAME");
        OracleTableColumn tableColumn = parent.getColumn(context.getProgressMonitor(), columnName);
        if (tableColumn == null) {
            log.debug("Column '" + columnName + "' not found in table '" + parent.getName() + "'");
        }
        return tableColumn;
    }

    static class TableCache extends JDBCStructCache<OracleSchema, OracleTableBase, OracleTableColumn> {
        
        protected TableCache()
        {
            super("TABLE_NAME");
        }

        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context, OracleSchema owner)
            throws SQLException, DBException
        {
            final JDBCPreparedStatement dbStat = context.prepareStatement(
                "SELECT /*+ USE_NL(tc)*/ tab.*,tc.COMMENTS FROM (\n" +
                    "SELECT t.OWNER,t.TABLE_NAME as TABLE_NAME,'TABLE' as TABLE_TYPE FROM SYS.ALL_ALL_TABLES t\n" +
                    "UNION ALL\n" +
                    "SELECT v.OWNER,v.VIEW_NAME as TABLE_NAME,'VIEW' as TABLE_TYPE FROM SYS.ALL_VIEWS v) tab\n" +
                "LEFT OUTER JOIN SYS.ALL_TAB_COMMENTS tc ON tc.OWNER=tab.OWNER AND tc.TABLE_NAME=tab.TABLE_NAME\n" +
                "WHERE tab.OWNER=?\n" +
                "ORDER BY tab.TABLE_NAME");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        protected OracleTableBase fetchObject(JDBCExecutionContext context, OracleSchema owner, ResultSet dbResult)
            throws SQLException, DBException
        {
            final String tableType = JDBCUtils.safeGetString(dbResult, "TABLE_TYPE");
            if ("TABLE".equals(tableType)) {
                return new OracleTable(owner, dbResult);
            } else {
                return new OracleView(owner, dbResult);
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

        protected JDBCPreparedStatement prepareChildrenStatement(JDBCExecutionContext context, OracleSchema owner, OracleTableBase forTable)
            throws SQLException, DBException
        {
            StringBuilder sql = new StringBuilder(500);
            sql
                .append("SELECT c.*,cc.COMMENTS\n" +
                    "FROM SYS.ALL_TAB_COLUMNS c\n" +
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
            dbStat.setString(1, owner.getName());
            if (forTable != null) {
                dbStat.setString(2, forTable.getName());
            }
            return dbStat;
        }

        protected OracleTableColumn fetchChild(JDBCExecutionContext context, OracleSchema owner, OracleTableBase table, ResultSet dbResult)
            throws SQLException, DBException
        {
            return new OracleTableColumn(context.getProgressMonitor(), table, dbResult);
        }
    }

    /**
     * Index cache implementation
     */
    class ConstraintCache extends JDBCCompositeCache<OracleSchema, OracleTable, OracleConstraint, OracleConstraintColumn> {
        protected ConstraintCache()
        {
            super(tableCache, OracleTable.class, "TABLE_NAME", "CONSTRAINT_NAME");
        }

        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context, OracleSchema owner, OracleTable forTable)
            throws SQLException, DBException
        {
            StringBuilder sql = new StringBuilder(500);
            sql
                .append("SELECT /*+USE_NL(col) USE_NL(ref)*/\n" +
                    "c.TABLE_NAME, c.CONSTRAINT_NAME,c.CONSTRAINT_TYPE,c.SEARCH_CONDITION,c.STATUS," +
                    "col.COLUMN_NAME,col.POSITION\n" +
                    "FROM SYS.ALL_CONSTRAINTS c\n" +
                    "JOIN SYS.ALL_CONS_COLUMNS col ON c.OWNER=col.OWNER AND c.CONSTRAINT_NAME=col.CONSTRAINT_NAME\n" +
                    "WHERE c.CONSTRAINT_TYPE<>'R' AND c.OWNER=?");
            if (forTable != null) {
                sql.append(" AND c.TABLE_NAME=?");
            }
            sql.append("\nORDER BY c.CONSTRAINT_NAME,col.POSITION");

            JDBCPreparedStatement dbStat = context.prepareStatement(sql.toString());
            dbStat.setString(1, OracleSchema.this.getName());
            if (forTable != null) {
                dbStat.setString(2, forTable.getName());
            }
            return dbStat;
        }

        protected OracleConstraint fetchObject(JDBCExecutionContext context, OracleTable parent, String indexName, ResultSet dbResult)
            throws SQLException, DBException
        {
            return new OracleConstraint(parent, dbResult);
        }

        protected OracleConstraintColumn fetchObjectRow(
            JDBCExecutionContext context,
            OracleTable parent, OracleConstraint object, ResultSet dbResult)
            throws SQLException, DBException
        {
            return new OracleConstraintColumn(
                object,
                getTableColumn(context, parent, dbResult),
                JDBCUtils.safeGetInt(dbResult, "POSITION"));
        }

        protected boolean isObjectsCached(OracleTable parent)
        {
            return parent.isConstraintsCached();
        }

        protected void cacheObjects(OracleTable parent, List<OracleConstraint> constraints)
        {
            parent.setConstraints(constraints);
        }

        protected void cacheChildren(OracleConstraint constraint, List<OracleConstraintColumn> rows)
        {
            constraint.setColumns(rows);
        }
    }

    class ForeignKeyCache extends JDBCCompositeCache<OracleSchema, OracleTable, OracleForeignKey, OracleForeignKeyColumn> {
        protected ForeignKeyCache()
        {
            super(tableCache, OracleTable.class, "TABLE_NAME", "CONSTRAINT_NAME");
        }

        protected void loadObjects(DBRProgressMonitor monitor, OracleSchema schema, OracleTable forParent)
            throws DBException
        {
            // Cache schema constraints in not table specified
            if (forParent == null) {
                getConstraintCache().getObject(monitor, schema, null);
            }
            super.loadObjects(monitor, schema, forParent);
        }

        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context, OracleSchema owner, OracleTable forTable)
            throws SQLException, DBException
        {
            StringBuilder sql = new StringBuilder(500);
            sql.append(
                "SELECT /*+USE_NL(col) USE_NL(ref)*/ \r\n" +
                "c.TABLE_NAME, c.CONSTRAINT_NAME,c.CONSTRAINT_TYPE,c.SEARCH_CONDITION,c.STATUS,c.R_OWNER,c.R_CONSTRAINT_NAME,ref.TABLE_NAME as R_TABLE_NAME,c.DELETE_RULE, \n" +
                "col.COLUMN_NAME,col.POSITION\r\n" +
                "FROM SYS.ALL_CONSTRAINTS c\n" +
                "JOIN SYS.ALL_CONS_COLUMNS col ON c.OWNER=col.OWNER AND c.CONSTRAINT_NAME=col.CONSTRAINT_NAME\n" +
                "LEFT OUTER JOIN SYS.ALL_CONSTRAINTS ref ON ref.OWNER=c.r_OWNER AND ref.CONSTRAINT_NAME=c.R_CONSTRAINT_NAME \n" +
                "WHERE c.CONSTRAINT_TYPE='R' AND c.OWNER=?");
            if (forTable != null) {
                sql.append(" AND c.TABLE_NAME=?");
            }
            sql.append("\nORDER BY c.CONSTRAINT_NAME,col.POSITION");

            JDBCPreparedStatement dbStat = context.prepareStatement(sql.toString());
            dbStat.setString(1, OracleSchema.this.getName());
            if (forTable != null) {
                dbStat.setString(2, forTable.getName());
            }
            return dbStat;
        }

        protected OracleForeignKey fetchObject(JDBCExecutionContext context, OracleTable parent, String indexName, ResultSet dbResult)
            throws SQLException, DBException
        {
            return new OracleForeignKey(context.getProgressMonitor(), parent, dbResult);
        }

        protected OracleForeignKeyColumn fetchObjectRow(
            JDBCExecutionContext context,
            OracleTable parent, OracleForeignKey object, ResultSet dbResult)
            throws SQLException, DBException
        {
            return new OracleForeignKeyColumn(
                object,
                getTableColumn(context, parent, dbResult),
                JDBCUtils.safeGetInt(dbResult, "POSITION"));
        }

        protected boolean isObjectsCached(OracleTable parent)
        {
            return parent.isForeignKeysCached();
        }

        protected void cacheObjects(OracleTable parent, List<OracleForeignKey> constraints)
        {
            parent.setForeignKeys(constraints);
        }

        @Override
        protected void cacheChildren(OracleForeignKey foreignKey, List<OracleForeignKeyColumn> rows)
        {
            foreignKey.setColumns((List)rows);
        }
    }


    /**
     * Index cache implementation
     */
    class IndexCache extends JDBCCompositeCache<OracleSchema, OracleTable, OracleIndex, OracleIndexColumn> {
        protected IndexCache()
        {
            super(tableCache, OracleTable.class, "TABLE_NAME", "INDEX_NAME");
        }

        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context, OracleSchema owner, OracleTable forTable)
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

        protected OracleIndex fetchObject(JDBCExecutionContext context, OracleTable parent, String indexName, ResultSet dbResult)
            throws SQLException, DBException
        {
            String indexTypeName = JDBCUtils.safeGetString(dbResult, "INDEX_TYPE");
            boolean isNonUnique = JDBCUtils.safeGetInt(dbResult, "UNIQUENESS") != 0;
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
            OracleTable parent, OracleIndex object, ResultSet dbResult)
            throws SQLException, DBException
        {
            String columnName = JDBCUtils.safeGetStringTrimmed(dbResult, "COLUMN_NAME");
            int ordinalPosition = JDBCUtils.safeGetInt(dbResult, "COLUMN_POSITION");
            boolean isAscending = "ASC".equals(JDBCUtils.safeGetStringTrimmed(dbResult, "DESCEND"));

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

        protected void cacheObjects(OracleTable parent, List<OracleIndex> indexes)
        {
            parent.setIndexes(indexes);
        }

        protected void cacheChildren(OracleIndex index, List<OracleIndexColumn> rows)
        {
            index.setColumns(rows);
        }
    }

    /**
     * DataType cache implementation
     */
    static class DataTypeCache extends JDBCObjectCache<OracleSchema, OracleDataType> {
        @Override
        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context, OracleSchema owner) throws SQLException, DBException
        {
            JDBCPreparedStatement dbStat = context.prepareStatement(
                "SELECT * FROM SYS.ALL_TYPES WHERE OWNER=? ORDER BY TYPE_NAME");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected OracleDataType fetchObject(JDBCExecutionContext context, OracleSchema owner, ResultSet resultSet) throws SQLException, DBException
        {
            return new OracleDataType(owner, resultSet);
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
     * Sequence cache implementation
     */
    static class SequenceCache extends JDBCObjectCache<OracleSchema, OracleSequence> {
        @Override
        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context, OracleSchema owner) throws SQLException, DBException
        {
            final JDBCPreparedStatement dbStat = context.prepareStatement(
                "SELECT * FROM SYS.ALL_SEQUENCES WHERE SEQUENCE_OWNER=? ORDER BY SEQUENCE_NAME");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected OracleSequence fetchObject(JDBCExecutionContext context, OracleSchema owner, ResultSet resultSet) throws SQLException, DBException
        {
            return new OracleSequence(owner, resultSet);
        }
    }

    /**
     * Procedures cache implementation
     */
    static class ProceduresCache extends JDBCStructCache<OracleSchema, OracleProcedure, OracleProcedureArgument> {

        ProceduresCache()
        {
            super(JDBCConstants.PROCEDURE_NAME);
        }

        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context, OracleSchema owner)
            throws SQLException, DBException
        {
            JDBCPreparedStatement dbStat = context.prepareStatement(
                "SELECT * FROM SYS.ALL_OBJECTS " +
                "WHERE OBJECT_TYPE IN ('PROCEDURE','FUNCTION') " +
                "AND OWNER=? " +
                "ORDER BY OBJECT_NAME");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        protected OracleProcedure fetchObject(JDBCExecutionContext context, OracleSchema owner, ResultSet dbResult)
            throws SQLException, DBException
        {
            return new OracleProcedure(owner, dbResult);
        }

        protected boolean isChildrenCached(OracleProcedure parent)
        {
            return parent.isColumnsCached();
        }

        protected void cacheChildren(OracleProcedure parent, List<OracleProcedureArgument> columns)
        {
            parent.cacheColumns(columns);
        }

        protected JDBCPreparedStatement prepareChildrenStatement(JDBCExecutionContext context, OracleSchema owner, OracleProcedure procedure)
            throws SQLException, DBException
        {
            JDBCPreparedStatement dbStat = context.prepareStatement(
                "SELECT * FROM SYS.ALL_ARGUMENTS " +
                "WHERE OWNER=? " + (procedure == null ? "" : "AND OBJECT_ID=? ") +
                "ORDER BY POSITION,SEQUENCE");
            dbStat.setString(1, owner.getName());
            if (procedure != null) {
                dbStat.setLong(2, procedure.getId());
            }
            return dbStat;
        }

        protected OracleProcedureArgument fetchChild(JDBCExecutionContext context, OracleSchema owner, OracleProcedure parent, ResultSet dbResult)
            throws SQLException, DBException
        {
            return new OracleProcedureArgument(context.getProgressMonitor(), parent, dbResult);
        }
    }

    static class PackageCache extends JDBCObjectCache<OracleSchema, OraclePackage> {

        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context, OracleSchema owner)
            throws SQLException, DBException
        {
            JDBCPreparedStatement dbStat = context.prepareStatement(        
                "SELECT * FROM SYS.ALL_OBJECTS WHERE OBJECT_TYPE='PACKAGE' AND OWNER=? " + 
                " ORDER BY OBJECT_NAME");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        protected OraclePackage fetchObject(JDBCExecutionContext context, OracleSchema owner, ResultSet dbResult)
            throws SQLException, DBException
        {
            return new OraclePackage(owner, dbResult);
        }

    }

    class TriggerCache extends JDBCCompositeCache<OracleSchema, OracleTable, OracleTrigger, OracleTriggerColumn> {
        protected TriggerCache()
        {
            super(tableCache, OracleTable.class, "TABLE_NAME", "TRIGGER_NAME");
        }

        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context, OracleSchema owner, OracleTable forParent) throws SQLException, DBException
        {
            JDBCPreparedStatement dbStat = context.prepareStatement(
                "SELECT * FROM SYS.ALL_TRIGGERS WHERE OBJECT_TYPE='PACKAGE' AND OWNER=? " +
                " ORDER BY OBJECT_NAME");
            dbStat.setString(1, getName());
            return dbStat;
        }

        protected OracleTrigger fetchObject(JDBCExecutionContext context, OracleTable oracleTable, String childName, ResultSet resultSet) throws SQLException, DBException
        {
            return null;
        }

        protected OracleTriggerColumn fetchObjectRow(JDBCExecutionContext context, OracleTable oracleTable, OracleTrigger forObject, ResultSet resultSet) throws SQLException, DBException
        {
            return null;
        }

        protected boolean isObjectsCached(OracleTable oracleTable)
        {
            return false;
        }

        @Override
        protected void cacheObjects(OracleTable oracleTable, List<OracleTrigger> oracleTriggers)
        {
        }

        @Override
        protected void cacheChildren(OracleTrigger oracleTrigger, List<OracleTriggerColumn> rows)
        {
        }

    }

}

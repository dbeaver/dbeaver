/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.OracleConstants;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
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
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * OracleSchema
 */
public class OracleSchema extends AbstractSchema<OracleDataSource> implements DBPSaveableObject
{
    static final Log log = LogFactory.getLog(OracleSchema.class);

    final TableCache tableCache = new TableCache();
    final ConstraintCache constraintCache = new ConstraintCache();
    final ForeignKeyCache foreignKeyCache = new ForeignKeyCache();
    final TriggerCache triggerCache = new TriggerCache();
    final IndexCache indexCache = new IndexCache();
    final DataTypeCache dataTypeCache = new DataTypeCache();
    final SequenceCache sequenceCache = new SequenceCache();
    final PackageCache packageCache = new PackageCache();
    final SynonymCache synonymCache = new SynonymCache();
    final OracleDBLinkCache dbLinkCache = new OracleDBLinkCache();
    final ProceduresCache proceduresCache = new ProceduresCache();

    private long id;
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
        return indexCache.getObjects(monitor, this, null);
    }

    @Association
    public Collection<OracleTable> getTables(DBRProgressMonitor monitor)
        throws DBException
    {
        return tableCache.getObjects(monitor, this, OracleTable.class);
    }

    public OracleTable getTable(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return tableCache.getObject(monitor, this, name, OracleTable.class);
    }

    @Association
    public Collection<OracleView> getViews(DBRProgressMonitor monitor)
        throws DBException
    {
        return tableCache.getObjects(monitor, this, OracleView.class);
    }

    public OracleView getView(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return tableCache.getObject(monitor, this, name, OracleView.class);
    }

    @Association
    public Collection<OracleMaterializedView> getMaterializedViews(DBRProgressMonitor monitor)
        throws DBException
    {
        return tableCache.getObjects(monitor, this, OracleMaterializedView.class);
    }

    @Association
    public Collection<OracleDataType> getDataTypes(DBRProgressMonitor monitor)
        throws DBException
    {
        return dataTypeCache.getObjects(monitor, this);
    }

    public OracleDataType getDataType(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return dataTypeCache.getObject(monitor, this, name);
    }

    @Association
    public Collection<OracleSequence> getSequences(DBRProgressMonitor monitor)
        throws DBException
    {
        return sequenceCache.getObjects(monitor, this);
    }

    @Association
    public Collection<OraclePackage> getPackages(DBRProgressMonitor monitor)
        throws DBException
    {
        return packageCache.getObjects(monitor, this);
    }

    @Association
    public Collection<OracleProcedure> getProcedures(DBRProgressMonitor monitor)
        throws DBException
    {
        return proceduresCache.getObjects(monitor, this);
    }

    @Association
    public Collection<OracleSynonym> getSynonyms(DBRProgressMonitor monitor)
        throws DBException
    {
        return synonymCache.getObjects(monitor, this);
    }

    @Association
    public Collection<OracleTrigger> getTriggers(DBRProgressMonitor monitor)
        throws DBException
    {
        return triggerCache.getObjects(monitor, this);
    }

    public Collection<OracleTableBase> getChildren(DBRProgressMonitor monitor)
        throws DBException
    {
        return tableCache.getObjects(monitor, this);
    }

    public OracleTableBase getChild(DBRProgressMonitor monitor, String childName)
        throws DBException
    {
        return tableCache.getObject(monitor, this, childName);
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
            tableCache.loadChildren(monitor, this, null);
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
        synonymCache.clearCache();
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
                "SELECT /*+ USE_NL(mv)*/ t.OWNER,\n" +
                "NVL(mv.MVIEW_NAME,t.TABLE_NAME) as TABLE_NAME,\n" +
                "CASE WHEN mv.MVIEW_NAME IS NULL THEN 'TABLE' ELSE 'MVIEW' END as TABLE_TYPE,t.STATUS,t.NUM_ROWS \n" +
                "FROM SYS.ALL_ALL_TABLES t\n" +
                "LEFT OUTER JOIN SYS.ALL_MVIEWS mv ON mv.OWNER=t.OWNER AND mv.CONTAINER_NAME=t.TABLE_NAME \n" +
                "WHERE t.OWNER=?\n" +
                "UNION ALL\n" +
                "SELECT v.OWNER,v.VIEW_NAME as TABLE_NAME,'VIEW' as TABLE_TYPE,NULL,NULL FROM SYS.ALL_VIEWS v WHERE v.OWNER=?\n" +
                ") tab\n" +
                "LEFT OUTER JOIN SYS.ALL_TAB_COMMENTS tc ON tc.OWNER=tab.OWNER AND tc.TABLE_NAME=tab.TABLE_NAME\n" +
                "ORDER BY tab.TABLE_NAME");
            dbStat.setString(1, owner.getName());
            dbStat.setString(2, owner.getName());
            return dbStat;
        }

        protected OracleTableBase fetchObject(JDBCExecutionContext context, OracleSchema owner, ResultSet dbResult)
            throws SQLException, DBException
        {
            final String tableType = JDBCUtils.safeGetString(dbResult, "TABLE_TYPE");
            if ("MVIEW".equals(tableType)) {
                return new OracleMaterializedView(owner, dbResult);
            } else if ("TABLE".equals(tableType)) {
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
                .append("SELECT /*+ USE_NL(cc)*/ c.*,cc.COMMENTS\n" +
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

        protected OracleConstraint fetchObject(JDBCExecutionContext context, OracleSchema owner, OracleTable parent, String indexName, ResultSet dbResult)
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
                constraintCache.getObject(monitor, schema, null);
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

        protected OracleForeignKey fetchObject(JDBCExecutionContext context, OracleSchema owner, OracleTable parent, String indexName, ResultSet dbResult)
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
    class IndexCache extends JDBCCompositeCache<OracleSchema, OracleTablePhysical, OracleIndex, OracleIndexColumn> {
        protected IndexCache()
        {
            super(tableCache, OracleTablePhysical.class, "TABLE_NAME", "INDEX_NAME");
        }

        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context, OracleSchema owner, OracleTablePhysical forTable)
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

        protected OracleIndex fetchObject(JDBCExecutionContext context, OracleSchema owner, OracleTablePhysical parent, String indexName, ResultSet dbResult)
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
            OracleTablePhysical parent, OracleIndex object, ResultSet dbResult)
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

        protected boolean isObjectsCached(OracleTablePhysical parent)
        {
            return parent.isIndexesCached();
        }

        protected void cacheObjects(OracleTablePhysical parent, List<OracleIndex> indexes)
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
    static class ProceduresCache extends JDBCObjectCache<OracleSchema, OracleProcedure> {

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

    static class SynonymCache extends JDBCObjectCache<OracleSchema, OracleSynonym> {

        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context, OracleSchema owner)
            throws SQLException, DBException
        {
            JDBCPreparedStatement dbStat = context.prepareStatement(        
                "SELECT /*+ USE_NL(O)*/ s.*,O.OBJECT_TYPE \n" +
                "FROM ALL_SYNONYMS S\n" +
                "JOIN ALL_OBJECTS O ON  O.OWNER=S.TABLE_OWNER AND O.OBJECT_NAME=S.TABLE_NAME\n" +
                "WHERE S.OWNER=? " +
                "ORDER BY S.SYNONYM_NAME");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        protected OracleSynonym fetchObject(JDBCExecutionContext context, OracleSchema owner, ResultSet dbResult)
            throws SQLException, DBException
        {
            return new OracleSynonym(context.getProgressMonitor(), owner, dbResult);
        }

    }

    static class OracleDBLinkCache extends JDBCObjectCache<OracleSchema, OracleDBLink> {

        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context, OracleSchema owner)
            throws SQLException, DBException
        {
            JDBCPreparedStatement dbStat = context.prepareStatement(        
                "SELECT * FROM SYS.ALL_DB_LINKS WHERE OWNER=? " +
                " ORDER BY DB_LINK");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        protected OracleDBLink fetchObject(JDBCExecutionContext context, OracleSchema owner, ResultSet dbResult)
            throws SQLException, DBException
        {
            return new OracleDBLink(context.getProgressMonitor(), owner, dbResult);
        }

    }

    static class TriggerCache extends JDBCStructCache<OracleSchema, OracleTrigger, OracleTriggerColumn> {
        protected TriggerCache()
        {
            super("TRIGGER_NAME");
        }

        public Collection<OracleTrigger> getObjects(DBRProgressMonitor monitor, OracleSchema oracleSchema, OracleTableBase table) throws DBException
        {
            final Collection<OracleTrigger> allTriggers = super.getObjects(monitor, oracleSchema);
            if (CommonUtils.isEmpty(allTriggers)) {
                return Collections.emptyList();
            }
            List<OracleTrigger> tableTriggers = new ArrayList<OracleTrigger>();
            for (OracleTrigger trigger : allTriggers) {
                if (trigger.getTable() == table) {
                    tableTriggers.add(trigger);
                }
            }
            return tableTriggers;
        }

        @Override
        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context, OracleSchema oracleSchema) throws SQLException, DBException
        {
            JDBCPreparedStatement dbStat = context.prepareStatement(
                "SELECT TRIGGER_NAME,TRIGGER_TYPE,TRIGGERING_EVENT,BASE_OBJECT_TYPE,TABLE_OWNER,TABLE_NAME,WHEN_CLAUSE,STATUS,DESCRIPTION\n" +
                "FROM SYS.ALL_TRIGGERS WHERE OWNER=?\n" +
                "ORDER BY TRIGGER_NAME");
            dbStat.setString(1, oracleSchema.getName());
            return dbStat;
        }

        @Override
        protected OracleTrigger fetchObject(JDBCExecutionContext context, OracleSchema oracleSchema, ResultSet resultSet) throws SQLException, DBException
        {
            OracleTableBase table = null;
            String tableName = JDBCUtils.safeGetString(resultSet, "TABLE_NAME");
            if (!CommonUtils.isEmpty(tableName)) {
                table = OracleTableBase.findTable(
                    context.getProgressMonitor(),
                    oracleSchema.getDataSource(),
                    JDBCUtils.safeGetString(resultSet, "TABLE_OWNER"),
                    tableName);
            }
            return new OracleTrigger(oracleSchema, table, resultSet);
        }

        @Override
        protected JDBCPreparedStatement prepareChildrenStatement(JDBCExecutionContext context, OracleSchema oracleSchema, OracleTrigger forObject) throws SQLException, DBException
        {
            JDBCPreparedStatement dbStat = context.prepareStatement(
                "SELECT TRIGGER_NAME,TABLE_OWNER,TABLE_NAME,COLUMN_NAME,COLUMN_LIST,COLUMN_USAGE\n" +
                "FROM SYS.ALL_TRIGGER_COLS WHERE TRIGGER_OWNER=?" +
                (forObject == null ? "" : " AND TRIGGER_NAME=?") +
                "\nORDER BY TRIGGER_NAME");
            dbStat.setString(1, oracleSchema.getName());
            if (forObject != null) {
                dbStat.setString(2, forObject.getName());
            }
            return dbStat;
        }

        @Override
        protected OracleTriggerColumn fetchChild(JDBCExecutionContext context, OracleSchema oracleSchema, OracleTrigger parent, ResultSet dbResult) throws SQLException, DBException
        {
            OracleTableBase refTable = OracleTableBase.findTable(
                context.getProgressMonitor(),
                oracleSchema.getDataSource(),
                JDBCUtils.safeGetString(dbResult, "TABLE_OWNER"),
                JDBCUtils.safeGetString(dbResult, "TABLE_NAME"));
            if (refTable != null) {
                final String columnName = JDBCUtils.safeGetString(dbResult, "COLUMN_NAME");
                OracleTableColumn tableColumn = refTable.getColumn(context.getProgressMonitor(), columnName);
                if (tableColumn == null) {
                    log.debug("Column '" + columnName + "' not found in table '" + refTable.getFullQualifiedName() + "' for trigger '" + parent.getName() + "'");
                }
                return new OracleTriggerColumn(context.getProgressMonitor(), parent, tableColumn, dbResult);
            }
            return null;
        }

        @Override
        protected boolean isChildrenCached(OracleTrigger parent)
        {
            return parent.isColumnsCached();
        }

        @Override
        protected void cacheChildren(OracleTrigger oracleTrigger, List<OracleTriggerColumn> rows)
        {
            oracleTrigger.setColumns(rows);
        }

    }

}

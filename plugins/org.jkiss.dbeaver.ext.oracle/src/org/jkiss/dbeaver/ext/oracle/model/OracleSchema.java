/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPSystemObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectLookup;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * OracleSchema
 */
public class OracleSchema extends OracleGlobalObject implements DBSSchema, DBPRefreshableObject, DBPSystemObject, DBSProcedureContainer
{
    private static final Log log = Log.getLog(OracleSchema.class);

    final public TableCache tableCache = new TableCache();
    final public MViewCache mviewCache = new MViewCache();
    final public ConstraintCache constraintCache = new ConstraintCache();
    final public ForeignKeyCache foreignKeyCache = new ForeignKeyCache();
    final public TriggerCache triggerCache = new TriggerCache();
    final public IndexCache indexCache = new IndexCache();
    final public DataTypeCache dataTypeCache = new DataTypeCache();
    final public SequenceCache sequenceCache = new SequenceCache();
    final public PackageCache packageCache = new PackageCache();
    final public SynonymCache synonymCache = new SynonymCache();
    final public DBLinkCache dbLinkCache = new DBLinkCache();
    final public ProceduresCache proceduresCache = new ProceduresCache();
    final public JavaCache javaCache = new JavaCache();
    final public RecycleBin recycleBin = new RecycleBin();

    private long id;
    private String name;
    private Date createTime;
    private transient OracleUser user;

    public OracleSchema(OracleDataSource dataSource, long id, String name)
    {
        super(dataSource, id > 0);
        this.id = id;
        this.name = name;
    }

    public OracleSchema(@NotNull OracleDataSource dataSource, @NotNull ResultSet dbResult)
    {
        super(dataSource, true);
        this.id = JDBCUtils.safeGetLong(dbResult, "USER_ID");
        this.name = JDBCUtils.safeGetString(dbResult, "USERNAME");
        if (CommonUtils.isEmpty(this.name)) {
            log.warn("Empty schema name fetched");
            this.name = "? " + super.hashCode();
        }
        this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATED");
    }

    public boolean isPublic()
    {
        return OracleConstants.USER_PUBLIC.equals(this.name);
    }

    @Property(viewable = false, order = 200)
    public long getId()
    {
        return id;
    }

    @Property(viewable = false, order = 190)
    public Date getCreateTime() {
        return createTime;
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, order = 1)
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return null;
    }

    /**
     * User reference never read directly from database.
     * It is used by managers to create/delete/alter schemas
     * @return user reference or null
     */
    public OracleUser getUser()
    {
        return user;
    }

    public void setUser(OracleUser user)
    {
        this.user = user;
    }

    @Association
    public Collection<OracleTableIndex> getIndexes(DBRProgressMonitor monitor)
        throws DBException
    {
        return indexCache.getObjects(monitor, this, null);
    }

    @Association
    public Collection<OracleTable> getTables(DBRProgressMonitor monitor)
        throws DBException
    {
        return tableCache.getTypedObjects(monitor, this, OracleTable.class);
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
        return tableCache.getTypedObjects(monitor, this, OracleView.class);
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
        return mviewCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<OracleDataType> getDataTypes(DBRProgressMonitor monitor)
        throws DBException
    {
        return dataTypeCache.getAllObjects(monitor, this);
    }

    public OracleDataType getDataType(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        OracleDataType type = dataTypeCache.getObject(monitor, this, name);
        if (type == null) {
            final OracleSynonym synonym = synonymCache.getObject(monitor, this, name);
            if (synonym != null && synonym.getObjectType() == OracleObjectType.TYPE) {
                Object object = synonym.getObject(monitor);
                if (object instanceof OracleDataType) {
                    return (OracleDataType)object;
                }
            }
            return null;
        } else {
            return type;
        }
    }

    @Association
    public Collection<OracleSequence> getSequences(DBRProgressMonitor monitor)
        throws DBException
    {
        return sequenceCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<OraclePackage> getPackages(DBRProgressMonitor monitor)
        throws DBException
    {
        return packageCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<OracleProcedureStandalone> getProcedures(DBRProgressMonitor monitor)
        throws DBException
    {
        return proceduresCache.getAllObjects(monitor, this);
    }

    @Override
    public OracleProcedureStandalone getProcedure(DBRProgressMonitor monitor, String uniqueName) throws DBException {
        return proceduresCache.getObject(monitor, this, uniqueName);
    }

    @Association
    public Collection<OracleSynonym> getSynonyms(DBRProgressMonitor monitor)
        throws DBException
    {
        return synonymCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<OracleTrigger> getTriggers(DBRProgressMonitor monitor)
        throws DBException
    {
        return triggerCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<OracleDBLink> getDatabaseLinks(DBRProgressMonitor monitor)
        throws DBException
    {
        return dbLinkCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<OracleJavaClass> getJavaClasses(DBRProgressMonitor monitor)
        throws DBException
    {
        return javaCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<OracleRecycledObject> getRecycledObjects(DBRProgressMonitor monitor)
        throws DBException
    {
        return recycleBin.getAllObjects(monitor, this);
    }

    @Override
    public Collection<OracleTableBase> getChildren(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return tableCache.getAllObjects(monitor, this);
    }

    @Override
    public OracleTableBase getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName)
        throws DBException
    {
        return tableCache.getObject(monitor, this, childName);
    }

    @Override
    public Class<? extends DBSEntity> getChildType(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return OracleTable.class;
    }

    @Override
    public synchronized void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope)
        throws DBException
    {
        monitor.subTask("Cache tables");
        tableCache.getAllObjects(monitor, this);
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
    public synchronized boolean refreshObject(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
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
        javaCache.clearCache();
        recycleBin.clearCache();
        return true;
    }

    @Override
    public boolean isSystem()
    {
        return ArrayUtils.contains(OracleConstants.SYSTEM_SCHEMAS, getName());
    }

    @Override
    public String toString()
    {
        return "Schema " + name;
    }

    protected static OracleTableColumn getTableColumn(JDBCSession session, OracleTableBase parent, ResultSet dbResult) throws DBException
    {
        String columnName = JDBCUtils.safeGetStringTrimmed(dbResult, "COLUMN_NAME");
        OracleTableColumn tableColumn = parent.getAttribute(session.getProgressMonitor(), columnName);
        if (tableColumn == null) {
            log.debug("Column '" + columnName + "' not found in table '" + parent.getName() + "'");
        }
        return tableColumn;
    }

    public static class TableCache extends JDBCStructCache<OracleSchema, OracleTableBase, OracleTableColumn> implements JDBCObjectLookup<OracleSchema> {

        private static final Comparator<? super OracleTableColumn> ORDER_COMPARATOR = new Comparator<OracleTableColumn>() {
            @Override
            public int compare(OracleTableColumn o1, OracleTableColumn o2) {
                return o1.getOrdinalPosition() - o2.getOrdinalPosition();
            }
        };

        protected TableCache()
        {
            super("TABLE_NAME");
            setListOrderComparator(DBUtils.<OracleTableBase>nameComparator());
        }

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OracleSchema owner)
            throws SQLException
        {
            return prepareObjectsStatement(session, owner, null);
        }

        @Override
        public JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OracleSchema owner, @Nullable String objectName) throws SQLException {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                    "\tSELECT " + OracleUtils.getSysCatalogHint(owner.getDataSource()) + " t.OWNER,t.TABLE_NAME as TABLE_NAME,'TABLE' as OBJECT_TYPE,'VALID' as STATUS,t.TABLE_TYPE_OWNER,t.TABLE_TYPE,t.TABLESPACE_NAME,t.PARTITIONED,t.IOT_TYPE,t.IOT_NAME,t.TEMPORARY,t.SECONDARY,t.NESTED,t.NUM_ROWS \n" +
                    "\tFROM SYS.ALL_ALL_TABLES t\n" +
                    "\tWHERE t.OWNER=? AND NESTED='NO'" + (objectName == null ? "": " AND t.TABLE_NAME=?") + "\n" +
                "UNION ALL\n" +
                    "\tSELECT " + OracleUtils.getSysCatalogHint(owner.getDataSource()) + " o.OWNER,o.OBJECT_NAME as TABLE_NAME,'VIEW' as OBJECT_TYPE,o.STATUS,NULL,NULL,NULL,NULL,NULL,NULL,o.TEMPORARY,o.SECONDARY,NULL,NULL \n" +
                    "\tFROM SYS.ALL_OBJECTS o \n" +
                    "\tWHERE o.OWNER=? AND o.OBJECT_TYPE='VIEW'" + (objectName == null ? "": " AND o.OBJECT_NAME=?") + "\n"
                );
            int index = 1;
            dbStat.setString(index++, owner.getName());
            if (objectName != null) dbStat.setString(index++, objectName);
            dbStat.setString(index++, owner.getName());
            if (objectName != null) dbStat.setString(index, objectName);
            return dbStat;
        }

        @Override
        protected OracleTableBase fetchObject(@NotNull JDBCSession session, @NotNull OracleSchema owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            final String tableType = JDBCUtils.safeGetString(dbResult, "OBJECT_TYPE");
            if ("TABLE".equals(tableType)) {
                return new OracleTable(session.getProgressMonitor(), owner, dbResult);
            } else {
                return new OracleView(owner, dbResult);
            }
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull OracleSchema owner, @Nullable OracleTableBase forTable)
            throws SQLException
        {
            StringBuilder sql = new StringBuilder(500);
            sql
                .append("SELECT c.*\n" +
                    "FROM SYS.ALL_TAB_COLS c\n" +
//                    "LEFT OUTER JOIN SYS.ALL_COL_COMMENTS cc ON CC.OWNER=c.OWNER AND cc.TABLE_NAME=c.TABLE_NAME AND cc.COLUMN_NAME=c.COLUMN_NAME\n" +
                    "WHERE c.OWNER=?");
            if (forTable != null) {
                sql.append(" AND c.TABLE_NAME=?");
            }
/*
            sql.append("\nORDER BY ");
            if (forTable != null) {
                sql.append("c.TABLE_NAME,");
            }
            sql.append("c.COLUMN_ID");
*/
            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            dbStat.setString(1, owner.getName());
            if (forTable != null) {
                dbStat.setString(2, forTable.getName());
            }
            return dbStat;
        }

        @Override
        protected OracleTableColumn fetchChild(@NotNull JDBCSession session, @NotNull OracleSchema owner, @NotNull OracleTableBase table, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new OracleTableColumn(session.getProgressMonitor(), table, dbResult);
        }

        @Override
        protected void cacheChildren(OracleTableBase parent, List<OracleTableColumn> oracleTableColumns) {
            Collections.sort(oracleTableColumns, ORDER_COMPARATOR);
            super.cacheChildren(parent, oracleTableColumns);
        }

    }

    /**
     * Index cache implementation
     */
    class ConstraintCache extends JDBCCompositeCache<OracleSchema, OracleTableBase, OracleTableConstraint, OracleTableConstraintColumn> {
        protected ConstraintCache()
        {
            super(tableCache, OracleTableBase.class, "TABLE_NAME", "CONSTRAINT_NAME");
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, OracleSchema owner, OracleTableBase forTable)
            throws SQLException
        {
            StringBuilder sql = new StringBuilder(500);
            sql
                .append("SELECT " + OracleUtils.getSysCatalogHint(owner.getDataSource()) + "\n" +
                    "c.TABLE_NAME, c.CONSTRAINT_NAME,c.CONSTRAINT_TYPE,c.STATUS,c.SEARCH_CONDITION," +
                    "col.COLUMN_NAME,col.POSITION\n" +
                    "FROM SYS.ALL_CONSTRAINTS c\n" +
                    "JOIN SYS.ALL_CONS_COLUMNS col ON c.OWNER=col.OWNER AND c.CONSTRAINT_NAME=col.CONSTRAINT_NAME\n" +
                    "WHERE c.CONSTRAINT_TYPE<>'R' AND c.OWNER=?");
            if (forTable != null) {
                sql.append(" AND c.TABLE_NAME=?");
            }
            sql.append("\nORDER BY c.CONSTRAINT_NAME,col.POSITION");

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            dbStat.setString(1, OracleSchema.this.getName());
            if (forTable != null) {
                dbStat.setString(2, forTable.getName());
            }
            return dbStat;
        }

        @Nullable
        @Override
        protected OracleTableConstraint fetchObject(JDBCSession session, OracleSchema owner, OracleTableBase parent, String indexName, JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new OracleTableConstraint(parent, dbResult);
        }

        @Nullable
        @Override
        protected OracleTableConstraintColumn[] fetchObjectRow(
            JDBCSession session,
            OracleTableBase parent, OracleTableConstraint object, JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            final OracleTableColumn tableColumn = getTableColumn(session, parent, dbResult);
            return tableColumn == null ? null : new OracleTableConstraintColumn[] { new OracleTableConstraintColumn(
                object,
                tableColumn,
                JDBCUtils.safeGetInt(dbResult, "POSITION")) };
        }

        @Override
        protected void cacheChildren(DBRProgressMonitor monitor, OracleTableConstraint constraint, List<OracleTableConstraintColumn> rows)
        {
            constraint.setColumns(rows);
        }
    }

    class ForeignKeyCache extends JDBCCompositeCache<OracleSchema, OracleTable, OracleTableForeignKey, OracleTableForeignKeyColumn> {
        protected ForeignKeyCache()
        {
            super(tableCache, OracleTable.class, "TABLE_NAME", "CONSTRAINT_NAME");
        }

        @Override
        protected void loadObjects(DBRProgressMonitor monitor, OracleSchema schema, OracleTable forParent)
            throws DBException
        {
            // Cache schema constraints in not table specified
            if (forParent == null) {
                constraintCache.getObject(monitor, schema, null);
            }
            super.loadObjects(monitor, schema, forParent);
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, OracleSchema owner, OracleTable forTable)
            throws SQLException
        {
            StringBuilder sql = new StringBuilder(500);
            sql.append(
                "SELECT " + OracleUtils.getSysCatalogHint(owner.getDataSource()) + " \r\n" +
                "c.TABLE_NAME, c.CONSTRAINT_NAME,c.CONSTRAINT_TYPE,c.STATUS,c.R_OWNER,c.R_CONSTRAINT_NAME,ref.TABLE_NAME as R_TABLE_NAME,c.DELETE_RULE, \n" +
                "col.COLUMN_NAME,col.POSITION\r\n" +
                "FROM SYS.ALL_CONSTRAINTS c\n" +
                "JOIN SYS.ALL_CONS_COLUMNS col ON c.OWNER=col.OWNER AND c.CONSTRAINT_NAME=col.CONSTRAINT_NAME\n" +
                "JOIN SYS.ALL_CONSTRAINTS ref ON ref.OWNER=c.r_OWNER AND ref.CONSTRAINT_NAME=c.R_CONSTRAINT_NAME \n" +
                "WHERE c.CONSTRAINT_TYPE='R' AND c.OWNER=?");
            if (forTable != null) {
                sql.append(" AND c.TABLE_NAME=?");
            }
            sql.append("\nORDER BY c.CONSTRAINT_NAME,col.POSITION");

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            dbStat.setString(1, OracleSchema.this.getName());
            if (forTable != null) {
                dbStat.setString(2, forTable.getName());
            }
            return dbStat;
        }

        @Nullable
        @Override
        protected OracleTableForeignKey fetchObject(JDBCSession session, OracleSchema owner, OracleTable parent, String indexName, JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new OracleTableForeignKey(session.getProgressMonitor(), parent, dbResult);
        }

        @Nullable
        @Override
        protected OracleTableForeignKeyColumn[] fetchObjectRow(
            JDBCSession session,
            OracleTable parent, OracleTableForeignKey object, JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            OracleTableColumn column = getTableColumn(session, parent, dbResult);
            return column == null ? null : new OracleTableForeignKeyColumn[] { new OracleTableForeignKeyColumn(
                object,
                column,
                JDBCUtils.safeGetInt(dbResult, "POSITION")) };
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void cacheChildren(DBRProgressMonitor monitor, OracleTableForeignKey foreignKey, List<OracleTableForeignKeyColumn> rows)
        {
            foreignKey.setColumns((List)rows);
        }
    }


    /**
     * Index cache implementation
     */
    class IndexCache extends JDBCCompositeCache<OracleSchema, OracleTablePhysical, OracleTableIndex, OracleTableIndexColumn> {
        protected IndexCache()
        {
            super(tableCache, OracleTablePhysical.class, "TABLE_NAME", "INDEX_NAME");
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, OracleSchema owner, OracleTablePhysical forTable)
            throws SQLException
        {
            StringBuilder sql = new StringBuilder();
            sql.append(
                "SELECT " + OracleUtils.getSysCatalogHint(owner.getDataSource()) + " " +
                    "i.OWNER,i.INDEX_NAME,i.INDEX_TYPE,i.TABLE_OWNER,i.TABLE_NAME,i.UNIQUENESS,i.TABLESPACE_NAME,i.STATUS,i.NUM_ROWS,i.SAMPLE_SIZE,\n" +
                    "ic.COLUMN_NAME,ic.COLUMN_POSITION,ic.COLUMN_LENGTH,ic.DESCEND\n" +
                    "FROM SYS.ALL_INDEXES i \n" +
                    "JOIN SYS.ALL_IND_COLUMNS ic ON ic.INDEX_OWNER=i.OWNER AND ic.INDEX_NAME=i.INDEX_NAME\n" +
                    "WHERE ");
            if (forTable == null) {
                sql.append(" i.OWNER=?");
            } else {
                sql.append(" i.TABLE_OWNER=? AND i.TABLE_NAME=?");
            }
            sql.append("\nORDER BY i.INDEX_NAME,ic.COLUMN_POSITION");

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            if (forTable == null) {
                dbStat.setString(1, OracleSchema.this.getName());
            } else {
                dbStat.setString(1, OracleSchema.this.getName());
                dbStat.setString(2, forTable.getName());
            }
            return dbStat;
        }

        @Nullable
        @Override
        protected OracleTableIndex fetchObject(JDBCSession session, OracleSchema owner, OracleTablePhysical parent, String indexName, JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new OracleTableIndex(owner, parent, indexName, dbResult);
        }

        @Nullable
        @Override
        protected OracleTableIndexColumn[] fetchObjectRow(
            JDBCSession session,
            OracleTablePhysical parent, OracleTableIndex object, JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            String columnName = JDBCUtils.safeGetStringTrimmed(dbResult, "COLUMN_NAME");
            int ordinalPosition = JDBCUtils.safeGetInt(dbResult, "COLUMN_POSITION");
            boolean isAscending = "ASC".equals(JDBCUtils.safeGetStringTrimmed(dbResult, "DESCEND"));

            OracleTableColumn tableColumn = parent.getAttribute(session.getProgressMonitor(), columnName);
            if (tableColumn == null) {
                log.debug("Column '" + columnName + "' not found in table '" + parent.getName() + "' for index '" + object.getName() + "'");
                return null;
            }

            return new OracleTableIndexColumn[] { new OracleTableIndexColumn(
                object,
                tableColumn,
                ordinalPosition,
                isAscending) };
        }

        @Override
        protected void cacheChildren(DBRProgressMonitor monitor, OracleTableIndex index, List<OracleTableIndexColumn> rows)
        {
            index.setColumns(rows);
        }
    }

    /**
     * DataType cache implementation
     */
    static class DataTypeCache extends JDBCObjectCache<OracleSchema, OracleDataType> {
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OracleSchema owner) throws SQLException
        {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT " + OracleUtils.getSysCatalogHint(owner.getDataSource()) + " * FROM SYS.ALL_TYPES WHERE OWNER=? ORDER BY TYPE_NAME");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected OracleDataType fetchObject(@NotNull JDBCSession session, @NotNull OracleSchema owner, @NotNull JDBCResultSet resultSet) throws SQLException
        {
            return new OracleDataType(owner, resultSet);
        }
    }

    /**
     * Sequence cache implementation
     */
    static class SequenceCache extends JDBCObjectCache<OracleSchema, OracleSequence> {
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OracleSchema owner) throws SQLException
        {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT " + OracleUtils.getSysCatalogHint(owner.getDataSource()) + " * FROM SYS.ALL_SEQUENCES WHERE SEQUENCE_OWNER=? ORDER BY SEQUENCE_NAME");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected OracleSequence fetchObject(@NotNull JDBCSession session, @NotNull OracleSchema owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException
        {
            return new OracleSequence(owner, resultSet);
        }
    }

    /**
     * Procedures cache implementation
     */
    static class ProceduresCache extends JDBCObjectCache<OracleSchema, OracleProcedureStandalone> {

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OracleSchema owner)
            throws SQLException
        {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT " + OracleUtils.getSysCatalogHint(owner.getDataSource()) + " * FROM SYS.ALL_OBJECTS " +
                "WHERE OBJECT_TYPE IN ('PROCEDURE','FUNCTION') " +
                "AND OWNER=? " +
                "ORDER BY OBJECT_NAME");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected OracleProcedureStandalone fetchObject(@NotNull JDBCSession session, @NotNull OracleSchema owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new OracleProcedureStandalone(owner, dbResult);
        }
    }

    static class PackageCache extends JDBCObjectCache<OracleSchema, OraclePackage> {

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OracleSchema owner)
            throws SQLException
        {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT " + OracleUtils.getSysCatalogHint(owner.getDataSource()) + " * FROM SYS.ALL_OBJECTS WHERE OBJECT_TYPE='PACKAGE' AND OWNER=? " +
                " ORDER BY OBJECT_NAME");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected OraclePackage fetchObject(@NotNull JDBCSession session, @NotNull OracleSchema owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new OraclePackage(owner, dbResult);
        }

    }

    /**
     * Sequence cache implementation
     */
    static class SynonymCache extends JDBCObjectCache<OracleSchema, OracleSynonym> {
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OracleSchema owner) throws SQLException
        {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT " + OracleUtils.getSysCatalogHint(owner.getDataSource()) + " s.*,O.OBJECT_TYPE \n" +
                "FROM ALL_SYNONYMS S\n" +
                "JOIN ALL_OBJECTS O ON  O.OWNER=S.TABLE_OWNER AND O.OBJECT_NAME=S.TABLE_NAME\n" +
                "WHERE S.OWNER=? AND O.OBJECT_TYPE NOT IN ('JAVA CLASS','PACKAGE BODY')\n" +
                "ORDER BY S.SYNONYM_NAME");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected OracleSynonym fetchObject(@NotNull JDBCSession session, @NotNull OracleSchema owner, @NotNull JDBCResultSet resultSet) throws SQLException, DBException
        {
            return new OracleSynonym(owner, resultSet);
        }
    }

    static class MViewCache extends JDBCObjectCache<OracleSchema, OracleMaterializedView> {

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OracleSchema owner)
            throws SQLException
        {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM SYS.ALL_MVIEWS WHERE OWNER=? " +
                "ORDER BY MVIEW_NAME");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected OracleMaterializedView fetchObject(@NotNull JDBCSession session, @NotNull OracleSchema owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new OracleMaterializedView(owner, dbResult);
        }

    }

    static class DBLinkCache extends JDBCObjectCache<OracleSchema, OracleDBLink> {

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OracleSchema owner)
            throws SQLException
        {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM SYS.ALL_DB_LINKS WHERE OWNER=? " +
                " ORDER BY DB_LINK");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected OracleDBLink fetchObject(@NotNull JDBCSession session, @NotNull OracleSchema owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new OracleDBLink(session.getProgressMonitor(), owner, dbResult);
        }

    }

    static class TriggerCache extends JDBCStructCache<OracleSchema, OracleTrigger, OracleTriggerColumn> {
        protected TriggerCache()
        {
            super("TRIGGER_NAME");
        }

        public Collection<OracleTrigger> getObjects(DBRProgressMonitor monitor, OracleSchema oracleSchema, OracleTableBase table) throws DBException
        {
            final Collection<OracleTrigger> allTriggers = super.getAllObjects(monitor, oracleSchema);
            if (CommonUtils.isEmpty(allTriggers)) {
                return Collections.emptyList();
            }
            List<OracleTrigger> tableTriggers = new ArrayList<>();
            for (OracleTrigger trigger : allTriggers) {
                if (trigger.getTable() == table) {
                    tableTriggers.add(trigger);
                }
            }
            return tableTriggers;
        }

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OracleSchema schema) throws SQLException
        {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT *\n" +
                "FROM " + OracleUtils.getAdminAllViewPrefix(schema.getDataSource()) + "TRIGGERS WHERE OWNER=?\n" +
                "ORDER BY TRIGGER_NAME");
            dbStat.setString(1, schema.getName());
            return dbStat;
        }

        @Override
        protected OracleTrigger fetchObject(@NotNull JDBCSession session, @NotNull OracleSchema oracleSchema, @NotNull JDBCResultSet resultSet) throws SQLException, DBException
        {
            OracleTableBase table = null;
            String tableName = JDBCUtils.safeGetString(resultSet, "TABLE_NAME");
            if (!CommonUtils.isEmpty(tableName)) {
                table = OracleTableBase.findTable(
                    session.getProgressMonitor(),
                    oracleSchema.getDataSource(),
                    JDBCUtils.safeGetString(resultSet, "TABLE_OWNER"),
                    tableName);
            }
            return new OracleTrigger(oracleSchema, table, resultSet);
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull OracleSchema oracleSchema, @Nullable OracleTrigger forObject) throws SQLException
        {
            JDBCPreparedStatement dbStat = session.prepareStatement(
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
        protected OracleTriggerColumn fetchChild(@NotNull JDBCSession session, @NotNull OracleSchema oracleSchema, @NotNull OracleTrigger parent, @NotNull JDBCResultSet dbResult) throws SQLException, DBException
        {
            OracleTableBase refTable = OracleTableBase.findTable(
                session.getProgressMonitor(),
                oracleSchema.getDataSource(),
                JDBCUtils.safeGetString(dbResult, "TABLE_OWNER"),
                JDBCUtils.safeGetString(dbResult, "TABLE_NAME"));
            if (refTable != null) {
                final String columnName = JDBCUtils.safeGetString(dbResult, "COLUMN_NAME");
                OracleTableColumn tableColumn = refTable.getAttribute(session.getProgressMonitor(), columnName);
                if (tableColumn == null) {
                    log.debug("Column '" + columnName + "' not found in table '" + refTable.getFullQualifiedName() + "' for trigger '" + parent.getName() + "'");
                }
                return new OracleTriggerColumn(session.getProgressMonitor(), parent, tableColumn, dbResult);
            }
            return null;
        }

    }

    static class JavaCache extends JDBCObjectCache<OracleSchema, OracleJavaClass> {

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OracleSchema owner)
            throws SQLException
        {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM SYS.ALL_JAVA_CLASSES WHERE OWNER=? ");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected OracleJavaClass fetchObject(@NotNull JDBCSession session, @NotNull OracleSchema owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new OracleJavaClass(owner, dbResult);
        }

    }

    static class RecycleBin extends JDBCObjectCache<OracleSchema, OracleRecycledObject> {

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OracleSchema owner)
            throws SQLException
        {
            final boolean isPublic = owner.isPublic();
            JDBCPreparedStatement dbStat = session.prepareStatement(
                isPublic ?
                    "SELECT * FROM SYS.USER_RECYCLEBIN" :
                    "SELECT * FROM SYS.DBA_RECYCLEBIN WHERE OWNER=?");
            if (!isPublic) {
                dbStat.setString(1, owner.getName());
            }
            return dbStat;
        }

        @Override
        protected OracleRecycledObject fetchObject(@NotNull JDBCSession session, @NotNull OracleSchema owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new OracleRecycledObject(owner, dbResult);
        }

    }

}

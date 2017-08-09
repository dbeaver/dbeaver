/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectLookupCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructLookupCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
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
    final public SchedulerJobCache schedulerJobCache = new SchedulerJobCache();
    final public SchedulerProgramCache schedulerProgramCache = new SchedulerProgramCache();
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
    public Collection<OracleSchemaTrigger> getTriggers(DBRProgressMonitor monitor)
        throws DBException
    {
        return triggerCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<OracleTableTrigger> getTableTriggers(DBRProgressMonitor monitor)
            throws DBException
    {
        List<OracleTableTrigger> allTableTriggers = new ArrayList<>();
        for (OracleTableBase table : tableCache.getAllObjects(monitor, this)) {
            Collection<OracleTableTrigger> triggers = table.getTriggers(monitor);
            if (!CommonUtils.isEmpty(triggers)) {
                allTableTriggers.addAll(triggers);
            }
        }
        allTableTriggers.sort(new Comparator<OracleTableTrigger>() {
            @Override
            public int compare(OracleTableTrigger o1, OracleTableTrigger o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return allTableTriggers;
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
    public Collection<OracleSchedulerJob> getSchedulerJobs(DBRProgressMonitor monitor)
            throws DBException
    {
        return schedulerJobCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<OracleSchedulerProgram> getSchedulerPrograms(DBRProgressMonitor monitor)
            throws DBException
    {
        return schedulerProgramCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<OracleRecycledObject> getRecycledObjects(DBRProgressMonitor monitor)
        throws DBException
    {
        return recycleBin.getAllObjects(monitor, this);
    }

    @Override
    public Collection<DBSObject> getChildren(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        List<DBSObject> children = new ArrayList<>();
        for (OracleTableBase table : tableCache.getAllObjects(monitor, this)) {
            children.add(table);
        }
        for (OraclePackage pack : packageCache.getAllObjects(monitor, this)) {
            children.add(pack);
        }
        return children;
    }

    @Override
    public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName)
        throws DBException
    {
        final OracleTableBase table = tableCache.getObject(monitor, this, childName);
        if (table != null) {
            return table;
        }
        return packageCache.getObject(monitor, this, childName);
    }

    @Override
    public Class<? extends DBSEntity> getChildType(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return DBSEntity.class;
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
    public synchronized DBSObject refreshObject(@NotNull DBRProgressMonitor monitor)
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
        schedulerJobCache.clearCache();
        recycleBin.clearCache();
        return this;
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

    private static OracleTableColumn getTableColumn(JDBCSession session, OracleTableBase parent, ResultSet dbResult) throws DBException
    {
        String columnName = JDBCUtils.safeGetStringTrimmed(dbResult, "COLUMN_NAME");
        OracleTableColumn tableColumn = columnName == null ? null : parent.getAttribute(session.getProgressMonitor(), columnName);
        if (tableColumn == null) {
            log.debug("Column '" + columnName + "' not found in table '" + parent.getName() + "'");
        }
        return tableColumn;
    }

    public static class TableCache extends JDBCStructLookupCache<OracleSchema, OracleTableBase, OracleTableColumn> {

        TableCache()
        {
            super("TABLE_NAME");
            setListOrderComparator(DBUtils.<OracleTableBase>nameComparator());
        }

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull OracleSchema owner, @Nullable OracleTableBase object, @Nullable String objectName) throws SQLException {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                    "\tSELECT " + OracleUtils.getSysCatalogHint(owner.getDataSource()) + " t.OWNER,t.TABLE_NAME as TABLE_NAME,'TABLE' as OBJECT_TYPE,'VALID' as STATUS,t.TABLE_TYPE_OWNER,t.TABLE_TYPE,t.TABLESPACE_NAME,t.PARTITIONED,t.IOT_TYPE,t.IOT_NAME,t.TEMPORARY,t.SECONDARY,t.NESTED,t.NUM_ROWS \n" +
                    "\tFROM SYS.ALL_ALL_TABLES t\n" +
                    "\tWHERE t.OWNER=? AND NESTED='NO'" + (object == null && objectName == null ? "": " AND t.TABLE_NAME=?") + "\n" +
                "UNION ALL\n" +
                    "\tSELECT " + OracleUtils.getSysCatalogHint(owner.getDataSource()) + " o.OWNER,o.OBJECT_NAME as TABLE_NAME,'VIEW' as OBJECT_TYPE,o.STATUS,NULL,NULL,NULL,'NO',NULL,NULL,o.TEMPORARY,o.SECONDARY,'NO',0 \n" +
                    "\tFROM SYS.ALL_OBJECTS o \n" +
                    "\tWHERE o.OWNER=? AND o.OBJECT_TYPE='VIEW'" + (object == null && objectName == null  ? "": " AND o.OBJECT_NAME=?") + "\n"
                );
            int index = 1;
            dbStat.setString(index++, owner.getName());
            if (object != null || objectName != null) dbStat.setString(index++, object != null ? object.getName() : objectName);
            dbStat.setString(index++, owner.getName());
            if (object != null || objectName != null) dbStat.setString(index, object != null ? object.getName() : objectName);
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
            String colsView = "ALL_TAB_COLS";
            if (!owner.getDataSource().isViewAvailable(session.getProgressMonitor(), "SYS", colsView)) {
                colsView = "ALL_TAB_COLUMNS";
            }
            StringBuilder sql = new StringBuilder(500);
            sql
                .append("SELECT ").append(OracleUtils.getSysCatalogHint(owner.getDataSource())).append("\nc.* " +
                    "FROM SYS.").append(colsView).append(" c\n" +
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
            Collections.sort(oracleTableColumns, DBUtils.orderComparator());
            super.cacheChildren(parent, oracleTableColumns);
        }

    }

    /**
     * Index cache implementation
     */
    class ConstraintCache extends JDBCCompositeCache<OracleSchema, OracleTableBase, OracleTableConstraint, OracleTableConstraintColumn> {
        ConstraintCache()
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
                .append("SELECT ").append(OracleUtils.getSysCatalogHint(owner.getDataSource())).append("\n" +
                    "c.TABLE_NAME, c.CONSTRAINT_NAME,c.CONSTRAINT_TYPE,c.STATUS,c.SEARCH_CONDITION," +
                    "col.COLUMN_NAME,col.POSITION\n" +
                    "FROM SYS.ALL_CONSTRAINTS c, SYS.ALL_CONS_COLUMNS col\n" +
                    "WHERE c.CONSTRAINT_TYPE<>'R' AND c.OWNER=? AND c.OWNER=col.OWNER AND c.CONSTRAINT_NAME=col.CONSTRAINT_NAME");
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
        ForeignKeyCache()
        {
            super(tableCache, OracleTable.class, "TABLE_NAME", "CONSTRAINT_NAME");
        }

        @Override
        protected void loadObjects(DBRProgressMonitor monitor, OracleSchema schema, OracleTable forParent)
            throws DBException
        {
            // Cache schema constraints if not table specified
            if (forParent == null) {
                constraintCache.getAllObjects(monitor, schema);
            }
            super.loadObjects(monitor, schema, forParent);
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, OracleSchema owner, OracleTable forTable)
            throws SQLException
        {
            StringBuilder sql = new StringBuilder(500);
            sql.append("SELECT ").append(OracleUtils.getSysCatalogHint(owner.getDataSource())).append(" \r\n" +
                "c.TABLE_NAME, c.CONSTRAINT_NAME,c.CONSTRAINT_TYPE,c.STATUS,c.R_OWNER,c.R_CONSTRAINT_NAME,rc.TABLE_NAME as R_TABLE_NAME,c.DELETE_RULE, \n" +
                "col.COLUMN_NAME,col.POSITION\r\n" +
                "FROM SYS.ALL_CONSTRAINTS c, SYS.ALL_CONS_COLUMNS col, SYS.ALL_CONSTRAINTS rc\n" +
                "WHERE c.CONSTRAINT_TYPE='R' AND c.OWNER=?\n" +
                "AND c.OWNER=col.OWNER AND c.CONSTRAINT_NAME=col.CONSTRAINT_NAME\n" +
                "AND rc.OWNER=c.r_OWNER AND rc.CONSTRAINT_NAME=c.R_CONSTRAINT_NAME");
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
        IndexCache()
        {
            super(tableCache, OracleTablePhysical.class, "TABLE_NAME", "INDEX_NAME");
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, OracleSchema owner, OracleTablePhysical forTable)
            throws SQLException
        {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT ").append(OracleUtils.getSysCatalogHint(owner.getDataSource())).append(" " +
                    "i.OWNER,i.INDEX_NAME,i.INDEX_TYPE,i.TABLE_OWNER,i.TABLE_NAME,i.UNIQUENESS,i.TABLESPACE_NAME,i.STATUS,i.NUM_ROWS,i.SAMPLE_SIZE,\n" +
                    "ic.COLUMN_NAME,ic.COLUMN_POSITION,ic.COLUMN_LENGTH,ic.DESCEND\n" +
                    "FROM SYS.ALL_INDEXES i, SYS.ALL_IND_COLUMNS ic\n" +
                    "WHERE ic.INDEX_OWNER=i.OWNER AND ic.INDEX_NAME=i.INDEX_NAME AND ");
            if (forTable == null) {
                sql.append("i.OWNER=?");
            } else {
                sql.append("i.TABLE_OWNER=? AND i.TABLE_NAME=?");
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

            OracleTableColumn tableColumn = columnName == null ? null : parent.getAttribute(session.getProgressMonitor(), columnName);
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
    static class ProceduresCache extends JDBCObjectLookupCache<OracleSchema, OracleProcedureStandalone> {

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull OracleSchema owner, @Nullable OracleProcedureStandalone object, @Nullable String objectName) throws SQLException {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT " + OracleUtils.getSysCatalogHint(owner.getDataSource()) + " * FROM SYS.ALL_OBJECTS " +
                    "WHERE OBJECT_TYPE IN ('PROCEDURE','FUNCTION') " +
                    "AND OWNER=? " +
                    (object == null && objectName == null ? "" : "AND OBJECT_NAME=? ") +
                    "ORDER BY OBJECT_NAME");
            dbStat.setString(1, owner.getName());
            if (object != null || objectName != null) dbStat.setString(2, object != null ? object.getName() : objectName);
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
                "SELECT s.*,O.OBJECT_TYPE \n" +
                "FROM ALL_SYNONYMS S, ALL_OBJECTS O\n" +
                "WHERE S.OWNER=? AND O.OBJECT_TYPE NOT IN ('JAVA CLASS','PACKAGE BODY')\n" +
                "AND O.OWNER=S.TABLE_OWNER AND O.OBJECT_NAME=S.TABLE_NAME\n" +
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

    static class TriggerCache extends JDBCObjectCache<OracleSchema, OracleSchemaTrigger> {

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OracleSchema schema) throws SQLException
        {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT *\n" +
                "FROM " + OracleUtils.getAdminAllViewPrefix(schema.getDataSource()) + "TRIGGERS WHERE OWNER=? AND TRIM(BASE_OBJECT_TYPE) IN ('DATABASE','SCHEMA')\n" +
                "ORDER BY TRIGGER_NAME");
            dbStat.setString(1, schema.getName());
            return dbStat;
        }

        @Override
        protected OracleSchemaTrigger fetchObject(@NotNull JDBCSession session, @NotNull OracleSchema oracleSchema, @NotNull JDBCResultSet resultSet) throws SQLException, DBException
        {
            return new OracleSchemaTrigger(oracleSchema, resultSet);
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

    static class SchedulerJobCache extends JDBCObjectCache<OracleSchema, OracleSchedulerJob> {

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OracleSchema owner)
                throws SQLException
        {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT * FROM SYS.ALL_SCHEDULER_JOBS WHERE OWNER=? ORDER BY JOB_NAME");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected OracleSchedulerJob fetchObject(@NotNull JDBCSession session, @NotNull OracleSchema owner, @NotNull JDBCResultSet dbResult)
                throws SQLException, DBException
        {
            return new OracleSchedulerJob(owner, dbResult);
        }

    }

    static class SchedulerProgramCache extends JDBCObjectCache<OracleSchema, OracleSchedulerProgram> {

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull OracleSchema owner)
                throws SQLException
        {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT * FROM SYS.ALL_SCHEDULER_PROGRAMS WHERE OWNER=? ORDER BY PROGRAM_NAME");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected OracleSchedulerProgram fetchObject(@NotNull JDBCSession session, @NotNull OracleSchema owner, @NotNull JDBCResultSet dbResult)
                throws SQLException, DBException
        {
            return new OracleSchedulerProgram(owner, dbResult);
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

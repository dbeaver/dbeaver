/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mssql.SQLServerConstants;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.DBCException;
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
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
* SQL Server schema
*/
public class SQLServerSchema implements DBSSchema, DBPSaveableObject, DBPQualifiedObject, DBPRefreshableObject, DBPSystemObject, SQLServerObject, DBSObjectWithScript, DBPObjectStatisticsCollector {

    private static final Log log = Log.getLog(SQLServerSchema.class);

    private final SQLServerDatabase database;
    private boolean persisted;
    private final long schemaId;
    private String name;
    private String description;
    private TableCache tableCache = new TableCache();
    private IndexCache indexCache = new IndexCache(tableCache);
    private UniqueConstraintCache uniqueConstraintCache = new UniqueConstraintCache(tableCache);
    private ForeignKeyCache foreignKeyCache = new ForeignKeyCache();
    private SequenceCache sequenceCache = new SequenceCache();
    private SynonymCache synonymCache = new SynonymCache();
    private ProcedureCache procedureCache = new ProcedureCache();
    private TriggerCache triggerCache = new TriggerCache();
    private volatile boolean hasTableStatistics;

    SQLServerSchema(SQLServerDatabase database, JDBCResultSet resultSet) {
        this.database = database;
        this.name = JDBCUtils.safeGetString(resultSet, "name");
        if (getDataSource().isServerVersionAtLeast(SQLServerConstants.SQL_SERVER_2005_VERSION_MAJOR ,0)) {
            this.schemaId = JDBCUtils.safeGetLong(resultSet, "schema_id");
        } else {
            this.schemaId = JDBCUtils.safeGetLong(resultSet, "uid");
        }
        this.description = JDBCUtils.safeGetString(resultSet, "description");

        this.persisted = true;
    }

    public TableCache getTableCache() {
        return tableCache;
    }

    public IndexCache getIndexCache() {
        return indexCache;
    }

    public UniqueConstraintCache getUniqueConstraintCache() {
        return uniqueConstraintCache;
    }

    public ForeignKeyCache getForeignKeyCache() {
        return foreignKeyCache;
    }

    public ProcedureCache getProcedureCache() {
        return procedureCache;
    }

    public TriggerCache getTriggerCache() {
        return triggerCache;
    }

    @NotNull
    @Override
    public SQLServerDataSource getDataSource() {
        return database.getDataSource();
    }

    @Property(viewable = false, editable = true, order = 10)
    public SQLServerDatabase getDatabase() {
        return database;
    }

    @Override
    @Property(viewable = true, editable = true, order = 1)
    public String getName() {
        return name;
    }

    @Property(viewable = false, editable = false, order = 5)
    public long getObjectId() {
        return schemaId;
    }

    @Override
    @Property(viewable = true, editable = true, updatable = true, multiline = true, order = 100)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public DBSObject getParentObject() {
        return database;
    }

    @Override
    public boolean isPersisted() {
        return this.persisted;
    }

    @Override
    public void setPersisted(boolean persisted) {
        this.persisted = persisted;
    }

    @Override
    public boolean isSystem() {
        return
            name.equalsIgnoreCase(SQLServerConstants.SQL_SERVER_SYSTEM_SCHEMA) ||
            name.equalsIgnoreCase(SQLServerConstants.INFORMATION_SCHEMA_SCHEMA);
    }

    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        if (!SQLServerUtils.supportsCrossDatabaseQueries(getDataSource())) {
            return DBUtils.getQuotedIdentifier(this);
        }
        return DBUtils.getFullQualifiedName(getDataSource(), getDatabase(), this);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        tableCache.clearCache();
        indexCache.clearCache();
        uniqueConstraintCache.clearCache();
        foreignKeyCache.clearCache();
        sequenceCache.clearCache();
        synonymCache.clearCache();
        procedureCache.clearCache();
        database.refreshDataTypes();
        hasTableStatistics = false;

        return this;
    }

    //////////////////////////////////////////////////
    // Data types

    @Association
    public List<SQLServerDataType> getDataTypes(DBRProgressMonitor monitor) throws DBException {
        if (SQLServerConstants.SQL_SERVER_SYSTEM_SCHEMA.equals(getName())) {
            return getDataSource().getLocalDataTypes();
        }
        List<SQLServerDataType> result = new ArrayList<>();
        for (SQLServerDataType dt : database.getDataTypes(monitor)) {
            if (dt.getSchemaId() == getObjectId()) {
                result.add(dt);
            }
        }
        return result;
    }

    //////////////////////////////////////////////////
    // Tables

    @Association
    public Collection<SQLServerTable> getTables(DBRProgressMonitor monitor) throws DBException {
        return tableCache.getTypedObjects(monitor, this, SQLServerTable.class);
    }

    public SQLServerTable getTable(DBRProgressMonitor monitor, long tableId) throws DBException {
        for (SQLServerTableBase table : tableCache.getAllObjects(monitor, this)) {
            if (table.getObjectId() == tableId && table instanceof SQLServerTable) {
                return (SQLServerTable) table;
            }
        }
        log.debug("Table '" + tableId + "' not found in schema " + getName());
        return null;
    }

    public SQLServerTableBase getTable(DBRProgressMonitor monitor, String name) throws DBException {
        return tableCache.getObject(monitor, this, name);
    }


    @Association
    public Collection<SQLServerView> getViews(DBRProgressMonitor monitor) throws DBException {
        return tableCache.getTypedObjects(monitor, this, SQLServerView.class);
    }

    @Association
    public Collection<SQLServerTableType> getTablesType(DBRProgressMonitor monitor) throws DBException {
        return tableCache.getTypedObjects(monitor, this, SQLServerTableType.class);
    }

    public SQLServerTableType getTableType(DBRProgressMonitor monitor, long tableId) throws DBException {
        for (SQLServerTableBase table : tableCache.getAllObjects(monitor, this)) {
            if (table.getObjectId() == tableId && table instanceof SQLServerTableType) {
                return (SQLServerTableType) table;
            }
        }
        log.debug("Table type '" + tableId + "' not found in schema " + getName());
        return null;
    }



    @Override
    public List<SQLServerObject> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException {
        List<SQLServerObject> result = new ArrayList<>();
        result.addAll(tableCache.getAllObjects(monitor, this));
        result.addAll(synonymCache.getAllObjects(monitor, this));
        return result;
    }

    @Override
    public SQLServerTableBase getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException {
        return tableCache.getObject(monitor, this, childName);
    }

    @NotNull
    @Override
    public Class<? extends DBSObject> getPrimaryChildType(@NotNull DBRProgressMonitor monitor) throws DBException {
        return SQLServerTable.class;
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException {
        if ((scope & STRUCT_ENTITIES) == STRUCT_ENTITIES) {
            tableCache.getAllObjects(monitor, this);
            synonymCache.getAllObjects(monitor, this);
        }
        if ((scope & STRUCT_ATTRIBUTES) == STRUCT_ATTRIBUTES) {
            tableCache.getChildren(monitor, this, null);
        }
        if ((scope & STRUCT_ASSOCIATIONS) == STRUCT_ASSOCIATIONS) {
            indexCache.getAllObjects(monitor, this);
            uniqueConstraintCache.getAllObjects(monitor, this);
            foreignKeyCache.getAllObjects(monitor, this);
        }
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        StringBuilder sql = new StringBuilder();
        sql.append("-- DROP SCHEMA ").append(DBUtils.getQuotedIdentifier(this)).append(";\n\n");
        sql.append("CREATE SCHEMA ").append(DBUtils.getQuotedIdentifier(this));
        sql.append(";\n");

        if (!monitor.isCanceled()) {
            Collection<SQLServerTableBase> tablesOrViews = tableCache.getAllObjects(monitor, this);
            DBStructUtils.generateTableListDDL(monitor, sql, tablesOrViews, options, false);
            monitor.done();
        }

        return sql.toString();
    }

    @Override
    public void setObjectDefinitionText(String source) {
        throw new IllegalStateException("Can't change schema definition");
    }

    @Override
    public boolean isStatisticsCollected() {
        return hasTableStatistics;
    }

    void resetTableStatistics() {
        this.hasTableStatistics = false;
    }

    @Override
    public void collectObjectStatistics(DBRProgressMonitor monitor, boolean totalSizeOnly, boolean forceRefresh) throws DBException {
        if (hasTableStatistics && !forceRefresh) {
            return;
        }
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Load table statistics")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT t.name, p.rows, SUM(a.total_pages) * 8 AS totalSize, SUM(a.used_pages) * 8 AS usedSize\n" +
                    "FROM " + SQLServerUtils.getSystemTableName(getDatabase(), "tables") + " t\n" +
                    "INNER JOIN " + SQLServerUtils.getSystemTableName(getDatabase(), "indexes") + " i ON t.OBJECT_ID = i.object_id\n" +
                    "INNER JOIN " + SQLServerUtils.getSystemTableName(getDatabase(), "partitions") + " p ON i.object_id = p.OBJECT_ID AND i.index_id = p.index_id\n" +
                    "INNER JOIN " + SQLServerUtils.getSystemTableName(getDatabase(), "allocation_units") + " a ON p.partition_id = a.container_id\n" +
                    "LEFT OUTER JOIN " + SQLServerUtils.getSystemTableName(getDatabase(), "schemas") +  " s ON t.schema_id = s.schema_id\n" +
                    "WHERE t.schema_id = ?\n" +
                    "GROUP BY t.name, p.rows"))
            {
                dbStat.setLong(1, getObjectId());
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String tableName = dbResult.getString("name");
                        SQLServerTableBase table = getTable(monitor, tableName);
                        if (table instanceof SQLServerTable) {
                            ((SQLServerTable)table).fetchTableStats(dbResult);
                        }
                    }
                }
                for (SQLServerTableBase table : tableCache.getCachedObjects()) {
                    if (table instanceof SQLServerTable && !((SQLServerTable)table).hasStatistics()) {
                        ((SQLServerTable)table).setDefaultTableStats();
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBCException("Error reading table statistics", e);
        } finally {
            hasTableStatistics = true;
        }
    }

    public static class TableCache extends JDBCStructLookupCache<SQLServerSchema, SQLServerTableBase, SQLServerTableColumn> {

        TableCache()
        {
            super("table_name");
            setListOrderComparator(DBUtils.nameComparator());
        }

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull SQLServerSchema owner, @Nullable SQLServerTableBase object, @Nullable String objectName) throws SQLException {
            StringBuilder sql = new StringBuilder();
            SQLServerDataSource dataSource = owner.getDataSource();
            sql.append("SELECT o.*,ep.value as description FROM ").append(SQLServerUtils.getSystemTableName(owner.getDatabase(), "all_objects")).append(" o");
            sql.append("\nLEFT OUTER JOIN ").append(SQLServerUtils.getExtendedPropsTableName(owner.getDatabase())).append(" ep ON ep.class=").append(SQLServerObjectClass.OBJECT_OR_COLUMN.getClassId()).append(" AND ep.major_id=o.object_id AND ep.minor_id=0 AND ep.name='").append(SQLServerConstants.PROP_MS_DESCRIPTION).append("'");
            sql.append("\nWHERE o.type IN ('U','S','V','TT') AND o.schema_id = ").append(owner.getObjectId());
            if (object != null || objectName != null) {
                sql.append(" AND o.name = ").append(SQLUtils.quoteString(session.getDataSource(), object != null ? object.getName() : objectName));
            } else {
                DBSObjectFilter tableFilters = dataSource.getContainer().getObjectFilter(SQLServerTableBase.class, owner, false);
                if (tableFilters != null && !tableFilters.isEmpty()) {
                    sql.append(" AND (");
                    boolean hasCond = false;
                    for (String incName : CommonUtils.safeCollection(tableFilters.getInclude())) {
                        if (hasCond) sql.append(" OR ");
                        hasCond = true;
                        sql.append(" o.name LIKE ").append(SQLUtils.quoteString(session.getDataSource(), incName));
                    }
                    hasCond = false;
                    for (String incName : CommonUtils.safeCollection(tableFilters.getExclude())) {
                        if (hasCond) sql.append(" OR ");
                        hasCond = true;
                        sql.append(" o.name NOT LIKE ").append(SQLUtils.quoteString(session.getDataSource(), incName));
                    }
                    sql.append(")");
                }
            }

            return session.prepareStatement(sql.toString());
        }

        @Override
        protected SQLServerTableBase fetchObject(@NotNull JDBCSession session, @NotNull SQLServerSchema owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            String type = JDBCUtils.safeGetStringTrimmed(dbResult, "type");
            if (SQLServerObjectType.U.name().equals(type) || SQLServerObjectType.S.name().equals(type)) {
                return new SQLServerTable(owner, dbResult);
            } else if (SQLServerObjectType.TT.name().equals(type)) {
                return new SQLServerTableType(owner, dbResult);
            } else {
                return new SQLServerView(owner, dbResult);
            }
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull SQLServerSchema owner, @Nullable SQLServerTableBase forTable)
            throws SQLException
        {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT c.*,t.name as table_name,t.schema_id");
            if (owner.getDataSource().supportsColumnProperty()) {
                sql.append(", COLUMNPROPERTY(c.object_id, c.name, 'charmaxlen') as char_max_length");
            }
            sql.append(", dc.definition as default_definition,ep.value as description\nFROM ")
                .append(SQLServerUtils.getSystemTableName(owner.getDatabase(), "all_columns")).append(" c")
                .append("\nJOIN ").append(SQLServerUtils.getSystemTableName(owner.getDatabase(), "all_objects")).append(" t ON t.object_id=c.object_id")
                .append("\nLEFT OUTER JOIN ").append(SQLServerUtils.getSystemTableName(owner.getDatabase(), "default_constraints")).append(" dc ON dc.parent_object_id=t.object_id AND dc.parent_column_id=c.column_id")
                .append("\nLEFT OUTER JOIN ").append(SQLServerUtils.getExtendedPropsTableName(owner.getDatabase())).append(" ep ON ep.class=").append(SQLServerObjectClass.OBJECT_OR_COLUMN.getClassId()).append(" AND ep.major_id=t.object_id AND ep.minor_id=c.column_id AND ep.name='").append(SQLServerConstants.PROP_MS_DESCRIPTION).append("'");
            sql.append("WHERE ");
            if (forTable != null) {
                sql.append("t.object_id=?");
            } else {
                sql.append("t.schema_id=?");
            }
            sql.append("\nORDER BY c.object_id,c.column_id");

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            if (forTable != null) {
                dbStat.setLong(1, forTable.getObjectId());
            } else {
                dbStat.setLong(1, owner.getObjectId());
            }
            return dbStat;
        }

        @Override
        protected SQLServerTableColumn fetchChild(@NotNull JDBCSession session, @NotNull SQLServerSchema owner, @NotNull SQLServerTableBase table, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new SQLServerTableColumn(session.getProgressMonitor(), table, dbResult);
        }

    }

    /////////////////////////////////////////////////////////
    // Indexes

    @Association
    public List<SQLServerTableIndex> getIndexes(DBRProgressMonitor monitor) throws DBException {
        List<SQLServerTableIndex> allIndexes = new ArrayList<>();
        for (SQLServerTableBase table : getTables(monitor)) {
            allIndexes.addAll(CommonUtils.safeCollection(table.getIndexes(monitor)));
        }
        return allIndexes;
    }

    /**
     * Index cache implementation
     */
    static class IndexCache extends JDBCCompositeCache<SQLServerSchema, SQLServerTableBase, SQLServerTableIndex, SQLServerTableIndexColumn> {
        IndexCache(TableCache tableCache)
        {
            super(tableCache, SQLServerTableBase.class, "table_name", "name");
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, SQLServerSchema owner, SQLServerTableBase forTable)
            throws SQLException
        {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT i.*,ic.index_column_id,ic.column_id,ic.key_ordinal,ic.is_descending_key,t.name as table_name\nFROM ")
                .append(SQLServerUtils.getSystemTableName(owner.getDatabase(), "indexes")).append(" i, ")
                .append(SQLServerUtils.getSystemTableName(owner.getDatabase(), "index_columns")).append(" ic, ")
                .append(SQLServerUtils.getSystemTableName(owner.getDatabase(), "all_objects")).append(" t").append("\n");
            sql.append("WHERE t.object_id = i.object_id AND ic.object_id=i.object_id AND ic.index_id=i.index_id");
            if (forTable != null) {
                sql.append(" AND t.object_id = ?");
            } else {
                sql.append(" AND t.schema_id = ?");
            }
            sql.append("\nORDER BY i.object_id,i.index_id,ic.index_column_id");

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            if (forTable != null) {
                dbStat.setLong(1, forTable.getObjectId());
            } else {
                dbStat.setLong(1, owner.getObjectId());
            }
            return dbStat;
        }

        @Nullable
        @Override
        protected SQLServerTableIndex fetchObject(JDBCSession session, SQLServerSchema owner, SQLServerTableBase parent, String indexName, JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            int indexTypeNum = JDBCUtils.safeGetInt(dbResult, "type");
            DBSIndexType indexType;
            switch (indexTypeNum) {
                case 0: indexType = SQLServerConstants.INDEX_TYPE_HEAP; break;
                case 1: indexType = DBSIndexType.CLUSTERED; break;
                case 2: indexType = SQLServerConstants.INDEX_TYPE_NON_CLUSTERED; break;
                default:
                    indexType = DBSIndexType.OTHER;
                    break;
            }
            return new SQLServerTableIndex(
                parent,
                indexName,
                indexType,
                dbResult);
        }

        @Nullable
        @Override
        protected SQLServerTableIndexColumn[] fetchObjectRow(
            JDBCSession session,
            SQLServerTableBase parent,
            SQLServerTableIndex object,
            JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            long indexColumnId = JDBCUtils.safeGetInt(dbResult, "index_column_id");
            long columnId = JDBCUtils.safeGetInt(dbResult, "column_id");
            SQLServerTableColumn tableColumn = columnId == 0 ? null : parent.getAttribute(session.getProgressMonitor(), columnId);
            int ordinal = JDBCUtils.safeGetInt(dbResult, "key_ordinal");
            boolean ascending = JDBCUtils.safeGetInt(dbResult, "is_descending_key") == 0;

            return new SQLServerTableIndexColumn[] {
                new SQLServerTableIndexColumn(object, indexColumnId, tableColumn, ordinal, ascending)
            };
        }

        @Override
        protected void cacheChildren(DBRProgressMonitor monitor, SQLServerTableIndex index, List<SQLServerTableIndexColumn> rows)
        {
            index.setColumns(rows);
        }
    }

    /**
     * Constraint cache implementation
     */
    static class UniqueConstraintCache extends JDBCCompositeCache<SQLServerSchema, SQLServerTableBase, SQLServerTableUniqueKey, SQLServerTableIndexColumn> {

        UniqueConstraintCache(TableCache tableCache) {
            super(tableCache, SQLServerTableBase.class, "table_name", "name");
        }

        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, SQLServerSchema schema, SQLServerTableBase forParent) throws SQLException {

            StringBuilder sql = new StringBuilder(500);
            sql.append(
                "SELECT kc.*,t.name as table_name FROM \n")
                .append(SQLServerUtils.getSystemTableName(schema.getDatabase(), "key_constraints")).append(" kc,")
                .append(SQLServerUtils.getSystemTableName(schema.getDatabase(), "all_objects")).append(" t\n");
            sql.append("WHERE kc.parent_object_id=t.object_id AND kc.schema_id=?");
            if (forParent != null) {
                sql.append(" AND kc.parent_object_id=?");
            }
            sql.append("\nORDER BY kc.name");

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            dbStat.setLong(1, schema.getObjectId());
            if (forParent != null) {
                dbStat.setLong(2, forParent.getObjectId());
            }
            return dbStat;
        }

        @Override
        protected SQLServerTableUniqueKey fetchObject(JDBCSession session, SQLServerSchema schema, SQLServerTableBase table, String childName, JDBCResultSet resultSet) throws SQLException, DBException {
            String name = JDBCUtils.safeGetString(resultSet, "name");
            String type = JDBCUtils.safeGetString(resultSet, "type");
            long indexId = JDBCUtils.safeGetLong(resultSet, "unique_index_id");
            boolean isSystemNamed = JDBCUtils.safeGetInt(resultSet, "is_system_named") != 0;
            SQLServerTableIndex index = table.getIndex(session.getProgressMonitor(), indexId);
            if (index == null) {
                return null;
            } else {
                DBSEntityConstraintType cType = "PK".equals(type) ? DBSEntityConstraintType.PRIMARY_KEY : DBSEntityConstraintType.UNIQUE_KEY;
                return new SQLServerTableUniqueKey(table, name, null, cType, index, true);
            }
        }

        @Override
        protected SQLServerTableIndexColumn[] fetchObjectRow(JDBCSession session, SQLServerTableBase table, SQLServerTableUniqueKey forObject, JDBCResultSet resultSet) throws SQLException, DBException {
            return new SQLServerTableIndexColumn[0];//forObject.getIndex().getAttributeReferences(session.getProgressMonitor()).toArray(new SQLServerTableIndexColumn[0]);
        }

        @Override
        protected void cacheChildren(DBRProgressMonitor monitor, SQLServerTableUniqueKey object, List<SQLServerTableIndexColumn> children) {

        }
    }

    class ForeignKeyCache extends JDBCCompositeCache<SQLServerSchema, SQLServerTableBase, SQLServerTableForeignKey, SQLServerTableForeignKeyColumn> {
        ForeignKeyCache()
        {
            super(tableCache, SQLServerTableBase.class, "table_name", "name");
        }

        @Override
        protected void loadObjects(DBRProgressMonitor monitor, SQLServerSchema schema, SQLServerTableBase forParent)
            throws DBException
        {
            // Cache schema indexes if no table specified
            if (forParent == null) {
                indexCache.getAllObjects(monitor, schema);
            }
            super.loadObjects(monitor, schema, forParent);
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, SQLServerSchema owner, SQLServerTableBase forTable)
            throws SQLException
        {
            StringBuilder sql = new StringBuilder(500);
            sql.append("SELECT t.name as table_name,fk.name,fk.key_index_id,fk.is_disabled,fk.delete_referential_action,fk.update_referential_action," +
                "fkc.*,tr.schema_id referenced_schema_id\nFROM ")
                .append(SQLServerUtils.getSystemTableName(owner.getDatabase(), "tables")).append(" t,")
                .append(SQLServerUtils.getSystemTableName(owner.getDatabase(), "foreign_keys")).append(" fk,")
                .append(SQLServerUtils.getSystemTableName(owner.getDatabase(), "foreign_key_columns")).append(" fkc, ")
                .append(SQLServerUtils.getSystemTableName(owner.getDatabase(), "tables")).append(" tr")
                .append("\nWHERE t.object_id = fk.parent_object_id AND fk.object_id=fkc.constraint_object_id AND tr.object_id=fk.referenced_object_id");
            if (forTable != null) {
                sql.append(" AND t.object_id=?");
            } else {
                sql.append(" AND t.schema_id=?");
            }
            sql.append("\nORDER BY fkc.constraint_object_id, fkc.constraint_column_id");

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            if (forTable != null) {
                dbStat.setLong(1, forTable.getObjectId());
            } else {
                dbStat.setLong(1, owner.getObjectId());
            }
            return dbStat;
        }

        @Nullable
        @Override
        protected SQLServerTableForeignKey fetchObject(JDBCSession session, SQLServerSchema owner, SQLServerTableBase parent, String indexName, JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            long refSchemaId = JDBCUtils.safeGetLong(dbResult, "referenced_schema_id");
            DBRProgressMonitor monitor = session.getProgressMonitor();
            SQLServerSchema refSchema = owner.getDatabase().getSchema(monitor, refSchemaId);
            if (refSchema == null) {
                log.debug("Ref schema " + refSchemaId + " not found");
                return null;
            }
            long refTableId = JDBCUtils.safeGetLong(dbResult, "referenced_object_id");
            SQLServerTable refTable = refSchema.getTable(monitor, refTableId);
            if (refTable == null) {
                log.debug("Ref table " + refTableId + " not found in schema " + refSchema.getName());
                return null;
            }
            long refIndexId = JDBCUtils.safeGetLong(dbResult, "key_index_id");
            SQLServerTableIndex index = refTable.getIndex(monitor, refIndexId);
            if (index == null) {
                log.debug("Ref index " + refIndexId + " not found in table " + refTable.getFullyQualifiedName(DBPEvaluationContext.UI));
                return null;
            }
            DBSEntityConstraint refConstraint = index;
            for (SQLServerTableUniqueKey refPK : refTable.getConstraints(monitor)) {
                if (refPK.getIndex() == index) {
                    refConstraint = refPK;
                    break;
                }
            }

            String fkName = JDBCUtils.safeGetString(dbResult, "name");
            DBSForeignKeyModifyRule deleteRule = SQLServerUtils.getForeignKeyModifyRule(JDBCUtils.safeGetInt(dbResult, "delete_referential_action"));
            DBSForeignKeyModifyRule updateRule = SQLServerUtils.getForeignKeyModifyRule(JDBCUtils.safeGetInt(dbResult, "update_referential_action"));
            return new SQLServerTableForeignKey((SQLServerTable) parent, fkName, null, refConstraint, deleteRule, updateRule, true);
        }

        @Nullable
        @Override
        protected SQLServerTableForeignKeyColumn[] fetchObjectRow(
            JDBCSession session,
            SQLServerTableBase parent,
            SQLServerTableForeignKey object,
            JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            DBRProgressMonitor monitor = session.getProgressMonitor();
            int columnId = JDBCUtils.safeGetInt(dbResult, "constraint_column_id");

            SQLServerTableBase fkTable = object.getParentObject();
            SQLServerTableBase refTable = object.getReferencedTable();
            SQLServerTableColumn fkColumn = fkTable.getAttribute(monitor, JDBCUtils.safeGetLong(dbResult, "parent_column_id"));
            SQLServerTableColumn refColumn = refTable.getAttribute(monitor, JDBCUtils.safeGetLong(dbResult, "referenced_column_id"));
            if (fkColumn == null || refColumn == null) {
                return null;
            }
            return new SQLServerTableForeignKeyColumn[] { new SQLServerTableForeignKeyColumn(
                object,
                fkColumn,
                columnId,
                refColumn) };
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void cacheChildren(DBRProgressMonitor monitor, SQLServerTableForeignKey foreignKey, List<SQLServerTableForeignKeyColumn> rows)
        {
            foreignKey.setColumns(rows);
        }

    }

    //////////////////////////////////////////////////
    // Sequences

    @Association
    public Collection<SQLServerSequence> getSequences(DBRProgressMonitor monitor) throws DBException {
        return sequenceCache.getAllObjects(monitor, this);
    }

    static class SequenceCache extends JDBCObjectCache<SQLServerSchema, SQLServerSequence> {

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull SQLServerSchema schema) throws SQLException {
            StringBuilder sql = new StringBuilder(500);
            sql.append(
                "SELECT * FROM \n")
                .append(SQLServerUtils.getSystemTableName(schema.getDatabase(), "sequences")).append("\n");
            sql.append("WHERE schema_id=?");
            sql.append("\nORDER BY name");

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            dbStat.setLong(1, schema.getObjectId());
            return dbStat;
        }

        @Override
        protected SQLServerSequence fetchObject(@NotNull JDBCSession session, @NotNull SQLServerSchema schema, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new SQLServerSequence(schema,
                JDBCUtils.safeGetLong(resultSet, "object_id"),
                JDBCUtils.safeGetString(resultSet, "name"),
                CommonUtils.toLong(JDBCUtils.safeGetObject(resultSet, "current_value")),
                CommonUtils.toLong(JDBCUtils.safeGetObject(resultSet, "minimum_value")),
                CommonUtils.toLong(JDBCUtils.safeGetObject(resultSet, "maximum_value")),
                CommonUtils.toLong(JDBCUtils.safeGetObject(resultSet, "increment")),
                true);
        }

    }

    //////////////////////////////////////////////////
    // Synonyms

    @Association
    public Collection<SQLServerSynonym> getSynonyms(DBRProgressMonitor monitor) throws DBException {
        return synonymCache.getAllObjects(monitor, this);
    }

    public SQLServerSynonym getSynonym(DBRProgressMonitor monitor, String name) throws DBException {
        return synonymCache.getObject(monitor, this, name);
    }

    static class SynonymCache extends JDBCObjectCache<SQLServerSchema, SQLServerSynonym> {

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull SQLServerSchema schema) throws SQLException {
            StringBuilder sql = new StringBuilder(500);
            sql.append("SELECT * FROM \n")
                .append(SQLServerUtils.getSystemTableName(schema.getDatabase(), "synonyms")).append("\n");
            sql.append("WHERE schema_id=?");
            sql.append("\nORDER BY name");

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            dbStat.setLong(1, schema.getObjectId());
            return dbStat;
        }

        @Override
        protected SQLServerSynonym fetchObject(@NotNull JDBCSession session, @NotNull SQLServerSchema schema, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new SQLServerSynonym(schema,
                JDBCUtils.safeGetLong(resultSet, "object_id"),
                JDBCUtils.safeGetString(resultSet, "name"),
                JDBCUtils.safeGetString(resultSet, "base_object_name"),
                true);
        }

    }

    //////////////////////////////////////////////////
    // Procedures

    @Association
    public Collection<SQLServerProcedure> getProcedures(DBRProgressMonitor monitor) throws DBException {
        return procedureCache.getAllObjects(monitor, this);
    }

    public SQLServerProcedure getProcedure(DBRProgressMonitor monitor, String name) throws DBException {
        return procedureCache.getObject(monitor, this, name);
    }

    static class ProcedureCache extends JDBCStructLookupCache<SQLServerSchema, SQLServerProcedure, SQLServerProcedureParameter> {

        public ProcedureCache() {
            super("proc_name");
        }

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(JDBCSession session, SQLServerSchema schema, SQLServerProcedure object, String objectName) throws SQLException {
            String sql = "SELECT p.*,ep.value as description" +
                "\nFROM " + SQLServerUtils.getSystemTableName(schema.getDatabase(), "all_objects") + " p" +
                "\nLEFT OUTER JOIN " + SQLServerUtils.getExtendedPropsTableName(schema.getDatabase()) + " ep ON ep.class=" + SQLServerObjectClass.OBJECT_OR_COLUMN.getClassId() + " AND ep.major_id=p.object_id AND ep.minor_id=0 AND ep.name='" + SQLServerConstants.PROP_MS_DESCRIPTION + "'" +
                "\nWHERE p.type IN ('P','PC','X','TF','FN','IF') AND p.schema_id=?" +
                (object != null || objectName != null ? " AND p.name=?" : "") +
                "\nORDER BY p.name";

            JDBCPreparedStatement dbStat = session.prepareStatement(sql);
            dbStat.setLong(1, schema.getObjectId());
            if (object != null || objectName != null) {
                dbStat.setString(2, object != null ? object.getName() : objectName);
            }
            return dbStat;
        }
        @Override
        protected SQLServerProcedure fetchObject(@NotNull JDBCSession session, @NotNull SQLServerSchema schema, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new SQLServerProcedure(schema, resultSet);
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull SQLServerSchema schema, SQLServerProcedure forObject) throws SQLException {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT p.name as proc_name,pp.* FROM \n")
                .append(SQLServerUtils.getSystemTableName(schema.getDatabase(), "all_objects")).append(" p, ")
                .append(SQLServerUtils.getSystemTableName(schema.getDatabase(), "all_parameters")).append(" pp\n")
                .append("\nWHERE p.type IN ('P','PC','X','TF','FN','IF') AND p.object_id = pp.object_id AND ");
            if (forObject == null) {
                sql.append("p.schema_id = ?");
            } else {
                sql.append("p.object_id = ?");
            }
            sql.append("\nORDER BY pp.object_id, pp.parameter_id");

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            if (forObject == null) {
                dbStat.setLong(1, schema.getObjectId());
            } else {
                dbStat.setLong(1, forObject.getObjectId());
            }
            return dbStat;
        }

        @Override
        protected SQLServerProcedureParameter fetchChild(@NotNull JDBCSession session, @NotNull SQLServerSchema schema, @NotNull SQLServerProcedure parent, @NotNull JDBCResultSet dbResult) throws SQLException, DBException {
            return new SQLServerProcedureParameter(session.getProgressMonitor(), parent, dbResult);
        }
    }

    //////////////////////////////////////////////////
    // Triggers

    @Association
    public Collection<SQLServerTableTrigger> getTriggers(DBRProgressMonitor monitor) throws DBException {
        return triggerCache.getAllObjects(monitor, this);
    }

    class TriggerCache extends JDBCObjectLookupCache<SQLServerSchema, SQLServerTableTrigger> {

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(JDBCSession session, SQLServerSchema schema, SQLServerTableTrigger object, String objectName) throws SQLException {
            StringBuilder sql = new StringBuilder(500);
            sql.append(
                "SELECT t.* FROM \n")
                .append(SQLServerUtils.getSystemTableName(schema.getDatabase(), "triggers")).append(" t,")
                .append(SQLServerUtils.getSystemTableName(schema.getDatabase(), "all_objects")).append(" o")
                .append("\n");
            sql.append("WHERE o.object_id=t.object_id AND o.schema_id=?");
            if (object != null || objectName != null) {
                sql.append(" AND t.name=?");
            }
            sql.append("\nORDER BY t.name");

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            dbStat.setLong(1, schema.getObjectId());
            if (object != null || objectName != null) {
                dbStat.setString(2, object != null ? object.getName() : objectName);
            }
            return dbStat;
        }

        @Override
        protected SQLServerTableTrigger fetchObject(@NotNull JDBCSession session, @NotNull SQLServerSchema schema, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            long tableId = JDBCUtils.safeGetLong(resultSet, "parent_id");
            SQLServerTable table = getTable(session.getProgressMonitor(), tableId);
            if (table == null) {
                log.debug("Trigger owner " + tableId + " not found");
                return null;
            }
            return new SQLServerTableTrigger(table, resultSet);
        }

    }

    @Override
    public String toString() {
        return getFullyQualifiedName(DBPEvaluationContext.UI);
    }
}

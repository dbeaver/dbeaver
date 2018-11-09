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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mssql.SQLServerConstants;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructLookupCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
* SQL Server schema
*/
public class SQLServerSchema implements DBSSchema, DBPSaveableObject, DBPRefreshableObject, DBPSystemObject, SQLServerObject {

    private final SQLServerDatabase database;
    private boolean persisted;
    private final long schemaId;
    private String name;
    private TableCache tableCache = new TableCache();
    private IndexCache indexCache = new IndexCache(tableCache);
    private ConstraintCache constraintCache = new ConstraintCache(tableCache);

    SQLServerSchema(SQLServerDatabase database, JDBCResultSet resultSet) {
        this.database = database;
        this.name = JDBCUtils.safeGetString(resultSet, "name");
        if (getDataSource().isServerVersionAtLeast(SQLServerConstants.SQL_SERVER_2005_VERSION_MAJOR ,0)) {
            this.schemaId = JDBCUtils.safeGetLong(resultSet, "schema_id");
        } else {
            this.schemaId = JDBCUtils.safeGetLong(resultSet, "uid");
        }

        this.persisted = true;
    }

    TableCache getTableCache() {
        return tableCache;
    }

    IndexCache getIndexCache() {
        return indexCache;
    }

    ConstraintCache getConstraintCache() {
        return constraintCache;
    }

    @Override
    public SQLServerDataSource getDataSource() {
        return database.getDataSource();
    }

    @Property(viewable = true, editable = true, order = 10)
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
    public String getDescription() {
        return null;
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
        return name.equals("msdb");
    }

    @Override
    public DBSObject refreshObject(DBRProgressMonitor monitor) throws DBException {
        return this;
    }

    //////////////////////////////////////////////////
    // Tables

    @Association
    public Collection<SQLServerTable> getTables(DBRProgressMonitor monitor) throws DBException {
        return tableCache.getAllObjects(monitor, this);
    }

    @Override
    public Collection<SQLServerTable> getChildren(DBRProgressMonitor monitor) throws DBException {
        return tableCache.getAllObjects(monitor, this);
    }

    @Override
    public SQLServerTable getChild(DBRProgressMonitor monitor, String childName) throws DBException {
        return tableCache.getObject(monitor, this, childName);
    }

    @Override
    public Class<? extends DBSObject> getChildType(DBRProgressMonitor monitor) throws DBException {
        return SQLServerTable.class;
    }

    @Override
    public void cacheStructure(DBRProgressMonitor monitor, int scope) throws DBException {
        if ((scope & STRUCT_ENTITIES) == STRUCT_ENTITIES) {
            tableCache.getAllObjects(monitor, this);
        }
        if ((scope & STRUCT_ATTRIBUTES) == STRUCT_ATTRIBUTES) {
            tableCache.getChildren(monitor, this, null);
        }
    }

    public static class TableCache extends JDBCStructLookupCache<SQLServerSchema, SQLServerTable, SQLServerTableColumn> {

        TableCache()
        {
            super("name");
            setListOrderComparator(DBUtils.nameComparator());
        }

        @NotNull
        @Override
        public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull SQLServerSchema owner, @Nullable SQLServerTable object, @Nullable String objectName) throws SQLException {
            StringBuilder sql = new StringBuilder();
            SQLServerDataSource dataSource = owner.getDataSource();
            sql.append("SELECT * FROM ").append(SQLServerUtils.getSystemTableName(owner.getDatabase(), "tables")).append("\n");
            sql.append("WHERE schema_id = ").append(owner.getObjectId());
            if (object != null || objectName != null) {
                sql.append(" AND name = ").append(SQLUtils.quoteString(session.getDataSource(), object != null ? object.getName() : objectName));
            } else {
                DBSObjectFilter tableFilters = dataSource.getContainer().getObjectFilter(SQLServerTable.class, owner, false);
                if (tableFilters != null && !tableFilters.isEmpty()) {
                    sql.append(" AND (");
                    boolean hasCond = false;
                    for (String incName : CommonUtils.safeCollection(tableFilters.getInclude())) {
                        if (hasCond) sql.append(" OR ");
                        hasCond = true;
                        sql.append(" name LIKE ").append(SQLUtils.quoteString(session.getDataSource(), incName));
                    }
                    hasCond = false;
                    for (String incName : CommonUtils.safeCollection(tableFilters.getExclude())) {
                        if (hasCond) sql.append(" OR ");
                        hasCond = true;
                        sql.append(" name NOT LIKE ").append(SQLUtils.quoteString(session.getDataSource(), incName));
                    }
                    sql.append(")");
                }
            }

            return session.prepareStatement(sql.toString());
        }

        @Override
        protected SQLServerTable fetchObject(@NotNull JDBCSession session, @NotNull SQLServerSchema owner, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new SQLServerTable(owner, dbResult);
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull SQLServerSchema owner, @Nullable SQLServerTable forTable)
            throws SQLException
        {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT * FROM ").append(SQLServerUtils.getSystemTableName(owner.getDatabase(), "columns")).append("\n");
            sql.append("WHERE object_id = ?\n");
            sql.append("ORDER BY column_id");

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            dbStat.setLong(1, forTable.getObjectId());
            return dbStat;
        }

        @Override
        protected SQLServerTableColumn fetchChild(@NotNull JDBCSession session, @NotNull SQLServerSchema owner, @NotNull SQLServerTable table, @NotNull JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            return new SQLServerTableColumn(table, dbResult);
        }

    }

    /////////////////////////////////////////////////////////
    // Indexes

    @Association
    public List<SQLServerTableIndex> getIndexes(DBRProgressMonitor monitor) throws DBException {
        List<SQLServerTableIndex> allIndexes = new ArrayList<>();
        for (SQLServerTable table : getTables(monitor)) {
            allIndexes.addAll(CommonUtils.safeCollection(table.getIndexes(monitor)));
        }
        return allIndexes;
    }

    /**
     * Index cache implementation
     */
    static class IndexCache extends JDBCCompositeCache<SQLServerSchema, SQLServerTable, SQLServerTableIndex, SQLServerTableIndexColumn> {
        IndexCache(TableCache tableCache)
        {
            super(tableCache, SQLServerTable.class, "object_id", "name");
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, SQLServerSchema owner, SQLServerTable forTable)
            throws SQLException
        {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT i.*,ic.index_column_id,ic.column_id,ic.key_ordinal,ic.is_descending_key\n" +
                "FROM ").append(SQLServerUtils.getSystemTableName(owner.getDatabase(), "indexes")).append(" i, ")
                .append(SQLServerUtils.getSystemTableName(owner.getDatabase(), "index_columns")).append(" ic").append("\n");
            sql.append("WHERE i.object_id = ? AND ic.object_id=i.object_id AND ic.index_id=i.index_id\n");
            sql.append("ORDER BY i.object_id,i.index_id,ic.index_column_id");

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            dbStat.setLong(1, forTable.getObjectId());
            return dbStat;
        }

        @Nullable
        @Override
        protected SQLServerTableIndex fetchObject(JDBCSession session, SQLServerSchema owner, SQLServerTable parent, String indexName, JDBCResultSet dbResult)
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
            SQLServerTable parent, SQLServerTableIndex object, JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            long indexColumnId = JDBCUtils.safeGetInt(dbResult, "index_column_id");
            long columnId = JDBCUtils.safeGetInt(dbResult, "column_id");
            SQLServerTableColumn tableColumn = columnId == 0 ? null : parent.getAttribute(session.getProgressMonitor(), columnId);
            int ordinal = JDBCUtils.safeGetInt(dbResult, "key_ordinal");
            boolean ascending = JDBCUtils.safeGetInt(dbResult, "is_descending_key") == 0;

            return new SQLServerTableIndexColumn[] {
                new SQLServerTableIndexColumn(object, indexColumnId, tableColumn, ordinal, ascending, false, null)
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
    static class ConstraintCache extends JDBCCompositeCache<SQLServerSchema, SQLServerTable, SQLServerTableConstraint, SQLServerTableConstraintColumn> {
        ConstraintCache(TableCache tableCache)
        {
            super(tableCache, SQLServerTable.class, "parent_object_id", "name");
        }

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, SQLServerSchema owner, SQLServerTable forTable)
            throws SQLException
        {
            StringBuilder sql = new StringBuilder(500);
            sql.append(
                "SELECT kc.CONSTRAINT_NAME,kc.TABLE_NAME,kc.COLUMN_NAME,kc.ORDINAL_POSITION\n" +
                    "FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE kc WHERE kc.TABLE_SCHEMA=? AND kc.REFERENCED_TABLE_NAME IS NULL");
            if (forTable != null) {
                sql.append(" AND kc.TABLE_NAME=?");
            }
            sql.append("\nORDER BY kc.CONSTRAINT_NAME,kc.ORDINAL_POSITION");

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            dbStat.setString(1, owner.getName());
            if (forTable != null) {
                dbStat.setString(2, forTable.getName());
            }
            return dbStat;
        }

        @Nullable
        @Override
        protected SQLServerTableConstraint fetchObject(JDBCSession session, SQLServerSchema owner, SQLServerTable parent, String constraintName, JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            if (constraintName.equals("PRIMARY")) {
                return new SQLServerTableConstraint(
                    parent, constraintName, null, DBSEntityConstraintType.PRIMARY_KEY, true);
            } else {
                return new SQLServerTableConstraint(
                    parent, constraintName, null, DBSEntityConstraintType.UNIQUE_KEY, true);
            }
        }

        @Nullable
        @Override
        protected SQLServerTableConstraintColumn[] fetchObjectRow(
            JDBCSession session,
            SQLServerTable parent, SQLServerTableConstraint object, JDBCResultSet dbResult)
            throws SQLException, DBException
        {
            String columnName = JDBCUtils.safeGetString(dbResult, "xxx");
            SQLServerTableColumn column = parent.getAttribute(session.getProgressMonitor(), columnName);
            if (column == null) {
                log.warn("Column '" + columnName + "' not found in table '" + parent.getFullyQualifiedName(DBPEvaluationContext.DDL) + "'");
                return null;
            }
            int ordinalPosition = JDBCUtils.safeGetInt(dbResult, "position");

            return new SQLServerTableConstraintColumn[] { new SQLServerTableConstraintColumn(
                object,
                column,
                ordinalPosition) };
        }

        @Override
        protected void cacheChildren(DBRProgressMonitor monitor, SQLServerTableConstraint constraint, List<SQLServerTableConstraintColumn> rows)
        {
            constraint.setColumns(rows);
        }
    }

}

/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.DBPSystemObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.utils.CommonUtils;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

/**
 * PostgreSchema
 */
public class PostgreSchema implements DBSSchema, DBPSaveableObject, DBPRefreshableObject, DBPSystemObject, DBSProcedureContainer {

    private PostgreDatabase database;
    private String name;
    private String ownerName;
    private PostgreCharset defaultCharset;
    private String sqlPath;
    private boolean persisted;

    final TableCache tableCache = new TableCache();
    final ProceduresCache proceduresCache = new ProceduresCache();
    final TriggerCache triggerCache = new TriggerCache();
    final ConstraintCache constraintCache = new ConstraintCache();
    final IndexCache indexCache = new IndexCache();

    public PostgreSchema(PostgreDatabase database, String name, ResultSet dbResult)
        throws SQLException
    {
        this.database = database;
        this.name = name;

        this.loadInfo(dbResult);
    }

    private void loadInfo(ResultSet dbResult)
        throws SQLException
    {
        this.ownerName = JDBCUtils.safeGetString(dbResult, "schema_owner");
        this.defaultCharset = null;
        this.sqlPath = JDBCUtils.safeGetString(dbResult, "sql_path");
        this.persisted = true;
    }

    @NotNull
    @Property(viewable = true, order = 1)
    public PostgreDatabase getDatabase() {
        return database;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 2)
    public String getName()
    {
        return name;
    }

    @Property(viewable = true, order = 4)
    public String getOwnerName() {
        return ownerName;
    }

    @NotNull
    public PostgreCharset getDefaultCharset() {
        return defaultCharset;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return null;
    }

    @Override
    public PostgreDatabase getParentObject()
    {
        return database;
    }

    @NotNull
    @Override
    public PostgreDataSource getDataSource() {
        return database.getDataSource();
    }

    @Override
    public boolean isPersisted() {
        return persisted;
    }

    @Override
    public void setPersisted(boolean persisted) {
        this.persisted = persisted;
    }

    @Association
    public Collection<PostgreTableIndex> getIndexes(DBRProgressMonitor monitor)
        throws DBException
    {
        return indexCache.getObjects(monitor, this, null);
    }

    @Association
    public Collection<PostgreTable> getTables(DBRProgressMonitor monitor)
        throws DBException
    {
        return tableCache.getTypedObjects(monitor, this, PostgreTable.class);
    }

    public PostgreTable getTable(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return tableCache.getObject(monitor, this, name, PostgreTable.class);
    }

    @Association
    public Collection<PostgreView> getViews(DBRProgressMonitor monitor)
        throws DBException
    {
        return tableCache.getTypedObjects(monitor, this, PostgreView.class);
    }

    @Association
    public Collection<PostgreProcedure> getProcedures(DBRProgressMonitor monitor)
        throws DBException
    {
        return proceduresCache.getAllObjects(monitor, this);
    }

    public PostgreProcedure getProcedure(DBRProgressMonitor monitor, String procName)
        throws DBException
    {
        return proceduresCache.getObject(monitor, this, procName);
    }

    @Association
    public Collection<PostgreTrigger> getTriggers(DBRProgressMonitor monitor)
        throws DBException
    {
        return triggerCache.getAllObjects(monitor, this);
    }

    public PostgreTrigger getTrigger(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return triggerCache.getObject(monitor, this, name);
    }

    @Override
    public Collection<PostgreTableBase> getChildren(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return tableCache.getAllObjects(monitor, this);
    }

    @Override
    public PostgreTableBase getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName)
        throws DBException
    {
        return tableCache.getObject(monitor, this, childName);
    }

    @Override
    public Class<? extends DBSEntity> getChildType(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        return PostgreTable.class;
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
            monitor.subTask("Cache table constraints");
            constraintCache.getAllObjects(monitor, this);
        }
    }

    @Override
    public synchronized boolean refreshObject(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        tableCache.clearCache();
        indexCache.clearCache();
        constraintCache.clearCache();
        proceduresCache.clearCache();
        triggerCache.clearCache();
        return true;
    }

    @Override
    public boolean isSystem()
    {
        return PostgreConstants.INFO_SCHEMA_NAME.equalsIgnoreCase(getName()) || PostgreConstants.MYSQL_SCHEMA_NAME.equalsIgnoreCase(getName());
    }

    public class TableCache extends JDBCStructCache<PostgreSchema, PostgreTableBase, PostgreTableColumn> {

        protected TableCache()
        {
            super(JDBCConstants.TABLE_NAME);
        }

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreSchema owner)
            throws SQLException
        {
            return session.prepareStatement("SHOW FULL TABLES FROM " + DBUtils.getQuotedIdentifier(PostgreSchema.this));
        }

        @Override
        protected PostgreTableBase fetchObject(@NotNull JDBCSession session, @NotNull PostgreSchema owner, @NotNull ResultSet dbResult)
            throws SQLException, DBException
        {
            final String tableType = JDBCUtils.safeGetString(dbResult, PostgreConstants.COL_TABLE_TYPE);
            if (tableType.contains("VIEW")) {
                return new PostgreView(PostgreSchema.this, dbResult);
            } else {
                return new PostgreTable(PostgreSchema.this, dbResult);
            }
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull PostgreSchema owner, @Nullable PostgreTableBase forTable)
            throws SQLException
        {
            StringBuilder sql = new StringBuilder();
            sql
                .append("SELECT * FROM ").append(PostgreConstants.META_TABLE_COLUMNS)
                .append(" WHERE ").append(PostgreConstants.COL_TABLE_SCHEMA).append("=?");
            if (forTable != null) {
                sql.append(" AND ").append(PostgreConstants.COL_TABLE_NAME).append("=?");
            }
            sql.append(" ORDER BY ").append(PostgreConstants.COL_ORDINAL_POSITION);

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            dbStat.setString(1, PostgreSchema.this.getName());
            if (forTable != null) {
                dbStat.setString(2, forTable.getName());
            }
            return dbStat;
        }

        @Override
        protected PostgreTableColumn fetchChild(@NotNull JDBCSession session, @NotNull PostgreSchema owner, @NotNull PostgreTableBase table, @NotNull ResultSet dbResult)
            throws SQLException, DBException
        {
            return new PostgreTableColumn(table, dbResult);
        }
    }

    /**
     * Index cache implementation
     */
    class IndexCache extends JDBCCompositeCache<PostgreSchema, PostgreTable, PostgreTableIndex, PostgreTableIndexColumn> {
        protected IndexCache()
        {
            super(tableCache, PostgreTable.class, PostgreConstants.COL_TABLE_NAME, PostgreConstants.COL_INDEX_NAME);
        }

        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, PostgreSchema owner, PostgreTable forTable)
            throws SQLException
        {
            StringBuilder sql = new StringBuilder();
            sql
                .append("SELECT * FROM ").append(PostgreConstants.META_TABLE_STATISTICS)
                .append(" WHERE ").append(PostgreConstants.COL_TABLE_SCHEMA).append("=?");
            if (forTable != null) {
                sql.append(" AND ").append(PostgreConstants.COL_TABLE_NAME).append("=?");
            }
            sql.append(" ORDER BY ").append(PostgreConstants.COL_INDEX_NAME).append(",").append(PostgreConstants.COL_SEQ_IN_INDEX);

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            dbStat.setString(1, PostgreSchema.this.getName());
            if (forTable != null) {
                dbStat.setString(2, forTable.getName());
            }
            return dbStat;
        }

        @Override
        protected PostgreTableIndex fetchObject(JDBCSession session, PostgreSchema owner, PostgreTable parent, String indexName, ResultSet dbResult)
            throws SQLException, DBException
        {
            String indexTypeName = JDBCUtils.safeGetString(dbResult, PostgreConstants.COL_INDEX_TYPE);
            DBSIndexType indexType;
            if (PostgreConstants.INDEX_TYPE_BTREE.getId().equals(indexTypeName)) {
                indexType = PostgreConstants.INDEX_TYPE_BTREE;
            } else if (PostgreConstants.INDEX_TYPE_FULLTEXT.getId().equals(indexTypeName)) {
                indexType = PostgreConstants.INDEX_TYPE_FULLTEXT;
            } else if (PostgreConstants.INDEX_TYPE_HASH.getId().equals(indexTypeName)) {
                indexType = PostgreConstants.INDEX_TYPE_HASH;
            } else if (PostgreConstants.INDEX_TYPE_RTREE.getId().equals(indexTypeName)) {
                indexType = PostgreConstants.INDEX_TYPE_RTREE;
            } else {
                indexType = DBSIndexType.OTHER;
            }
            return new PostgreTableIndex(
                parent,
                indexName,
                indexType,
                dbResult);
        }

        @Override
        protected PostgreTableIndexColumn fetchObjectRow(
            JDBCSession session,
            PostgreTable parent, PostgreTableIndex object, ResultSet dbResult)
            throws SQLException, DBException
        {
            int ordinalPosition = JDBCUtils.safeGetInt(dbResult, PostgreConstants.COL_SEQ_IN_INDEX);
            String columnName = JDBCUtils.safeGetStringTrimmed(dbResult, PostgreConstants.COL_COLUMN_NAME);
            String ascOrDesc = JDBCUtils.safeGetStringTrimmed(dbResult, PostgreConstants.COL_COLLATION);
            boolean nullable = "YES".equals(JDBCUtils.safeGetStringTrimmed(dbResult, PostgreConstants.COL_NULLABLE));

            PostgreTableColumn tableColumn = parent.getAttribute(session.getProgressMonitor(), columnName);
            if (tableColumn == null) {
                log.debug("Column '" + columnName + "' not found in table '" + parent.getName() + "' for index '" + object.getName() + "'");
                return null;
            }

            return new PostgreTableIndexColumn(
                object,
                tableColumn,
                ordinalPosition,
                "A".equalsIgnoreCase(ascOrDesc),
                nullable);
        }

        @Override
        protected void cacheChildren(PostgreTableIndex index, List<PostgreTableIndexColumn> rows)
        {
            index.setColumns(rows);
        }
    }

    /**
     * Constraint cache implementation
     */
    class ConstraintCache extends JDBCCompositeCache<PostgreSchema, PostgreTable, PostgreTableConstraint, PostgreTableConstraintColumn> {
        protected ConstraintCache()
        {
            super(tableCache, PostgreTable.class, PostgreConstants.COL_TABLE_NAME, PostgreConstants.COL_CONSTRAINT_NAME);
        }

        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, PostgreSchema owner, PostgreTable forTable)
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
            dbStat.setString(1, PostgreSchema.this.getName());
            if (forTable != null) {
                dbStat.setString(2, forTable.getName());
            }
            return dbStat;
        }

        @Override
        protected PostgreTableConstraint fetchObject(JDBCSession session, PostgreSchema owner, PostgreTable parent, String constraintName, ResultSet dbResult)
            throws SQLException, DBException
        {
            if (constraintName.equals("PRIMARY")) {
                return new PostgreTableConstraint(
                    parent, constraintName, null, DBSEntityConstraintType.PRIMARY_KEY, true);
            } else {
                return new PostgreTableConstraint(
                    parent, constraintName, null, DBSEntityConstraintType.UNIQUE_KEY, true);
            }
        }

        @Override
        protected PostgreTableConstraintColumn fetchObjectRow(
            JDBCSession session,
            PostgreTable parent, PostgreTableConstraint object, ResultSet dbResult)
            throws SQLException, DBException
        {
            String columnName = JDBCUtils.safeGetString(dbResult, PostgreConstants.COL_COLUMN_NAME);
            PostgreTableColumn column = parent.getAttribute(session.getProgressMonitor(), columnName);
            if (column == null) {
                log.warn("Column '" + columnName + "' not found in table '" + parent.getFullQualifiedName() + "'");
                return null;
            }
            int ordinalPosition = JDBCUtils.safeGetInt(dbResult, PostgreConstants.COL_ORDINAL_POSITION);

            return new PostgreTableConstraintColumn(
                object,
                column,
                ordinalPosition);
        }

        @Override
        protected void cacheChildren(PostgreTableConstraint constraint, List<PostgreTableConstraintColumn> rows)
        {
            constraint.setColumns(rows);
        }
    }

    /**
     * Procedures cache implementation
     */
    class ProceduresCache extends JDBCStructCache<PostgreSchema, PostgreProcedure, PostgreProcedureParameter> {

        ProceduresCache()
        {
            super(JDBCConstants.PROCEDURE_NAME);
        }

        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreSchema owner)
            throws SQLException
        {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM " + PostgreConstants.META_TABLE_ROUTINES +
                    " WHERE " + PostgreConstants.COL_ROUTINE_SCHEMA + "=?" +
                    " ORDER BY " + PostgreConstants.COL_ROUTINE_NAME
            );
            dbStat.setString(1, getName());
            return dbStat;
        }

        @Override
        protected PostgreProcedure fetchObject(@NotNull JDBCSession session, @NotNull PostgreSchema owner, @NotNull ResultSet dbResult)
            throws SQLException, DBException
        {
            return new PostgreProcedure(PostgreSchema.this, dbResult);
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull PostgreSchema owner, @Nullable PostgreProcedure procedure)
            throws SQLException
        {
            // Load procedure columns through Postgre metadata
            // There is no metadata table about proc/func columns -
            // it should be parsed from SHOW CREATE PROCEDURE/FUNCTION query
            // Lets driver do it instead of me
            return session.getMetaData().getProcedureColumns(
                getName(),
                null,
                procedure.getName(),
                null).getSourceStatement();
        }

        @Override
        protected PostgreProcedureParameter fetchChild(@NotNull JDBCSession session, @NotNull PostgreSchema owner, @NotNull PostgreProcedure parent, @NotNull ResultSet dbResult)
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
            //int radix = JDBCUtils.safeGetInt(dbResult, JDBCConstants.RADIX);
            //DBSDataType dataType = getDataSourceContainer().getInfo().getSupportedDataType(typeName);
            DBSProcedureParameterKind parameterType;
            switch (columnTypeNum) {
                case DatabaseMetaData.procedureColumnIn: parameterType = DBSProcedureParameterKind.IN; break;
                case DatabaseMetaData.procedureColumnInOut: parameterType = DBSProcedureParameterKind.INOUT; break;
                case DatabaseMetaData.procedureColumnOut: parameterType = DBSProcedureParameterKind.OUT; break;
                case DatabaseMetaData.procedureColumnReturn: parameterType = DBSProcedureParameterKind.RETURN; break;
                case DatabaseMetaData.procedureColumnResult: parameterType = DBSProcedureParameterKind.RESULTSET; break;
                default: parameterType = DBSProcedureParameterKind.UNKNOWN; break;
            }
            if (CommonUtils.isEmpty(columnName) && parameterType == DBSProcedureParameterKind.RETURN) {
                columnName = "RETURN";
            }
            return new PostgreProcedureParameter(
                parent,
                columnName,
                typeName,
                valueType,
                position,
                columnSize,
                scale, precision, notNull,
                parameterType);
        }
    }

    class TriggerCache extends JDBCObjectCache<PostgreSchema, PostgreTrigger> {
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull PostgreSchema owner)
            throws SQLException
        {
            return session.prepareStatement(
                "SHOW FULL TRIGGERS FROM " + DBUtils.getQuotedIdentifier(PostgreSchema.this));
        }

        @Override
        protected PostgreTrigger fetchObject(@NotNull JDBCSession session, @NotNull PostgreSchema owner, @NotNull ResultSet dbResult)
            throws SQLException, DBException
        {
            String tableName = JDBCUtils.safeGetString(dbResult, "TABLE");
            PostgreTable triggerTable = CommonUtils.isEmpty(tableName) ? null : getTable(session.getProgressMonitor(), tableName);
            return new PostgreTrigger(PostgreSchema.this, triggerTable, dbResult);
        }

    }

}

/*
 * Copyright (C) 2010-2014 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.dbeaver.core.Log;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mssql.MSSQLConstants;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.DBPSystemObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
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
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterType;
import org.jkiss.utils.CommonUtils;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

/**
 * GenericCatalog
 */
public class MSSQLCatalog implements DBSCatalog, DBPSaveableObject, DBPRefreshableObject, DBPSystemObject
{
    static final Log log = Log.getLog(MSSQLCatalog.class);

    final TableCache tableCache = new TableCache();
    final ProceduresCache proceduresCache = new ProceduresCache();
    final TriggerCache triggerCache = new TriggerCache();
    final ConstraintCache constraintCache = new ConstraintCache();
    final IndexCache indexCache = new IndexCache();

    private MSSQLDataSource dataSource;
    private String name;
    private MSSQLCharset defaultCharset;
    private MSSQLCollation defaultCollation;
    private String sqlPath;
    private boolean persisted;

    public MSSQLCatalog(MSSQLDataSource dataSource, ResultSet dbResult)
    {
        tableCache.setCaseSensitive(false);
        this.dataSource = dataSource;
        if (dbResult != null) {
            this.name = JDBCUtils.safeGetString(dbResult, MSSQLConstants.COL_SCHEMA_NAME);
            defaultCharset = dataSource.getCharset(JDBCUtils.safeGetString(dbResult, MSSQLConstants.COL_DEFAULT_CHARACTER_SET_NAME));
            defaultCollation = dataSource.getCollation(JDBCUtils.safeGetString(dbResult, MSSQLConstants.COL_DEFAULT_COLLATION_NAME));
            sqlPath = JDBCUtils.safeGetString(dbResult, MSSQLConstants.COL_SQL_PATH);
            persisted = true;
        } else {
            persisted = false;
        }
    }

    @Override
    public DBSObject getParentObject()
    {
        return dataSource.getContainer();
    }

    @NotNull
    @Override
    public MSSQLDataSource getDataSource()
    {
        return dataSource;
    }

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

    @Override
    public boolean isPersisted()
    {
        return persisted;
    }

    @Override
    public void setPersisted(boolean persisted)
    {
        this.persisted = persisted;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return null;
    }

    @Property(viewable = true, order = 2)
    public MSSQLCharset getDefaultCharset()
    {
        return defaultCharset;
    }

    void setDefaultCharset(MSSQLCharset defaultCharset)
    {
        this.defaultCharset = defaultCharset;
    }

    @Property(viewable = true, order = 3)
    public MSSQLCollation getDefaultCollation()
    {
        return defaultCollation;
    }

    void setDefaultCollation(MSSQLCollation defaultCollation)
    {
        this.defaultCollation = defaultCollation;
    }

    @Property(viewable = true, order = 3)
    public String getSqlPath()
    {
        return sqlPath;
    }

    void setSqlPath(String sqlPath)
    {
        this.sqlPath = sqlPath;
    }

    public TableCache getTableCache()
    {
        return tableCache;
    }

    public ProceduresCache getProceduresCache()
    {
        return proceduresCache;
    }

    public TriggerCache getTriggerCache()
    {
        return triggerCache;
    }

    public ConstraintCache getConstraintCache()
    {
        return constraintCache;
    }

    public IndexCache getIndexCache()
    {
        return indexCache;
    }

    @Association
    public Collection<MSSQLTableIndex> getIndexes(DBRProgressMonitor monitor)
        throws DBException
    {
        return indexCache.getObjects(monitor, this, null);
    }

    @Association
    public Collection<MSSQLTable> getTables(DBRProgressMonitor monitor)
        throws DBException
    {
        return tableCache.getTypedObjects(monitor, this, MSSQLTable.class);
    }

    public MSSQLTable getTable(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return tableCache.getObject(monitor, this, name, MSSQLTable.class);
    }

    @Association
    public Collection<MSSQLView> getViews(DBRProgressMonitor monitor)
        throws DBException
    {
        return tableCache.getTypedObjects(monitor, this, MSSQLView.class);
    }

    @Association
    public Collection<MSSQLProcedure> getProcedures(DBRProgressMonitor monitor)
        throws DBException
    {
        return proceduresCache.getObjects(monitor, this);
    }

    public MSSQLProcedure getProcedure(DBRProgressMonitor monitor, String procName)
        throws DBException
    {
        return proceduresCache.getObject(monitor, this, procName);
    }

    @Association
    public Collection<MSSQLTrigger> getTriggers(DBRProgressMonitor monitor)
        throws DBException
    {
        return triggerCache.getObjects(monitor, this);
    }

    public MSSQLTrigger getTrigger(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return triggerCache.getObject(monitor, this, name);
    }

    @Override
    public Collection<MSSQLTableBase> getChildren(DBRProgressMonitor monitor)
        throws DBException
    {
        return tableCache.getObjects(monitor, this);
    }

    @Override
    public MSSQLTableBase getChild(DBRProgressMonitor monitor, String childName)
        throws DBException
    {
        return tableCache.getObject(monitor, this, childName);
    }

    @Override
    public Class<? extends DBSEntity> getChildType(DBRProgressMonitor monitor)
        throws DBException
    {
        return MSSQLTable.class;
    }

    @Override
    public synchronized void cacheStructure(DBRProgressMonitor monitor, int scope)
        throws DBException
    {
        monitor.subTask("Cache tables");
        tableCache.loadObjects(monitor, this);
        if ((scope & STRUCT_ATTRIBUTES) != 0) {
            monitor.subTask("Cache table columns");
            tableCache.loadChildren(monitor, this, null);
        }
        if ((scope & STRUCT_ASSOCIATIONS) != 0) {
            monitor.subTask("Cache table constraints");
            constraintCache.getObjects(monitor, this);
        }
    }

    @Override
    public synchronized boolean refreshObject(DBRProgressMonitor monitor)
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
        return MSSQLConstants.INFO_SCHEMA_NAME.equalsIgnoreCase(getName()) || MSSQLConstants.MYSQL_SCHEMA_NAME.equalsIgnoreCase(getName());
    }

    @Override
    public String toString()
    {
        return name + " [" + dataSource.getContainer().getName() + "]";
    }

    public class TableCache extends JDBCStructCache<MSSQLCatalog, MSSQLTableBase, MSSQLTableColumn> {
        
        protected TableCache()
        {
            super(JDBCConstants.TABLE_NAME);
        }

        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, MSSQLCatalog owner)
            throws SQLException
        {
            return session.prepareStatement("SHOW FULL TABLES FROM " + DBUtils.getQuotedIdentifier(MSSQLCatalog.this));
        }

        @Override
        protected MSSQLTableBase fetchObject(JDBCSession session, MSSQLCatalog owner, ResultSet dbResult)
            throws SQLException, DBException
        {
            final String tableType = JDBCUtils.safeGetString(dbResult, MSSQLConstants.COL_TABLE_TYPE);
            if (tableType.contains("VIEW")) {
                return new MSSQLView(MSSQLCatalog.this, dbResult);
            } else {
                return new MSSQLTable(MSSQLCatalog.this, dbResult);
            }
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(JDBCSession session, MSSQLCatalog owner, MSSQLTableBase forTable)
            throws SQLException
        {
            StringBuilder sql = new StringBuilder();
            sql
                .append("SELECT * FROM ").append(MSSQLConstants.META_TABLE_COLUMNS)
                .append(" WHERE ").append(MSSQLConstants.COL_TABLE_SCHEMA).append("=?");
            if (forTable != null) {
                sql.append(" AND ").append(MSSQLConstants.COL_TABLE_NAME).append("=?");
            }
            sql.append(" ORDER BY ").append(MSSQLConstants.COL_ORDINAL_POSITION);

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            dbStat.setString(1, MSSQLCatalog.this.getName());
            if (forTable != null) {
                dbStat.setString(2, forTable.getName());
            }
            return dbStat;
        }

        @Override
        protected MSSQLTableColumn fetchChild(JDBCSession session, MSSQLCatalog owner, MSSQLTableBase table, ResultSet dbResult)
            throws SQLException, DBException
        {
            return new MSSQLTableColumn(table, dbResult);
        }
    }

    /**
     * Index cache implementation
     */
    class IndexCache extends JDBCCompositeCache<MSSQLCatalog, MSSQLTable, MSSQLTableIndex, MSSQLTableIndexColumn> {
        protected IndexCache()
        {
            super(tableCache, MSSQLTable.class, MSSQLConstants.COL_TABLE_NAME, MSSQLConstants.COL_INDEX_NAME);
        }

        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, MSSQLCatalog owner, MSSQLTable forTable)
            throws SQLException
        {
            StringBuilder sql = new StringBuilder();
            sql
                .append("SELECT * FROM ").append(MSSQLConstants.META_TABLE_STATISTICS)
                .append(" WHERE ").append(MSSQLConstants.COL_TABLE_SCHEMA).append("=?");
            if (forTable != null) {
                sql.append(" AND ").append(MSSQLConstants.COL_TABLE_NAME).append("=?");
            }
            sql.append(" ORDER BY ").append(MSSQLConstants.COL_INDEX_NAME).append(",").append(MSSQLConstants.COL_SEQ_IN_INDEX);

            JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString());
            dbStat.setString(1, MSSQLCatalog.this.getName());
            if (forTable != null) {
                dbStat.setString(2, forTable.getName());
            }
            return dbStat;
        }

        @Override
        protected MSSQLTableIndex fetchObject(JDBCSession session, MSSQLCatalog owner, MSSQLTable parent, String indexName, ResultSet dbResult)
            throws SQLException, DBException
        {
            boolean isNonUnique = JDBCUtils.safeGetInt(dbResult, MSSQLConstants.COL_NON_UNIQUE) != 0;
            String indexTypeName = JDBCUtils.safeGetString(dbResult, MSSQLConstants.COL_INDEX_TYPE);
            DBSIndexType indexType;
            if (MSSQLConstants.INDEX_TYPE_BTREE.getId().equals(indexTypeName)) {
                indexType = MSSQLConstants.INDEX_TYPE_BTREE;
            } else if (MSSQLConstants.INDEX_TYPE_FULLTEXT.getId().equals(indexTypeName)) {
                indexType = MSSQLConstants.INDEX_TYPE_FULLTEXT;
            } else if (MSSQLConstants.INDEX_TYPE_HASH.getId().equals(indexTypeName)) {
                indexType = MSSQLConstants.INDEX_TYPE_HASH;
            } else if (MSSQLConstants.INDEX_TYPE_RTREE.getId().equals(indexTypeName)) {
                indexType = MSSQLConstants.INDEX_TYPE_RTREE;
            } else {
                indexType = DBSIndexType.OTHER;
            }
            final String comment = JDBCUtils.safeGetString(dbResult, MSSQLConstants.COL_COMMENT);

            return new MSSQLTableIndex(
                parent,
                isNonUnique,
                indexName,
                indexType,
                comment);
        }

        @Override
        protected MSSQLTableIndexColumn fetchObjectRow(
            JDBCSession session,
            MSSQLTable parent, MSSQLTableIndex object, ResultSet dbResult)
            throws SQLException, DBException
        {
            int ordinalPosition = JDBCUtils.safeGetInt(dbResult, MSSQLConstants.COL_SEQ_IN_INDEX);
            String columnName = JDBCUtils.safeGetStringTrimmed(dbResult, MSSQLConstants.COL_COLUMN_NAME);
            String ascOrDesc = JDBCUtils.safeGetStringTrimmed(dbResult, MSSQLConstants.COL_COLLATION);
            boolean nullable = "YES".equals(JDBCUtils.safeGetStringTrimmed(dbResult, MSSQLConstants.COL_NULLABLE));

            MSSQLTableColumn tableColumn = parent.getAttribute(session.getProgressMonitor(), columnName);
            if (tableColumn == null) {
                log.debug("Column '" + columnName + "' not found in table '" + parent.getName() + "' for index '" + object.getName() + "'");
                return null;
            }

            return new MSSQLTableIndexColumn(
                object,
                tableColumn,
                ordinalPosition,
                "A".equalsIgnoreCase(ascOrDesc),
                nullable);
        }

        @Override
        protected void cacheChildren(MSSQLTableIndex index, List<MSSQLTableIndexColumn> rows)
        {
            index.setColumns(rows);
        }
    }

    /**
     * Constraint cache implementation
     */
    class ConstraintCache extends JDBCCompositeCache<MSSQLCatalog, MSSQLTable, MSSQLTableConstraint, MSSQLTableConstraintColumn> {
        protected ConstraintCache()
        {
            super(tableCache, MSSQLTable.class, MSSQLConstants.COL_TABLE_NAME, MSSQLConstants.COL_CONSTRAINT_NAME);
        }

        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, MSSQLCatalog owner, MSSQLTable forTable)
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
            dbStat.setString(1, MSSQLCatalog.this.getName());
            if (forTable != null) {
                dbStat.setString(2, forTable.getName());
            }
            return dbStat;
        }

        @Override
        protected MSSQLTableConstraint fetchObject(JDBCSession session, MSSQLCatalog owner, MSSQLTable parent, String constraintName, ResultSet dbResult)
            throws SQLException, DBException
        {
            if (constraintName.equals("PRIMARY")) {
                return new MSSQLTableConstraint(
                    parent, constraintName, null, DBSEntityConstraintType.PRIMARY_KEY, true);
            } else {
                return new MSSQLTableConstraint(
                    parent, constraintName, null, DBSEntityConstraintType.UNIQUE_KEY, true);
            }
        }

        @Override
        protected MSSQLTableConstraintColumn fetchObjectRow(
            JDBCSession session,
            MSSQLTable parent, MSSQLTableConstraint object, ResultSet dbResult)
            throws SQLException, DBException
        {
            String columnName = JDBCUtils.safeGetString(dbResult, MSSQLConstants.COL_COLUMN_NAME);
            MSSQLTableColumn column = parent.getAttribute(session.getProgressMonitor(), columnName);
            if (column == null) {
                log.warn("Column '" + columnName + "' not found in table '" + parent.getFullQualifiedName() + "'");
                return null;
            }
            int ordinalPosition = JDBCUtils.safeGetInt(dbResult, MSSQLConstants.COL_ORDINAL_POSITION);

            return new MSSQLTableConstraintColumn(
                object,
                column,
                ordinalPosition);
        }

        @Override
        protected void cacheChildren(MSSQLTableConstraint constraint, List<MSSQLTableConstraintColumn> rows)
        {
            constraint.setColumns(rows);
        }
    }

    /**
     * Procedures cache implementation
     */
    class ProceduresCache extends JDBCStructCache<MSSQLCatalog, MSSQLProcedure, MSSQLProcedureParameter> {

        ProceduresCache()
        {
            super(JDBCConstants.PROCEDURE_NAME);
        }

        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, MSSQLCatalog owner)
            throws SQLException
        {
            JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM " + MSSQLConstants.META_TABLE_ROUTINES +
                " WHERE " + MSSQLConstants.COL_ROUTINE_SCHEMA + "=?" +
                " ORDER BY " + MSSQLConstants.COL_ROUTINE_NAME
            );
            dbStat.setString(1, getName());
            return dbStat;
        }

        @Override
        protected MSSQLProcedure fetchObject(JDBCSession session, MSSQLCatalog owner, ResultSet dbResult)
            throws SQLException, DBException
        {
            return new MSSQLProcedure(MSSQLCatalog.this, dbResult);
        }

        @Override
        protected JDBCStatement prepareChildrenStatement(JDBCSession session, MSSQLCatalog owner, MSSQLProcedure procedure)
            throws SQLException
        {
            // Load procedure columns through MSSQL metadata
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
        protected MSSQLProcedureParameter fetchChild(JDBCSession session, MSSQLCatalog owner, MSSQLProcedure parent, ResultSet dbResult)
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
            DBSProcedureParameterType parameterType;
            switch (columnTypeNum) {
                case DatabaseMetaData.procedureColumnIn: parameterType = DBSProcedureParameterType.IN; break;
                case DatabaseMetaData.procedureColumnInOut: parameterType = DBSProcedureParameterType.INOUT; break;
                case DatabaseMetaData.procedureColumnOut: parameterType = DBSProcedureParameterType.OUT; break;
                case DatabaseMetaData.procedureColumnReturn: parameterType = DBSProcedureParameterType.RETURN; break;
                case DatabaseMetaData.procedureColumnResult: parameterType = DBSProcedureParameterType.RESULTSET; break;
                default: parameterType = DBSProcedureParameterType.UNKNOWN; break;
            }
            if (CommonUtils.isEmpty(columnName) && parameterType == DBSProcedureParameterType.RETURN) {
                columnName = "RETURN";
            }
            return new MSSQLProcedureParameter(
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

    class TriggerCache extends JDBCObjectCache<MSSQLCatalog, MSSQLTrigger> {
        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, MSSQLCatalog owner)
            throws SQLException
        {
            return session.prepareStatement(
                "SHOW FULL TRIGGERS FROM " + DBUtils.getQuotedIdentifier(MSSQLCatalog.this));
        }

        @Override
        protected MSSQLTrigger fetchObject(JDBCSession session, MSSQLCatalog owner, ResultSet dbResult)
            throws SQLException, DBException
        {
            String tableName = JDBCUtils.safeGetString(dbResult, "TABLE");
            MSSQLTable triggerTable = CommonUtils.isEmpty(tableName) ? null : getTable(session.getProgressMonitor(), tableName);
            return new MSSQLTrigger(MSSQLCatalog.this, triggerTable, dbResult);
        }

    }

}

/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.model;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.impl.struct.AbstractCatalog;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSConstraintType;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSIndexType;
import org.jkiss.dbeaver.model.struct.DBSProcedureColumnType;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GenericCatalog
 */
public class MySQLCatalog extends AbstractCatalog<MySQLDataSource> implements DBPSaveableObject
{
    static final Log log = LogFactory.getLog(MySQLCatalog.class);

    private MySQLCharset defaultCharset;
    private MySQLCollation defaultCollation;
    private String sqlPath;
    private final TableCache tableCache = new TableCache();
    private final ProceduresCache proceduresCache = new ProceduresCache();
    private final TriggerCache triggerCache = new TriggerCache();
    private final IndexCache indexCache = new IndexCache();
    private boolean constraintsCached = false;
    private boolean persisted;

    public MySQLCatalog(MySQLDataSource dataSource, ResultSet dbResult)
    {
        super(dataSource, null);
        if (dbResult != null) {
            setName(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_SCHEMA_NAME));
            defaultCharset = dataSource.getCharset(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_DEFAULT_CHARACTER_SET_NAME));
            defaultCollation = dataSource.getCollation(JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_DEFAULT_COLLATION_NAME));
            sqlPath = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_SQL_PATH);
            persisted = true;
        } else {
            persisted = false;
        }
    }

    TableCache getTableCache()
    {
        return tableCache;
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
    @Property(name = "Schema Name", viewable = true, editable = true, order = 1)
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

    @Property(name = "Default Charset", viewable = true, order = 2)
    public MySQLCharset getDefaultCharset()
    {
        return defaultCharset;
    }

    void setDefaultCharset(MySQLCharset defaultCharset)
    {
        this.defaultCharset = defaultCharset;
    }

    @Property(name = "Default Collation", viewable = true, order = 3)
    public MySQLCollation getDefaultCollation()
    {
        return defaultCollation;
    }

    void setDefaultCollation(MySQLCollation defaultCollation)
    {
        this.defaultCollation = defaultCollation;
    }

    @Property(name = "SQL Path", viewable = true, order = 3)
    public String getSqlPath()
    {
        return sqlPath;
    }

    void setSqlPath(String sqlPath)
    {
        this.sqlPath = sqlPath;
    }

    public Collection<MySQLIndex> getIndexes(DBRProgressMonitor monitor)
        throws DBException
    {
        return indexCache.getObjects(monitor, null);
    }

    public Collection<MySQLTable> getTables(DBRProgressMonitor monitor)
        throws DBException
    {
        return tableCache.getObjects(monitor, getDataSource(), MySQLTable.class);
    }

    public MySQLTable getTable(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return tableCache.getObject(monitor, getDataSource(), name, MySQLTable.class);
    }

    public Collection<MySQLView> getViews(DBRProgressMonitor monitor)
        throws DBException
    {
        return tableCache.getObjects(monitor, getDataSource(), MySQLView.class);
    }

    public Collection<MySQLProcedure> getProcedures(DBRProgressMonitor monitor)
        throws DBException
    {
        return proceduresCache.getObjects(monitor, getDataSource());
    }

    public MySQLProcedure getProcedure(DBRProgressMonitor monitor, String procName)
        throws DBException
    {
        return proceduresCache.getObject(monitor, getDataSource(), procName);
    }

    public Collection<MySQLTrigger> getTriggers(DBRProgressMonitor monitor)
        throws DBException
    {
        return triggerCache.getObjects(monitor, getDataSource());
    }

    public MySQLTrigger getTrigger(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return triggerCache.getObject(monitor, getDataSource(), name);
    }

    public Collection<MySQLTableBase> getChildren(DBRProgressMonitor monitor)
        throws DBException
    {
        return tableCache.getObjects(monitor, getDataSource());
    }

    public MySQLTableBase getChild(DBRProgressMonitor monitor, String childName)
        throws DBException
    {
        return tableCache.getObject(monitor, getDataSource(), childName);
    }

    public Class<? extends DBSEntity> getChildType(DBRProgressMonitor monitor)
        throws DBException
    {
        return MySQLTable.class;
    }

    public void cacheStructure(DBRProgressMonitor monitor, int scope)
        throws DBException
    {
        monitor.subTask("Cache tables");
        tableCache.loadObjects(monitor, getDataSource());
        if ((scope & STRUCT_ATTRIBUTES) != 0) {
            monitor.subTask("Cache table columns");
            tableCache.loadChildren(monitor, getDataSource(), null);
        }
        monitor.subTask("Cache table constraints");
        loadConstraints(monitor, null);
    }

    @Override
    public boolean refreshEntity(DBRProgressMonitor monitor)
        throws DBException
    {
        super.refreshEntity(monitor);
        tableCache.clearCache();
        indexCache.clearCache();
        proceduresCache.clearCache();
        triggerCache.clearCache();
        return true;
    }

    void loadConstraints(DBRProgressMonitor monitor, MySQLTable forTable)
        throws DBException
    {
        if (constraintsCached) {
            return;
        }
        if (forTable == null) {
            tableCache.getObjects(monitor, getDataSource());
        } else if (!forTable.isPersisted()) {
            return;
        }
        JDBCExecutionContext context = getDataSource().openContext(monitor, DBCExecutionPurpose.META, "Load constraints");
        try {
            Map<String, String> constrTypeMap = new HashMap<String, String>();
            Map<String, MySQLConstraint> constrMap = new HashMap<String, MySQLConstraint>();

            // Read constraints and their types
            StringBuilder query = new StringBuilder();
            query.append("SELECT " + MySQLConstants.COL_CONSTRAINT_NAME + "," + MySQLConstants.COL_TABLE_NAME + "," + MySQLConstants.COL_CONSTRAINT_TYPE +
                " FROM " + MySQLConstants.META_TABLE_TABLE_CONSTRAINTS + " WHERE " + MySQLConstants.COL_TABLE_SCHEMA + "=?");
            if (forTable != null) {
                query.append(" AND " + MySQLConstants.COL_TABLE_NAME + "=?");
            }
            JDBCPreparedStatement dbStat = context.prepareStatement(query.toString());
            try {
                dbStat.setString(1, getName());
                if (forTable != null) {
                    dbStat.setString(2, forTable.getName());
                }
                JDBCResultSet dbResult = dbStat.executeQuery();
                try {
                    while (dbResult.next()) {
                        String constraintName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_CONSTRAINT_NAME);
                        String constraintType = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_CONSTRAINT_TYPE);
                        if (MySQLConstants.CONSTRAINT_FOREIGN_KEY.equals(constraintType)) {
                            // Skip foreign keys
                            continue;
                        }
                        String tableName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_TABLE_NAME);
                        MySQLTable table = forTable;
                        if (table == null) {
                            table = getTable(monitor, tableName);
                            if (table == null) {
                                log.warn("Table '" + tableName + "' not found");
                                continue;
                            }
                            if (table.uniqueKeysCached()) {
                                // Already cached
                                continue;
                            }
                        }
                        constrTypeMap.put(tableName + "." + constraintName, constraintType);
                    }
                }
                finally {
                    dbResult.close();
                }
            }
            finally {
                dbStat.close();
            }

            // Read constraint columns
            query = new StringBuilder();
            query.append("SELECT CONSTRAINT_NAME,TABLE_NAME,COLUMN_NAME,ORDINAL_POSITION,REFERENCED_TABLE_SCHEMA,REFERENCED_TABLE_NAME,REFERENCED_COLUMN_NAME FROM ")
                .append(MySQLConstants.META_TABLE_KEY_COLUMN_USAGE)
                .append(" tc WHERE tc.TABLE_SCHEMA=?");
            if (forTable != null) {
                query.append(" AND tc.TABLE_NAME=?");
            }
            dbStat = context.prepareStatement(query.toString());
            try {
                dbStat.setString(1, getName());
                if (forTable != null) {
                    dbStat.setString(2, forTable.getName());
                }
                JDBCResultSet dbResult = dbStat.executeQuery();
                try {
                    while (dbResult.next()) {
                        String constraintName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_CONSTRAINT_NAME);
                        String tableName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_TABLE_NAME);
                        String constId = tableName + "." + constraintName;
                        String constraintType = constrTypeMap.get(constId);
                        if (constraintType == null) {
                            // Skip this one
                            continue;
                        }
                        MySQLTable table = forTable;
                        if (table == null) {
                            table = getTable(monitor, tableName);
                            if (table == null) {
                                log.warn("Table '" + tableName + "' not found");
                                continue;
                            }
                        }
                        String columnName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_COLUMN_NAME);
                        MySQLTableColumn column = table.getColumn(monitor, columnName);
                        if (column == null) {
                            log.warn("Column '" + columnName + "' not found in table '" + tableName + "'");
                            continue;
                        }
                        int ordinalPosition = JDBCUtils.safeGetInt(dbResult, MySQLConstants.COL_ORDINAL_POSITION);

                        MySQLConstraint constraint = constrMap.get(constId);
                        if (constraint == null) {
                            if (MySQLConstants.CONSTRAINT_PRIMARY_KEY.equals(constraintType)) {
                                constraint = new MySQLConstraint(table, constraintName, "", DBSConstraintType.PRIMARY_KEY, true);
                            } else if (MySQLConstants.CONSTRAINT_UNIQUE.equals(constraintType)) {
                                constraint = new MySQLConstraint(table, constraintName, "", DBSConstraintType.UNIQUE_KEY, true);
                            } else {
                                log.warn("Constraint type '" + constraintType + "' is not supported");
                                continue;
                            }
                            constrMap.put(constId, constraint);
                            table.cacheUniqueKey(constraint);
                        }
                        constraint.addColumn(
                            new MySQLConstraintColumn(
                                constraint,
                                column,
                                ordinalPosition));
                    }
                }
                finally {
                    dbResult.close();
                }
            }
            finally {
                dbStat.close();
            }
        } catch (SQLException ex) {
            throw new DBException(ex);
        }
        finally {
            context.close();
        }
        if (forTable == null) {
            constraintsCached = true;
        }
    }

    public boolean isSystem()
    {
        return MySQLConstants.INFO_SCHEMA_NAME.equalsIgnoreCase(getName()) || MySQLConstants.MYSQL_SCHEMA_NAME.equalsIgnoreCase(getName());
    }

    class TableCache extends JDBCStructCache<MySQLTableBase, MySQLTableColumn> {
        
        protected TableCache()
        {
            super(JDBCConstants.TABLE_NAME);
        }

        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context)
            throws SQLException, DBException
        {
            return context.prepareStatement("SHOW FULL TABLES FROM " + DBUtils.getQuotedIdentifier(getDataSource(), getName()));
        }

        protected MySQLTableBase fetchObject(JDBCExecutionContext context, ResultSet dbResult)
            throws SQLException, DBException
        {
            final String tableType = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_TABLE_TYPE);
            if (tableType.indexOf("VIEW") != -1) {
                return new MySQLView(MySQLCatalog.this, dbResult);
            } else {
                return new MySQLTable(MySQLCatalog.this, dbResult);
            }
        }

        protected boolean isChildrenCached(MySQLTableBase table)
        {
            return table.isColumnsCached();
        }

        protected void cacheChildren(MySQLTableBase table, List<MySQLTableColumn> columns)
        {
            table.setColumns(columns);
        }

        protected JDBCPreparedStatement prepareChildrenStatement(JDBCExecutionContext context, MySQLTableBase forTable)
            throws SQLException, DBException
        {
            StringBuilder sql = new StringBuilder();
            sql
                .append("SELECT * FROM ").append(MySQLConstants.META_TABLE_COLUMNS)
                .append(" WHERE ").append(MySQLConstants.COL_TABLE_SCHEMA).append("=?");
            if (forTable != null) {
                sql.append(" AND ").append(MySQLConstants.COL_TABLE_NAME).append("=?");
            }
            sql.append(" ORDER BY ").append(MySQLConstants.COL_ORDINAL_POSITION);

            JDBCPreparedStatement dbStat = context.prepareStatement(sql.toString());
            dbStat.setString(1, MySQLCatalog.this.getName());
            if (forTable != null) {
                dbStat.setString(2, forTable.getName());
            }
            return dbStat;
        }

        protected MySQLTableColumn fetchChild(JDBCExecutionContext context, MySQLTableBase table, ResultSet dbResult)
            throws SQLException, DBException
        {
            return new MySQLTableColumn(table, dbResult);
        }
    }

    /**
     * Index cache implementation
     */
    class IndexCache extends JDBCCompositeCache<MySQLTable, MySQLIndex, MySQLIndexColumn> {
        protected IndexCache()
        {
            super(tableCache, MySQLTable.class, MySQLConstants.COL_TABLE_NAME, MySQLConstants.COL_INDEX_NAME);
        }

        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context, MySQLTable forTable)
            throws SQLException, DBException
        {
            StringBuilder sql = new StringBuilder();
            sql
                .append("SELECT * FROM ").append(MySQLConstants.META_TABLE_STATISTICS)
                .append(" WHERE ").append(MySQLConstants.COL_TABLE_SCHEMA).append("=?");
            if (forTable != null) {
                sql.append(" AND ").append(MySQLConstants.COL_TABLE_NAME).append("=?");
            }
            sql.append(" ORDER BY ").append(MySQLConstants.COL_INDEX_NAME).append(",").append(MySQLConstants.COL_SEQ_IN_INDEX);

            JDBCPreparedStatement dbStat = context.prepareStatement(sql.toString());
            dbStat.setString(1, MySQLCatalog.this.getName());
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

        protected MySQLIndex fetchObject(JDBCExecutionContext context, ResultSet dbResult, MySQLTable parent, String indexName)
            throws SQLException, DBException
        {
            boolean isNonUnique = JDBCUtils.safeGetInt(dbResult, MySQLConstants.COL_NON_UNIQUE) != 0;
            String indexTypeName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_INDEX_TYPE);
            DBSIndexType indexType;
            if (MySQLConstants.INDEX_TYPE_BTREE.getId().equals(indexTypeName)) {
                indexType = MySQLConstants.INDEX_TYPE_BTREE;
            } else if (MySQLConstants.INDEX_TYPE_FULLTEXT.getId().equals(indexTypeName)) {
                indexType = MySQLConstants.INDEX_TYPE_FULLTEXT;
            } else if (MySQLConstants.INDEX_TYPE_HASH.getId().equals(indexTypeName)) {
                indexType = MySQLConstants.INDEX_TYPE_HASH;
            } else if (MySQLConstants.INDEX_TYPE_RTREE.getId().equals(indexTypeName)) {
                indexType = MySQLConstants.INDEX_TYPE_RTREE;
            } else {
                indexType = DBSIndexType.OTHER;
            }
            final String comment = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_COMMENT);

            return new MySQLIndex(
                parent,
                isNonUnique,
                indexName,
                indexType,
                comment);
        }

        protected MySQLIndexColumn fetchObjectRow(
            JDBCExecutionContext context,
            ResultSet dbResult,
            MySQLTable parent,
            MySQLIndex object)
            throws SQLException, DBException
        {
            int ordinalPosition = JDBCUtils.safeGetInt(dbResult, MySQLConstants.COL_SEQ_IN_INDEX);
            String columnName = JDBCUtils.safeGetStringTrimmed(dbResult, MySQLConstants.COL_COLUMN_NAME);
            String ascOrDesc = JDBCUtils.safeGetStringTrimmed(dbResult, MySQLConstants.COL_COLLATION);
            boolean nullable = "YES".equals(JDBCUtils.safeGetStringTrimmed(dbResult, MySQLConstants.COL_NULLABLE));

            MySQLTableColumn tableColumn = parent.getColumn(context.getProgressMonitor(), columnName);
            if (tableColumn == null) {
                log.debug("Column '" + columnName + "' not found in table '" + parent.getName() + "' for index '" + object.getName() + "'");
                return null;
            }

            return new MySQLIndexColumn(
                object,
                tableColumn,
                ordinalPosition,
                "A".equalsIgnoreCase(ascOrDesc),
                nullable);
        }

        protected boolean isObjectsCached(MySQLTable parent)
        {
            return parent.isIndexesCached();
        }

        protected void cacheObjects(DBRProgressMonitor monitor, MySQLTable parent, List<MySQLIndex> indexes)
        {
            parent.setIndexes(indexes);
        }

        protected void cacheRows(DBRProgressMonitor monitor, MySQLIndex index, List<MySQLIndexColumn> rows)
        {
            index.setColumns(rows);
        }
    }

    /**
     * Procedures cache implementation
     */
    class ProceduresCache extends JDBCStructCache<MySQLProcedure, MySQLProcedureColumn> {

        ProceduresCache()
        {
            super(JDBCConstants.PROCEDURE_NAME);
        }

        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context)
            throws SQLException, DBException
        {
            JDBCPreparedStatement dbStat = context.prepareStatement(
                "SELECT * FROM " + MySQLConstants.META_TABLE_ROUTINES +
                " WHERE " + MySQLConstants.COL_ROUTINE_SCHEMA + "=?" +
                " ORDER BY " + MySQLConstants.COL_ROUTINE_NAME
            );
            dbStat.setString(1, getName());
            return dbStat;
        }

        protected MySQLProcedure fetchObject(JDBCExecutionContext context, ResultSet dbResult)
            throws SQLException, DBException
        {
            return new MySQLProcedure(MySQLCatalog.this, dbResult);
        }

        protected boolean isChildrenCached(MySQLProcedure parent)
        {
            return parent.isColumnsCached();
        }

        protected void cacheChildren(MySQLProcedure parent, List<MySQLProcedureColumn> columns)
        {
            parent.cacheColumns(columns);
        }

        protected JDBCPreparedStatement prepareChildrenStatement(JDBCExecutionContext context, MySQLProcedure procedure)
            throws SQLException, DBException
        {
            // Load procedure columns thru MySQL metadata
            // There is no metadata table about proc/func columns -
            // it should be parsed from SHOW CREATE PROCEDURE/FUNCTION query
            // Lets driver do it instead of me
            return context.getMetaData().getProcedureColumns(
                getName(),
                null,
                procedure.getName(),
                null).getSource();
        }

        protected MySQLProcedureColumn fetchChild(JDBCExecutionContext context, MySQLProcedure parent, ResultSet dbResult)
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
            return new MySQLProcedureColumn(
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

    class TriggerCache extends JDBCObjectCache<MySQLTrigger> {
        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context)
            throws SQLException, DBException
        {
            JDBCPreparedStatement dbStat = context.prepareStatement(        
                "SELECT * FROM " + MySQLConstants.META_TABLE_TRIGGERS +
                " WHERE " + MySQLConstants.COL_TRIGGER_SCHEMA + "=?" +
                " ORDER BY " + MySQLConstants.COL_TRIGGER_NAME);
            dbStat.setString(1, getName());
            return dbStat;
        }

        protected MySQLTrigger fetchObject(JDBCExecutionContext context, ResultSet dbResult)
            throws SQLException, DBException
        {
            String tableSchema = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_TRIGGER_EVENT_OBJECT_SCHEMA);
            String tableName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_TRIGGER_EVENT_OBJECT_TABLE);
            MySQLTable triggerTable = getDataSource().findTable(context.getProgressMonitor(), tableSchema, tableName);
            return new MySQLTrigger(MySQLCatalog.this, triggerTable, dbResult);
        }

    }

}

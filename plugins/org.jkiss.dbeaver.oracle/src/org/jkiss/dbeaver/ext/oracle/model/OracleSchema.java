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
import org.jkiss.dbeaver.model.impl.struct.AbstractSchema;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OracleSchema
 */
public class OracleSchema extends AbstractSchema<OracleDataSource> implements DBPSaveableObject
{
    static final Log log = LogFactory.getLog(OracleSchema.class);

    private OracleCharset defaultCharset;
    private OracleCollation defaultCollation;
    private String sqlPath;
    private final TableCache tableCache = new TableCache();
    private final ProceduresCache proceduresCache = new ProceduresCache();
    private final TriggerCache triggerCache = new TriggerCache();
    private final IndexCache indexCache = new IndexCache();
    private boolean constraintsCached = false;
    private boolean persisted;

    public OracleSchema(OracleDataSource dataSource, ResultSet dbResult)
    {
        super(dataSource, null);
        if (dbResult != null) {
            setName(JDBCUtils.safeGetString(dbResult, OracleConstants.COL_SCHEMA_NAME));
            defaultCharset = dataSource.getCharset(JDBCUtils.safeGetString(dbResult, OracleConstants.COL_DEFAULT_CHARACTER_SET_NAME));
            defaultCollation = dataSource.getCollation(JDBCUtils.safeGetString(dbResult, OracleConstants.COL_DEFAULT_COLLATION_NAME));
            sqlPath = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_SQL_PATH);
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
    public OracleCharset getDefaultCharset()
    {
        return defaultCharset;
    }

    void setDefaultCharset(OracleCharset defaultCharset)
    {
        this.defaultCharset = defaultCharset;
    }

    @Property(name = "Default Collation", viewable = true, order = 3)
    public OracleCollation getDefaultCollation()
    {
        return defaultCollation;
    }

    void setDefaultCollation(OracleCollation defaultCollation)
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
        loadConstraints(monitor, null);
    }

    @Override
    public boolean refreshEntity(DBRProgressMonitor monitor)
        throws DBException
    {
        super.refreshEntity(monitor);
        tableCache.clearCache();
        proceduresCache.clearCache();
        triggerCache.clearCache();
        return true;
    }

    void loadConstraints(DBRProgressMonitor monitor, OracleTable forTable)
        throws DBException
    {
        if (constraintsCached) {
            return;
        }
        if (forTable == null) {
            tableCache.getObjects(monitor);
        } else if (!forTable.isPersisted()) {
            return;
        }
        JDBCExecutionContext context = getDataSource().openContext(monitor, DBCExecutionPurpose.META, "Load constraints");
        try {
            Map<String, String> constrTypeMap = new HashMap<String, String>();
            Map<String, OracleConstraint> constrMap = new HashMap<String, OracleConstraint>();

            // Read constraints and their types
            StringBuilder query = new StringBuilder();
            query.append("SELECT " + OracleConstants.COL_CONSTRAINT_NAME + "," + OracleConstants.COL_TABLE_NAME + "," + OracleConstants.COL_CONSTRAINT_TYPE +
                " FROM " + OracleConstants.META_TABLE_TABLE_CONSTRAINTS + " WHERE " + OracleConstants.COL_TABLE_SCHEMA + "=?");
            if (forTable != null) {
                query.append(" AND " + OracleConstants.COL_TABLE_NAME + "=?");
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
                        String constraintName = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_CONSTRAINT_NAME);
                        String constraintType = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_CONSTRAINT_TYPE);
                        if (OracleConstants.CONSTRAINT_FOREIGN_KEY.equals(constraintType)) {
                            // Skip foreign keys
                            continue;
                        }
                        String tableName = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_TABLE_NAME);
                        OracleTable table = forTable;
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
                .append(OracleConstants.META_TABLE_KEY_COLUMN_USAGE)
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
                        String constraintName = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_CONSTRAINT_NAME);
                        String tableName = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_TABLE_NAME);
                        String constId = tableName + "." + constraintName;
                        String constraintType = constrTypeMap.get(constId);
                        if (constraintType == null) {
                            // Skip this one
                            continue;
                        }
                        OracleTable table = forTable;
                        if (table == null) {
                            table = getTable(monitor, tableName);
                            if (table == null) {
                                log.warn("Table '" + tableName + "' not found");
                                continue;
                            }
                        }
                        String columnName = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_COLUMN_NAME);
                        OracleTableColumn column = table.getColumn(monitor, columnName);
                        if (column == null) {
                            log.warn("Column '" + columnName + "' not found in table '" + tableName + "'");
                            continue;
                        }
                        int ordinalPosition = JDBCUtils.safeGetInt(dbResult, OracleConstants.COL_ORDINAL_POSITION);

                        OracleConstraint constraint = constrMap.get(constId);
                        if (constraint == null) {
                            if (OracleConstants.CONSTRAINT_PRIMARY_KEY.equals(constraintType)) {
                                constraint = new OracleConstraint(table, constraintName, "", DBSConstraintType.PRIMARY_KEY, true);
                            } else if (OracleConstants.CONSTRAINT_UNIQUE.equals(constraintType)) {
                                constraint = new OracleConstraint(table, constraintName, "", DBSConstraintType.UNIQUE_KEY, true);
                            } else {
                                log.warn("Constraint type '" + constraintType + "' is not supported");
                                continue;
                            }
                            constrMap.put(constId, constraint);
                            table.cacheUniqueKey(constraint);
                        }
                        constraint.addColumn(
                            new OracleConstraintColumn(
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
        return OracleConstants.INFO_SCHEMA_NAME.equalsIgnoreCase(getName()) || OracleConstants.MYSQL_SCHEMA_NAME.equalsIgnoreCase(getName());
    }

    public DBSCatalog getCatalog()
    {
        return null;
    }

    class TableCache extends JDBCStructCache<OracleTableBase, OracleTableColumn> {
        
        protected TableCache()
        {
            super(getDataSource(), JDBCConstants.TABLE_NAME);
        }

        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context)
            throws SQLException, DBException
        {
            return context.prepareStatement("SHOW FULL TABLES FROM " + DBUtils.getQuotedIdentifier(getDataSource(), getName()));
        }

        protected OracleTableBase fetchObject(JDBCExecutionContext context, ResultSet dbResult)
            throws SQLException, DBException
        {
            final String tableType = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_TABLE_TYPE);
            if (tableType.indexOf("VIEW") != -1) {
                return new OracleView(OracleSchema.this, dbResult);
            } else {
                return new OracleTable(OracleSchema.this, dbResult);
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

        protected JDBCPreparedStatement prepareChildrenStatement(JDBCExecutionContext context, OracleTableBase forTable)
            throws SQLException, DBException
        {
            StringBuilder sql = new StringBuilder();
            sql
                .append("SELECT * FROM ").append(OracleConstants.META_TABLE_COLUMNS)
                .append(" WHERE ").append(OracleConstants.COL_TABLE_SCHEMA).append("=?");
            if (forTable != null) {
                sql.append(" AND ").append(OracleConstants.COL_TABLE_NAME).append("=?");
            }
            sql.append(" ORDER BY ").append(OracleConstants.COL_ORDINAL_POSITION);

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

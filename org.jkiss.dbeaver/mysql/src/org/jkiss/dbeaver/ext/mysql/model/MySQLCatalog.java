package org.jkiss.dbeaver.ext.mysql.model;

import net.sf.jkiss.utils.CommonUtils;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.model.anno.Property;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.impl.meta.AbstractCatalog;
import org.jkiss.dbeaver.model.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * GenericCatalog
 */
public class MySQLCatalog extends AbstractCatalog<MySQLDataSource> implements DBSStructureAssistant
{
    static final Log log = LogFactory.getLog(MySQLCatalog.class);

    private String defaultCharset;
    private String defaultCollation;
    private String sqlPath;
    private TableCache tableCache = new TableCache();
    private ProceduresCache proceduresCache = new ProceduresCache();
    private TriggerCache triggerCache = new TriggerCache();

    public MySQLCatalog(MySQLDataSource dataSource, String catalogName)
    {
        super(dataSource, catalogName);
    }

    TableCache getTableCache()
    {
        return tableCache;
    }

    ProceduresCache getProceduresCache()
    {
        return proceduresCache;
    }

    TriggerCache getTriggerCache()
    {
        return triggerCache;
    }

    public String getDescription()
    {
        return null;
    }

    @Property(name = "Default Charset", viewable = true, order = 2)
    public String getDefaultCharset()
    {
        return defaultCharset;
    }

    void setDefaultCharset(String defaultCharset)
    {
        this.defaultCharset = defaultCharset;
    }

    public String getDefaultCollation()
    {
        return defaultCollation;
    }

    void setDefaultCollation(String defaultCollation)
    {
        this.defaultCollation = defaultCollation;
    }

    public String getSqlPath()
    {
        return sqlPath;
    }

    void setSqlPath(String sqlPath)
    {
        this.sqlPath = sqlPath;
    }

    public List<MySQLIndex> getIndexes(DBRProgressMonitor monitor)
        throws DBException
    {
        // Cache tables and columns
        tableCache.loadChildren(monitor, null);

        // Copy indexes from tables because we do not want
        // to place the same objects in different places of the tree model
        List<MySQLIndex> indexList = new ArrayList<MySQLIndex>();
        for (MySQLTable table : getTables(monitor)) {
            for (MySQLIndex index : table.getIndexes(monitor)) {
                indexList.add(new MySQLIndex(index));
            }
        }
        return indexList;
    }

    public List<MySQLTable> getTables(DBRProgressMonitor monitor)
        throws DBException
    {
        return tableCache.getObjects(monitor);
    }

    public MySQLTable getTable(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return tableCache.getObject(monitor, name);
    }

    public List<MySQLProcedure> getProcedures(DBRProgressMonitor monitor)
        throws DBException
    {
        return proceduresCache.getObjects(monitor);
    }

    public List<MySQLTrigger> getTriggers(DBRProgressMonitor monitor)
        throws DBException
    {
        return triggerCache.getObjects(monitor);
    }

    public MySQLTrigger getTrigger(DBRProgressMonitor monitor, String name)
        throws DBException
    {
        return triggerCache.getObject(monitor, name);
    }

    public List<DBSTablePath> findTableNames(DBRProgressMonitor monitor, String tableMask, int maxResults) throws DBException
    {
        return getDataSource().findTableNames(monitor, tableMask, maxResults);
    }

    public Collection<MySQLTable> getChildren(DBRProgressMonitor monitor)
        throws DBException
    {
        return getTables(monitor);
    }

    public MySQLTable getChild(DBRProgressMonitor monitor, String childName)
        throws DBException
    {
        return getTable(monitor, childName);
    }

    public Class<? extends DBSObject> getChildType(DBRProgressMonitor monitor)
        throws DBException
    {
        return MySQLTable.class;
    }

    public void cacheStructure(DBRProgressMonitor monitor, int scope)
        throws DBException
    {
        tableCache.loadObjects(monitor);
        if ((scope & STRUCT_ATTRIBUTES) != 0) {
            tableCache.loadChildren(monitor, null);
        }
    }

    @Override
    public boolean refreshObject(DBRProgressMonitor monitor)
        throws DBException
    {
        super.refreshObject(monitor);
        tableCache.clearCache();
        proceduresCache.clearCache();
        triggerCache.clearCache();
        return true;
    }

    private void loadConstraints(DBRProgressMonitor monitor, MySQLTable forTable)
        throws DBException
    {
        if (forTable == null) {
            tableCache.getObjects(monitor);
        }
        JDBCExecutionContext context = getDataSource().openContext(monitor);
        try {
            Map<String, String> constrMap = new HashMap<String, String>();

            // Read constraints and their types
            StringBuilder query = new StringBuilder();
            query.append("SELECT tc.CONSTRAINT_NAME,tc.TABLE_NAME,tc.CONSTRAINT_TYPE FROM ").append(MySQLConstants.META_TABLE_TABLE_CONSTRAINTS)
                .append(" tc WHERE tc.TABLE_SCHEMA=?");
            if (forTable != null) {
                query.append(" AND tc.TABLE_NAME=?");
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
                        String tableName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_TABLE_NAME);
                        constrMap.put(tableName + "." + constraintName, constraintType);
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
                        String constraintType = constrMap.get(tableName + "." + constraintName);
                        if (constraintType == null) {
                            log.warn("Constraint '" + constraintName + "' is not recognized");
                            continue;
                        }
                        if (forTable == null) {
                            forTable = getTable(monitor, tableName);
                            if (forTable == null) {
                                log.warn("Table '" + tableName + "' not found");
                                continue;
                            }
                        }

                        MySQLConstraint constraint;
                        if (MySQLConstants.CONSTRAINT_PRIMARY_KEY.equals(constraintType)) {
                            constraint = new MySQLConstraint(forTable, constraintName, "", DBSConstraintType.PRIMARY_KEY);
                        } else if (MySQLConstants.CONSTRAINT_UNIQUE.equals(constraintType)) {
                            constraint = new MySQLConstraint(forTable, constraintName, "", DBSConstraintType.UNIQUE_KEY);
                        } else if (MySQLConstants.CONSTRAINT_FOREIGN_KEY.equals(constraintType)) {
                            //constraint = new MySQLForeignKey(forTable, constraintName, "", DBSConstraintType.PRIMARY_KEY);
                        } else {
                            log.warn("Constraint type '" + constraintType + "' is not supported");
                            continue;
                        }
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
    }

    class TableCache extends JDBCStructCache<MySQLTable, MySQLTableColumn> {
        
        protected TableCache()
        {
            super(getDataSource(), JDBCConstants.TABLE_NAME);
        }

        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context)
            throws SQLException, DBException
        {
            JDBCPreparedStatement dbStat = context.prepareStatement(MySQLConstants.QUERY_SELECT_TABLES);
            dbStat.setString(1, getName());
            return dbStat;
        }

        protected MySQLTable fetchObject(JDBCExecutionContext context, ResultSet dbResult)
            throws SQLException, DBException
        {
            return new MySQLTable(MySQLCatalog.this, dbResult);
        }

        protected boolean isChildrenCached(MySQLTable table)
        {
            return table.isColumnsCached();
        }

        protected void cacheChildren(MySQLTable table, List<MySQLTableColumn> columns)
        {
            table.setColumns(columns);
        }

        protected JDBCPreparedStatement prepareChildrenStatement(JDBCExecutionContext context, MySQLTable forTable)
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

        protected MySQLTableColumn fetchChild(JDBCExecutionContext context, MySQLTable table, ResultSet dbResult)
            throws SQLException, DBException
        {
            return new MySQLTableColumn(table, dbResult);
        }
    }

    /**
     * Procedures cache implementation
     */
    class ProceduresCache extends JDBCStructCache<MySQLProcedure, MySQLProcedureColumn> {

        ProceduresCache()
        {
            super(getDataSource(), JDBCConstants.PROCEDURE_NAME);
        }

        protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context)
            throws SQLException, DBException
        {
            JDBCPreparedStatement dbStat = context.prepareStatement(MySQLConstants.QUERY_SELECT_ROUTINES);
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
                null).getStatement();
        }

        protected MySQLProcedureColumn fetchChild(JDBCExecutionContext context, MySQLProcedure parent, ResultSet dbResult)
            throws SQLException, DBException
        {
            String columnName = JDBCUtils.safeGetString(dbResult, JDBCConstants.COLUMN_NAME);
            int columnTypeNum = JDBCUtils.safeGetInt(dbResult, JDBCConstants.COLUMN_TYPE);
            int valueType = JDBCUtils.safeGetInt(dbResult, JDBCConstants.DATA_TYPE);
            String typeName = JDBCUtils.safeGetString(dbResult, JDBCConstants.TYPE_NAME);
            int position = JDBCUtils.safeGetInt(dbResult, JDBCConstants.ORDINAL_POSITION);
            int columnSize = JDBCUtils.safeGetInt(dbResult, JDBCConstants.LENGTH);
            boolean isNullable = JDBCUtils.safeGetInt(dbResult, JDBCConstants.NULLABLE) != DatabaseMetaData.procedureNoNulls;
            int scale = JDBCUtils.safeGetInt(dbResult, JDBCConstants.SCALE);
            int precision = JDBCUtils.safeGetInt(dbResult, JDBCConstants.PRECISION);
            int radix = JDBCUtils.safeGetInt(dbResult, JDBCConstants.RADIX);
            String remarks = JDBCUtils.safeGetString(dbResult, JDBCConstants.REMARKS);
            //DBSDataType dataType = getDataSource().getInfo().getSupportedDataType(typeName);
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
                scale, precision, radix, isNullable,
                remarks,
                columnType);
        }
    }

    class TriggerCache extends JDBCObjectCache<MySQLTrigger> {
        
        protected TriggerCache()
        {
            super(getDataSource());
        }

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

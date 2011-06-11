/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.OracleConstants;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCStructureAssistant;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectType;

import java.sql.SQLException;
import java.util.List;

/**
 * OracleStructureAssistant
 */
public class OracleStructureAssistant extends JDBCStructureAssistant
{
    private final OracleDataSource dataSource;

    public OracleStructureAssistant(OracleDataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    @Override
    protected JDBCDataSource getDataSource()
    {
        return dataSource;
    }

    public DBSObjectType[] getSupportedObjectTypes()
    {
        return new DBSObjectType[] {
            RelationalObjectType.TYPE_TABLE,
            RelationalObjectType.TYPE_CONSTRAINT,
            RelationalObjectType.TYPE_PROCEDURE,
            RelationalObjectType.TYPE_TABLE_COLUMN,
            };
    }

    @Override
    protected void findObjectsByMask(JDBCExecutionContext context, DBSObjectType objectType, DBSObject parentObject, String objectNameMask, int maxResults, List<DBSObject> objects) throws DBException, SQLException
    {
        OracleSchema schema = parentObject instanceof OracleSchema ? (OracleSchema) parentObject : null;
        if (objectType == RelationalObjectType.TYPE_TABLE) {
            findTablesByMask(context, schema, objectNameMask, maxResults, objects);
        } else if (objectType == RelationalObjectType.TYPE_CONSTRAINT) {
            findConstraintsByMask(context, schema, objectNameMask, maxResults, objects);
        } else if (objectType == RelationalObjectType.TYPE_PROCEDURE) {
            findProceduresByMask(context, schema, objectNameMask, maxResults, objects);
        } else if (objectType == RelationalObjectType.TYPE_TABLE_COLUMN) {
            findTableColumnsByMask(context, schema, objectNameMask, maxResults, objects);
        }
    }

    private void findTablesByMask(JDBCExecutionContext context, OracleSchema schema, String tableNameMask, int maxResults, List<DBSObject> objects)
        throws SQLException, DBException
    {
        DBRProgressMonitor monitor = context.getProgressMonitor();

        // Load tables
        JDBCPreparedStatement dbStat = context.prepareStatement(
            "SELECT " + OracleConstants.COL_TABLE_SCHEMA + "," + OracleConstants.COL_TABLE_NAME +
            " FROM " + OracleConstants.META_TABLE_TABLES + " WHERE " + OracleConstants.COL_TABLE_NAME + " LIKE ? " +
                (schema == null ? "" : " AND " + OracleConstants.COL_TABLE_SCHEMA + "=?") +
                " ORDER BY " + OracleConstants.COL_TABLE_NAME);
        try {
            dbStat.setString(1, tableNameMask.toLowerCase());
            if (schema != null) {
                dbStat.setString(2, schema.getName());
            }
            JDBCResultSet dbResult = dbStat.executeQuery();
            try {
                int tableNum = maxResults;
                while (dbResult.next() && tableNum-- > 0) {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    String schemaName = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_TABLE_SCHEMA);
                    String tableName = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_TABLE_NAME);
                    OracleSchema tableSchema = schema != null ? schema : dataSource.getSchema(monitor, schemaName);
                    if (tableSchema == null) {
                        log.debug("Table schema '" + schemaName + "' not found");
                        continue;
                    }
                    OracleTable table = tableSchema.getTable(monitor, tableName);
                    if (table == null) {
                        log.debug("Table '" + tableName + "' not found in schema '" + tableSchema.getName() + "'");
                        continue;
                    }
                    objects.add(table);
                }
            }
            finally {
                dbResult.close();
            }
        } finally {
            dbStat.close();
        }
    }

    private void findProceduresByMask(JDBCExecutionContext context, OracleSchema schema, String procNameMask, int maxResults, List<DBSObject> objects)
        throws SQLException, DBException
    {
        DBRProgressMonitor monitor = context.getProgressMonitor();

        // Load tables
        JDBCPreparedStatement dbStat = context.prepareStatement(
            "SELECT " + OracleConstants.COL_ROUTINE_SCHEMA + "," + OracleConstants.COL_ROUTINE_NAME +
            " FROM " + OracleConstants.META_TABLE_ROUTINES + " WHERE " + OracleConstants.COL_ROUTINE_NAME + " LIKE ? " +
                (schema == null ? "" : " AND " + OracleConstants.COL_ROUTINE_SCHEMA + "=?") +
                " ORDER BY " + OracleConstants.COL_ROUTINE_NAME);
        try {
            dbStat.setString(1, procNameMask.toLowerCase());
            if (schema != null) {
                dbStat.setString(2, schema.getName());
            }
            JDBCResultSet dbResult = dbStat.executeQuery();
            try {
                int tableNum = maxResults;
                while (dbResult.next() && tableNum-- > 0) {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    String schemaName = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_ROUTINE_SCHEMA);
                    String procName = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_ROUTINE_NAME);
                    OracleSchema procSchema = schema != null ? schema : dataSource.getSchema(monitor, schemaName);
                    if (procSchema == null) {
                        log.debug("Procedure schema '" + schemaName + "' not found");
                        continue;
                    }
                    OracleProcedure procedure = procSchema.getProcedure(monitor, procName);
                    if (procedure == null) {
                        log.debug("Procedure '" + procName + "' not found in schema '" + procSchema.getName() + "'");
                        continue;
                    }
                    objects.add(procedure);
                }
            }
            finally {
                dbResult.close();
            }
        } finally {
            dbStat.close();
        }
    }

    private void findConstraintsByMask(JDBCExecutionContext context, OracleSchema schema, String constrNameMask, int maxResults, List<DBSObject> objects)
        throws SQLException, DBException
    {
        DBRProgressMonitor monitor = context.getProgressMonitor();

        // Load tables
        JDBCPreparedStatement dbStat = context.prepareStatement(
            "SELECT " + OracleConstants.COL_TABLE_SCHEMA + "," + OracleConstants.COL_TABLE_NAME + "," + OracleConstants.COL_CONSTRAINT_NAME + "," + OracleConstants.COL_CONSTRAINT_TYPE +
            " FROM " + OracleConstants.META_TABLE_TABLE_CONSTRAINTS + " WHERE " + OracleConstants.COL_CONSTRAINT_NAME + " LIKE ? " +
                (schema == null ? "" : " AND " + OracleConstants.COL_TABLE_SCHEMA + "=?") +
                " ORDER BY " + OracleConstants.COL_CONSTRAINT_NAME);
        try {
            dbStat.setString(1, constrNameMask.toLowerCase());
            if (schema != null) {
                dbStat.setString(2, schema.getName());
            }
            JDBCResultSet dbResult = dbStat.executeQuery();
            try {
                int tableNum = maxResults;
                while (dbResult.next() && tableNum-- > 0) {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    String schemaName = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_TABLE_SCHEMA);
                    String tableName = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_TABLE_NAME);
                    String constrName = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_CONSTRAINT_NAME);
                    String constrType = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_CONSTRAINT_TYPE);
                    OracleSchema tableSchema = schema != null ? schema : dataSource.getSchema(monitor, schemaName);
                    if (tableSchema == null) {
                        log.debug("Constraint schema '" + schemaName + "' not found");
                        continue;
                    }
                    OracleTable table = tableSchema.getTable(monitor, tableName);
                    if (table == null) {
                        log.debug("Constraint table '" + tableName + "' not found in schema '" + tableSchema.getName() + "'");
                        continue;
                    }
                    DBSObject constraint;
                    if (OracleConstants.CONSTRAINT_FOREIGN_KEY.equals(constrType)) {
                        constraint = table.getForeignKey(monitor, constrName);
                    } else {
                        constraint = table.getConstraint(monitor, constrName);
                    }
                    if (constraint == null) {
                        log.debug("Constraint '" + constrName + "' not found in table '" + table.getFullQualifiedName() + "'");
                    } else {
                        objects.add(constraint);
                    }
                }
            }
            finally {
                dbResult.close();
            }
        } finally {
            dbStat.close();
        }
    }

    private void findTableColumnsByMask(JDBCExecutionContext context, OracleSchema schema, String constrNameMask, int maxResults, List<DBSObject> objects)
        throws SQLException, DBException
    {
        DBRProgressMonitor monitor = context.getProgressMonitor();

        // Load tables
        JDBCPreparedStatement dbStat = context.prepareStatement(
            "SELECT " + OracleConstants.COL_TABLE_SCHEMA + "," + OracleConstants.COL_TABLE_NAME + "," + OracleConstants.COL_COLUMN_NAME +
            " FROM " + OracleConstants.META_TABLE_COLUMNS + " WHERE " + OracleConstants.COL_COLUMN_NAME + " LIKE ? " +
                (schema == null ? "" : " AND " + OracleConstants.COL_TABLE_SCHEMA + "=?") +
                " ORDER BY " + OracleConstants.COL_COLUMN_NAME);
        try {
            dbStat.setString(1, constrNameMask.toLowerCase());
            if (schema != null) {
                dbStat.setString(2, schema.getName());
            }
            JDBCResultSet dbResult = dbStat.executeQuery();
            try {
                int tableNum = maxResults;
                while (dbResult.next() && tableNum-- > 0) {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    String schemaName = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_TABLE_SCHEMA);
                    String tableName = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_TABLE_NAME);
                    String columnName = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_COLUMN_NAME);
                    OracleSchema tableSchema = schema != null ? schema : dataSource.getSchema(monitor, schemaName);
                    if (tableSchema == null) {
                        log.debug("Column schema '" + schemaName + "' not found");
                        continue;
                    }
                    OracleTable table = tableSchema.getTable(monitor, tableName);
                    if (table == null) {
                        log.debug("Column table '" + tableName + "' not found in schema '" + tableSchema.getName() + "'");
                        continue;
                    }
                    OracleTableColumn column = table.getColumn(monitor, columnName);
                    if (column == null) {
                        log.debug("Column '" + columnName + "' not found in table '" + table.getFullQualifiedName() + "'");
                    } else {
                        objects.add(column);
                    }
                }
            }
            finally {
                dbResult.close();
            }
        } finally {
            dbStat.close();
        }
    }

}

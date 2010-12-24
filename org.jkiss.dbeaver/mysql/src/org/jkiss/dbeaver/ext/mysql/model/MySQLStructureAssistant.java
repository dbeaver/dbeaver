/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.model.DBUtils;
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
 * MySQLStructureAssistant
 */
public class MySQLStructureAssistant extends JDBCStructureAssistant
{
    private final MySQLDataSource dataSource;

    public MySQLStructureAssistant(MySQLDataSource dataSource)
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
            RelationalObjectType.TYPE_INDEX,
            RelationalObjectType.TYPE_PROCEDURE,
            RelationalObjectType.TYPE_TABLE_COLUMN,
            };
    }

    @Override
    protected void findObjectsByMask(JDBCExecutionContext context, DBSObjectType objectType, DBSObject parentObject, String objectNameMask, int maxResults, List<DBSObject> objects) throws DBException, SQLException
    {
        MySQLCatalog catalog = parentObject instanceof MySQLCatalog ? (MySQLCatalog) parentObject : null;
        if (objectType == RelationalObjectType.TYPE_TABLE) {
            findTablesByMask(context, catalog, objectNameMask, maxResults, objects);
        } else if (objectType == RelationalObjectType.TYPE_PROCEDURE) {
            findProceduresByMask(context, catalog, objectNameMask, maxResults, objects);
        }
    }

    private void findTablesByMask(JDBCExecutionContext context, MySQLCatalog catalog, String tableNameMask, int maxResults, List<DBSObject> objects)
        throws SQLException, DBException
    {
        DBRProgressMonitor monitor = context.getProgressMonitor();

        // Load tables
        JDBCPreparedStatement dbStat = context.prepareStatement(
            "SELECT " + MySQLConstants.COL_TABLE_SCHEMA + "," + MySQLConstants.COL_TABLE_NAME +
            " FROM " + MySQLConstants.META_TABLE_TABLES + " WHERE " + MySQLConstants.COL_TABLE_NAME + " LIKE ? " +
                (catalog == null ? "" : " AND " + MySQLConstants.COL_TABLE_SCHEMA + "=?") +
                " ORDER BY " + MySQLConstants.COL_TABLE_NAME);
        try {
            dbStat.setString(1, tableNameMask.toLowerCase());
            if (catalog != null) {
                dbStat.setString(2, catalog.getName());
            }
            JDBCResultSet dbResult = dbStat.executeQuery();
            try {
                int tableNum = maxResults;
                while (dbResult.next() && tableNum-- > 0) {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    String catalogName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_TABLE_SCHEMA);
                    String tableName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_TABLE_NAME);
                    MySQLCatalog tableCatalog = catalog != null ? catalog : dataSource.getCatalog(catalogName);
                    if (tableCatalog == null) {
                        log.debug("Table catalog '" + catalogName + "' not found");
                        continue;
                    }
                    MySQLTable table = tableCatalog.getTable(monitor, tableName);
                    if (table == null) {
                        log.debug("Table '" + tableName + "' not found in catalog '" + tableCatalog.getName() + "'");
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

    private void findProceduresByMask(JDBCExecutionContext context, MySQLCatalog catalog, String procNameMask, int maxResults, List<DBSObject> objects)
        throws SQLException, DBException
    {
        DBRProgressMonitor monitor = context.getProgressMonitor();

        // Load tables
        JDBCPreparedStatement dbStat = context.prepareStatement(
            "SELECT " + MySQLConstants.COL_ROUTINE_SCHEMA + "," + MySQLConstants.COL_ROUTINE_NAME +
            " FROM " + MySQLConstants.META_TABLE_ROUTINES + " WHERE " + MySQLConstants.COL_ROUTINE_NAME + " LIKE ? " +
                (catalog == null ? "" : " AND " + MySQLConstants.COL_ROUTINE_SCHEMA + "=?") +
                " ORDER BY " + MySQLConstants.COL_ROUTINE_NAME);
        try {
            dbStat.setString(1, procNameMask.toLowerCase());
            if (catalog != null) {
                dbStat.setString(2, catalog.getName());
            }
            JDBCResultSet dbResult = dbStat.executeQuery();
            try {
                int tableNum = maxResults;
                while (dbResult.next() && tableNum-- > 0) {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    String catalogName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_ROUTINE_SCHEMA);
                    String procName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_ROUTINE_NAME);
                    MySQLCatalog procCatalog = catalog != null ? catalog : dataSource.getCatalog(catalogName);
                    if (procCatalog == null) {
                        log.debug("Procedure catalog '" + catalogName + "' not found");
                        continue;
                    }
                    MySQLProcedure procedure = procCatalog.getProcedure(monitor, procName);
                    if (procedure == null) {
                        log.debug("Procedure '" + procName + "' not found in catalog '" + procCatalog.getName() + "'");
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

}

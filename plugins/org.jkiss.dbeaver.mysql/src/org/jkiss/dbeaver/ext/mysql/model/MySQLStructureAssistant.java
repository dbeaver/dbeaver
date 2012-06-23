/*
 *
 *  * Copyright (C) 2010-2012 Serge Rieder
 *  * serge@jkiss.org
 *  *
 *  * This library is free software; you can redistribute it and/or
 *  * modify it under the terms of the GNU Lesser General Public
 *  * License as published by the Free Software Foundation; either
 *  * version 2.1 of the License, or (at your option) any later version.
 *  *
 *  * This library is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  * Lesser General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public
 *  * License along with this library; if not, write to the Free Software
 *  * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
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

    @Override
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
        MySQLCatalog catalog = parentObject instanceof MySQLCatalog ? (MySQLCatalog) parentObject : null;
        if (objectType == RelationalObjectType.TYPE_TABLE) {
            findTablesByMask(context, catalog, objectNameMask, maxResults, objects);
        } else if (objectType == RelationalObjectType.TYPE_CONSTRAINT) {
            findConstraintsByMask(context, catalog, objectNameMask, maxResults, objects);
        } else if (objectType == RelationalObjectType.TYPE_PROCEDURE) {
            findProceduresByMask(context, catalog, objectNameMask, maxResults, objects);
        } else if (objectType == RelationalObjectType.TYPE_TABLE_COLUMN) {
            findTableColumnsByMask(context, catalog, objectNameMask, maxResults, objects);
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

    private void findConstraintsByMask(JDBCExecutionContext context, MySQLCatalog catalog, String constrNameMask, int maxResults, List<DBSObject> objects)
        throws SQLException, DBException
    {
        DBRProgressMonitor monitor = context.getProgressMonitor();

        // Load tables
        JDBCPreparedStatement dbStat = context.prepareStatement(
            "SELECT " + MySQLConstants.COL_TABLE_SCHEMA + "," + MySQLConstants.COL_TABLE_NAME + "," + MySQLConstants.COL_CONSTRAINT_NAME + "," + MySQLConstants.COL_CONSTRAINT_TYPE +
            " FROM " + MySQLConstants.META_TABLE_TABLE_CONSTRAINTS + " WHERE " + MySQLConstants.COL_CONSTRAINT_NAME + " LIKE ? " +
                (catalog == null ? "" : " AND " + MySQLConstants.COL_TABLE_SCHEMA + "=?") +
                " ORDER BY " + MySQLConstants.COL_CONSTRAINT_NAME);
        try {
            dbStat.setString(1, constrNameMask.toLowerCase());
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
                    String constrName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_CONSTRAINT_NAME);
                    String constrType = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_CONSTRAINT_TYPE);
                    MySQLCatalog tableCatalog = catalog != null ? catalog : dataSource.getCatalog(catalogName);
                    if (tableCatalog == null) {
                        log.debug("Constraint catalog '" + catalogName + "' not found");
                        continue;
                    }
                    MySQLTable table = tableCatalog.getTable(monitor, tableName);
                    if (table == null) {
                        log.debug("Constraint table '" + tableName + "' not found in catalog '" + tableCatalog.getName() + "'");
                        continue;
                    }
                    DBSObject constraint;
                    if (MySQLConstants.CONSTRAINT_FOREIGN_KEY.equals(constrType)) {
                        constraint = table.getAssociation(monitor, constrName);
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

    private void findTableColumnsByMask(JDBCExecutionContext context, MySQLCatalog catalog, String constrNameMask, int maxResults, List<DBSObject> objects)
        throws SQLException, DBException
    {
        DBRProgressMonitor monitor = context.getProgressMonitor();

        // Load tables
        JDBCPreparedStatement dbStat = context.prepareStatement(
            "SELECT " + MySQLConstants.COL_TABLE_SCHEMA + "," + MySQLConstants.COL_TABLE_NAME + "," + MySQLConstants.COL_COLUMN_NAME +
            " FROM " + MySQLConstants.META_TABLE_COLUMNS + " WHERE " + MySQLConstants.COL_COLUMN_NAME + " LIKE ? " +
                (catalog == null ? "" : " AND " + MySQLConstants.COL_TABLE_SCHEMA + "=?") +
                " ORDER BY " + MySQLConstants.COL_COLUMN_NAME);
        try {
            dbStat.setString(1, constrNameMask.toLowerCase());
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
                    String columnName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_COLUMN_NAME);
                    MySQLCatalog tableCatalog = catalog != null ? catalog : dataSource.getCatalog(catalogName);
                    if (tableCatalog == null) {
                        log.debug("Column catalog '" + catalogName + "' not found");
                        continue;
                    }
                    MySQLTable table = tableCatalog.getTable(monitor, tableName);
                    if (table == null) {
                        log.debug("Column table '" + tableName + "' not found in catalog '" + tableCatalog.getName() + "'");
                        continue;
                    }
                    MySQLTableColumn column = table.getColumn(monitor, columnName);
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

/*
 * Copyright (C) 2010-2015 Serge Rieder
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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mssql.MSSQLConstants;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCStructureAssistant;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractObjectReference;
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectReference;
import org.jkiss.dbeaver.model.struct.DBSObjectType;

import java.sql.SQLException;
import java.util.List;

/**
 * MSSQLStructureAssistant
 */
public class MSSQLStructureAssistant extends JDBCStructureAssistant
{
    private final MSSQLDataSource dataSource;

    public MSSQLStructureAssistant(MSSQLDataSource dataSource)
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
    protected void findObjectsByMask(JDBCSession session, DBSObjectType objectType, DBSObject parentObject, String objectNameMask, boolean caseSensitive, int maxResults, List<DBSObjectReference> references) throws DBException, SQLException
    {
        MSSQLCatalog catalog = parentObject instanceof MSSQLCatalog ? (MSSQLCatalog) parentObject : null;
        if (objectType == RelationalObjectType.TYPE_TABLE) {
            findTablesByMask(session, catalog, objectNameMask, maxResults, references);
        } else if (objectType == RelationalObjectType.TYPE_CONSTRAINT) {
            findConstraintsByMask(session, catalog, objectNameMask, maxResults, references);
        } else if (objectType == RelationalObjectType.TYPE_PROCEDURE) {
            findProceduresByMask(session, catalog, objectNameMask, maxResults, references);
        } else if (objectType == RelationalObjectType.TYPE_TABLE_COLUMN) {
            findTableColumnsByMask(session, catalog, objectNameMask, maxResults, references);
        }
    }

    private void findTablesByMask(JDBCSession session, final MSSQLCatalog catalog, String tableNameMask, int maxResults, List<DBSObjectReference> objects)
        throws SQLException, DBException
    {
        DBRProgressMonitor monitor = session.getProgressMonitor();

        // Load tables
        JDBCPreparedStatement dbStat = session.prepareStatement(
            "SELECT " + MSSQLConstants.COL_TABLE_SCHEMA + "," + MSSQLConstants.COL_TABLE_NAME +
            " FROM " + MSSQLConstants.META_TABLE_TABLES + " WHERE " + MSSQLConstants.COL_TABLE_NAME + " LIKE ? " +
                (catalog == null ? "" : " AND " + MSSQLConstants.COL_TABLE_SCHEMA + "=?") +
                " ORDER BY " + MSSQLConstants.COL_TABLE_NAME + " LIMIT " + maxResults);
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
                    final String catalogName = JDBCUtils.safeGetString(dbResult, MSSQLConstants.COL_TABLE_SCHEMA);
                    final String tableName = JDBCUtils.safeGetString(dbResult, MSSQLConstants.COL_TABLE_NAME);
                    objects.add(new AbstractObjectReference(tableName, dataSource.getCatalog(catalogName), null, RelationalObjectType.TYPE_TABLE) {
                        @Override
                        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException
                        {
                            MSSQLCatalog tableCatalog = catalog != null ? catalog : dataSource.getCatalog(catalogName);
                            if (tableCatalog == null) {
                                throw new DBException("Table catalog '" + catalogName + "' not found");
                            }
                            MSSQLTableBase table = tableCatalog.getTableCache().getObject(monitor, tableCatalog, tableName);
                            if (table == null) {
                                throw new DBException("Table '" + tableName + "' not found in catalog '" + catalogName + "'");
                            }
                            return table;
                        }
                    });
                }
            }
            finally {
                dbResult.close();
            }
        } finally {
            dbStat.close();
        }
    }

    private void findProceduresByMask(JDBCSession session, final MSSQLCatalog catalog, String procNameMask, int maxResults, List<DBSObjectReference> objects)
        throws SQLException, DBException
    {
        DBRProgressMonitor monitor = session.getProgressMonitor();

        // Load procedures
        JDBCPreparedStatement dbStat = session.prepareStatement(
            "SELECT " + MSSQLConstants.COL_ROUTINE_SCHEMA + "," + MSSQLConstants.COL_ROUTINE_NAME +
            " FROM " + MSSQLConstants.META_TABLE_ROUTINES + " WHERE " + MSSQLConstants.COL_ROUTINE_NAME + " LIKE ? " +
                (catalog == null ? "" : " AND " + MSSQLConstants.COL_ROUTINE_SCHEMA + "=?") +
                " ORDER BY " + MSSQLConstants.COL_ROUTINE_NAME + " LIMIT " + maxResults);
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
                    final String catalogName = JDBCUtils.safeGetString(dbResult, MSSQLConstants.COL_ROUTINE_SCHEMA);
                    final String procName = JDBCUtils.safeGetString(dbResult, MSSQLConstants.COL_ROUTINE_NAME);
                    objects.add(new AbstractObjectReference(procName, dataSource.getCatalog(catalogName), null, RelationalObjectType.TYPE_PROCEDURE) {
                        @Override
                        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException
                        {
                            MSSQLCatalog procCatalog = catalog != null ? catalog : dataSource.getCatalog(catalogName);
                            if (procCatalog == null) {
                                throw new DBException("Procedure catalog '" + catalogName + "' not found");
                            }
                            MSSQLProcedure procedure = procCatalog.getProcedure(monitor, procName);
                            if (procedure == null) {
                                throw new DBException("Procedure '" + procName + "' not found in catalog '" + procCatalog.getName() + "'");
                            }
                            return procedure;
                        }
                    });
                }
            }
            finally {
                dbResult.close();
            }
        } finally {
            dbStat.close();
        }
    }

    private void findConstraintsByMask(JDBCSession session, final MSSQLCatalog catalog, String constrNameMask, int maxResults, List<DBSObjectReference> objects)
        throws SQLException, DBException
    {
        DBRProgressMonitor monitor = session.getProgressMonitor();

        // Load constraints
        JDBCPreparedStatement dbStat = session.prepareStatement(
            "SELECT " + MSSQLConstants.COL_TABLE_SCHEMA + "," + MSSQLConstants.COL_TABLE_NAME + "," + MSSQLConstants.COL_CONSTRAINT_NAME + "," + MSSQLConstants.COL_CONSTRAINT_TYPE +
            " FROM " + MSSQLConstants.META_TABLE_TABLE_CONSTRAINTS + " WHERE " + MSSQLConstants.COL_CONSTRAINT_NAME + " LIKE ? " +
                (catalog == null ? "" : " AND " + MSSQLConstants.COL_TABLE_SCHEMA + "=?") +
                " ORDER BY " + MSSQLConstants.COL_CONSTRAINT_NAME + " LIMIT " + maxResults);
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
                    final String catalogName = JDBCUtils.safeGetString(dbResult, MSSQLConstants.COL_TABLE_SCHEMA);
                    final String tableName = JDBCUtils.safeGetString(dbResult, MSSQLConstants.COL_TABLE_NAME);
                    final String constrName = JDBCUtils.safeGetString(dbResult, MSSQLConstants.COL_CONSTRAINT_NAME);
                    final String constrType = JDBCUtils.safeGetString(dbResult, MSSQLConstants.COL_CONSTRAINT_TYPE);
                    objects.add(new AbstractObjectReference(constrName, dataSource.getCatalog(catalogName), null, RelationalObjectType.TYPE_CONSTRAINT) {
                        @Override
                        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException
                        {
                            MSSQLCatalog tableCatalog = catalog != null ? catalog : dataSource.getCatalog(catalogName);
                            if (tableCatalog == null) {
                                throw new DBException("Constraint catalog '" + catalogName + "' not found");
                            }
                            MSSQLTable table = tableCatalog.getTable(monitor, tableName);
                            if (table == null) {
                                throw new DBException("Constraint table '" + tableName + "' not found in catalog '" + tableCatalog.getName() + "'");
                            }
                            DBSObject constraint;
                            if (MSSQLConstants.CONSTRAINT_FOREIGN_KEY.equals(constrType)) {
                                constraint = table.getAssociation(monitor, constrName);
                            } else {
                                constraint = table.getConstraint(monitor, constrName);
                            }
                            if (constraint == null) {
                                throw new DBException("Constraint '" + constrName + "' not found in table '" + table.getFullQualifiedName() + "'");
                            }
                            return constraint;
                        }
                    });
                }
            }
            finally {
                dbResult.close();
            }
        } finally {
            dbStat.close();
        }
    }

    private void findTableColumnsByMask(JDBCSession session, final MSSQLCatalog catalog, String constrNameMask, int maxResults, List<DBSObjectReference> objects)
        throws SQLException, DBException
    {
        DBRProgressMonitor monitor = session.getProgressMonitor();

        // Load columns
        JDBCPreparedStatement dbStat = session.prepareStatement(
            "SELECT " + MSSQLConstants.COL_TABLE_SCHEMA + "," + MSSQLConstants.COL_TABLE_NAME + "," + MSSQLConstants.COL_COLUMN_NAME +
            " FROM " + MSSQLConstants.META_TABLE_COLUMNS + " WHERE " + MSSQLConstants.COL_COLUMN_NAME + " LIKE ? " +
                (catalog == null ? "" : " AND " + MSSQLConstants.COL_TABLE_SCHEMA + "=?") +
                " ORDER BY " + MSSQLConstants.COL_COLUMN_NAME + " LIMIT " + maxResults);
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
                    final String catalogName = JDBCUtils.safeGetString(dbResult, MSSQLConstants.COL_TABLE_SCHEMA);
                    final String tableName = JDBCUtils.safeGetString(dbResult, MSSQLConstants.COL_TABLE_NAME);
                    final String columnName = JDBCUtils.safeGetString(dbResult, MSSQLConstants.COL_COLUMN_NAME);
                    objects.add(new AbstractObjectReference(columnName, dataSource.getCatalog(catalogName), null, RelationalObjectType.TYPE_TABLE_COLUMN) {
                        @Override
                        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException
                        {
                            MSSQLCatalog tableCatalog = catalog != null ? catalog : dataSource.getCatalog(catalogName);
                            if (tableCatalog == null) {
                                throw new DBException("Column catalog '" + catalogName + "' not found");
                            }
                            MSSQLTable table = tableCatalog.getTable(monitor, tableName);
                            if (table == null) {
                                throw new DBException("Column table '" + tableName + "' not found in catalog '" + tableCatalog.getName() + "'");
                            }
                            MSSQLTableColumn column = table.getAttribute(monitor, columnName);
                            if (column == null) {
                                throw new DBException("Column '" + columnName + "' not found in table '" + table.getFullQualifiedName() + "'");
                            }
                            return column;
                        }
                    });
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

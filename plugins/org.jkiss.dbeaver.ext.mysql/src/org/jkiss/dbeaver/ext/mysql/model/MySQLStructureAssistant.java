/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
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
import java.util.Locale;

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
    public DBSObjectType[] getHyperlinkObjectTypes()
    {
        return new DBSObjectType[] {
            RelationalObjectType.TYPE_TABLE,
            RelationalObjectType.TYPE_PROCEDURE
        };
    }

    @Override
    public DBSObjectType[] getAutoCompleteObjectTypes()
    {
        return new DBSObjectType[] {
            RelationalObjectType.TYPE_TABLE,
            RelationalObjectType.TYPE_PROCEDURE,
        };
    }

    @Override
    protected void findObjectsByMask(JDBCSession session, DBSObjectType objectType, DBSObject parentObject, String objectNameMask, boolean caseSensitive, int maxResults, List<DBSObjectReference> references) throws DBException, SQLException
    {
        MySQLCatalog catalog = parentObject instanceof MySQLCatalog ? (MySQLCatalog) parentObject : null;
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

    private void findTablesByMask(JDBCSession session, @Nullable final MySQLCatalog catalog, String tableNameMask, int maxResults, List<DBSObjectReference> objects)
        throws SQLException, DBException
    {
        DBRProgressMonitor monitor = session.getProgressMonitor();

        // Load tables
        try (JDBCPreparedStatement dbStat = session.prepareStatement(
            "SELECT " + MySQLConstants.COL_TABLE_SCHEMA + "," + MySQLConstants.COL_TABLE_NAME +
                " FROM " + MySQLConstants.META_TABLE_TABLES + " WHERE " + MySQLConstants.COL_TABLE_NAME + " LIKE ? " +
                (catalog == null ? "" : " AND " + MySQLConstants.COL_TABLE_SCHEMA + "=?") +
                " ORDER BY " + MySQLConstants.COL_TABLE_NAME + " LIMIT " + maxResults)) {
            dbStat.setString(1, tableNameMask.toLowerCase(Locale.ENGLISH));
            if (catalog != null) {
                dbStat.setString(2, catalog.getName());
            }
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                int tableNum = maxResults;
                while (dbResult.next() && tableNum-- > 0) {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    final String catalogName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_TABLE_SCHEMA);
                    final String tableName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_TABLE_NAME);
                    objects.add(new AbstractObjectReference(tableName, dataSource.getCatalog(catalogName), null, RelationalObjectType.TYPE_TABLE) {
                        @Override
                        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
                            MySQLCatalog tableCatalog = catalog != null ? catalog : dataSource.getCatalog(catalogName);
                            if (tableCatalog == null) {
                                throw new DBException("Table catalog '" + catalogName + "' not found");
                            }
                            MySQLTableBase table = tableCatalog.getTableCache().getObject(monitor, tableCatalog, tableName);
                            if (table == null) {
                                throw new DBException("Table '" + tableName + "' not found in catalog '" + catalogName + "'");
                            }
                            return table;
                        }
                    });
                }
            }
        }
    }

    private void findProceduresByMask(JDBCSession session, @Nullable final MySQLCatalog catalog, String procNameMask, int maxResults, List<DBSObjectReference> objects)
        throws SQLException, DBException
    {
        DBRProgressMonitor monitor = session.getProgressMonitor();

        // Load procedures
        try (JDBCPreparedStatement dbStat = session.prepareStatement(
            "SELECT " + MySQLConstants.COL_ROUTINE_SCHEMA + "," + MySQLConstants.COL_ROUTINE_NAME +
                " FROM " + MySQLConstants.META_TABLE_ROUTINES + " WHERE " + MySQLConstants.COL_ROUTINE_NAME + " LIKE ? " +
                (catalog == null ? "" : " AND " + MySQLConstants.COL_ROUTINE_SCHEMA + "=?") +
                " ORDER BY " + MySQLConstants.COL_ROUTINE_NAME + " LIMIT " + maxResults)) {
            dbStat.setString(1, procNameMask.toLowerCase(Locale.ENGLISH));
            if (catalog != null) {
                dbStat.setString(2, catalog.getName());
            }
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                int tableNum = maxResults;
                while (dbResult.next() && tableNum-- > 0) {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    final String catalogName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_ROUTINE_SCHEMA);
                    final String procName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_ROUTINE_NAME);
                    objects.add(new AbstractObjectReference(procName, dataSource.getCatalog(catalogName), null, RelationalObjectType.TYPE_PROCEDURE) {
                        @Override
                        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
                            MySQLCatalog procCatalog = catalog != null ? catalog : dataSource.getCatalog(catalogName);
                            if (procCatalog == null) {
                                throw new DBException("Procedure catalog '" + catalogName + "' not found");
                            }
                            MySQLProcedure procedure = procCatalog.getProcedure(monitor, procName);
                            if (procedure == null) {
                                throw new DBException("Procedure '" + procName + "' not found in catalog '" + procCatalog.getName() + "'");
                            }
                            return procedure;
                        }
                    });
                }
            }
        }
    }

    private void findConstraintsByMask(JDBCSession session, @Nullable final MySQLCatalog catalog, String constrNameMask, int maxResults, List<DBSObjectReference> objects)
        throws SQLException, DBException
    {
        DBRProgressMonitor monitor = session.getProgressMonitor();

        // Load constraints
        try (JDBCPreparedStatement dbStat = session.prepareStatement(
            "SELECT " + MySQLConstants.COL_TABLE_SCHEMA + "," + MySQLConstants.COL_TABLE_NAME + "," + MySQLConstants.COL_CONSTRAINT_NAME + "," + MySQLConstants.COL_CONSTRAINT_TYPE +
                " FROM " + MySQLConstants.META_TABLE_TABLE_CONSTRAINTS + " WHERE " + MySQLConstants.COL_CONSTRAINT_NAME + " LIKE ? " +
                (catalog == null ? "" : " AND " + MySQLConstants.COL_TABLE_SCHEMA + "=?") +
                " ORDER BY " + MySQLConstants.COL_CONSTRAINT_NAME + " LIMIT " + maxResults)) {
            dbStat.setString(1, constrNameMask.toLowerCase(Locale.ENGLISH));
            if (catalog != null) {
                dbStat.setString(2, catalog.getName());
            }
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                int tableNum = maxResults;
                while (dbResult.next() && tableNum-- > 0) {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    final String catalogName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_TABLE_SCHEMA);
                    final String tableName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_TABLE_NAME);
                    final String constrName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_CONSTRAINT_NAME);
                    final String constrType = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_CONSTRAINT_TYPE);
                    objects.add(new AbstractObjectReference(constrName, dataSource.getCatalog(catalogName), null, RelationalObjectType.TYPE_CONSTRAINT) {
                        @Override
                        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
                            MySQLCatalog tableCatalog = catalog != null ? catalog : dataSource.getCatalog(catalogName);
                            if (tableCatalog == null) {
                                throw new DBException("Constraint catalog '" + catalogName + "' not found");
                            }
                            MySQLTable table = tableCatalog.getTable(monitor, tableName);
                            if (table == null) {
                                throw new DBException("Constraint table '" + tableName + "' not found in catalog '" + tableCatalog.getName() + "'");
                            }
                            DBSObject constraint;
                            if (MySQLConstants.CONSTRAINT_FOREIGN_KEY.equals(constrType)) {
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
        }
    }

    private void findTableColumnsByMask(JDBCSession session, @Nullable final MySQLCatalog catalog, String constrNameMask, int maxResults, List<DBSObjectReference> objects)
        throws SQLException, DBException
    {
        DBRProgressMonitor monitor = session.getProgressMonitor();

        // Load columns
        try (JDBCPreparedStatement dbStat = session.prepareStatement(
            "SELECT " + MySQLConstants.COL_TABLE_SCHEMA + "," + MySQLConstants.COL_TABLE_NAME + "," + MySQLConstants.COL_COLUMN_NAME +
                " FROM " + MySQLConstants.META_TABLE_COLUMNS + " WHERE " + MySQLConstants.COL_COLUMN_NAME + " LIKE ? " +
                (catalog == null ? "" : " AND " + MySQLConstants.COL_TABLE_SCHEMA + "=?") +
                " ORDER BY " + MySQLConstants.COL_COLUMN_NAME + " LIMIT " + maxResults)) {
            dbStat.setString(1, constrNameMask.toLowerCase(Locale.ENGLISH));
            if (catalog != null) {
                dbStat.setString(2, catalog.getName());
            }
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                int tableNum = maxResults;
                while (dbResult.next() && tableNum-- > 0) {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    final String catalogName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_TABLE_SCHEMA);
                    final String tableName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_TABLE_NAME);
                    final String columnName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_COLUMN_NAME);
                    objects.add(new AbstractObjectReference(columnName, dataSource.getCatalog(catalogName), null, RelationalObjectType.TYPE_TABLE_COLUMN) {
                        @NotNull
                        @Override
                        public String getFullQualifiedName() {
                            return DBUtils.getQuotedIdentifier(dataSource, catalogName) +
                                '.' +
                                DBUtils.getQuotedIdentifier(dataSource, tableName) +
                                '.' +
                                DBUtils.getQuotedIdentifier(dataSource, columnName);

                        }

                        @Override
                        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
                            MySQLCatalog tableCatalog = catalog != null ? catalog : dataSource.getCatalog(catalogName);
                            if (tableCatalog == null) {
                                throw new DBException("Column catalog '" + catalogName + "' not found");
                            }
                            MySQLTableBase table = tableCatalog.getTableCache().getObject(monitor, tableCatalog, tableName);
                            if (table == null) {
                                throw new DBException("Column table '" + tableName + "' not found in catalog '" + tableCatalog.getName() + "'");
                            }
                            MySQLTableColumn column = table.getAttribute(monitor, columnName);
                            if (column == null) {
                                throw new DBException("Column '" + columnName + "' not found in table '" + table.getFullQualifiedName() + "'");
                            }
                            return column;
                        }
                    });
                }
            }
        }
    }

}

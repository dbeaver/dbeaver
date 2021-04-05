/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
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
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * MySQLStructureAssistant
 */
public class MySQLStructureAssistant extends JDBCStructureAssistant<MySQLExecutionContext> {
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
    protected void findObjectsByMask(@NotNull MySQLExecutionContext executionContext, @NotNull JDBCSession session, @NotNull DBSObjectType objectType,
                                     @NotNull ObjectsSearchParams params, @NotNull List<DBSObjectReference> references)
                                        throws SQLException {
        MySQLCatalog catalog = params.getParentObject() instanceof MySQLCatalog ? (MySQLCatalog) params.getParentObject() : null;
        if (catalog == null && !params.isGlobalSearch()) {
            catalog = executionContext.getContextDefaults().getDefaultCatalog();
        }
        if (objectType == RelationalObjectType.TYPE_TABLE) {
            findTablesByMask(session, catalog, params, references);
        } else if (objectType == RelationalObjectType.TYPE_CONSTRAINT) {
            findConstraintsByMask(session, catalog, params, references);
        } else if (objectType == RelationalObjectType.TYPE_PROCEDURE) {
            findProceduresByMask(session, catalog, params, references);
        } else if (objectType == RelationalObjectType.TYPE_TABLE_COLUMN) {
            findTableColumnsByMask(session, catalog, params, references);
        }
    }

    private void findTablesByMask(JDBCSession session, @Nullable final MySQLCatalog catalog, @NotNull ObjectsSearchParams params,
                                  List<DBSObjectReference> objects) throws SQLException {
        DBRProgressMonitor monitor = session.getProgressMonitor();

        String sql = generateQuery(
            params,
            MySQLConstants.COL_TABLE_NAME,
            "TABLE_COMMENT",
            MySQLConstants.COL_TABLE_SCHEMA,
            MySQLConstants.COL_TABLE_SCHEMA + "," + MySQLConstants.COL_TABLE_NAME,
            MySQLConstants.META_TABLE_TABLES,
            catalog,
            objects
        );

        // Load tables
        try (JDBCPreparedStatement dbStat = session.prepareStatement(sql)) {
            fillParameters(dbStat, params, catalog, true);
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (!monitor.isCanceled() && dbResult.next()) {
                    final String catalogName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_TABLE_SCHEMA);
                    final String tableName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_TABLE_NAME);
                    objects.add(new AbstractObjectReference(tableName, dataSource.getCatalog(catalogName), null, MySQLTableBase.class, RelationalObjectType.TYPE_TABLE) {
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

    private void findProceduresByMask(JDBCSession session, @Nullable final MySQLCatalog catalog, @NotNull ObjectsSearchParams params,
                                      List<DBSObjectReference> objects) throws SQLException {
        DBRProgressMonitor monitor = session.getProgressMonitor();

        String sql = generateQuery(
                params,
                MySQLConstants.COL_ROUTINE_NAME,
                "ROUTINE_COMMENT",
                MySQLConstants.COL_ROUTINE_SCHEMA,
                MySQLConstants.COL_ROUTINE_SCHEMA + "," + MySQLConstants.COL_ROUTINE_NAME,
                MySQLConstants.META_TABLE_ROUTINES,
                catalog,
                objects
        );

        // Load procedures
        try (JDBCPreparedStatement dbStat = session.prepareStatement(sql)) {
            fillParameters(dbStat, params, catalog, true);
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (!monitor.isCanceled() && dbResult.next()) {
                    final String catalogName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_ROUTINE_SCHEMA);
                    final String procName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_ROUTINE_NAME);
                    objects.add(new AbstractObjectReference(procName, dataSource.getCatalog(catalogName), null, MySQLProcedure.class, RelationalObjectType.TYPE_PROCEDURE) {
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

    private void findConstraintsByMask(JDBCSession session, @Nullable final MySQLCatalog catalog, @NotNull ObjectsSearchParams params,
                                       List<DBSObjectReference> objects) throws SQLException {
        DBRProgressMonitor monitor = session.getProgressMonitor();

        String sql = generateQuery(
                params,
                MySQLConstants.COL_CONSTRAINT_NAME,
                null,
                MySQLConstants.COL_TABLE_SCHEMA,
                MySQLConstants.COL_TABLE_SCHEMA + "," + MySQLConstants.COL_TABLE_NAME + "," + MySQLConstants.COL_CONSTRAINT_NAME + "," + MySQLConstants.COL_CONSTRAINT_TYPE,
                MySQLConstants.META_TABLE_TABLE_CONSTRAINTS,
                catalog,
                objects
        );

        // Load constraints
        try (JDBCPreparedStatement dbStat = session.prepareStatement(sql)) {
            fillParameters(dbStat, params, catalog, false);
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (!monitor.isCanceled() && dbResult.next()) {
                    final String catalogName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_TABLE_SCHEMA);
                    final String tableName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_TABLE_NAME);
                    final String constrName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_CONSTRAINT_NAME);
                    final String constrType = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_CONSTRAINT_TYPE);
                    final boolean isFK = MySQLConstants.CONSTRAINT_FOREIGN_KEY.equals(constrType);
                    final boolean isCheck = MySQLConstants.CONSTRAINT_CHECK.equals(constrType);
                    objects.add(new AbstractObjectReference(constrName, dataSource.getCatalog(catalogName), null, isFK ? MySQLTableForeignKey.class : MySQLTableConstraint.class, RelationalObjectType.TYPE_CONSTRAINT) {
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
                            if (isFK) {
                                constraint = table.getAssociation(monitor, constrName);
                            }
                            else if (isCheck) {
                                constraint = table.getCheckConstraint(monitor, constrName);
                            }
                            else {
                                constraint = table.getUniqueKey(monitor, constrName);
                            }
                            if (constraint == null) {
                                throw new DBException("Constraint '" + constrName + "' not found in table '" + table.getFullyQualifiedName(DBPEvaluationContext.DDL) + "'");
                            }
                            return constraint;
                        }
                    });
                }
            }
        }
    }

    private void findTableColumnsByMask(JDBCSession session, @Nullable final MySQLCatalog catalog, @NotNull ObjectsSearchParams params,
                                        List<DBSObjectReference> objects) throws SQLException {
        DBRProgressMonitor monitor = session.getProgressMonitor();

        String sql = generateQuery(
                params,
                MySQLConstants.COL_COLUMN_NAME,
                "COLUMN_COMMENT",
                MySQLConstants.COL_TABLE_SCHEMA,
                MySQLConstants.COL_TABLE_SCHEMA + "," + MySQLConstants.COL_TABLE_NAME + "," + MySQLConstants.COL_COLUMN_NAME,
                MySQLConstants.META_TABLE_COLUMNS,
                catalog,
                objects
        );

        // Load columns
        try (JDBCPreparedStatement dbStat = session.prepareStatement(sql)) {
            fillParameters(dbStat, params, catalog, true);
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (!monitor.isCanceled() && dbResult.next()) {
                    final String catalogName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_TABLE_SCHEMA);
                    final String tableName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_TABLE_NAME);
                    final String columnName = JDBCUtils.safeGetString(dbResult, MySQLConstants.COL_COLUMN_NAME);
                    objects.add(new AbstractObjectReference(columnName, dataSource.getCatalog(catalogName), null, MySQLTableColumn.class, RelationalObjectType.TYPE_TABLE_COLUMN) {
                        @NotNull
                        @Override
                        public String getFullyQualifiedName(DBPEvaluationContext context) {
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
                                throw new DBException("Column '" + columnName + "' not found in table '" + table.getFullyQualifiedName(DBPEvaluationContext.DDL) + "'");
                            }
                            return column;
                        }
                    });
                }
            }
        }
    }

    private static String generateQuery(@NotNull ObjectsSearchParams params, @NotNull String objectNameColumn, @Nullable String commentColumnName,
                                        @NotNull String schemaColumnName, @NotNull String select, @NotNull String from,
                                        @Nullable MySQLCatalog catalog, @NotNull Collection<DBSObjectReference> references) {
        StringBuilder sql = new StringBuilder("SELECT ").append(select).append(" FROM ").append(from).append(" WHERE ");
        if (params.isSearchInComments() && commentColumnName != null) {
            sql.append("(");
        }
        sql.append(objectNameColumn).append(" LIKE ? ");
        if (params.isSearchInComments() && commentColumnName != null) {
            sql.append("OR ").append(commentColumnName).append(" LIKE ?) ");
        }
        if (catalog != null) {
            sql.append("AND ").append(schemaColumnName).append(" = ? ");
        }
        sql.append("ORDER BY ").append(objectNameColumn).append(" LIMIT ").append(params.getMaxResults() - references.size());
        return sql.toString();
    }

    private static void fillParameters(@NotNull JDBCPreparedStatement statement, @NotNull ObjectsSearchParams params,
                                       @Nullable MySQLCatalog catalog, boolean hasCommentColumn) throws SQLException {
        String mask = params.getMask().toLowerCase(Locale.ENGLISH);
        statement.setString(1, mask);
        int catalogNameIdx = 2;
        if (params.isSearchInComments() && hasCommentColumn) {
            statement.setString(2, mask);
            catalogNameIdx++;
        }
        if (catalog != null) {
            statement.setString(catalogNameIdx, catalog.getName());
        }
    }
}

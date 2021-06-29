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

        QueryParams queryParams = new QueryParams(
            MySQLConstants.COL_TABLE_NAME,
            MySQLConstants.COL_TABLE_SCHEMA + "," + MySQLConstants.COL_TABLE_NAME,
            MySQLConstants.META_TABLE_TABLES
        );
        if (params.isSearchInComments()) {
            queryParams.setCommentColumnName("TABLE_COMMENT");
        }
        if (catalog != null) {
            queryParams.setSchemaColumnName(MySQLConstants.COL_TABLE_SCHEMA);
        }
        queryParams.setMaxResults(params.getMaxResults() - objects.size());
        String sql = generateQuery(queryParams);

        // Load tables
        try (JDBCPreparedStatement dbStat = session.prepareStatement(sql)) {
            fillParameters(dbStat, params, catalog, true, false);
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

        QueryParams queryParams = new QueryParams(
            MySQLConstants.COL_ROUTINE_NAME,
            MySQLConstants.COL_ROUTINE_SCHEMA + "," + MySQLConstants.COL_ROUTINE_NAME,
            MySQLConstants.META_TABLE_ROUTINES
        );
        if (params.isSearchInComments()) {
            queryParams.setCommentColumnName("ROUTINE_COMMENT");
        }
        if (catalog != null) {
            queryParams.setSchemaColumnName(MySQLConstants.COL_ROUTINE_SCHEMA);
        }
        queryParams.setMaxResults(params.getMaxResults() - objects.size());
        if (params.isSearchInDefinitions()) {
            queryParams.setDefinitionColumnName(MySQLConstants.COL_ROUTINE_DEFINITION);
        }
        String sql = generateQuery(queryParams);

        // Load procedures
        try (JDBCPreparedStatement dbStat = session.prepareStatement(sql)) {
            fillParameters(dbStat, params, catalog, true, true);
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

        QueryParams queryParams = new QueryParams(
            MySQLConstants.COL_CONSTRAINT_NAME,
            MySQLConstants.COL_TABLE_SCHEMA + "," + MySQLConstants.COL_TABLE_NAME + "," + MySQLConstants.COL_CONSTRAINT_NAME + "," + MySQLConstants.COL_CONSTRAINT_TYPE,
            MySQLConstants.META_TABLE_TABLE_CONSTRAINTS
        );
        if (catalog != null) {
            queryParams.setSchemaColumnName(MySQLConstants.COL_TABLE_SCHEMA);
        }
        queryParams.setMaxResults(params.getMaxResults() - objects.size());
        String sql = generateQuery(queryParams);

        // Load constraints
        try (JDBCPreparedStatement dbStat = session.prepareStatement(sql)) {
            fillParameters(dbStat, params, catalog, false, false);
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

        QueryParams queryParams = new QueryParams(
            MySQLConstants.COL_COLUMN_NAME,
            MySQLConstants.COL_TABLE_SCHEMA + "," + MySQLConstants.COL_TABLE_NAME + "," + MySQLConstants.COL_COLUMN_NAME,
            MySQLConstants.META_TABLE_COLUMNS
        );
        if (params.isSearchInComments()) {
            queryParams.setCommentColumnName("COLUMN_COMMENT");
        }
        if (catalog != null) {
            queryParams.setSchemaColumnName(MySQLConstants.COL_TABLE_SCHEMA);
        }
        queryParams.setMaxResults(params.getMaxResults() - objects.size());
        if (params.isSearchInDefinitions()) {
            queryParams.setDefinitionColumnName(MySQLConstants.COL_COLUMN_GENERATION_EXPRESSION);
        }
        String sql = generateQuery(queryParams);

        // Load columns
        try (JDBCPreparedStatement dbStat = session.prepareStatement(sql)) {
            fillParameters(dbStat, params, catalog, true, true);
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

    private static String generateQuery(@NotNull QueryParams params) {
        StringBuilder sql = new StringBuilder("SELECT ").append(params.getSelect()).append(" FROM ").append(params.getFrom()).append(" WHERE ");
        boolean addParentheses = params.getCommentColumnName() != null || params.getDefinitionColumnName() != null;
        if (addParentheses) {
            sql.append("(");
        }
        sql.append(params.getObjectNameColumn()).append(" LIKE ? ");
        if (params.getCommentColumnName() != null) {
            sql.append("OR ").append(params.getCommentColumnName()).append(" LIKE ?");
        }
        if (params.getDefinitionColumnName() != null) {
            sql.append(" OR ").append(params.getDefinitionColumnName()).append(" LIKE ?");
        }
        if (addParentheses) {
            sql.append(") ");
        }
        if (params.getSchemaColumnName() != null) {
            sql.append("AND ").append(params.getSchemaColumnName()).append(" = ? ");
        }
        sql.append("ORDER BY ").append(params.getObjectNameColumn()).append(" LIMIT ").append(params.getMaxResults());
        return sql.toString();
    }

    private static void fillParameters(@NotNull JDBCPreparedStatement statement, @NotNull ObjectsSearchParams params,
                                       @Nullable MySQLCatalog catalog, boolean hasCommentColumn, boolean hasDefinitionColumn) throws SQLException {
        String mask = params.getMask().toLowerCase(Locale.ENGLISH);
        statement.setString(1, mask);
        int idx = 2;
        if (params.isSearchInComments() && hasCommentColumn) {
            statement.setString(idx, mask);
            idx++;
        }
        if (params.isSearchInDefinitions() && hasDefinitionColumn) {
            statement.setString(idx, mask);
            idx++;
        }
        if (catalog != null) {
            statement.setString(idx, catalog.getName());
        }
    }

    private static final class QueryParams {
        @NotNull
        private final String objectNameColumn;

        @Nullable
        private String commentColumnName;

        @Nullable
        private String schemaColumnName;

        @NotNull
        private final String select;

        @NotNull
        private final String from;

        private int maxResults;

        @Nullable
        private String definitionColumnName;

        private QueryParams(@NotNull String objectNameColumn, @NotNull String select, @NotNull String from) {
            this.objectNameColumn = objectNameColumn;
            this.select = select;
            this.from = from;
        }

        @NotNull
        private String getObjectNameColumn() {
            return objectNameColumn;
        }

        @Nullable
        private String getCommentColumnName() {
            return commentColumnName;
        }

        private void setCommentColumnName(@Nullable String commentColumnName) {
            this.commentColumnName = commentColumnName;
        }

        @Nullable
        private String getSchemaColumnName() {
            return schemaColumnName;
        }

        private void setSchemaColumnName(@Nullable String schemaColumnName) {
            this.schemaColumnName = schemaColumnName;
        }

        @NotNull
        public String getSelect() {
            return select;
        }

        @NotNull
        public String getFrom() {
            return from;
        }

        @Nullable
        private String getDefinitionColumnName() {
            return definitionColumnName;
        }

        private void setDefinitionColumnName(@Nullable String definitionColumnName) {
            this.definitionColumnName = definitionColumnName;
        }

        private int getMaxResults() {
            return maxResults;
        }

        private void setMaxResults(int maxResults) {
            this.maxResults = maxResults;
        }
    }

    @Override
    public boolean supportsSearchInCommentsFor(@NotNull DBSObjectType objectType) {
        return objectType == RelationalObjectType.TYPE_TABLE
            || objectType == RelationalObjectType.TYPE_PROCEDURE
            || objectType == RelationalObjectType.TYPE_TABLE_COLUMN;
    }

    @Override
    public boolean supportsSearchInDefinitionsFor(@NotNull DBSObjectType objectType) {
        return objectType == RelationalObjectType.TYPE_PROCEDURE || objectType == RelationalObjectType.TYPE_TABLE_COLUMN;
    }
}

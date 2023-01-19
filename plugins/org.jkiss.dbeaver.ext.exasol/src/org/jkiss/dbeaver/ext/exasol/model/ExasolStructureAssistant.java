/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.exasol.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.exasol.ExasolSysTablePrefix;
import org.jkiss.dbeaver.ext.exasol.editors.ExasolObjectType;
import org.jkiss.dbeaver.ext.exasol.tools.ExasolUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class ExasolStructureAssistant extends JDBCStructureAssistant<ExasolExecutionContext> {
    private static final Log log = Log.getLog(ExasolStructureAssistant.class);

    private static final DBSObjectType[] SUPP_OBJ_TYPES = {ExasolObjectType.TABLE, ExasolObjectType.VIEW, ExasolObjectType.COLUMN, ExasolObjectType.SCHEMA, ExasolObjectType.SCRIPT, ExasolObjectType.FOREIGNKEY, ExasolObjectType.PRIMARYKEY};
    private static final DBSObjectType[] HYPER_LINKS_TYPES = {ExasolObjectType.TABLE, ExasolObjectType.COLUMN, ExasolObjectType.VIEW, ExasolObjectType.SCHEMA, ExasolObjectType.SCRIPT, ExasolObjectType.FOREIGNKEY, ExasolObjectType.PRIMARYKEY};
    private static final DBSObjectType[] AUTOC_OBJ_TYPES = {ExasolObjectType.TABLE, ExasolObjectType.VIEW, ExasolObjectType.COLUMN, ExasolObjectType.SCHEMA, ExasolObjectType.SCRIPT};

    private final ExasolDataSource dataSource;
    private final String sqlConstraintsSchema;
    private final String sqlConstraintsAll;

    // -----------------
    // Constructors
    // -----------------
    public ExasolStructureAssistant(ExasolDataSource dataSource) {
        this.dataSource = dataSource;
        this.sqlConstraintsAll = "/*snapshot execution*/ SELECT CONSTRAINT_SCHEMA,CONSTRAINT_TABLE, CONSTRAINT_TYPE, CONSTRAINT_NAME FROM SYS."
                + dataSource.getTablePrefix(ExasolSysTablePrefix.ALL) + "_CONSTRAINTS WHERE CONSTRAINT_TYPE <> 'NOT NULL' "
                + " AND CONSTRAINT_NAME like '%s' AND CONSTRAINT_TYPE = '%s'";
        this.sqlConstraintsSchema = sqlConstraintsAll + " AND CONSTRAINT_SCHEMA = '%s'";
    }

    // -----------------
    // Method Interface
    // -----------------
    @Override
    public DBSObjectType[] getSupportedObjectTypes() {
        return SUPP_OBJ_TYPES;
    }

    @Override
    public DBSObjectType[] getSearchObjectTypes() {
        return getSupportedObjectTypes();
    }

    @Override
    public DBSObjectType[] getHyperlinkObjectTypes() {
        return HYPER_LINKS_TYPES;
    }

    @Override
    public DBSObjectType[] getAutoCompleteObjectTypes() {
        return AUTOC_OBJ_TYPES;
    }

    @Override
    protected void findObjectsByMask(@NotNull ExasolExecutionContext executionContext, @NotNull JDBCSession session,
                                     @NotNull DBSObjectType objectType, @NotNull ObjectsSearchParams params,
                                     @NotNull List<DBSObjectReference> references) throws DBException, SQLException {
        String objectNameMask = params.getMask();
        DBSObject parentObject = params.getParentObject();
        log.debug("Search Mask:" + objectNameMask + " Object Type:" + objectType.getTypeName());

        ExasolSchema schema = parentObject instanceof ExasolSchema ? (ExasolSchema) parentObject : null;
        if (schema == null && !params.isGlobalSearch()) {
            schema = executionContext.getContextDefaults().getDefaultSchema();
        }

        if (objectType == ExasolObjectType.TABLE) {
            findTableObjectByName(session, schema, params, references);
        } else if (objectType == ExasolObjectType.VIEW) {
            findViews(session, schema, params, references);
        } else if (objectType == ExasolObjectType.FOREIGNKEY) {
            findConstraintsByMask(session, schema, params, references, "FOREIGN KEY");
        } else if (objectType == ExasolObjectType.PRIMARYKEY) {
            findConstraintsByMask(session, schema, params, references, "PRIMARY KEY");
        } else if (objectType == ExasolObjectType.SCRIPT) {
            findProceduresByMask(session, schema, params, references);
        } else if (objectType == ExasolObjectType.COLUMN) {
            findTableColumnsByMask(session, schema, params, references);
        }
    }

    private void findTableColumnsByMask(@NotNull JDBCSession session, @Nullable ExasolSchema schema, @NotNull ObjectsSearchParams params,
                                        @NotNull List<DBSObjectReference> references) throws SQLException, DBException {
        DBRProgressMonitor monitor = session.getProgressMonitor();

        //don't use parameter marks because of performance
        String schemaMask = schema == null ? "%" : ExasolUtils.quoteString(schema.getName());
        String mask = ExasolUtils.quoteString(params.getMask());
        StringBuilder sql = new StringBuilder("/*snapshot execution*/ SELECT TABLE_SCHEM,TABLE_NAME as column_table,COLUMN_NAME from \"$ODBCJDBC\".ALL_COLUMNS WHERE TABLE_SCHEM like '");
        sql.append(schemaMask).append("' AND ");
        if (params.isSearchInComments()) {
            sql.append("(");
        }
        sql.append("COLUMN_NAME LIKE '").append(mask).append("'");
        if (params.isSearchInComments()) {
            sql.append(" OR COLUMN_COMMENT LIKE '").append(mask).append("')");
        }

        try (JDBCStatement dbStat = session.createStatement()) {
            try (JDBCResultSet dbResult = dbStat.executeQuery(sql.toString())) {
                if (dbResult == null) {
                    log.warn("Result set is null while looking for Exasol table columns");
                    return;
                }
                while (!monitor.isCanceled() && dbResult.next() && references.size() <= params.getMaxResults()) {
                    String schemaName = JDBCUtils.safeGetString(dbResult, "TABLE_SCHEM");
                    String tableName  = JDBCUtils.safeGetString(dbResult, "COLUMN_TABLE");
                    String columnName = JDBCUtils.safeGetString(dbResult, "COLUMN_NAME");
                    if (tableName == null || columnName == null) {
                        continue;
                    }
                    ExasolSchema tableSchema = schema != null ? schema : dataSource.getSchema(monitor, schemaName);
                    references.add(
                            new AbstractObjectReference<DBSObject>(columnName, tableSchema, null, ExasolTableColumn.class, RelationalObjectType.TYPE_TABLE_COLUMN) {
                                @Override
                                public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
                                    if (tableSchema == null) {
                                        throw new DBException("Table schema '" + schemaName + "' not found");
                                    }
                                    ExasolTableBase table = tableSchema.getTable(monitor, tableName);
                                    if (table == null) {
                                        table = tableSchema.getViewCache().getObject(monitor, tableSchema, tableName);
                                        if (table == null) {
                                            throw new DBException("nor Table or view with name '" + tableName + "'  found in schema '" + schemaName + "'");
                                        }
                                    }
                                    ExasolTableColumn tableColumn = table.getAttribute(monitor, columnName);
                                    if (tableColumn == null) {
                                        throw new DBException("no table column with name '" + columnName + "'  found in table '" + schemaName + "." + tableName + "'");
                                    }
                                    return tableColumn;
                                }
                            }
                    );
                }
            }
        }
    }

    private void findProceduresByMask(@NotNull JDBCSession session, @Nullable ExasolSchema schema, @NotNull ObjectsSearchParams params,
                                      @NotNull List<DBSObjectReference> references) throws SQLException, DBException {
//      /*snapshot execution*/
//      SELECT
//          SCRIPT_SCHEMA,
//          SCRIPT_NAME
//      FROM
//          SYS.EXA_DBA_SCRIPTS
//      WHERE
//          (SCRIPT_NAME LIKE ?
//              OR SCRIPT_TEXT LIKE ?
//              OR SCRIPT_COMMENT LIKE ?)
//          AND SCRIPT_SCHEMA = ?

        //don't use parameter marks because of performance
        String mask = ExasolUtils.quoteString(params.getMask());
        StringBuilder sql = new StringBuilder("/*snapshot execution*/ SELECT SCRIPT_SCHEMA, SCRIPT_NAME FROM SYS.");
        sql.append(dataSource.getTablePrefix(ExasolSysTablePrefix.ALL)).append("_SCRIPTS WHERE ");
        List<String> clause = new ArrayList<>(3);
        clause.add(getLikeClause("SCRIPT_NAME", mask, params.isCaseSensitive()));
        if (params.isSearchInDefinitions()) {
            clause.add(getLikeClause("SCRIPT_TEXT", mask, params.isCaseSensitive()));
        }
        if (params.isSearchInComments()) {
            clause.add(getLikeClause("SCRIPT_COMMENT", mask, params.isCaseSensitive()));
        }
        if (clause.size() == 1) {
            sql.append(clause.get(0));
        } else {
            sql.append("(").append(String.join(" OR ", clause)).append(")");
        }
        if (schema != null) {
            sql.append(String.format(" AND SCRIPT_SCHEMA = '%s'", ExasolUtils.quoteString(schema.getName())));
        }

        DBRProgressMonitor monitor = session.getProgressMonitor();
        try (JDBCStatement dbStat = session.createStatement()) {
            try (JDBCResultSet dbResult = dbStat.executeQuery(sql.toString())) {
                if (dbResult == null) {
                    log.debug("Result set is null when looking for Exasol procedures");
                    return;
                }
                while (!monitor.isCanceled() && references.size() < params.getMaxResults() && dbResult.next()) {
                    String schemaName = JDBCUtils.safeGetString(dbResult, "SCRIPT_SCHEMA");
                    String scriptName = JDBCUtils.safeGetString(dbResult, "SCRIPT_NAME");
                    if (scriptName == null) {
                        continue;
                    }
                    ExasolSchema tableSchema = schema != null ? schema : dataSource.getSchema(monitor, schemaName);
                    references.add(
                            new AbstractObjectReference<>(scriptName, tableSchema,null, ExasolScript.class, RelationalObjectType.TYPE_PROCEDURE) {
                                @Override
                                public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
                                    if (tableSchema == null) {
                                        throw new DBException("Table schema '" + schemaName + "' not found");
                                    }
                                    ExasolScript script = tableSchema.scriptCache.getObject(monitor, tableSchema, scriptName);
                                    if (script == null) {
                                        throw new DBException("Script '" + script + "'  not found in schema '" + schemaName + "'");
                                    }
                                    return script;
                                }
                            }
                    );
                }
            }
        }
    }

    private void findConstraintsByMask(@NotNull JDBCSession session, @Nullable ExasolSchema schema, @NotNull ObjectsSearchParams params,
                                       @NotNull List<DBSObjectReference> references, String constType) throws SQLException, DBException {
        DBRProgressMonitor monitor = session.getProgressMonitor();
        //don't use parameter marks because of performance
        String sql = "";
        if (schema == null) {
            sql = String.format(sqlConstraintsAll, ExasolUtils.quoteString(params.getMask()), constType);
        } else {
            sql = String.format(sqlConstraintsSchema, ExasolUtils.quoteString(schema.getName()), constType, ExasolUtils.quoteString(params.getMask()));
        }
        try (JDBCStatement dbStat = session.createStatement()) {
            try (JDBCResultSet dbResult = dbStat.executeQuery(sql)) {
                if (dbResult == null) {
                    log.debug("Result set is null when looking for Exasol constraints");
                    return;
                }
                while (!monitor.isCanceled() && dbResult.next() && references.size() < params.getMaxResults()) {
                    final String schemaName = JDBCUtils.safeGetString(dbResult, "CONSTRAINT_SCHEMA");
                    final String tableName  = JDBCUtils.safeGetString(dbResult, "CONSTRAINT_TABLE");
                    final String constName       = JDBCUtils.safeGetString(dbResult, "CONSTRAINT_NAME");
                    final Class<?> classType;

                    if (constType.equals("PRIMARY KEY"))
                    {
                        classType = ExasolTableUniqueKey.class;
                    } else if (constType.equals("FOREIGN KEY"))
                    {
                        classType = ExasolTableForeignKey.class;
                    } else {
                        throw new DBException("Unkown constraint type" + constType);
                    }

                    references.add(new AbstractObjectReference<DBSObject>(constName, dataSource.getSchema(monitor, schemaName), null, classType, RelationalObjectType.TYPE_CONSTRAINT) {

                        @Override
                        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
                            ExasolSchema tableSchema = schema != null ? schema : dataSource.getSchema(monitor, schemaName);
                            if (tableSchema == null)
                            {
                                throw new DBException("Table schema '" + schemaName + "' not found");
                            }
                            ExasolTable table = tableSchema.getTable(monitor, tableName);

                            if (table == null)
                            {
                                throw new DBException("Table '" + tableName + "' not found in schema  '" + schemaName + "' not found");
                            }
                            if (classType.equals(ExasolTableForeignKey.class)) {
                                ExasolTableForeignKey foreignKey = (ExasolTableForeignKey) table.getAssociation(monitor, constName);
                                if (foreignKey == null)
                                    throw new DBException("Foreign Key  '" + constName + "' for Table '" + tableName + "' not found in schema '" + schemaName + "'");
                                return foreignKey;
                            } else  {
                                ExasolTableUniqueKey primaryKey = table.getConstraint(monitor, constName);
                                if (primaryKey == null)
                                    throw new DBException("Primary Key '" + constName + "' for Table '" + tableName + "' not found in schema '" + schemaName + "'");
                                return primaryKey;
                            }
                        }
                    });
                }
            }
        }
    }

    private void findTableObjectByName(@NotNull JDBCSession session, @Nullable ExasolSchema schema, @NotNull ObjectsSearchParams params,
                                       @NotNull Collection<? super DBSObjectReference> references) throws SQLException, DBException {
        DBRProgressMonitor monitor = session.getProgressMonitor();
        //don't use parameter marks because of performance

        String mask = ExasolUtils.quoteString(params.getMask());
        StringBuilder sql = new StringBuilder("/*snapshot execution*/ SELECT table_schem,table_name as column_table,table_type from \"$ODBCJDBC\".ALL_TABLES WHERE ");
        if (schema != null) {
            sql.append("TABLE_SCHEM = '").append(schema.getName()).append("' AND ");
        }
        if (params.isSearchInComments()) {
            sql.append("(");
        }
        sql.append("TABLE_NAME LIKE '").append(mask).append("' ");
        if (params.isSearchInComments()) {
            sql.append("OR REMARKS LIKE '").append(mask).append("') ");
        }
        sql.append("AND TABLE_TYPE = 'TABLE'");

        try (JDBCStatement dbStat = session.createStatement()) {
            try (JDBCResultSet dbResult = dbStat.executeQuery(sql.toString())) {
                if (dbResult == null) {
                    log.debug("Result set is null when looking for Exasol table objects");
                    return;
                }
                while (!monitor.isCanceled() && references.size() < params.getMaxResults() && dbResult.next()) {
                    String schemaName = JDBCUtils.safeGetString(dbResult, "TABLE_SCHEM");
                    String tableName  = JDBCUtils.safeGetString(dbResult, "COLUMN_TABLE");
                    if (tableName == null) {
                        continue;
                    }
                    ExasolSchema exasolSchema = dataSource.getSchema(monitor, schemaName);
                    references.add(
                            new AbstractObjectReference<DBSObject>(tableName, exasolSchema, null, ExasolTable.class, RelationalObjectType.TYPE_TABLE) {
                                @Override
                                public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
                                    ExasolSchema tableSchema = schema != null ? schema : dataSource.getSchema(monitor, schemaName);
                                    if (tableSchema == null)
                                    {
                                        throw new DBException("Table schema '" + schemaName + "' not found");
                                    }
                                    ExasolTable table = tableSchema.getTableCache().getObject(monitor, tableSchema, tableName);
                                    if (table == null) {
                                        throw new DBException("Table '" + tableName + "' not found in schema '" + schemaName + "'");
                                    }
                                    return table;
                                }
                            }
                    );
                }
            }
        }
    }

    private void findViews(@NotNull JDBCSession session, @Nullable ExasolSchema schema, @NotNull ObjectsSearchParams params,
                           @NotNull Collection<? super DBSObjectReference> references) throws DBException, SQLException {
//      /*snapshot execution*/
//      SELECT
//          VIEW_SCHEMA,
//          VIEW_NAME
//      FROM
//          SYS.EXA_ALL_VIEWS
//      WHERE
//          (VIEW_NAME LIKE ?
//            OR VIEW_TEXT LIKE ?
//            OR VIEW_COMMENT LIKE ?)
//          AND VIEW_SCHEMA = ?
        String viewSchemaColumn = "VIEW_SCHEMA";
        String viewNameColumn = "VIEW_NAME";
        StringBuilder sql = new StringBuilder(ExasolUtils.SNAPSHOT_EXEC);
        sql.append(" SELECT ");
        if (schema == null) {
            sql.append(viewSchemaColumn).append(", ");
        }
        sql.append(viewNameColumn).append(" FROM ").append(getMetadataTableName("VIEWS")).append(" WHERE ");
        List<String> clause = new ArrayList<>(3);
        String mask = ExasolUtils.quoteString(params.getMask());
        clause.add(getLikeClause(viewNameColumn, mask, params.isCaseSensitive()));
        if (params.isSearchInDefinitions()) {
            clause.add(getLikeClause("VIEW_TEXT", mask, params.isCaseSensitive()));
        }
        if (params.isSearchInComments()) {
            clause.add(getLikeClause("VIEW_COMMENT", mask, params.isCaseSensitive()));
        }
        if (clause.size() == 1) {
            sql.append(clause.get(0));
        } else {
            sql.append("(").append(String.join(" OR ", clause)).append(")");
        }
        if (schema != null) {
            sql.append(String.format(" AND VIEW_SCHEMA = '%s'", ExasolUtils.quoteString(schema.getName())));
        }

        DBRProgressMonitor monitor = session.getProgressMonitor();
        int limit = params.getMaxResults();
        if (isFetchCompleted(monitor, references, limit)) {
            return;
        }
        // Let's say that query execution takes 50% of the time and result set traversal takes another 50%
        int halfOfWork = limit - references.size();
        monitor.beginTask("Fetching objects", halfOfWork * 2);

        try (JDBCStatement statement = session.createStatement()) {
            try (JDBCResultSet resultSet = statement.executeQuery(sql.toString())) {
                monitor.worked(halfOfWork);
                if (resultSet == null) {
                    log.debug("Result set is null");
                    return;
                }
                while (!isFetchCompleted(monitor, references, limit) && resultSet.next()) {
                    ExasolSchema viewSchema = schema;
                    if (viewSchema == null) {
                        String viewSchemaName = JDBCUtils.safeGetString(resultSet, viewSchemaColumn);
                        if (viewSchemaName == null) {
                            continue;
                        }
                        viewSchema = dataSource.getSchema(session.getProgressMonitor(), viewSchemaName);
                        if (viewSchema == null) {
                            log.warn(String.format("Schema '%s' not found in schema cache of datasource '%s'", viewSchemaName, dataSource));
                            continue;
                        }
                    }
                    String viewName = JDBCUtils.safeGetString(resultSet, viewNameColumn);
                    if (viewName == null) {
                        continue;
                    }
                    references.add(new ViewReference(viewSchema, viewName));
                }
            }
        } finally {
            monitor.done();
        }
    }

    private static boolean isFetchCompleted(@NotNull DBRProgressMonitor monitor, @NotNull Collection<?> objects, int limit) {
        return monitor.isCanceled() || objects.size() >= limit;
    }

    @NotNull
    private static String getLikeClause(@NotNull String identifier, @NotNull String mask, boolean isCaseSensitive) {
        if (isCaseSensitive) {
            return String.format("%s LIKE '%s'", identifier, mask);
        }
        return String.format("UPPER(%s) LIKE '%s'", identifier, mask.toUpperCase(Locale.ROOT));
    }

    @NotNull
    private String getMetadataTableName(@NotNull String suffix) {
        return "SYS." + dataSource.getTablePrefix(ExasolSysTablePrefix.ALL) + "_" + suffix;
    }

    @Override
    protected JDBCDataSource getDataSource() {
        return this.dataSource;
    }

    @Override
    public boolean supportsSearchInCommentsFor(@NotNull DBSObjectType objectType) {
        return objectType == ExasolObjectType.TABLE
            || objectType == ExasolObjectType.VIEW
            || objectType == ExasolObjectType.SCRIPT
            || objectType == ExasolObjectType.COLUMN;
    }

    @Override
    public boolean supportsSearchInDefinitionsFor(@NotNull DBSObjectType objectType) {
        return objectType == ExasolObjectType.VIEW
            || objectType == ExasolObjectType.SCRIPT;
    }

    private static final class ViewReference extends AbstractObjectReference<ExasolSchema> {
        private ViewReference(@NotNull ExasolSchema container, @NotNull String name) {
            super(name, container, null, ExasolView.class, RelationalObjectType.TYPE_VIEW);
        }

        @Override
        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
            ExasolSchema schema = getContainer();
            String viewName = getName();
            DBSObject view = schema.getViewCache().getObject(monitor, schema, viewName);
            if (view == null) {
                throw new DBException(String.format("View '%s' not found in schema '%s'", viewName, schema.getName()));
            }
            return view;
        }
    }
}

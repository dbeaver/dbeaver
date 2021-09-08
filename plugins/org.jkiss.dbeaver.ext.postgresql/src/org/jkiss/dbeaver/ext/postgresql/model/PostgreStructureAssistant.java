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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractObjectReference;
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.messages.ModelMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.struct.DBSObjectReference;
import org.jkiss.dbeaver.model.struct.DBSObjectType;
import org.jkiss.dbeaver.model.struct.DBSStructureAssistant;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * PostgreStructureAssistant
 */
public class PostgreStructureAssistant implements DBSStructureAssistant<PostgreExecutionContext> {
    private static final Log log = Log.getLog(PostgreStructureAssistant.class);

    private final PostgreDataSource dataSource;

    public PostgreStructureAssistant(PostgreDataSource dataSource)
    {
        this.dataSource = dataSource;
    }

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
            RelationalObjectType.TYPE_DATA_TYPE,
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
    public DBSObjectType[] getSearchObjectTypes() {
        //TODO: currently, we do not search for data types, although it's absolutely possible.
        return new DBSObjectType[]{
            RelationalObjectType.TYPE_TABLE,
            RelationalObjectType.TYPE_CONSTRAINT,
            RelationalObjectType.TYPE_PROCEDURE,
            RelationalObjectType.TYPE_TABLE_COLUMN,
        };
    }

    @NotNull
    @Override
    public List<DBSObjectReference> findObjectsByMask(@NotNull DBRProgressMonitor monitor, @NotNull PostgreExecutionContext executionContext,
                                                      @NotNull ObjectsSearchParams params) throws DBException {
        DBSObject parentObject = params.getParentObject();
        PostgreSchema ownerSchema = parentObject instanceof PostgreSchema ? (PostgreSchema) parentObject : null;
        PostgreDataSource dataSource = executionContext.getDataSource();

        PostgreDatabase database = parentObject instanceof PostgreObject ?
            ((PostgreObject) parentObject).getDatabase() : executionContext.getDefaultCatalog();
        if (database == null) {
            database = dataSource.getDefaultInstance();
        }
        List<PostgreSchema> nsList = new ArrayList<>();
        if (ownerSchema != null) {
            nsList.add(0, ownerSchema);
        } else if (!params.isGlobalSearch()) {
            // Limit object search with search path
            for (String sn : executionContext.getSearchPath()) {
                PostgreSchema schema = database.getSchema(monitor, PostgreUtils.getRealSchemaName(database, sn));
                if (schema != null) {
                    nsList.add(schema);
                }
            }
            PostgreSchema pgCatalog = database.getCatalogSchema(monitor);
            if (pgCatalog != null) {
                nsList.add(pgCatalog);
            }
        } else {
            // Limit object search with available schemas (use filters - #648)
            DBSObjectFilter schemaFilter = dataSource.getContainer().getObjectFilter(PostgreSchema.class, database, true);
            if (schemaFilter != null && schemaFilter.isEnabled()) {
                for (PostgreSchema schema : database.getSchemas(monitor)) {
                    if (schemaFilter.matches(schema.getName())) {
                        nsList.add(schema);
                    }
                }
            }
        }

        if (executionContext.getDefaultCatalog() != database) {
            executionContext = database.getMetaContext();
        }

        List<DBSObjectReference> references = new ArrayList<>();
        try (JDBCSession session = executionContext.openSession(monitor, DBCExecutionPurpose.META, ModelMessages.model_jdbc_find_objects_by_name)) {
            for (DBSObjectType type : params.getObjectTypes()) {
                if (type == RelationalObjectType.TYPE_TABLE) {
                    findTablesByMask(session, database, nsList, params, references);
                } else if (type == RelationalObjectType.TYPE_CONSTRAINT) {
                    findConstraintsByMask(session, database, nsList, params, references);
                } else if (type == RelationalObjectType.TYPE_PROCEDURE) {
                    findProceduresByMask(session, database, nsList, params, references);
                } else if (type == RelationalObjectType.TYPE_TABLE_COLUMN) {
                    findTableColumnsByMask(session, database, nsList, params, references);
                }
                if (references.size() >= params.getMaxResults()) {
                    break;
                }
            }
        } catch (SQLException ex) {
            throw new DBException(ex, getDataSource());
        }
        return references;
    }

    private static void findTablesByMask(@NotNull JDBCSession session, @NotNull PostgreDatabase database, @NotNull final List<PostgreSchema> schemas,
                                         @NotNull ObjectsSearchParams params, @NotNull Collection<? super DBSObjectReference> objects)
                                            throws SQLException, DBException {
        DBRProgressMonitor monitor = session.getProgressMonitor();

        QueryParams queryParams = new QueryParams(
            "pc.oid,pc.relname,pc.relnamespace,pc.relkind",
            "pg_catalog.pg_class pc",
            "pc.relname",
            schemas,
            "pc.relnamespace",
            "pc.relname"
        );
        queryParams.setWhereClause("pc.relkind in('r','v','m','f','p')"); // r = ordinary table, v = view, m = materialized view, f = foreign table, p = partitioned table
        queryParams.setCaseSensitive(params.isCaseSensitive());
        if (params.isSearchInComments()) {
            queryParams.setDescriptionClause("obj_description(pc.oid, 'pg_class')");
        }
        if (params.isSearchInDefinitions()) {
            queryParams.setDefinitionClause("pc.relkind = 'v' AND pg_get_viewdef(pc.\"oid\")");
        }
        queryParams.setMaxResults(params.getMaxResults() - objects.size());
        String sql = buildFindQuery(queryParams);

        // Load tables
        try (JDBCPreparedStatement dbStat = session.prepareStatement(sql)) {
            fillParams(dbStat, params, schemas, params.isSearchInDefinitions());
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (!monitor.isCanceled() && dbResult.next()) {
                    final long schemaId = JDBCUtils.safeGetLong(dbResult, "relnamespace");
                    final long tableId = JDBCUtils.safeGetLong(dbResult, "oid");
                    final String tableName = JDBCUtils.safeGetString(dbResult, "relname");
                    final PostgreClass.RelKind tableType = PostgreClass.RelKind.valueOf(JDBCUtils.safeGetString(dbResult, "relkind"));
                    final PostgreSchema tableSchema = database.getSchema(session.getProgressMonitor(), schemaId);
                    if (tableSchema == null) {
                        log.debug("Can't resolve table '" + tableName + "' - owner schema " + schemaId + " not found");
                        continue;
                    }
                    objects.add(new AbstractObjectReference(tableName, tableSchema, null,
                        tableType == PostgreClass.RelKind.r ? PostgreTable.class :
                            (tableType == PostgreClass.RelKind.v ? PostgreView.class : PostgreMaterializedView.class),
                        RelationalObjectType.TYPE_TABLE) {
                        @Override
                        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
                            PostgreTableBase table = tableSchema.getTable(monitor, tableId);
                            if (table == null) {
                                throw new DBException("Table '" + tableName + "' not found in schema '" + tableSchema.getName() + "'");
                            }
                            return table;
                        }
                    });
                }
            }
        }
    }

    private static void findProceduresByMask(@NotNull JDBCSession session, @NotNull PostgreDatabase database,
                                             @NotNull final List<PostgreSchema> schemas, @NotNull ObjectsSearchParams params,
                                             @NotNull Collection<? super DBSObjectReference> objects) throws SQLException, DBException {
        DBRProgressMonitor monitor = session.getProgressMonitor();

        PostgreServerExtension serverType = database.getDataSource().getServerType();
        QueryParams queryParams = new QueryParams(
            "pp." + serverType.getProceduresOidColumn() + " as poid, pp.*",
            "pg_catalog." + serverType.getProceduresSystemTable() + " pp",
            "pp.proname",
            schemas,
            "pp.pronamespace",
            "pp.proname"
        );
        //queryParams.setWhereClause("pp.proname NOT LIKE '\\_%'");
        queryParams.setCaseSensitive(params.isCaseSensitive());
        if (params.isSearchInComments()) {
            queryParams.setDescriptionClause("obj_description(pp.oid, 'pg_proc')");
        }
        if (params.isSearchInDefinitions()) {
            queryParams.setDefinitionClause("pp.prokind <> 'm' AND pp.prokind <> 'a' AND pg_get_functiondef(pp.\"oid\")");
        }
        queryParams.setMaxResults(params.getMaxResults() - objects.size());
        String sql = buildFindQuery(queryParams);

        // Load procedures
        try (JDBCPreparedStatement dbStat = session.prepareStatement(sql)) {
            fillParams(dbStat, params, schemas, params.isSearchInDefinitions());
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (!monitor.isCanceled() && dbResult.next()) {
                    final long schemaId = JDBCUtils.safeGetLong(dbResult, "pronamespace");
                    final String procName = JDBCUtils.safeGetString(dbResult, "proname");
                    final long procId = JDBCUtils.safeGetLong(dbResult, "poid");
                    final PostgreSchema procSchema = database.getSchema(session.getProgressMonitor(), schemaId);
                    if (procSchema == null) {
                        log.debug("Procedure's schema '" + schemaId + "' not found");
                        continue;
                    }
                    PostgreProcedure proc = new PostgreProcedure(monitor, procSchema, dbResult);

                    objects.add(new AbstractObjectReference(procName, procSchema, null, PostgreProcedure.class, RelationalObjectType.TYPE_PROCEDURE,
                        DBUtils.getQuotedIdentifier(procSchema) + "." + proc.getOverloadedName()) {
                        @Override
                        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
                            PostgreProcedure procedure = procSchema.getProcedure(monitor, procId);
                            if (procedure == null) {
                                throw new DBException("Procedure '" + procName + "' not found in schema '" + procSchema.getName() + "'");
                            }
                            return procedure;
                        }
                    });
                }
            }
        }
    }

    private static void findConstraintsByMask(@NotNull JDBCSession session, @NotNull PostgreDatabase database, @NotNull final List<PostgreSchema> schemas,
                                              @NotNull ObjectsSearchParams params, @NotNull Collection<? super DBSObjectReference> objects)
                                                throws SQLException, DBException {
        DBRProgressMonitor monitor = session.getProgressMonitor();

        QueryParams queryParams = new QueryParams(
            "pc.oid, pc.conname, pc.connamespace",
            "pg_catalog.pg_constraint pc",
            "pc.conname",
            schemas,
            "pc.connamespace",
            "pc.conname"
        );
        queryParams.setCaseSensitive(params.isCaseSensitive());
        if (params.isSearchInComments()) {
            queryParams.setDescriptionClause("obj_description(pc.oid, 'pg_constraint')");
        }
        if (params.isSearchInDefinitions()) {
            queryParams.setDefinitionClause("pg_get_constraintdef(pc.\"oid\")");
        }
        queryParams.setMaxResults(params.getMaxResults() - objects.size());
        String sql = buildFindQuery(queryParams);

        // Load constraints
        try (JDBCPreparedStatement dbStat = session.prepareStatement(sql)) {
            fillParams(dbStat, params, schemas, params.isSearchInDefinitions());
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (!monitor.isCanceled() && dbResult.next()) {
                    final long schemaId = JDBCUtils.safeGetLong(dbResult, "connamespace");
                    final long constrId = JDBCUtils.safeGetLong(dbResult, "oid");
                    final String constrName = JDBCUtils.safeGetString(dbResult, "conname");
                    final PostgreSchema constrSchema = database.getSchema(session.getProgressMonitor(), schemaId);
                    if (constrSchema == null) {
                        log.debug("Constraint's schema '" + schemaId + "' not found");
                        continue;
                    }
                    objects.add(new AbstractObjectReference(constrName, constrSchema, null, PostgreTableConstraintBase.class, RelationalObjectType.TYPE_CONSTRAINT) {
                        @Override
                        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
                            final PostgreTableConstraintBase constraint = PostgreUtils.getObjectById(monitor, constrSchema.getConstraintCache(), constrSchema, constrId);
                            if (constraint == null) {
                                throw new DBException("Constraint '" + constrName + "' not found in schema '" + constrSchema.getName() + "'");
                            }
                            return constraint;
                        }
                    });
                }
            }
        }
    }

    private static void findTableColumnsByMask(@NotNull JDBCSession session, @NotNull PostgreDatabase database, @NotNull List<PostgreSchema> schemas,
                                               @NotNull ObjectsSearchParams objectsSearchParams,
                                               @NotNull Collection<? super DBSObjectReference> objects) throws SQLException, DBException {
        DBRProgressMonitor monitor = session.getProgressMonitor();

        QueryParams queryParams = new QueryParams(
            "x.attname,x.attrelid,x.atttypid,c.relnamespace",
            "pg_catalog.pg_attribute x, pg_catalog.pg_class c",
            "x.attname",
            schemas,
            "c.relnamespace",
            "x.attname"
        );
        queryParams.setWhereClause("c.oid=x.attrelid");
        if (objectsSearchParams.isSearchInComments()) {
            queryParams.setDescriptionClause("col_description(c.oid, x.attnum)");
        }
        queryParams.setMaxResults(objectsSearchParams.getMaxResults() - objects.size());
        queryParams.setCaseSensitive(objectsSearchParams.isCaseSensitive());

        String sql = buildFindQuery(queryParams);

        // Load constraints
        try (JDBCPreparedStatement dbStat = session.prepareStatement(sql)) {
            fillParams(dbStat, objectsSearchParams, schemas, false);
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (!monitor.isCanceled() && dbResult.next()) {
                    final long schemaId = JDBCUtils.safeGetLong(dbResult, "relnamespace");
                    final long tableId = JDBCUtils.safeGetLong(dbResult, "attrelid");
                    final String attributeName = JDBCUtils.safeGetString(dbResult, "attname");
                    final PostgreSchema constrSchema = database.getSchema(session.getProgressMonitor(), schemaId);
                    if (constrSchema == null) {
                        log.debug("Attribute's schema '" + schemaId + "' not found");
                        continue;
                    }
                    objects.add(new AbstractObjectReference(attributeName, constrSchema, null, PostgreTableBase.class, RelationalObjectType.TYPE_TABLE_COLUMN) {
                        @Override
                        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
                            final PostgreTableBase table = PostgreUtils.getObjectById(monitor, constrSchema.getTableCache(), constrSchema, tableId);
                            if (table == null) {
                                throw new DBException("Table '" + tableId + "' not found in schema '" + constrSchema.getName() + "'");
                            }
                            return table.getAttribute(monitor, attributeName);
                        }
                    });
                }
            }
        }
    }

    private static String buildFindQuery(@NotNull QueryParams queryParams) {
        StringBuilder sql = new StringBuilder("SELECT ").append(queryParams.getColumnsToSelect());
        sql.append(" FROM ").append(queryParams.getFromClause()).append(" WHERE ");
        if (queryParams.getWhereClause() != null) {
            sql.append(queryParams.getWhereClause()).append(" AND ");
        }
        boolean addParentheses = queryParams.getDefinitionClause() != null || queryParams.getDescriptionClause() != null;
        if (addParentheses) {
            sql.append("(");
        }
        String likeClause = queryParams.isCaseSensitive() ? " LIKE ?" : " ILIKE ?";
        sql.append(queryParams.getName()).append(likeClause).append(" ");
        if (queryParams.getDescriptionClause() != null) {
            sql.append("OR ").append(queryParams.getDescriptionClause()).append(likeClause);
        }
        if (queryParams.getDefinitionClause() != null) {
            sql.append(" OR (").append(queryParams.getDefinitionClause()).append(likeClause).append(")");
        }
        if (addParentheses) {
            sql.append(")");
        }
        if (!queryParams.getSchemas().isEmpty()) {
            sql.append("AND ").append(queryParams.getNamespace()).append(" IN (");
            sql.append(SQLUtils.generateParamList(queryParams.getSchemas().size())).append(") ");
        }
        sql.append("ORDER BY ").append(queryParams.getOrderBy()).append(" LIMIT ").append(queryParams.getMaxResults());
        return sql.toString();
    }

    private static void fillParams(@NotNull JDBCPreparedStatement statement, @NotNull ObjectsSearchParams params,
                                   @Nullable List<? extends PostgreSchema> schema, boolean fillSearchInDefinitions) throws SQLException {
        statement.setString(1, params.getMask());
        int idx = 2;
        if (params.isSearchInComments()) {
            statement.setString(idx, params.getMask());
            idx++;
        }
        if (fillSearchInDefinitions) {
            statement.setString(idx, params.getMask());
            idx++;
        }
        if (!CommonUtils.isEmpty(schema)) {
            PostgreUtils.setArrayParameter(statement, idx, schema);
        }
    }

    private static final class QueryParams {
        @NotNull
        private final String columnsToSelect;

        @NotNull
        private final String fromClause;

        @Nullable
        private String whereClause;

        @NotNull
        private final String name;

        private boolean caseSensitive;

        @Nullable
        private String descriptionClause;

        @NotNull
        private final Collection<? extends PostgreSchema> schemas;

        @NotNull
        private final String namespace;

        @NotNull
        private final String orderBy;

        private int maxResults;

        @Nullable
        private String definitionClause;

        private QueryParams(@NotNull String columnsToSelect, @NotNull String fromClause, @NotNull String name,
                            @NotNull Collection<? extends PostgreSchema> schemas, @NotNull String namespace, @NotNull String orderBy) {
            this.columnsToSelect = columnsToSelect;
            this.fromClause = fromClause;
            this.name = name;
            this.schemas = schemas;
            this.namespace = namespace;
            this.orderBy = orderBy;
        }

        @NotNull
        private String getColumnsToSelect() {
            return columnsToSelect;
        }

        @NotNull
        private String getFromClause() {
            return fromClause;
        }

        @Nullable
        private String getWhereClause() {
            return whereClause;
        }

        private void setWhereClause(@Nullable String whereClause) {
            this.whereClause = whereClause;
        }

        @NotNull
        private String getName() {
            return name;
        }

        private boolean isCaseSensitive() {
            return caseSensitive;
        }

        private void setCaseSensitive(boolean caseSensitive) {
            this.caseSensitive = caseSensitive;
        }

        @Nullable
        private String getDescriptionClause() {
            return descriptionClause;
        }

        private void setDescriptionClause(@Nullable String descriptionClause) {
            this.descriptionClause = descriptionClause;
        }

        @NotNull
        private Collection<PostgreSchema> getSchemas() {
            return Collections.unmodifiableCollection(schemas);
        }

        @NotNull
        private String getNamespace() {
            return namespace;
        }

        @NotNull
        private String getOrderBy() {
            return orderBy;
        }

        private int getMaxResults() {
            return maxResults;
        }

        private void setMaxResults(int maxResults) {
            this.maxResults = maxResults;
        }

        @Nullable
        private String getDefinitionClause() {
            return definitionClause;
        }

        private void setDefinitionClause(@Nullable String definitionClause) {
            this.definitionClause = definitionClause;
        }
    }

    @Override
    public boolean supportsSearchInCommentsFor(@NotNull DBSObjectType objectType) {
        return objectType == RelationalObjectType.TYPE_TABLE
            || objectType == RelationalObjectType.TYPE_CONSTRAINT
            || objectType == RelationalObjectType.TYPE_PROCEDURE
            || objectType == RelationalObjectType.TYPE_TABLE_COLUMN;
    }

    @Override
    public boolean supportsSearchInDefinitionsFor(@NotNull DBSObjectType objectType) {
        return objectType == RelationalObjectType.TYPE_CONSTRAINT || objectType == RelationalObjectType.TYPE_PROCEDURE;
    }
}

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
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
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
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.struct.DBSObjectReference;
import org.jkiss.dbeaver.model.struct.DBSObjectType;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * PostgreStructureAssistant
 */
public class PostgreStructureAssistant extends JDBCStructureAssistant<PostgreExecutionContext>
{
    private final PostgreDataSource dataSource;

    public PostgreStructureAssistant(PostgreDataSource dataSource)
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
    protected void findObjectsByMask(PostgreExecutionContext executionContext, JDBCSession session, DBSObjectType objectType, DBSObject parentObject,
                                     String objectNameMask, boolean caseSensitive, boolean globalSearch, int maxResults,
                                     List<DBSObjectReference> references) throws DBException, SQLException {
        findObjectsByMask(
            executionContext,
            session,
            objectType,
            parentObject,
            objectNameMask,
            caseSensitive,
            globalSearch,
            false,
            maxResults,
            references
        );
    }

    @Override
    protected void findObjectsByMask(@NotNull PostgreExecutionContext executionContext, @NotNull JDBCSession session, DBSObjectType objectType,
                                     @Nullable DBSObject parentObject, String mask, boolean caseSensitive, boolean globalSearch,
                                     boolean searchInComments, int maxResults, @NotNull List<DBSObjectReference> references)
                                        throws DBException, SQLException {
        PostgreSchema ownerSchema = parentObject instanceof PostgreSchema ? (PostgreSchema) parentObject : null;
        final PostgreDataSource dataSource = (PostgreDataSource) session.getDataSource();

        PostgreDatabase database = parentObject instanceof PostgreObject ? ((PostgreObject) parentObject).getDatabase() :
            executionContext.getDefaultCatalog();
        if (database == null) {
            database = dataSource.getDefaultInstance();
        }
        List<PostgreSchema> nsList = new ArrayList<>();
        if (ownerSchema != null) {
            nsList.add(0, ownerSchema);
        } else if (!globalSearch) {
            // Limit object search with search path
            for (String sn : executionContext.getSearchPath()) {
                final PostgreSchema schema = database.getSchema(
                    session.getProgressMonitor(),
                    PostgreUtils.getRealSchemaName(database, sn));
                if (schema != null) {
                    nsList.add(schema);
                }
            }
            PostgreSchema pgCatalog = database.getCatalogSchema(session.getProgressMonitor());
            if (pgCatalog != null) {
                nsList.add(pgCatalog);
            }
        } else {
            // Limit object search with available schemas (use filters - #648)
            DBSObjectFilter schemaFilter = dataSource.getContainer().getObjectFilter(PostgreSchema.class, database, true);
            if (schemaFilter != null && schemaFilter.isEnabled()) {
                for (PostgreSchema schema : database.getSchemas(session.getProgressMonitor())) {
                    if (schemaFilter.matches(schema.getName())) {
                        nsList.add(schema);
                    }
                }
            }
        }

        if (objectType == RelationalObjectType.TYPE_TABLE) {
            findTablesByMask(session, database, nsList, mask, caseSensitive, searchInComments, maxResults, references);
        } else if (objectType == RelationalObjectType.TYPE_CONSTRAINT) {
            findConstraintsByMask(session, database, nsList, mask, caseSensitive, searchInComments, maxResults, references);
        } else if (objectType == RelationalObjectType.TYPE_PROCEDURE) {
            findProceduresByMask(session, database, nsList, mask, caseSensitive, searchInComments, maxResults, references);
        } else if (objectType == RelationalObjectType.TYPE_TABLE_COLUMN) {
            findTableColumnsByMask(session, database, nsList, mask, caseSensitive, searchInComments, maxResults, references);
        }
    }

    private void findTablesByMask(@NotNull JDBCSession session, @NotNull PostgreDatabase database, @Nullable final List<PostgreSchema> schema,
                                  String tableNameMask, boolean caseSensitive, boolean searchInComments, int maxResults,
                                  @NotNull List<DBSObjectReference> objects) throws SQLException, DBException {
        DBRProgressMonitor monitor = session.getProgressMonitor();

        StringBuilder sql = new StringBuilder("SELECT pc.oid,pc.relname,pc.relnamespace,pc.relkind FROM pg_catalog.pg_class pc ");
        if (searchInComments) {
            sql.append("LEFT JOIN pg_catalog.pg_description pd on pc.\"oid\" = pd.objoid ");
        }
        sql.append("WHERE pc.relkind in('r','v','m') AND ");
        if (searchInComments) {
            sql.append("( ");
        }
        sql.append("pc.relname ");
        String likeClause = caseSensitive ? "LIKE" : "ILIKE";
        sql.append(likeClause);
        sql.append(" ? ");
        if (searchInComments) {
            sql.append("OR pd.description ").append(likeClause).append(" ?) AND COALESCE(pd.objsubid, 0) = 0 ");
        }
        if (!CommonUtils.isEmpty(schema)) {
            sql.append("AND pc.relnamespace IN (").append(SQLUtils.generateParamList(schema.size())).append(") ");
        }
        sql.append("ORDER BY pc.relname LIMIT ").append(maxResults);

        // Load tables
        try (JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString())) {
            dbStat.setString(1, tableNameMask);
            if (searchInComments) {
                dbStat.setString(2, tableNameMask);
            }
            if (!CommonUtils.isEmpty(schema)) {
                PostgreUtils.setArrayParameter(dbStat, 2 + (searchInComments ? 1 : 0), schema);
            }
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                int tableNum = maxResults;
                while (dbResult.next() && tableNum-- > 0) {
                    if (monitor.isCanceled()) {
                        break;
                    }
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

    private void findProceduresByMask(@NotNull JDBCSession session, @NotNull PostgreDatabase database, @Nullable final List<PostgreSchema> schema,
                                      @NotNull String procNameMask, boolean caseSensitive, boolean searchInComments,
                                      int maxResults, @NotNull List<DBSObjectReference> objects) throws SQLException, DBException {
        DBRProgressMonitor monitor = session.getProgressMonitor();

        StringBuilder sql = new StringBuilder("SELECT pp.oid, pp.* FROM pg_catalog.pg_proc pp ");
        if (searchInComments) {
            sql.append("LEFT JOIN pg_catalog.pg_description pd on pp.\"oid\" = pd.objoid ");
        }
        sql.append("WHERE ");
        if (searchInComments) {
            sql.append("( ");
        }
        sql.append("pp.proname ");
        String likeClause = caseSensitive ? "LIKE" : "ILIKE";
        sql.append(likeClause);
        sql.append(" ? ");
        if (searchInComments) {
            sql.append("OR pd.description ").append(likeClause).append(" ?) AND COALESCE(pd.objsubid, 0) = 0 ");
        }
        sql.append("AND pp.proname NOT LIKE '\\_%'");
        if (!CommonUtils.isEmpty(schema)) {
            sql.append("AND pp.pronamespace IN (").append(SQLUtils.generateParamList(schema.size())).append(") ");
        }
        sql.append("ORDER BY pp.proname LIMIT ").append(maxResults);

        // Load procedures
        try (JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString())) {
            dbStat.setString(1, procNameMask);
            if (searchInComments) {
                dbStat.setString(2, procNameMask);
            }
            if (!CommonUtils.isEmpty(schema)) {
                PostgreUtils.setArrayParameter(dbStat, 2 + (searchInComments ? 1 : 0), schema);
            }
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                int tableNum = maxResults;
                while (dbResult.next() && tableNum-- > 0) {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    final long schemaId = JDBCUtils.safeGetLong(dbResult, "pronamespace");
                    final String procName = JDBCUtils.safeGetString(dbResult, "proname");
                    final long procId = JDBCUtils.safeGetLong(dbResult, "oid");
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

    private void findConstraintsByMask(@NotNull JDBCSession session, @NotNull PostgreDatabase database, @Nullable final List<PostgreSchema> schema,
                                       String nameMask, boolean caseSensitive, boolean searchInComments, int maxResults,
                                       @NotNull List<DBSObjectReference> objects) throws SQLException, DBException {
        DBRProgressMonitor monitor = session.getProgressMonitor();

        StringBuilder sql = new StringBuilder("SELECT pc.oid, pc.conname, pc.connamespace FROM pg_catalog.pg_constraint pc ");
        if (searchInComments) {
            sql.append("LEFT JOIN pg_catalog.pg_description pd on pc.\"oid\" = pd.objoid ");
        }
        sql.append("WHERE ");
        if (searchInComments) {
            sql.append("( ");
        }
        sql.append("pc.conname ");
        String likeClause = caseSensitive ? "LIKE" : "ILIKE";
        sql.append(likeClause);
        sql.append(" ? ");
        if (searchInComments) {
            sql.append("OR pd.description ").append(likeClause).append(" ?) AND COALESCE(pd.objsubid, 0) = 0 ");
        }
        if (!CommonUtils.isEmpty(schema)) {
            sql.append("AND pc.connamespace IN (").append(SQLUtils.generateParamList(schema.size())).append(") ");
        }
        sql.append("ORDER BY pc.conname LIMIT ").append(maxResults);

        // Load constraints
        try (JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString())) {
            dbStat.setString(1, nameMask);
            if (searchInComments) {
                dbStat.setString(2, nameMask);
            }
            if (!CommonUtils.isEmpty(schema)) {
                PostgreUtils.setArrayParameter(dbStat, 2 + (searchInComments ? 1 : 0), schema);
            }
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                int tableNum = maxResults;
                while (dbResult.next() && tableNum-- > 0) {
                    if (monitor.isCanceled()) {
                        break;
                    }
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

    private void findTableColumnsByMask(@NotNull JDBCSession session, @NotNull PostgreDatabase database, @Nullable final List<PostgreSchema> schema,
                                        String columnNameMask, boolean caseSensitive, boolean searchInComments, int maxResults,
                                        @NotNull List<DBSObjectReference> objects) throws SQLException, DBException {
        DBRProgressMonitor monitor = session.getProgressMonitor();

        StringBuilder sql = new StringBuilder(
            "SELECT DISTINCT x.attname,x.attrelid,x.atttypid,c.relnamespace FROM pg_catalog.pg_attribute x, pg_catalog.pg_class c "
        );
        if (searchInComments) {
            sql.append("LEFT join pg_catalog.pg_description pd on c.\"oid\" = pd.objoid ");
        }
        sql.append("WHERE c.oid=x.attrelid AND ");
        if (searchInComments) {
            sql.append("( ");
        }
        sql.append("x.attname ");
        String likeClause = caseSensitive ? "LIKE" : "ILIKE";
        sql.append(likeClause);
        sql.append(" ? ");
        if (searchInComments) {
            sql.append("OR pd.description ").append(likeClause).append(" ?) ");
        }
        if (!CommonUtils.isEmpty(schema)) {
            sql.append("AND c.relnamespace IN (").append(SQLUtils.generateParamList(schema.size())).append(") ");
        }
        sql.append("ORDER BY x.attname LIMIT ").append(maxResults);

        // Load constraints
        try (JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString())) {
            dbStat.setString(1, columnNameMask);
            if (searchInComments) {
                dbStat.setString(2, columnNameMask);
            }
            if (!CommonUtils.isEmpty(schema)) {
                PostgreUtils.setArrayParameter(dbStat, 2 + (searchInComments ? 1 : 0), schema);
            }
            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                int tableNum = maxResults;
                while (dbResult.next() && tableNum-- > 0) {
                    if (monitor.isCanceled()) {
                        break;
                    }
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
}

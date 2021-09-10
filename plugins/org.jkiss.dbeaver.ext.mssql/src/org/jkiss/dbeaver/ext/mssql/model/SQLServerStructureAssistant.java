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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mssql.SQLServerConstants;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractObjectReference;
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SQLServerStructureAssistant
 */
public class SQLServerStructureAssistant implements DBSStructureAssistant<SQLServerExecutionContext> {
    private static final Log log = Log.getLog(SQLServerStructureAssistant.class);

    private final SQLServerDataSource dataSource;

    public SQLServerStructureAssistant(SQLServerDataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    @Override
    public DBSObjectType[] getSupportedObjectTypes()
    {
        return new DBSObjectType[] {
            SQLServerObjectType.S,
            SQLServerObjectType.U,
            SQLServerObjectType.IT,
            SQLServerObjectType.V,
            SQLServerObjectType.SN,
            SQLServerObjectType.P,
            SQLServerObjectType.FN,
            SQLServerObjectType.FT,
            SQLServerObjectType.FS,
            SQLServerObjectType.X,
            };
    }

    @Override
    public DBSObjectType[] getSearchObjectTypes() {
        return new DBSObjectType[] {
            RelationalObjectType.TYPE_TABLE,
            RelationalObjectType.TYPE_VIEW,
            SQLServerObjectType.SN,
            RelationalObjectType.TYPE_PROCEDURE,
        };
    }

    @Override
    public DBSObjectType[] getHyperlinkObjectTypes()
    {
        return new DBSObjectType[] {
            SQLServerObjectType.S,
            SQLServerObjectType.U,
            SQLServerObjectType.IT,
            SQLServerObjectType.V,
            RelationalObjectType.TYPE_PROCEDURE,
        };
    }

    @Override
    public DBSObjectType[] getAutoCompleteObjectTypes()
    {
        return new DBSObjectType[] {
            SQLServerObjectType.U,
            SQLServerObjectType.V,
            SQLServerObjectType.P,
            SQLServerObjectType.FN,
            SQLServerObjectType.IF,
            SQLServerObjectType.TF,
            SQLServerObjectType.X
        };
    }

    @NotNull
    @Override
    public List<DBSObjectReference> findObjectsByMask(@NotNull DBRProgressMonitor monitor, @NotNull SQLServerExecutionContext executionContext,
                                                      @NotNull ObjectsSearchParams params) throws DBException {
        if (params.getMask().startsWith("%#") || params.getMask().startsWith("#")) {
            try (JDBCSession session = executionContext.openSession(monitor, DBCExecutionPurpose.META, "Find temp tables by name")) {
                List<DBSObjectReference> objects = new ArrayList<>();
                searchTempTables(session, params, objects);
                return objects;
            }
        }

        return findAllObjects(monitor, executionContext, params);
    }

    private List<DBSObjectReference> findAllObjects(@NotNull DBRProgressMonitor monitor, @NotNull SQLServerExecutionContext executionContext,
                                                    @NotNull ObjectsSearchParams params) throws DBException {

        DBSObject parentObject = params.getParentObject();
        boolean globalSearch = params.isGlobalSearch();
        Collection<SQLServerDatabase> databases;
        SQLServerSchema schema = null;

        if (parentObject instanceof DBPDataSourceContainer) {
            if (globalSearch) {
                databases = executionContext.getDataSource().getDatabases(monitor);
            } else {
                SQLServerDatabase database = executionContext.getContextDefaults().getDefaultCatalog();
                if (database == null) {
                    database = executionContext.getDataSource().getDefaultDatabase(monitor);
                }
                schema = executionContext.getContextDefaults().getDefaultSchema();
                if (database == null || schema == null) {
                    return Collections.emptyList();
                }
                databases = Collections.singletonList(database);
            }
        } else if (parentObject instanceof SQLServerDatabase) {
            SQLServerDatabase database = (SQLServerDatabase) parentObject;
            databases = Collections.singletonList(database);
            if (!globalSearch) {
                schema = executionContext.getContextDefaults().getDefaultSchema();
                if (schema == null) {
                    return Collections.emptyList();
                }
            }
        } else if (parentObject instanceof SQLServerSchema) {
            schema = (SQLServerSchema) parentObject;
            SQLServerDatabase database = schema.getDatabase();
            databases = Collections.singletonList(database);
        } else {
            return Collections.emptyList();
        }

        if (CommonUtils.isEmpty(databases)) {
            return Collections.emptyList();
        }

        Collection<SQLServerObjectType> supObjectTypes = new ArrayList<>(params.getObjectTypes().length + 2);
        for (DBSObjectType objectType : params.getObjectTypes()) {
            if (objectType instanceof SQLServerObjectType) {
                supObjectTypes.add((SQLServerObjectType) objectType);
            } else if (objectType == RelationalObjectType.TYPE_PROCEDURE) {
                supObjectTypes.addAll(SQLServerObjectType.getTypesForClass(SQLServerProcedure.class));
            } else if (objectType == RelationalObjectType.TYPE_TABLE) {
                supObjectTypes.addAll(SQLServerObjectType.getTypesForClass(SQLServerTable.class));
            } else if (objectType == RelationalObjectType.TYPE_CONSTRAINT) {
                supObjectTypes.addAll(SQLServerObjectType.getTypesForClass(SQLServerTableCheckConstraint.class));
                supObjectTypes.addAll(SQLServerObjectType.getTypesForClass(SQLServerTableForeignKey.class));
            } else if (objectType == RelationalObjectType.TYPE_VIEW) {
                supObjectTypes.addAll(SQLServerObjectType.getTypesForClass(SQLServerView.class));
            }
        }
        if (supObjectTypes.isEmpty()) {
            return Collections.emptyList();
        }
        StringBuilder objectTypeClause = new StringBuilder(100);
        for (SQLServerObjectType objectType : supObjectTypes) {
            if (objectTypeClause.length() > 0) objectTypeClause.append(",");
            objectTypeClause.append("'").append(objectType.getTypeID()).append("'");
        }
        if (objectTypeClause.length() == 0) {
            return Collections.emptyList();
        }

        StringBuilder sqlBuilder = new StringBuilder("SELECT TOP %d * FROM %s o");
        if (params.isSearchInComments()) {
            sqlBuilder.append(" LEFT JOIN sys.extended_properties ep ON ((o.parent_object_id = 0 AND ep.minor_id = 0 AND o.object_id = ep.major_id) OR (o.parent_object_id <> 0 AND ep.minor_id = o.parent_object_id AND ep.major_id = o.object_id)) ");
        }
        sqlBuilder.append(" WHERE o.type IN (").append(objectTypeClause).append(") AND ");
        boolean addParentheses = params.isSearchInComments() || params.isSearchInDefinitions();
        if (addParentheses) {
            sqlBuilder.append("(");
        }
        sqlBuilder.append("o.name LIKE ? ");
        if (params.isSearchInComments()) {
            sqlBuilder.append("OR (ep.name = 'MS_Description' AND CAST(ep.value AS nvarchar) LIKE ?)");
        }
        if (params.isSearchInDefinitions()) {
            if (params.isSearchInComments()) {
                sqlBuilder.append(" ");
            }
            sqlBuilder.append("OR OBJECT_DEFINITION(o.object_id) LIKE ?");
        }
        if (addParentheses) {
            sqlBuilder.append(") ");
        }
        if (schema != null) {
            sqlBuilder.append("AND o.schema_id = ? ");
        }
        sqlBuilder.append("ORDER BY o.name");
        String template = sqlBuilder.toString();

        List<DBSObjectReference> objects = new ArrayList<>();
        try (JDBCSession session = executionContext.openSession(monitor, DBCExecutionPurpose.META, "Find objects by name")) {
            for (SQLServerDatabase database: databases) {
                int rowsToFetch = params.getMaxResults() - objects.size();
                if (rowsToFetch < 1) {
                    break;
                }
                String sql = String.format(template, rowsToFetch, SQLServerUtils.getSystemTableName(database, "all_objects"));
                try (JDBCPreparedStatement dbStat = session.prepareStatement(sql)) {
                    dbStat.setString(1, params.getMask());
                    int idx = 2;
                    if (params.isSearchInComments()) {
                        dbStat.setString(idx, params.getMask());
                        idx++;
                    }
                    if (params.isSearchInDefinitions()) {
                        dbStat.setString(idx, params.getMask());
                        idx++;
                    }
                    if (schema != null) {
                        dbStat.setLong(idx, schema.getObjectId());
                    }
                    dbStat.setFetchSize(DBConstants.METADATA_FETCH_SIZE);

                    try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                        while (dbResult.next() && !session.getProgressMonitor().isCanceled() && objects.size() < params.getMaxResults()) {
                            final long schemaId = JDBCUtils.safeGetLong(dbResult, "schema_id");
                            final String objectName = JDBCUtils.safeGetString(dbResult, "name");
                            final String objectTypeName = JDBCUtils.safeGetStringTrimmed(dbResult, "type");
                            final SQLServerObjectType objectType = SQLServerObjectType.valueOf(objectTypeName);
                            SQLServerSchema objectSchema = schemaId == 0 ? null : database.getSchema(session.getProgressMonitor(), schemaId);
                            objects.add(new AbstractObjectReference(
                                objectName,
                                objectSchema != null ? objectSchema : database,
                                null,
                                objectType.getTypeClass(),
                                objectType)
                            {
                                @Override
                                public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
                                    DBSObject object = objectType.findObject(session.getProgressMonitor(), database, objectSchema, objectName);
                                    if (object == null) {
                                        throw new DBException(objectTypeName + " '" + objectName + "' not found");
                                    }
                                    return object;
                                }
                            });
                        }
                    }
                } catch (SQLException e) {
                    // Among other reasons, this can happen if access is denied for the current database
                    log.debug(
                    "Unable to perform metadata search in mssql instance. databaseName="
                            + database.getName()
                            + ", schema="
                            + (schema != null ? schema.getName() : "null"),
                        e
                    );
                }
            }
        }

        return objects;
    }

    @Override
    public boolean supportsSearchInCommentsFor(@NotNull DBSObjectType objectType) {
        return true;
    }

    @Override
    public boolean supportsSearchInDefinitionsFor(@NotNull DBSObjectType objectType) {
        return true;
    }
  
    private void searchTempTables(@NotNull JDBCSession session, @NotNull ObjectsSearchParams params, @NotNull List<DBSObjectReference> objects) throws DBException {
        final SQLServerDatabase database = dataSource.getDatabase(session.getProgressMonitor(), SQLServerConstants.TEMPDB_DATABASE);
        final SQLServerSchema schema = database.getSchema(session.getProgressMonitor(), SQLServerConstants.DEFAULT_SCHEMA_NAME);

        final StringBuilder sql = new StringBuilder()
            .append("SELECT TOP ").append(params.getMaxResults() - objects.size()).append(" *")
            .append("\nFROM ").append(SQLServerUtils.getSystemTableName(database, "all_objects"))
            .append("\nWHERE type = '").append(SQLServerObjectType.U.name())
            .append("' AND name LIKE '#%' AND name LIKE ? AND OBJECT_ID(CONCAT('").append(SQLServerConstants.TEMPDB_DATABASE).append("..', QUOTENAME(name))) <> 0");

        try (JDBCPreparedStatement dbStat = session.prepareStatement(sql.toString())) {
            dbStat.setString(1, "%" + params.getMask() + "%");
            dbStat.setFetchSize(DBConstants.METADATA_FETCH_SIZE);

            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (dbResult.next() && !session.getProgressMonitor().isCanceled()) {
                    final String objectName = JDBCUtils.safeGetString(dbResult, "name");
                    final String objectNameTrimmed = extractTempTableName(objectName);
                    final SQLServerObjectType objectType = SQLServerObjectType.valueOf(JDBCUtils.safeGetStringTrimmed(dbResult, "type"));

                    objects.add(new AbstractObjectReference(objectName, database, null, objectType.getTypeClass(), objectType) {
                        @Override
                        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException {
                            DBSObject object = schema.getChild(session.getProgressMonitor(), objectName);
                            if (object == null) {
                                // Likely not cached, invalidate and try again
                                schema.getTableCache().setFullCache(false);
                                object = schema.getChild(session.getProgressMonitor(), objectName);
                            }
                            if (object == null) {
                                throw new DBException(objectType.name() + " '" + objectName + "' not found");
                            }
                            return object;
                        }

                        @NotNull
                        @Override
                        public String getFullyQualifiedName(DBPEvaluationContext context) {
                            return objectNameTrimmed;
                        }
                    });
                }
            }
        } catch (Throwable e) {
            throw new DBException("Error while searching in system catalog", e, dataSource);
        }
    }

    @NotNull
    private static String extractTempTableName(@NotNull String originalName) {
        if (originalName.startsWith("##")) {
            // Global temporary tables does not contain padding in their names. Use as-is
            return originalName;
        }
        final String name = originalName.substring(0, 116);
        for (int i = name.length() - 1; i >= 0; i--) {
            if (name.charAt(i) != '_') {
                return name.substring(0, i + 1);
            }
        }
        return name;
    }
}

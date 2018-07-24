/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.ext.mssql.SQLServerConstants;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPErrorAssistant;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformProvider;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformType;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformer;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SQLServerMetaModel
 */
public class SQLServerMetaModel extends GenericMetaModel implements DBCQueryTransformProvider
{
    private static final Log log = Log.getLog(SQLServerMetaModel.class);

    private final boolean sqlServer;

    public SQLServerMetaModel() {
        this(true);
    }

    public SQLServerMetaModel(boolean sqlServer) {
        super();
        this.sqlServer = sqlServer;
    }

    public boolean isSqlServer() {
        return sqlServer;
    }

    @Override
    public SQLServerDataSource createDataSourceImpl(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
        return new SQLServerDataSource(monitor, container, this);
    }

    @Override
    public SQLServerDatabase createCatalogImpl(GenericDataSource dataSource, String catalogName) {
        return new SQLServerDatabase(dataSource, catalogName);
    }

    @Override
    public SQLServerSchema createSchemaImpl(GenericDataSource dataSource, GenericCatalog catalog, String schemaName) throws DBException {
        return new SQLServerSchema(dataSource, catalog, schemaName, 0);
    }

    public String getViewDDL(DBRProgressMonitor monitor, GenericTable sourceObject, Map<String, Object> options) throws DBException {
        return extractSource(monitor, sourceObject.getDataSource(), sourceObject.getCatalog(), sourceObject.getSchema().getName(), sourceObject.getName());
    }

    @Override
    public SQLServerProcedure createProcedureImpl(GenericStructContainer container, String procedureName, String specificName, String remarks, DBSProcedureType procedureType, GenericFunctionResultType functionResultType) {
        return new SQLServerProcedure(container, procedureName, specificName, remarks, procedureType, functionResultType);
    }

    @Override
    public String getProcedureDDL(DBRProgressMonitor monitor, GenericProcedure sourceObject) throws DBException {
        if (isSqlServer() && sourceObject.getDataSource().isServerVersionAtLeast(SQLServerConstants.SQL_SERVER_2005_VERSION_MAJOR,0)) {
            try (JDBCSession session = DBUtils.openMetaSession(monitor, sourceObject, "Read routine definition")) {
                try (JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT definition FROM " + DBUtils.getQuotedIdentifier(sourceObject.getCatalog()) + ".sys.sql_modules WHERE object_id=OBJECT_ID(?)"
                )) {
                    dbStat.setString(1, sourceObject.getFullyQualifiedName(DBPEvaluationContext.DML));
                    try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                        if (dbResult.nextRow()) {
                            return dbResult.getString(1);
                        }
                        return "-- Routine '" + sourceObject.getName() + "' definition not found in ";
                    }
                }
            } catch (SQLException e) {
                throw new DBException(e, sourceObject.getDataSource());
            }
        }
        return extractSource(monitor, sourceObject.getDataSource(), sourceObject.getCatalog(), sourceObject.getSchema().getName(), sourceObject.getName());
    }

    @Override
    public boolean supportsTriggers(@NotNull GenericDataSource dataSource) {
        return true;
    }

    @Override
    public List<GenericTrigger> loadTriggers(DBRProgressMonitor monitor, @NotNull GenericStructContainer container, @Nullable GenericTable table) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container, "Read triggers")) {
            String schema = getSystemSchemaFQN(container.getDataSource(), container.getCatalog());
            StringBuilder query = new StringBuilder("SELECT triggers.name FROM " + schema + ".sysobjects triggers");
            GenericSchema tableSchema = table == null ? null : table.getSchema();
            long schemaId = tableSchema instanceof SQLServerSchema ? ((SQLServerSchema) tableSchema).getSchemaId() : 0;

            if (table != null) {
                query.append(",").append(schema).append(".sysobjects tables");
            }
            query.append("\nWHERE triggers.type = 'TR'\n");
            if (table != null) {
                query.append("AND triggers.deltrig = tables.id\n");
                if (schemaId == 0) {
                    query.append("AND user_name(tables.uid) = ?");
                } else {
                    query.append("AND tables.uid = ?");
                }
                query.append(" AND tables.name = ?");
            }

            try (JDBCPreparedStatement dbStat = session.prepareStatement(query.toString())) {
                if (table != null) {
                    if (schemaId == 0) {
                        dbStat.setString(1, tableSchema.getName());
                    } else {
                        dbStat.setLong(1, schemaId);
                    }
                    dbStat.setString(2, table.getName());
                }
                List<GenericTrigger> result = new ArrayList<>();

                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String name = JDBCUtils.safeGetString(dbResult, 1);
                        if (name == null) {
                            continue;
                        }
                        name = name.trim();
                        SQLServerTrigger trigger = new SQLServerTrigger(container, table, name, null);
                        result.add(trigger);
                    }
                }
                return result;
            }
        } catch (SQLException e) {
            throw new DBException(e, container.getDataSource());
        }
    }

    @Override
    public String getTriggerDDL(@NotNull DBRProgressMonitor monitor, @NotNull GenericTrigger trigger) throws DBException {
        GenericTable table = trigger.getTable();
        assert table != null;
        return extractSource(monitor, table.getDataSource(), table.getCatalog(), table.getSchema().getName(), trigger.getName());
    }

    @Nullable
    @Override
    public DBCQueryTransformer createQueryTransformer(@NotNull DBCQueryTransformType type) {
        if (type == DBCQueryTransformType.RESULT_SET_LIMIT) {
            //return new QueryTransformerTop();
        }
        return null;
    }

    private String extractSource(DBRProgressMonitor monitor, GenericDataSource dataSource, GenericCatalog catalog, String schema, String name) throws DBException {
        ServerType serverType = getServerType();
        String systemSchema = getSystemSchemaFQN(dataSource, catalog);
        try (JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Read source code")) {
            String mdQuery = serverType == ServerType.SQL_SERVER ?
                systemSchema + ".sp_helptext '" + DBUtils.getQuotedIdentifier(dataSource, schema) + "." + DBUtils.getQuotedIdentifier(dataSource, name) + "'"
                :
                "SELECT sc.text\n" +
                "FROM " + systemSchema + ".sysobjects so\n" +
                "INNER JOIN " + systemSchema + ".syscomments sc on sc.id = so.id\n" +
                "WHERE user_name(so.uid)=? AND so.name=?";
            try (JDBCPreparedStatement dbStat = session.prepareStatement(mdQuery)) {
                if (serverType == ServerType.SYBASE) {
                    dbStat.setString(1, schema);
                    dbStat.setString(2, name);
                }
                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    StringBuilder sql = new StringBuilder();
                    while (dbResult.nextRow()) {
                        sql.append(dbResult.getString(1));
                    }
                    return sql.toString();
                }
            }
        } catch (SQLException e) {
            throw new DBException(e, dataSource);
        }
    }

    public ServerType getServerType() {
        return sqlServer ? ServerType.SQL_SERVER : ServerType.SYBASE;
    }

    @Override
    public SQLServerIndex createIndexImpl(GenericTable table, boolean nonUnique, String qualifier, long cardinality, String indexName, DBSIndexType indexType, boolean persisted) {
        return new SQLServerIndex(table, nonUnique, qualifier, cardinality, indexName, indexType, persisted);
    }

    @Override
    public String getAutoIncrementClause(GenericTableColumn column) {
        return "IDENTITY(1,1)";
    }

    @Override
    public boolean useCatalogInObjectNames() {
        return false;
    }

    @Override
    public List<GenericSchema> loadSchemas(JDBCSession session, GenericDataSource dataSource, GenericCatalog catalog) throws DBException {
        if (catalog == null) {
            // Schemas MUST be in catalog
            return null;
        }
        boolean showAllSchemas = ((SQLServerDataSource) dataSource).isShowAllSchemas();
        final DBSObjectFilter schemaFilters = dataSource.getContainer().getObjectFilter(GenericSchema.class, catalog, false);

        String sysSchema = getSystemSchemaFQN(dataSource, catalog);
        String sql;
        if (showAllSchemas) {
            if (getServerType() == ServerType.SQL_SERVER && dataSource.isServerVersionAtLeast(SQLServerConstants.SQL_SERVER_2005_VERSION_MAJOR ,0)) {
                sql = "SELECT * FROM " + sysSchema + ".schemas";
            } else {
                sql = "SELECT * FROM " + sysSchema + ".sysusers";
            }
        } else {
            if (getServerType() == ServerType.SQL_SERVER) {
                sql = "SELECT DISTINCT s.*\n" +
                    "FROM " + sysSchema + ".schemas s, " + sysSchema + ".sysobjects o\n" +
                    "WHERE s.schema_id=o.uid\n" +
                    "ORDER BY 1";
            } else {
                sql = "SELECT DISTINCT u.name,u.uid\n" +
                    "FROM " + sysSchema + ".sysusers u, " + sysSchema + ".sysobjects o\n" +
                    "WHERE u.uid=o.uid\n" +
                    "ORDER BY 1";
            }
        }

        boolean schemaReadFailed = false;
        List<GenericSchema> result = new ArrayList<>();
        try (JDBCPreparedStatement dbStat = session.prepareStatement(sql)) {

            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (dbResult.next()) {
                    String name = JDBCUtils.safeGetString(dbResult, "name");
                    if (name == null) {
                        continue;
                    }
                    name = name.trim();
                    if (schemaFilters != null && !schemaFilters.matches(name)) {
                        // Doesn't match filter
                        continue;
                    }

                    long schemaId = JDBCUtils.safeGetLong(dbResult, "schema_id");
                    SQLServerSchema schema = new SQLServerSchema(
                        dataSource, catalog, name, schemaId);
                    result.add(schema);
                }
            }
        } catch (SQLException e) {
            if (dataSource.discoverErrorType(e) == DBPErrorAssistant.ErrorType.CONNECTION_LOST) {
                throw new DBException(e, dataSource);
            } else {
                log.warn("Schema read failed: empty list returned. Try generic method.", e);
                schemaReadFailed = true;
            }
        }
        if (result.isEmpty()) {
            if (!schemaReadFailed && !showAllSchemas) {
                // Perhaps all schemas were filtered out
                result.add(new GenericSchema(dataSource, catalog, SQLServerConstants.DEFAULT_SCHEMA_NAME));
            } else {
                // Maybe something went wrong. LEt's try to use native function
                return super.loadSchemas(session, dataSource, catalog);
            }
        }
        return result;
    }

    @Override
    public boolean supportsSequences(GenericDataSource dataSource) {
        return getServerType() == ServerType.SQL_SERVER;
    }

    @Override
    public List<GenericSequence> loadSequences(DBRProgressMonitor monitor, GenericStructContainer container) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container, "Read system sequences")) {
            try (JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM " + getSystemSchemaFQN(container.getDataSource(), container.getCatalog()) + ".sequences WHERE schema_name(schema_id)=?")) {
                dbStat.setString(1, container.getSchema().getName());
                List<GenericSequence> result = new ArrayList<>();

                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String name = JDBCUtils.safeGetString(dbResult, "name");
                        if (name == null) {
                            continue;
                        }
                        name = name.trim();
                        GenericSequence sequence = new GenericSequence(
                            container,
                            name,
                            null,
                            JDBCUtils.safeGetLong(dbResult, "current_value"),
                            JDBCUtils.safeGetLong(dbResult, "minimum_value"),
                            JDBCUtils.safeGetLong(dbResult, "maximum_value"),
                            JDBCUtils.safeGetLong(dbResult, "increment")
                        );
                        result.add(sequence);
                    }
                }
                return result;

            }
        } catch (SQLException e) {
            throw new DBException(e, container.getDataSource());
        }
    }

    @Override
    public SQLServerTable createTableImpl(GenericStructContainer container, String tableName, String tableType, JDBCResultSet dbResult) {
        return new SQLServerTable(container, tableName, tableType, dbResult);
    }

    @Override
    public boolean isSystemTable(GenericTable table) {
        return table.getSchema() != null && getSystemSchema().equals(table.getSchema().getName()) && table.getName().startsWith("sys");
    }

    @NotNull
    private String getSystemSchema() {
        return sqlServer ? "sys" : "dbo";
    }

    private String getSystemSchemaFQN(GenericDataSource dataSource, GenericCatalog catalog) {
        return dataSource.isServerVersionAtLeast(SQLServerConstants.SQL_SERVER_2005_VERSION_MAJOR ,0) ?
            DBUtils.getQuotedIdentifier(catalog) + "." + getSystemSchema() : getSystemSchema();
    }
}

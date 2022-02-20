/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mssql.model.generic;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.*;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.ext.mssql.SQLServerConstants;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerView;
import org.jkiss.dbeaver.ext.mssql.model.ServerType;
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
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SQLServerMetaModel
 */
public class SQLServerMetaModel extends GenericMetaModel implements DBCQueryTransformProvider
{
    private static final Log log = Log.getLog(SQLServerMetaModel.class);

    private final boolean sqlServer;
    private final Map<String, Boolean> sysViewsCache = new HashMap<>();

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

    private boolean isSapIQ(GenericDataSource dataSource) {
        String productName = dataSource.getInfo().getDatabaseProductName();
        return productName != null && (productName.contains("IQ SAP") || productName.contains("SAP IQ") || productName.contains("Sybase IQ"));
    }

    @Override
    public SQLServerGenericDataSource createDataSourceImpl(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
        return new SQLServerGenericDataSource(monitor, container, this);
    }

    @Override
    public SQLServerGenericDatabase createCatalogImpl(@NotNull GenericDataSource dataSource, @NotNull String catalogName) {
        return new SQLServerGenericDatabase(dataSource, catalogName);
    }

    @Override
    public SQLServerGenericSchema createSchemaImpl(@NotNull GenericDataSource dataSource, GenericCatalog catalog, @NotNull String schemaName) throws DBException {
        return new SQLServerGenericSchema(dataSource, catalog, schemaName, 0);
    }

    public String getViewDDL(DBRProgressMonitor monitor, GenericView sourceObject, Map<String, Object> options) throws DBException {
        return extractSource(monitor, sourceObject.getDataSource(), sourceObject, sourceObject.getCatalog(), sourceObject.getSchema().getName(), sourceObject.getName());
    }

    @Override
    public void loadProcedures(DBRProgressMonitor monitor, @NotNull GenericObjectContainer container) throws DBException {
        if (!isSqlServer()) {
            // #4378
            GenericDataSource dataSource = container.getDataSource();
            String dbName = DBUtils.getQuotedIdentifier(container.getParentObject());
            try (JDBCSession session = DBUtils.openMetaSession(monitor, container, "Sybase procedure list")) {
                // P – Transact-SQL or SQLJ procedure
                // SF – scalar or user-defined functions - from SAP Adaptive Server version 16
                try (JDBCPreparedStatement dbStat = session.prepareStatement(
                    "select distinct so.name as proc_name,su.name as schema_name, so.[type] as type_name\n" +
                        "from " + dbName + ".dbo.sysobjects so, "+ dbName + ".dbo.sysusers su\n" +
                        "where so.type IN ('P', 'SF')\n" +
                        "and su.uid = so.uid\n" +
                        "and su.name=?"))
                {
                    dbStat.setString(1, container.getName());
                    try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                        while (dbResult.nextRow()) {
                            String typeName = JDBCUtils.safeGetString(dbResult, "type_name");
                            DBSProcedureType procedureType = DBSProcedureType.PROCEDURE;
                            if ("SF".equals(typeName)) {
                                procedureType = DBSProcedureType.FUNCTION;
                            }
                            final GenericProcedure procedure = createProcedureImpl(
                                container,
                                JDBCUtils.safeGetString(dbResult, "proc_name"),
                                null,
                                null,
                                procedureType,
                                null);
                            procedure.setSource(JDBCUtils.safeGetString(dbResult, "definition"));
                            container.addProcedure(procedure);
                        }
                    }
                }
            } catch (SQLException e) {
                throw new DBException(e, dataSource);
            }
        } else {
            super.loadProcedures(monitor, container);
        }
    }

    @Override
    public SQLServerGenericProcedure createProcedureImpl(GenericStructContainer container, String procedureName, String specificName, String remarks, DBSProcedureType procedureType, GenericFunctionResultType functionResultType) {
        return new SQLServerGenericProcedure(container, procedureName, specificName, remarks, procedureType, functionResultType);
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
        return extractSource(monitor, sourceObject.getDataSource(), sourceObject, sourceObject.getCatalog(), sourceObject.getSchema().getName(), sourceObject.getName());
    }

    @Override
    public boolean supportsTriggers(@NotNull GenericDataSource dataSource) {
        return true;
    }

    @Override
    public JDBCStatement prepareTableTriggersLoadStatement(@NotNull JDBCSession session, @NotNull GenericStructContainer container, @Nullable GenericTableBase table) throws SQLException {
        String schema = SQLServerUtils.getSystemSchemaFQN(container.getDataSource(), container.getCatalog().getName(), getSystemSchema());
        StringBuilder query = new StringBuilder("SELECT triggers.name as TRIGGER_NAME, triggers.*, tables.name as OWNER FROM " + schema + ".sysobjects triggers");
        GenericSchema tableSchema = table == null ? null : table.getSchema();
        long schemaId = tableSchema instanceof SQLServerGenericSchema ? ((SQLServerGenericSchema) tableSchema).getSchemaId() : 0;
        query.append(",").append(schema).append(".sysobjects tables").append("\nWHERE triggers.type = 'TR' AND triggers.deltrig = tables.id\n");
        if (table != null) {
            if (schemaId == 0) {
                query.append("AND user_name(tables.uid) = ?");
            } else {
                query.append("AND tables.uid = ?");
            }
            query.append(" AND tables.name = ?");
        }
        JDBCPreparedStatement dbStat = session.prepareStatement(query.toString());
        if (table != null) {
            if (schemaId == 0) {
                dbStat.setString(1, tableSchema.getName());
            } else {
                dbStat.setLong(1, schemaId);
            }
            dbStat.setString(2, table.getName());
        }
        return dbStat;
    }

    @Override
    public GenericTrigger createTableTriggerImpl(@NotNull JDBCSession session, @NotNull GenericStructContainer genericStructContainer, @NotNull GenericTableBase genericTableBase, String name, @NotNull JDBCResultSet resultSet) throws DBException {
        if (CommonUtils.isEmpty(name)) {
            name = JDBCUtils.safeGetString(resultSet, 1);
        }
        if (name == null) {
            return null;
        }
        name = name.trim();
        return  new SQLServerGenericTrigger(genericTableBase, name, null);
    }

    @Override
    public List<GenericTrigger> loadTriggers(DBRProgressMonitor monitor, @NotNull GenericStructContainer container, @Nullable GenericTableBase table) throws DBException {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, container, "Read triggers")) {

            try (JDBCPreparedStatement dbStat = (JDBCPreparedStatement) prepareTableTriggersLoadStatement(session, container, table)) {
                List<GenericTrigger> result = new ArrayList<>();

                try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                    while (dbResult.next()) {
                        String name = JDBCUtils.safeGetString(dbResult, 1);
                        if (name == null) {
                            continue;
                        }
                        name = name.trim();
                        SQLServerGenericTrigger trigger = new SQLServerGenericTrigger(table, name, null);
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
        GenericTableBase table = ((GenericTableTrigger) trigger).getTable();
        assert table != null;
        return extractSource(monitor, table.getDataSource(), table, table.getCatalog(), table.getSchema().getName(), trigger.getName());
    }

    @Override
    public boolean isColumnNotNullByDefault() {
        return true;
    }

    @Nullable
    @Override
    public DBCQueryTransformer createQueryTransformer(@NotNull DBCQueryTransformType type) {
        if (type == DBCQueryTransformType.RESULT_SET_LIMIT) {
            //return new QueryTransformerTop();
        }
        return null;
    }

    private String extractSource(DBRProgressMonitor monitor, GenericDataSource dataSource, DBSObject object, GenericCatalog catalog, String schema, String name) throws DBException {
        ServerType serverType = getServerType();
        String systemSchema = SQLServerUtils.getSystemSchemaFQN(dataSource, catalog.getName(), getSystemSchema());
        try (JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Read source code")) {
            String mdQuery;
            if (serverType == ServerType.SQL_SERVER) {
                final String objectFQN = DBUtils.getQuotedIdentifier(dataSource, schema) + "." + DBUtils.getQuotedIdentifier(dataSource, name);
                if (SQLServerUtils.isDriverBabelfish(dataSource.getContainer().getDriver())) {
                    mdQuery = "SELECT definition FROM sys.sql_modules WHERE object_id = (OBJECT_ID(N'" + objectFQN + "'))";
                } else {
                    mdQuery = systemSchema + ".sp_helptext '" + objectFQN + "'";
                }
            } else {
                if (isSapIQ(dataSource)) {
                    mdQuery = "SELECT s.source\n" +
                        "FROM " + systemSchema + ".sysobjects AS so\n" +
                        "JOIN sys.sysuser AS u ON u.user_id = so.uid\n" +
                        "JOIN sys.syssource AS s ON s.object_id = so.id\n" +
                        "WHERE user_name(so.uid)=? AND so.name=?";
                } else {
                    mdQuery = "SELECT sc.text\n" +
                        "FROM " + systemSchema + ".sysobjects so, " + systemSchema + ".syscomments sc\n" +
                        "WHERE user_name(so.uid)=? AND so.name=? and sc.id = so.id\n" +
                        "ORDER BY sc.colid";
                }
            }
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
                    String ddl = sql.toString();
                    if (object instanceof SQLServerView) {
                        ddl = ddl.replaceAll("(?i)CREATE VIEW", "CREATE OR REPLACE VIEW");
                    }
                    return ddl;
                }
            }
        } catch (SQLException e) {
            throw new DBException(e, dataSource);
        }
    }

    private boolean hasSybaseSystemView(JDBCSession session, String systemSchema, String viewName) throws SQLException {
        Boolean check;
        synchronized (sysViewsCache) {
            check = sysViewsCache.get(viewName);
        }
        if (check == null) {
            check = JDBCUtils.queryString(session, "SELECT name from " + systemSchema + ".sysobjects where name=?", viewName) != null;
            synchronized (sysViewsCache) {
                sysViewsCache.put(viewName, check);
            }
        }
        return check;
    }

    public ServerType getServerType() {
        return sqlServer ? ServerType.SQL_SERVER : ServerType.SYBASE;
    }

    @Override
    public SQLServerGenericIndex createIndexImpl(GenericTableBase table, boolean nonUnique, String qualifier, long cardinality, String indexName, DBSIndexType indexType, boolean persisted) {
        return new SQLServerGenericIndex(table, nonUnique, qualifier, cardinality, indexName, indexType, persisted);
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
    public boolean isSchemasOptional() {
        return true;
    }

    @Override
    public List<GenericSchema> loadSchemas(JDBCSession session, GenericDataSource dataSource, GenericCatalog catalog) throws DBException {
        boolean showAllSchemas = SQLServerUtils.isShowAllSchemas(dataSource);
        final DBSObjectFilter schemaFilters = dataSource.getContainer().getObjectFilter(GenericSchema.class, catalog, false);

        String sysSchema = SQLServerUtils.getSystemSchemaFQN(dataSource, catalog.getName(), getSystemSchema());
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

                    long schemaId;
                    if (isSqlServer()) {
                        schemaId = JDBCUtils.safeGetLong(dbResult, "schema_id");
                    } else {
                        schemaId = JDBCUtils.safeGetLong(dbResult, "uid");
                    }
                    SQLServerGenericSchema schema = new SQLServerGenericSchema(
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
    public boolean supportsSequences(@NotNull GenericDataSource dataSource) {
        return getServerType() == ServerType.SQL_SERVER;
    }

    @Override
    public JDBCStatement prepareSequencesLoadStatement(@NotNull JDBCSession session, @NotNull GenericStructContainer container) throws SQLException {
        JDBCPreparedStatement dbStat = session.prepareStatement(
            "SELECT * FROM " +
                SQLServerUtils.getSystemSchemaFQN(container.getDataSource(), container.getCatalog().getName(), getSystemSchema()) +
                ".sequences WHERE schema_name(schema_id)=?");
        dbStat.setString(1, container.getSchema().getName());
        return dbStat;
    }

    @Override
    public GenericSequence createSequenceImpl(@NotNull JDBCSession session, @NotNull GenericStructContainer container, @NotNull JDBCResultSet dbResult) {
        String name = JDBCUtils.safeGetString(dbResult, "name");
        if (CommonUtils.isEmpty(name)) {
            return null;
        }
        name = name.trim();
        return new GenericSequence(
            container,
            name,
            null,
            CommonUtils.toLong(JDBCUtils.safeGetObject(dbResult, "current_value")),
            CommonUtils.toLong(JDBCUtils.safeGetObject(dbResult, "minimum_value")),
            CommonUtils.toLong(JDBCUtils.safeGetObject(dbResult, "maximum_value")),
            CommonUtils.toLong(JDBCUtils.safeGetObject(dbResult, "increment"))
        );
    }

    @Override
    public boolean supportsSynonyms(@NotNull GenericDataSource dataSource) {
        return isSqlServer();
    }

    @Override
    public JDBCStatement prepareSynonymsLoadStatement(@NotNull JDBCSession session, @NotNull GenericStructContainer container) throws SQLException {
        JDBCPreparedStatement dbStat = session.prepareStatement(
            "SELECT * FROM " + SQLServerUtils.getSystemSchemaFQN(container.getDataSource(), container.getCatalog().getName(), getSystemSchema()) + ".synonyms WHERE schema_name(schema_id)=?");
        dbStat.setString(1, container.getSchema().getName());
        return dbStat;
    }

    @Override
    public GenericSynonym createSynonymImpl(@NotNull JDBCSession session, @NotNull GenericStructContainer container, @NotNull JDBCResultSet dbResult) throws DBException {
        String name = JDBCUtils.safeGetString(dbResult, "name");
        if (CommonUtils.isEmpty(name)) {
            return null;
        }
        name = name.trim();
        return new SQLServerGenericSynonym(
            container,
            name,
            null,
            JDBCUtils.safeGetString(dbResult, "base_object_name"));
    }

    @Override
    public GenericTableBase createTableImpl(GenericStructContainer container, String tableName, String tableType, JDBCResultSet dbResult) {
        if (tableType != null && isView(tableType)) {
            return new SQLServerGenericView(container, tableName, tableType, dbResult);
        } else {
            return new SQLServerGenericTable(container, tableName, tableType, dbResult);
        }
    }

    @Override
    public GenericTableColumn createTableColumnImpl(@NotNull DBRProgressMonitor monitor, @Nullable JDBCResultSet dbResult, @NotNull GenericTableBase table, String columnName, String typeName, int valueType, int sourceType, int ordinalPos, long columnSize, long charLength, Integer scale, Integer precision, int radix, boolean notNull, String remarks, String defaultValue, boolean autoIncrement, boolean autoGenerated) throws DBException {
        return new SQLServerGenericTableColumn(table,
            columnName,
            typeName, valueType, sourceType, ordinalPos,
            columnSize,
            charLength, scale, precision, radix, notNull,
            remarks, defaultValue, autoIncrement, autoGenerated
        );
    }

    @Override
    public boolean isSystemTable(GenericTableBase table) {
        return table.getSchema() != null && getSystemSchema().equals(table.getSchema().getName()) && table.getName().startsWith("sys");
    }

    @NotNull
    private String getSystemSchema() {
        return sqlServer ? SQLServerConstants.SQL_SERVER_SYSTEM_SCHEMA : SQLServerConstants.SYBASE_SYSTEM_SCHEMA;
    }

}

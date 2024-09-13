/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.db.internal;

import org.apache.commons.dbcp2.DriverConnectionFactory;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPApplication;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.InternalDatabaseConfig;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.LoggingProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLDialectSchemaController;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.SystemVariablesResolver;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

public abstract class InternalDB {

    private static final Log log = Log.getLog(InternalDB.class);

    private InternalDatabaseConfig internalDatabaseConfig;

    private DBPApplication dbpApplication;

    public InternalDB(InternalDatabaseConfig internalDatabaseConfig, DBPApplication dbpApplication) {
        this.internalDatabaseConfig = internalDatabaseConfig;
        this.dbpApplication = dbpApplication;
    }

    protected PoolingDataSource<PoolableConnection> initConnectionPool(
        DBPDriver driver,
        String dbURL,
        Properties dbProperties,
        Driver driverInstance
    ) throws SQLException, DBException {
        // Create connection pool with custom connection factory
        log.debug("\tInitiate connection pool with management database (" + driver.getFullName() + "; " + dbURL + ")");
        DriverConnectionFactory conFactory = new DriverConnectionFactory(driverInstance, dbURL, dbProperties);
        PoolableConnectionFactory pcf = new PoolableConnectionFactory(conFactory, null);
        pcf.setValidationQuery(internalDatabaseConfig.getPool().getValidationQuery());

        GenericObjectPoolConfig<PoolableConnection> config = new GenericObjectPoolConfig<>();
        config.setMinIdle(internalDatabaseConfig.getPool().getMinIdleConnections());
        config.setMaxIdle(internalDatabaseConfig.getPool().getMaxIdleConnections());
        config.setMaxTotal(internalDatabaseConfig.getPool().getMaxConnections());
        GenericObjectPool<PoolableConnection> connectionPool = new GenericObjectPool<>(pcf, config);
        pcf.setPool(connectionPool);
        return new PoolingDataSource<>(connectionPool);
    }

    protected String getCurrentInstanceId() throws IOException {
        // 16 chars - workspace ID
        String workspaceId = DBWorkbench.getPlatform().getWorkspace().getWorkspaceId();
        if (workspaceId.length() > 16) {
            workspaceId = workspaceId.substring(0, 16);
        }

        StringBuilder id = new StringBuilder(36);
        id.append("000000000000"); // there was mac address, but it generates dynamically when docker is used
        id.append(":").append(workspaceId).append(":");
        while (id.length() < 36) {
            id.append("X");
        }
        return id.toString();
    }

    /**
     * Replaces all predefined prefixes in sql query.
     */
    @NotNull
    public String normalizeTableNames(@NotNull String sql) {
        return CommonUtils.normalizeTableNames(sql, internalDatabaseConfig.getSchema());
    }

    protected InternalDatabaseConfig getDatabaseConfiguration() {
        return internalDatabaseConfig;
    }

    protected DBPApplication getApplication() {
        return DBWorkbench.getPlatform().getApplication();
    }

    protected static DataSourceProviderRegistry getDataSourceProviderRegistry() {
        return DataSourceProviderRegistry.getInstance();
    }

    protected DBPDriver findDriver(DataSourceProviderRegistry dataSourceProviderRegistry) throws DBException {
        DBPDriver driver = dataSourceProviderRegistry.findDriver(internalDatabaseConfig.getDriver());
        if (driver == null) {
            throw new DBException("Driver '" + internalDatabaseConfig.getDriver() + "' not found");
        }
        return driver;
    }

    @NotNull
    protected static Driver getDriverInstance(DBPDriver driver, DBRProgressMonitor monitor) throws DBException {
        return driver.getDriverInstance(monitor);
    }

    @NotNull
    protected String getDbURL() {
        return GeneralUtils.replaceVariables(internalDatabaseConfig.getUrl(), SystemVariablesResolver.INSTANCE);
    }

    private static boolean isSchemaExist(Connection connection, String schemaExistQuery) throws SQLException {
        return JDBCUtils.executeQuery(connection, schemaExistQuery) != null;
    }

    protected void createSchemaIfNotExists(Connection connection, SQLDialectSchemaController schemaController, String schemaName) {
        if (CommonUtils.isNotEmpty(schemaName)) {
            var schemaExistQuery = schemaController.getSchemaExistQuery(schemaName);

            try {
                if (!isSchemaExist(connection, schemaExistQuery)) {
                    log.info("Schema " + schemaName + " not exist, create new one");
                    String createSchemaQuery = schemaController.getCreateSchemaQuery(schemaName);
                    JDBCUtils.executeStatement(connection, createSchemaQuery);
                }
            } catch (SQLException e) {
                log.error("Failed to create schema: " + schemaName, e);
                try {
                    connection.close();
                } catch (SQLException ex) {
                    log.error("Error closing connection after failure to create schema: " + schemaName, ex);
                }
            }
        }
    }

    protected static void upsertSchemaInfo(Connection connection, String tableName, String schemaName, int version) throws SQLException {
        String updateQuery = CommonUtils.normalizeTableNames(
            "UPDATE {table_prefix}" + tableName + " SET VERSION=?, UPDATE_TIME=CURRENT_TIMESTAMP",
            schemaName
        );

        var updateCount = JDBCUtils.executeUpdate(connection, updateQuery, version);

        if (updateCount <= 0) {
            String insertQuery = CommonUtils.normalizeTableNames(
                "INSERT INTO {table_prefix}" + tableName + " (VERSION, UPDATE_TIME) VALUES(?, CURRENT_TIMESTAMP)",
                schemaName
            );
            JDBCUtils.executeSQL(connection, insertQuery, version);
        }
    }

    public static int getVersionFromSchema(Connection connection, String tableName, String schemaName) throws SQLException {
        String query = CommonUtils.normalizeTableNames("SELECT VERSION FROM {table_prefix}" + tableName, schemaName);
        return CommonUtils.toInt(JDBCUtils.executeQuery(connection, query));
    }

}

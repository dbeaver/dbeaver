/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sql.db;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPConnectionInformation;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderRegistry;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.InternalDatabaseConfig;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLDialectSchemaController;
import org.jkiss.dbeaver.model.sql.schema.ClassLoaderScriptSource;
import org.jkiss.dbeaver.model.sql.schema.SQLSchemaConfig;
import org.jkiss.dbeaver.model.sql.schema.SQLSchemaManager;
import org.jkiss.dbeaver.model.sql.schema.SQLSchemaVersionManager;
import org.jkiss.utils.CommonUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

public abstract class InternalDB<T extends InternalDatabaseConfig> {
    private static final Log log = Log.getLog(InternalDB.class);
    protected final T databaseConfig;
    protected final SQLSchemaConfig schemaConfig;
    private final String name;
    protected SQLDialect dialect;
    protected DataSource dataSource;
    protected DBPConnectionInformation dbConnectionInformation;

    protected InternalDB(@NotNull String name, @NotNull T databaseConfig, @NotNull SQLSchemaConfig config) {
        this.databaseConfig = databaseConfig;
        this.name = name;
        this.schemaConfig = config;
    }

    public synchronized Connection getConnection() {
        try {
            if (dataSource == null) {
                return null;
            }
            var conn = dataSource.getConnection();
            try {
                if (!conn.getAutoCommit()) {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                log.error("Error setting auto-commit state", e);
            }
            return conn;
        } catch (SQLException e) {
            //todo throw it?
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public SQLDialect getDialect() {
        return dialect;
    }

    /**
     * Returns internal database metadata.
     */
    @NotNull
    public DBPConnectionInformation getMetaDataInfo() {
        return dbConnectionInformation;
    }

    /**
     * Replaces all predefined prefixes in sql query.
     */
    @NotNull
    public String normalizeTableNames(@NotNull String sql) {
        return CommonUtils.normalizeTableNames(sql, databaseConfig.getSchema());
    }

    public T getDatabaseConfig() {
        return databaseConfig;
    }

    protected void initSchema(@NotNull DBRProgressMonitor monitor, @NotNull Connection connection) throws Exception {
        DatabaseMetaData metaData = connection.getMetaData();
        initializeSchema(monitor, connection);

        String dbName = metaData.getDatabaseProductName();
        String dbVersion = metaData.getDatabaseProductVersion();
        log.debug("\t" + name + " DB server started (" + dbName + " " + dbVersion + ")");

        dbConnectionInformation = new DBPConnectionInformation(
            databaseConfig.getUrl(),
            databaseConfig.getDriver(),
            dbName,
            dbVersion
        );
    }

    protected abstract void initializeSchema(@NotNull DBRProgressMonitor monitor, @Nullable Connection connection) throws Exception;

    protected void updateSchema(
        @NotNull DBRProgressMonitor monitor,
        @NotNull Connection connection,
        @NotNull ClassLoader classLoader,
        @NotNull SQLSchemaVersionManager versionManager
    ) throws DBException {
        SQLSchemaManager schemaManager = new SQLSchemaManager(
            schemaConfig.schemaId(),
            new ClassLoaderScriptSource(
                classLoader,
                schemaConfig.createScriptPath(),
                schemaConfig.updateScriptPrefix()
            ),
            monitor1 -> connection,
            versionManager,
            dialect,
            schemaConfig.schemaVersionActual(),
            schemaConfig.schemaVersionObsolete(),
            databaseConfig
        );
        schemaManager.updateSchema(monitor);
    }

    protected void createSchemaIfNotExists(@NotNull Connection connection) throws SQLException {
        final String schemaName = databaseConfig.getSchema();
        if (dialect instanceof SQLDialectSchemaController schemaController && CommonUtils.isNotEmpty(schemaName)) {
            var schemaExistQuery = schemaController.getSchemaExistQuery(schemaName);
            boolean schemaExist = JDBCUtils.executeQuery(connection, schemaExistQuery) != null;
            if (!schemaExist) {
                log.info("Schema " + schemaName + " not exist, create new one");
                String createSchemaQuery = schemaController.getCreateSchemaQuery(schemaName);
                try {
                    JDBCUtils.executeStatement(connection, createSchemaQuery);
                } catch (SQLException e) {
                    log.error("Failed to create schema: " + schemaName, e);
                    closeConnection();
                    throw e;
                }
            }
        }
    }

    protected abstract DataSource initConnectionPool(@NotNull Driver driverInstance, @NotNull String fullName);

    @NotNull
    protected DBPDriver getDatabaseDriver(@NotNull DBPDataSourceProviderRegistry dataSourceProviderRegistry) throws DBException {
        if (CommonUtils.isEmpty(databaseConfig.getDriver())) {
            throw new DBException("No database driver configured for CloudBeaver database");
        }
        DBPDriver driver = dataSourceProviderRegistry.findDriver(databaseConfig.getDriver());
        if (driver == null) {
            throw new DBException("Driver '" + databaseConfig.getDriver() + "' not found");
        }
        return driver;
    }

    @NotNull
    protected Properties getProperties() {
        Properties conProperties = new Properties();
        if (!CommonUtils.isEmpty(databaseConfig.getUser())) {
            conProperties.put(DBConstants.DATA_SOURCE_PROPERTY_USER, databaseConfig.getUser());
            if (!CommonUtils.isEmpty(databaseConfig.getPassword())) {
                conProperties.put(DBConstants.DATA_SOURCE_PROPERTY_PASSWORD, databaseConfig.getPassword());
            }
        }
        return conProperties;
    }

    protected void closeConnection() {
        if (dataSource instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception e) {
                log.warn("Error while stopping " + name + " database", e);
            }
            dataSource = null;
        }
    }
}

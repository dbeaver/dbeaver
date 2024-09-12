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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPApplication;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.InternalDatabaseConfig;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

public abstract class InternalDB {

    private static final Log log = Log.getLog(InternalDB.class);

    public static final String SCHEMA_CREATE_SQL_PATH = "db/cb_schema_create.sql";
    public static final String SCHEMA_UPDATE_SQL_PATH = "db/cb_schema_update_";

    private static final int LEGACY_SCHEMA_VERSION = 1;
    private static final int CURRENT_SCHEMA_VERSION = 21;

    private static final String DEFAULT_DB_USER_NAME = "cb-data";
    private static final String DEFAULT_DB_PWD_FILE = ".database-credentials.dat";
    private static final String V1_DB_NAME = "cb.h2.dat";
    private static final String V2_DB_NAME = "cb.h2v2.dat";

    public static final String DATABASE_DEFAULT_DRIVER_CLASS = "org.h2.Driver";
    public static final String EMBEDDED_DATABASE_ID = "qmdb";
    private static final String EMPTY_DOMAIN_NAME = "*";

    private InternalDatabaseConfig internalDatabaseConfig;

    private DBPApplication dbpApplication;
    private static InternalDB instance;

    private String instanceId;

    private SQLDialect dialect;

    private PoolingDataSource<PoolableConnection> dbConnection;

    private transient volatile Connection exclusiveConnection;

    public Connection openConnection() throws SQLException {
        if (exclusiveConnection != null) {
            return exclusiveConnection;
        }
        return dbConnection.getConnection();
    }

    public PoolingDataSource<PoolableConnection> getConnectionPool() {
        return dbConnection;
    }


    public static synchronized void setInstance(@NotNull InternalDB internalDB) throws DBException {
        if (instance != null) {
            throw new DBException("Instance already initialized");
        }
        instance = internalDB;
    }


    @NotNull
    public SQLDialect getDialect() {
        return dialect;
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

    private String getCurrentInstanceId() throws IOException {
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

    private void closeConnection() {
        if (dbConnection != null) {
            try {
                dbConnection.close();
            } catch (Exception e) {
                log.warn("Error while stopping database", e);
            }
            dbConnection = null;
        }
    }

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

}

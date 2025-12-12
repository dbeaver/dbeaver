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
package org.jkiss.dbeaver.ui.config.migration.wizards.pgadmin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.google.gson.annotations.SerializedName;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.registry.DataSourceProviderRegistry;
import org.jkiss.dbeaver.ui.config.migration.wizards.ImportConnectionInfo;
import org.jkiss.dbeaver.ui.config.migration.wizards.ImportData;
import org.jkiss.dbeaver.ui.config.migration.wizards.ImportDriverInfo;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.StandardConstants;
import org.jkiss.utils.StringUtils;

import java.io.Reader;
import java.util.Map;

/**
 * pgAdmin servers.json import
 */
public class PgAdminImportConfigurationService {
    private static final Log log = Log.getLog(PgAdminImportConfigurationService.class);

    private static final String DRIVER_ID_POSTGRESQL = "postgres-jdbc";
    private static final String STORAGE_PLACEHOLDER = "<STORAGE_DIR>";

    public static final PgAdminImportConfigurationService INSTANCE = new PgAdminImportConfigurationService();

    private static final Gson GSON = new GsonBuilder()
        .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
        .create();

    private final ImportDriverInfo postgresqlDriver;

    private PgAdminImportConfigurationService() {
        DataSourceProviderRegistry registry = DataSourceProviderRegistry.getInstance();
        DBPDriver driver = registry.findDriver(DRIVER_ID_POSTGRESQL);
        if (driver != null) {
            this.postgresqlDriver = new ImportDriverInfo(driver);
        } else {
            this.postgresqlDriver = new ImportDriverInfo(
                DRIVER_ID_POSTGRESQL,
                "PostgreSQL",
                "jdbc:postgresql://{host}[:{port}]/{database}",
                "org.postgresql.Driver"
            );
        }
    }

    public void importJSON(@NotNull ImportData importData, @NotNull Reader reader) {
        PgAdminRoot root = GSON.fromJson(reader, PgAdminRoot.class);
        if (root == null || root.servers == null || root.servers.isEmpty()) {
            log.debug("Empty or invalid pgAdmin config JSON or no 'Servers' found");
            return;
        }

        if (importData.getDriverByID(postgresqlDriver.getId()) == null) {
            importData.addDriver(postgresqlDriver);
        }

        for (Map.Entry<String, PgAdminServer> entry : root.servers.entrySet()) {
            PgAdminServer pgAdminServer = entry.getValue();
            if (pgAdminServer == null) {
                continue;
            }

            String host = StringUtils.firstNonEmpty(pgAdminServer.host, pgAdminServer.hostAlt);
            if (CommonUtils.isEmpty(host)) {
                continue;
            }
            String db = pgAdminServer.maintenanceDB;
            String user = pgAdminServer.username;
            String port = String.valueOf(pgAdminServer.port);

            ImportConnectionInfo conn = new ImportConnectionInfo(
                postgresqlDriver,
                null,
                pgAdminServer.name,
                null,
                host,
                port,
                CommonUtils.isEmpty(db) ? null : db,
                CommonUtils.isEmpty(user) ? null : user,
                null
            );

            if (pgAdminServer.connectionParameters != null && !pgAdminServer.connectionParameters.isEmpty()) {
                applyConnectionParameters(conn, pgAdminServer.connectionParameters);
            }

            importData.addConnection(conn);
        }
    }

    @Nullable
    private String expandStorageDir(@Nullable String value) {
        if (CommonUtils.isEmpty(value)) {
            return value;
        }
        String home = System.getProperty(StandardConstants.ENV_USER_HOME);
        return value.replace(STORAGE_PLACEHOLDER, CommonUtils.notEmpty(home));
    }

    private void applyConnectionParameters(@NotNull ImportConnectionInfo conn, @NotNull Map<String, Object> params) {
        for (Map.Entry<String, Object> pe : params.entrySet()) {
            String propertyName = pe.getKey();
            Object propertyValue = pe.getValue();
            if (CommonUtils.isEmpty(propertyName) || propertyValue == null) {
                continue;
            }
            String formattedName = StringUtils.underscoreToCamelCase(propertyName);
            String strVal = expandStorageDir(String.valueOf(propertyValue));
            if (CommonUtils.isNotEmpty(strVal)) {
                conn.setProperty(formattedName, strVal);
            }
        }
    }

    private static class PgAdminRoot {
        @SerializedName("Servers")
        Map<String, PgAdminServer> servers;
    }

    private static class PgAdminServer {
        @SerializedName("Host")
        String host;
        @SerializedName("host")
        String hostAlt;

        @SerializedName("Name")
        String name;

        @SerializedName("Port")
        Integer port;

        @SerializedName("Username")
        String username;

        @SerializedName("MaintenanceDB")
        String maintenanceDB;

        @SerializedName("ConnectionParameters")
        Map<String, Object> connectionParameters;
    }
}

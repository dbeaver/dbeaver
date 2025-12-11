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
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.registry.driver.DriverUtils;
import org.jkiss.dbeaver.ui.config.migration.wizards.ImportConnectionInfo;
import org.jkiss.dbeaver.ui.config.migration.wizards.ImportData;
import org.jkiss.dbeaver.ui.config.migration.wizards.ImportDriverInfo;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.StringUtils;

import java.io.Reader;
import java.util.List;
import java.util.Map;

/**
 * pgAdmin servers.json import
 */
public class PgAdminImportConfigurationService {
    private static final Log log = Log.getLog(PgAdminImportConfigurationService.class);

    private static final String DRIVER_ID_POSTGRESQL = "postgresql";
    private static final String KEY_SERVERS = "Servers";
    private static final String KEY_HOST = "Host";
    private static final String KEY_HOST_ALT = "host";
    private static final String KEY_NAME = "Name";
    private static final String KEY_PORT = "Port";
    private static final String KEY_USERNAME = "Username";
    private static final String KEY_DB = "MaintenanceDB";
    private static final String KEY_CONNECTION_PARAMETERS = "ConnectionParameters";
    private static final String STORAGE_PLACEHOLDER = "<STORAGE_DIR>";

    public static final PgAdminImportConfigurationService INSTANCE = new PgAdminImportConfigurationService();

    private static final Gson GSON = new GsonBuilder()
        .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
        .create();

    private final ImportDriverInfo postgresqlDriver;

    private PgAdminImportConfigurationService() {
        List<DBPDriver> drivers = DriverUtils.getAllDrivers();
        this.postgresqlDriver = drivers.stream()
            .filter(d -> d.getId().equals(DRIVER_ID_POSTGRESQL))
            .findFirst()
            .map(ImportDriverInfo::new)
            .orElse(new ImportDriverInfo(
                DRIVER_ID_POSTGRESQL,
                "PostgreSQL",
                "jdbc:postgresql://{host}[:{port}]/{database}",
                "org.postgresql.Driver"
            ));
    }

    public void importJSON(@NotNull ImportData importData, @NotNull Reader reader) {
        Map<String, Object> root = JSONUtils.parseMap(GSON, reader);
        if (root.isEmpty()) {
            log.debug("Empty or invalid pgAdmin config JSON");
            return;
        }

        Map<String, Object> servers = JSONUtils.getObject(root, KEY_SERVERS);
        if (servers.isEmpty()) {
            log.debug("No 'Servers' found in pgAdmin config");
            return;
        }

        if (importData.getDriverByID(postgresqlDriver.getId()) == null) {
            importData.addDriver(postgresqlDriver);
        }

        for (Map.Entry<String, Object> entry : servers.entrySet()) {
            Object val = entry.getValue();
            if (!(val instanceof Map)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> s = (Map<String, Object>) val;

            String host = StringUtils.firstNonEmpty(JSONUtils.getString(s, KEY_HOST), JSONUtils.getString(s, KEY_HOST_ALT));
            if (CommonUtils.isEmpty(host)) {
                continue;
            }
            String name  = JSONUtils.getString(s, KEY_NAME);
            int portInt = JSONUtils.getInteger(s, KEY_PORT, 5432);
            String port = String.valueOf(portInt);
            String user = JSONUtils.getString(s, KEY_USERNAME);
            String db = JSONUtils.getString(s, KEY_DB);


            ImportConnectionInfo conn = new ImportConnectionInfo(
                postgresqlDriver,
                null,
                name,
                null,
                host,
                port,
                CommonUtils.isEmpty(db) ? null : db,
                CommonUtils.isEmpty(user) ? null : user,
                null
            );

            Object cp = s.get(KEY_CONNECTION_PARAMETERS);
            if (cp instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> params = (Map<String, Object>) cp;
                if (!params.isEmpty()) {
                    applyConnectionParameters(conn, params);
                }
            }

            importData.addConnection(conn);
        }
    }

    @Nullable
    private String expandStorageDir(@Nullable String value) {
        if (CommonUtils.isEmpty(value)) {
            return value;
        }
        String home = System.getProperty("user.home");
        return value.replace(STORAGE_PLACEHOLDER, CommonUtils.notEmpty(home));
    }

    private void applyConnectionParameters(@NotNull ImportConnectionInfo conn, @NotNull Map<String, Object> params) {
        for (Map.Entry<String, Object> pe : params.entrySet()) {
            String propertyName = pe.getKey();
            String propertyValue = String.valueOf(pe.getValue());
            if (CommonUtils.isEmpty(propertyName) || CommonUtils.isEmpty(propertyValue)) {
                continue;
            }
            String formattedName = StringUtils.underScoreToCamelCase(propertyName);
            String strVal = expandStorageDir(propertyValue);
            if (CommonUtils.isNotEmpty(strVal)) {
                conn.setProperty(formattedName, strVal);
            }
        }
    }
}

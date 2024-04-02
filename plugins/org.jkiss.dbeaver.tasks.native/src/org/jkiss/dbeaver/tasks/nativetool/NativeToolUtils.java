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
package org.jkiss.dbeaver.tasks.nativetool;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DatabaseURL;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriverConfigurationType;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public abstract class NativeToolUtils {

    private static final Log log = Log.getLog(NativeToolUtils.class);
    
    public static final String VARIABLE_HOST = DBPConnectionConfiguration.VARIABLE_HOST;
    public static final String VARIABLE_DATABASE = DBPConnectionConfiguration.VARIABLE_DATABASE;
    public static final String VARIABLE_SCHEMA = "schema";
    public static final String VARIABLE_TABLE = "table";
    public static final String VARIABLE_DATE = "date";
    public static final String VARIABLE_TIMESTAMP = "timestamp";
    public static final String VARIABLE_YEAR = "year";
    public static final String VARIABLE_MONTH = "month";
    public static final String VARIABLE_DAY = "day";
    public static final String VARIABLE_HOUR = "hour";
    public static final String VARIABLE_MINUTE = "minute";
    public static final String VARIABLE_CONN_TYPE = DBPConnectionConfiguration.VARIABLE_CONN_TYPE;

    public static final String[] ALL_VARIABLES = {
        VARIABLE_HOST,
        VARIABLE_DATABASE,
        VARIABLE_SCHEMA,
        VARIABLE_TABLE,
        VARIABLE_DATE,
        VARIABLE_TIMESTAMP,
        VARIABLE_YEAR,
        VARIABLE_MONTH,
        VARIABLE_DAY,
        VARIABLE_HOUR,
        VARIABLE_MINUTE,
        VARIABLE_CONN_TYPE
    };
    public static final String[] LIMITED_VARIABLES = {
        VARIABLE_HOST,
        VARIABLE_DATABASE,
        VARIABLE_DATE,
        VARIABLE_TIMESTAMP,
        VARIABLE_YEAR,
        VARIABLE_MONTH,
        VARIABLE_DAY,
        VARIABLE_HOUR,
        VARIABLE_MINUTE,
        VARIABLE_CONN_TYPE
    };

    public static boolean isSecureString(AbstractNativeToolSettings settings, String string) {
        String userPassword = settings.getDataSourceContainer().getActualConnectionConfiguration().getUserPassword();
        String toolUserPassword = settings.getToolUserPassword();
        return !CommonUtils.isEmpty(toolUserPassword) && string.endsWith(toolUserPassword) ||
            !CommonUtils.isEmpty(userPassword) && string.endsWith(userPassword);
    }

    @NotNull
    public static String replaceVariables(@NotNull String name) {
        switch (name) {
            case NativeToolUtils.VARIABLE_DATE:
                return RuntimeUtils.getCurrentDate();
            case NativeToolUtils.VARIABLE_YEAR:
                return new SimpleDateFormat("yyyy").format(new Date());
            case NativeToolUtils.VARIABLE_MONTH:
                return new SimpleDateFormat("MM").format(new Date());
            case NativeToolUtils.VARIABLE_DAY:
                return new SimpleDateFormat("dd").format(new Date());
            case NativeToolUtils.VARIABLE_HOUR:
                return new SimpleDateFormat("HH").format(new Date());
            case NativeToolUtils.VARIABLE_MINUTE:
                return new SimpleDateFormat("mm").format(new Date());
            default:
                return System.getProperty(name);
        }
    }
    
    /**
     * Appends {@code --host} and {@code --port} parameters from connection settings to the list of command parameters
     */
    public static void addHostAndPortParamsToCmd(@NotNull DBPDataSourceContainer dataSourceContainer, @NotNull List<String> cmd) {
        DBPConnectionConfiguration connectionInfo = dataSourceContainer.getActualConnectionConfiguration();
        String hostname = "";
        String port = "";
        if (connectionInfo.getConfigurationType().equals(DBPDriverConfigurationType.MANUAL)) {
            hostname = connectionInfo.getHostName();
            port = connectionInfo.getHostPort();
        } else {
            String url = connectionInfo.getUrl();
            String sampleUrl = dataSourceContainer.getDriver().getSampleURL();
            if (sampleUrl != null) {
                DBPConnectionConfiguration config = DatabaseURL.extractConfigurationFromUrl(sampleUrl, url);
                if (config != null) {
                    hostname = config.getHostName();
                    port = config.getHostPort();
                }
            } else {
                try {
                    URI uri = URI.create(url);
                    hostname = uri.getHost();
                    port = String.valueOf(uri.getPort());
                } catch (Exception e) {
                    log.error("Can't parse connection URL", e);
                }
            }
        }
        cmd.add("--host=" + hostname);
        if (!CommonUtils.isEmpty(port)) {
            cmd.add("--port=" + port);
        }
    }
}

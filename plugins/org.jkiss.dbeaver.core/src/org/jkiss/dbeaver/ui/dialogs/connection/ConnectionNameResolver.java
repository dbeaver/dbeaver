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
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.net.DBWUtils;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.IVariableResolver;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.util.StringTokenizer;

public class ConnectionNameResolver implements IVariableResolver {
    private final DataSourceDescriptor descriptor;
    private final DBPConnectionConfiguration configuration;
    private final DBPDataSourceContainer dataSourceContainer;

    public static final String[] CONNECTION_NAME_VARIABLES = new String[]{
        DBPConnectionConfiguration.VARIABLE_HOST,
        DBPConnectionConfiguration.VARIABLE_HOST_TUNNEL,
        DBPConnectionConfiguration.VARIABLE_PORT,
        DBPConnectionConfiguration.VARIABLE_SERVER,
        DBPConnectionConfiguration.VARIABLE_DATABASE,
        DBPConnectionConfiguration.VARIABLE_USER,
        DBPConnectionConfiguration.VARIABLE_URL,
        DBPConnectionConfiguration.VARIABLE_CONN_TYPE,
        DBPConnectionConfiguration.VAR_PROJECT_NAME,
        DBPConnectionConfiguration.VARIABLE_DATE,
        DBPConnectionConfiguration.VAR_HOST_OR_DATABASE
    };

    public static final String[][] CONNECTION_NAME_VARIABLES_INFO = new String[][]{
        {DBPConnectionConfiguration.VARIABLE_HOST, "target database host"},
        {DBPConnectionConfiguration.VARIABLE_HOST_TUNNEL, "tunnel database host"},
        {DBPConnectionConfiguration.VARIABLE_PORT, "target database port"},
        {DBPConnectionConfiguration.VARIABLE_SERVER, "target server name"},
        {DBPConnectionConfiguration.VARIABLE_DATABASE, "target database name"},
        {DBPConnectionConfiguration.VARIABLE_USER, "database user name"},
        {DBPConnectionConfiguration.VARIABLE_URL, "connection URL"},
        {DBPConnectionConfiguration.VARIABLE_CONN_TYPE, "connection type"},
        {DBPConnectionConfiguration.VAR_PROJECT_NAME, "project name"},
        {DBPConnectionConfiguration.VARIABLE_DATE, "current date"},
        {DBPConnectionConfiguration.VAR_HOST_OR_DATABASE, "Legacy configuration for the connection name"}
    };

    @NotNull
    public static String[] getConnectionVariables() {
        return CONNECTION_NAME_VARIABLES;
    }

    ;

    @NotNull
    public static String[][] getConnectionVariablesInfo() {
        return CONNECTION_NAME_VARIABLES_INFO;
    }

    public ConnectionNameResolver(DBPDataSourceContainer dataSourceContainer, DBPConnectionConfiguration configuration, @Nullable DataSourceDescriptor descriptor) {
        this.dataSourceContainer = dataSourceContainer;
        this.configuration = configuration;
        this.descriptor = descriptor;
    }

    public DBPDataSourceContainer getDataSourceContainer() {
        return dataSourceContainer;
    }

    public DBPConnectionConfiguration getConfiguration() {
        return configuration;
    }

    @NotNull
    private String generateLegacyConnectionName() {
        String newName = descriptor == null ? "" : getDataSourceContainer().getName(); //$NON-NLS-1$
        if (CommonUtils.isEmpty(newName)) {
            newName = getConfiguration().getDatabaseName();
            if (CommonUtils.isEmpty(newName) || newName.length() < 3 || CommonUtils.isInt(newName)) {
                // Database name is too short or not a string
                newName = getConfiguration().getHostName();
            }
            if (CommonUtils.isEmpty(newName)) {
                newName = getConfiguration().getServerName();
            }
            if (CommonUtils.isEmpty(newName)) {
                newName = getDataSourceContainer().getDriver().getName();
            }
            if (CommonUtils.isEmpty(newName)) {
                newName = CoreMessages.dialog_connection_wizard_final_default_new_connection_name;
            }
            StringTokenizer st = new StringTokenizer(newName, "/\\:,?=%$#@!^&*()"); //$NON-NLS-1$
            while (st.hasMoreTokens()) {
                String nextPart = st.nextToken();
                if (nextPart.matches("[0-9]+")) {
                    continue;
                }
                newName = nextPart;
            }
            //newName = settings.getDriver().getName() + " - " + newName; //$NON-NLS-1$
            newName = CommonUtils.truncateString(newName, 50);
        }
        return newName;
    }

    @Override
    public String get(String name) {
        if (configuration != null) {
            switch (name) {
                case DBPConnectionConfiguration.VARIABLE_HOST:
                    return configuration.getHostName();
                case DBPConnectionConfiguration.VARIABLE_HOST_TUNNEL:
                    return DBWUtils.getTargetTunnelHostName(dataSourceContainer, configuration);
                case DBPConnectionConfiguration.VARIABLE_PORT:
                    return configuration.getHostPort();
                case DBPConnectionConfiguration.VARIABLE_SERVER:
                    return configuration.getServerName();
                case DBPConnectionConfiguration.VARIABLE_DATABASE:
                    return configuration.getDatabaseName();
                case DBPConnectionConfiguration.VARIABLE_USER:
                    return configuration.getUserName();
                case DBPConnectionConfiguration.VAR_HOST_OR_DATABASE:
                    return generateLegacyConnectionName();
                case DBPConnectionConfiguration.VARIABLE_URL:
                    return configuration.getUrl();
                case DBPConnectionConfiguration.VARIABLE_CONN_TYPE:
                case DBPConnectionConfiguration.VARIABLE_CONN_TYPE_LEGACY:
                    return configuration.getConnectionType().getId();
                default:
                    break;
            }
        }
        if (dataSourceContainer != null) {
            switch (name) {
                case DBPConnectionConfiguration.VAR_PROJECT_NAME:
                    return dataSourceContainer.getProject().getName();
                case DBPConnectionConfiguration.VARIABLE_DATE:
                    return RuntimeUtils.getCurrentDate();
                default:
                    break;
            }
        }
        return System.getenv(name);
    }
}

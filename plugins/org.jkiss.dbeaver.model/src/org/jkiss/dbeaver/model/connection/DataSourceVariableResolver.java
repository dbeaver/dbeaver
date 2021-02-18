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
package org.jkiss.dbeaver.model.connection;

import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.utils.SystemVariablesResolver;

public class DataSourceVariableResolver extends SystemVariablesResolver {
    private final DBPDataSourceContainer dataSourceContainer;
    private final DBPConnectionConfiguration configuration;

    public DataSourceVariableResolver(DBPDataSourceContainer dataSourceContainer, DBPConnectionConfiguration configuration) {
        this.dataSourceContainer = dataSourceContainer;
        this.configuration = configuration;
    }

    @Override
    public String get(String name) {
        switch (name) {
            case DBPConnectionConfiguration.VARIABLE_HOST:
                return configuration.getHostName();
            case DBPConnectionConfiguration.VARIABLE_PORT:
                return configuration.getHostPort();
            case DBPConnectionConfiguration.VARIABLE_SERVER:
                return configuration.getServerName();
            case DBPConnectionConfiguration.VARIABLE_DATABASE:
                return configuration.getDatabaseName();
            case DBPConnectionConfiguration.VARIABLE_USER:
                return configuration.getUserName();
            case DBPConnectionConfiguration.VARIABLE_PASSWORD:
                return configuration.getUserPassword();
            case DBPConnectionConfiguration.VARIABLE_URL:
                return configuration.getUrl();
            case DBPConnectionConfiguration.VARIABLE_CONN_TYPE:
                return configuration.getConnectionType().getId();
            case DBPConnectionConfiguration.VAR_PROJECT_PATH:
                return dataSourceContainer == null ? null : dataSourceContainer.getProject().getAbsolutePath().getAbsolutePath();
            case DBPConnectionConfiguration.VAR_PROJECT_NAME:
                return dataSourceContainer == null ? null : dataSourceContainer.getProject().getName();
            default:
                String var = super.get(name);
                if (var != null) {
                    return var;
                }
                var = System.getProperty(name);
                if (var != null) {
                    return var;
                }
                return System.getenv(name);
        }
    }
}

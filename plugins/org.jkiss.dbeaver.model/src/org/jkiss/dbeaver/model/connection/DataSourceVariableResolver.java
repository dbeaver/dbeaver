/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.dbeaver.utils.SystemVariablesResolver;

public class DataSourceVariableResolver extends SystemVariablesResolver {
    private final DBPDataSourceContainer dataSourceContainer;
    private final DBPConnectionConfiguration configuration;

    public DataSourceVariableResolver(
        @Nullable DBPDataSourceContainer dataSourceContainer, @Nullable DBPConnectionConfiguration configuration) {
        this.dataSourceContainer = dataSourceContainer;
        this.configuration = configuration;
    }

    public boolean isSecure() {
        return true;
    }

    protected DBPDataSourceContainer getDataSourceContainer() {
        return dataSourceContainer;
    }

    protected DBPConnectionConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public String get(String name) {
        if (configuration != null) {
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
                case DBPConnectionConfiguration.VARIABLE_URL:
                    return configuration.getUrl();
                case DBPConnectionConfiguration.VARIABLE_CONN_TYPE:
                    return configuration.getConnectionType().getId();
            }
            if (DBPConnectionConfiguration.VARIABLE_PASSWORD.equals(name) && isSecure()) {
                return configuration.getUserPassword();
            }
        }
        if (dataSourceContainer != null) {
            switch (name) {
                case DBPConnectionConfiguration.VARIABLE_DATASOURCE:
                    return dataSourceContainer.getName();
                case DBPConnectionConfiguration.VAR_PROJECT_PATH:
                    return dataSourceContainer.getProject().getAbsolutePath().toAbsolutePath().toString();
                case DBPConnectionConfiguration.VAR_PROJECT_NAME:
                    return dataSourceContainer.getProject().getName();
                case DBPConnectionConfiguration.VARIABLE_DATE:
                    return RuntimeUtils.getCurrentDate();
            }
        }
        return super.get(name);
    }
}

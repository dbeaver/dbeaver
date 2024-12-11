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
package org.jkiss.dbeaver.model.connection;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceOrigin;
import org.jkiss.dbeaver.model.net.DBWUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.dbeaver.utils.SystemVariablesResolver;
import org.jkiss.utils.BeanUtils;

public class DataSourceVariableResolver extends SystemVariablesResolver {
    private static final Log log = Log.getLog(DataSourceVariableResolver.class);

    private final DBPDataSourceContainer dataSourceContainer;
    private final DBPConnectionConfiguration configuration;

    public DataSourceVariableResolver(
        @Nullable DBPDataSourceContainer dataSourceContainer, @Nullable DBPConnectionConfiguration configuration) {
        this.dataSourceContainer = dataSourceContainer;
        this.configuration = configuration;
    }

    public boolean isSecure() {
        return false; // see dbeaver/pro#1861
    }

    public DBPDataSourceContainer getDataSourceContainer() {
        return dataSourceContainer;
    }

    protected DBPConnectionConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    protected boolean isResolveSystemVariables() {
        return DBWorkbench.getPlatform().getApplication().isEnvironmentVariablesAccessible();
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
                case DBPConnectionConfiguration.VARIABLE_URL:
                    return configuration.getUrl();
                case DBPConnectionConfiguration.VARIABLE_CONN_TYPE:
                case DBPConnectionConfiguration.VARIABLE_CONN_TYPE_LEGACY:
                    return configuration.getConnectionType().getId();
            }
            // isSecure() is always false here due to dbeaver/pro#1861
            if (DBPConnectionConfiguration.VARIABLE_PASSWORD.equals(name) && isSecure()) {
                return configuration.getUserPassword();
            }
            if (name.startsWith(DBPConnectionConfiguration.VARIABLE_PREFIX_PROPERTIES)) {
                return configuration.getProperty(
                    name.substring(DBPConnectionConfiguration.VARIABLE_PREFIX_PROPERTIES.length()));
            }
            if (name.startsWith(DBPConnectionConfiguration.VARIABLE_PREFIX_AUTH)) {
                return configuration.getAuthProperty(
                    name.substring(DBPConnectionConfiguration.VARIABLE_PREFIX_AUTH.length()));
            }
            String propValue = configuration.getProperty(name);
            if (propValue != null) {
                return propValue;
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

            if (name.startsWith(DBPConnectionConfiguration.VARIABLE_PREFIX_ORIGIN)) {
                String originProperty = name.substring(DBPConnectionConfiguration.VARIABLE_PREFIX_ORIGIN.length());
                DBPDataSourceOrigin origin = dataSourceContainer.getOrigin();
                try {
                    Object value = BeanUtils.readObjectProperty(origin, originProperty);
                    if (value != null) {
                        return value.toString();
                    }
                } catch (Exception e) {
                    log.debug("Invalid datasource origin property '" + originProperty + "': " + e.getMessage(), e);
                }
            }
            if (name.startsWith(DBPConnectionConfiguration.VARIABLE_PREFIX_TAG)) {
                return dataSourceContainer.getTagValue(
                    name.substring(DBPConnectionConfiguration.VARIABLE_PREFIX_TAG.length()));
            }

        }

        return super.get(name);
    }
}

/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.snowflake;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.snowflake.model.SnowflakeDataSource;
import org.jkiss.dbeaver.ext.snowflake.model.SnowflakeMetaModel;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceProvider;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

public class SnowflakeDataSourceProvider extends JDBCDataSourceProvider {

    private static final Log log = Log.getLog(SnowflakeDataSourceProvider.class);

    public SnowflakeDataSourceProvider()
    {
    }

    @Override
    public void init(@NotNull DBPPlatform platform) {

    }

    @Override
    public long getFeatures()
    {
        return FEATURE_SCHEMAS;
    }

    @Override
    public String getConnectionURL(DBPDriver driver, DBPConnectionConfiguration connectionInfo)
    {
        StringBuilder url = new StringBuilder();
        url.append("jdbc:snowflake://").append(connectionInfo.getHostName());
        if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
            url.append(":").append(connectionInfo.getHostPort());
        }
        url.append("/?");
        boolean hasParam = addParameter(url, "db", connectionInfo.getDatabaseName(), false);
        hasParam = addParameter(url, "warehouse", connectionInfo.getServerName(), hasParam);
        hasParam = addParameter(url, "schema", connectionInfo.getProviderProperty(SnowflakeConstants.PROP_SCHEMA), hasParam);
        addParameter(url, "role", connectionInfo.getProviderProperty(SnowflakeConstants.PROP_ROLE), hasParam);

        return url.toString();
    }

    private static boolean addParameter(StringBuilder url, String name, String value, boolean hasParam) {
        if (!CommonUtils.isEmpty(value)) {
            if (hasParam) url.append("&");
            url.append(name).append("=").append(value);
            return true;
        }
        return hasParam;
    }

    @NotNull
    @Override
    public DBPDataSource openDataSource(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSourceContainer container)
        throws DBException
    {
        return new SnowflakeDataSource(monitor, container, new SnowflakeMetaModel());
    }

}

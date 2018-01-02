/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2010-2017 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.mssql;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mssql.model.SQLServerDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPClientHome;
import org.jkiss.dbeaver.model.connection.DBPClientManager;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceProvider;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SQLServerDataSourceProvider extends JDBCDataSourceProvider implements DBPClientManager {

    private static Map<String,String> connectionsProps;

    static {
        connectionsProps = new HashMap<>();
    }

    public static Map<String,String> getConnectionsProps() {
        return connectionsProps;
    }

    public SQLServerDataSourceProvider()
    {
    }

    @Override
    public long getFeatures() {
        return FEATURE_CATALOGS | FEATURE_SCHEMAS;
    }

    @Override
    public String getConnectionURL(DBPDriver driver, DBPConnectionConfiguration connectionInfo) {
        StringBuilder url = new StringBuilder();
        boolean isJtds = SQLServerUtils.isDriverJtds(driver);
        boolean isSqlServer = SQLServerUtils.isDriverSqlServer(driver);
        if (isSqlServer) {
            // SQL Server
            if (isJtds) {
                url.append("jdbc:jtds:sqlserver://");
            } else {
                url.append("jdbc:sqlserver://");
            }
            url.append(connectionInfo.getHostName());
            if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
                url.append(":").append(connectionInfo.getHostPort());
            }
            if (isJtds) {
                if (!CommonUtils.isEmpty(connectionInfo.getDatabaseName())) {
                    url.append("/").append(connectionInfo.getDatabaseName());
                }
            } else {
                url.append(";");
                if (!CommonUtils.isEmpty(connectionInfo.getDatabaseName())) {
                    url.append("databaseName=").append(connectionInfo.getDatabaseName());
                }
            }
            if ("TRUE".equalsIgnoreCase(connectionInfo.getProviderProperty(SQLServerConstants.PROP_CONNECTION_WINDOWS_AUTH))) {
                url.append(";integratedSecurity=true");
            }
        } else {
            // Sybase
            if (isJtds) {
                url.append("jdbc:jtds:sybase://");
                url.append(connectionInfo.getHostName());
                if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
                    url.append(":").append(connectionInfo.getHostPort());
                }
                if (!CommonUtils.isEmpty(connectionInfo.getDatabaseName())) {
                    url.append("/").append(connectionInfo.getDatabaseName());
                }
            } else {
                url.append("jdbc:sybase:Tds:");
                url.append(connectionInfo.getHostName());
                if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
                    url.append(":").append(connectionInfo.getHostPort());
                }
                if (!CommonUtils.isEmpty(connectionInfo.getDatabaseName())) {
                    url.append("?ServiceName=").append(connectionInfo.getDatabaseName());
                }
            }
        }

        return url.toString();
    }

    @NotNull
    @Override
    public DBPDataSource openDataSource(
            @NotNull DBRProgressMonitor monitor,
            @NotNull DBPDataSourceContainer container)
            throws DBException
    {
        return new SQLServerDataSource(monitor, container);
    }

    @Override
    public Collection<String> findClientHomeIds() {
        return Collections.emptyList();
    }

    @Override
    public String getDefaultClientHomeId() {
        return null;
    }

    @Override
    public DBPClientHome getClientHome(String homeId) {
        return null;
    }
}

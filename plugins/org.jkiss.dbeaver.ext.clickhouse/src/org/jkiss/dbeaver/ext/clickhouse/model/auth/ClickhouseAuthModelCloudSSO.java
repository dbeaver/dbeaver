/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.clickhouse.model.auth;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.Properties;

/**
 * Browser based single sign-on to ClickHouse Cloud.
 * <p>
 * The user signs in with the ClickHouse Cloud identity provider, and the resulting token is exchanged
 * for a JWT scoped to the target service. This is the same flow as {@code clickhouse-client --login}.
 */
public class ClickhouseAuthModelCloudSSO extends ClickhouseAuthModelJWTBase {
    private static final Log log = Log.getLog(ClickhouseAuthModelCloudSSO.class);

    public static final String ID = "clickhouse_cloud_sso";

    private static final String PROP_SSL = "ssl";

    @Override
    public Object initAuthentication(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSource dataSource,
        @NotNull ClickhouseJWTCredentials credentials,
        @NotNull DBPConnectionConfiguration configuration,
        @NotNull Properties connProperties
    ) throws DBException {
        // ClickHouse Cloud only listens for HTTPS, and the driver would otherwise send the JWT
        // in cleartext, so SSL is enforced even when the connection settings disable it
        Object sslSetting = connProperties.put(PROP_SSL, "true");
        if (sslSetting != null && !CommonUtils.toBoolean(sslSetting.toString())) {
            log.debug("SSL is disabled in the connection settings, enabling it for ClickHouse Cloud SSO");
        }
        return super.initAuthentication(monitor, dataSource, credentials, configuration, connProperties);
    }

    @NotNull
    @Override
    protected String getProviderKey(
        @NotNull DBPDataSourceContainer dataSource,
        @NotNull DBPConnectionConfiguration configuration
    ) {
        // Tokens are issued for a particular service, so the host name is a part of the key
        return dataSource.getId() + ":" + CommonUtils.notEmpty(configuration.getHostName());
    }

    @NotNull
    @Override
    protected ClickhouseJWTProvider getProvider(
        @NotNull DBPDataSourceContainer dataSource,
        @NotNull DBPConnectionConfiguration configuration
    ) throws DBException {
        String hostName = configuration.getHostName();
        if (CommonUtils.isEmpty(hostName)) {
            throw new DBException("Host name is not specified");
        }
        String key = getProviderKey(dataSource, configuration);
        ClickhouseJWTProvider provider = ClickhouseJWTProviderRegistry.get(key);
        if (provider != null) {
            return provider;
        }
        ClickhouseCloudJWTProvider cloudProvider = ClickhouseCloudJWTProvider.create(hostName);
        if (cloudProvider == null) {
            throw new DBException(
                "Host '" + hostName + "' is not a ClickHouse Cloud service. " +
                "Use a different authentication method to connect to it.");
        }
        return ClickhouseJWTProviderRegistry.getOrCreate(key, id -> cloudProvider);
    }
}

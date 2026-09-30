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
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.utils.CommonUtils;

/**
 * Single sign-on with an external OpenID Connect provider (Microsoft Entra ID, Okta, etc.).
 * <p>
 * The access token issued by the provider is passed to ClickHouse as a JWT, so the target service
 * must be configured to trust the same issuer, audience and JWKS.
 */
public class ClickhouseAuthModelOIDC extends ClickhouseAuthModelJWTBase {
    public static final String ID = "clickhouse_oidc";

    /** Settings are stored as auth properties, next to the tokens */
    public static final String PROP_ISSUER = "oidc-issuer";
    public static final String PROP_CLIENT_ID = "oidc-client-id";
    public static final String PROP_CLIENT_SECRET = "oidc-client-secret";
    public static final String PROP_AUDIENCE = "oidc-audience";
    public static final String PROP_SCOPES = "oidc-scopes";
    public static final String PROP_CALLBACK_PORT = "oidc-callback-port";
    public static final String PROP_USE_DEVICE_CODE = "oidc-use-device-code";
    public static final String PROP_EMAIL = "oidc-email";

    @NotNull
    @Override
    protected String getProviderKey(
        @NotNull DBPDataSourceContainer dataSource,
        @NotNull DBPConnectionConfiguration configuration
    ) {
        // Tokens depend on the identity provider and the client, not on the ClickHouse host
        return dataSource.getId() + ":"
            + CommonUtils.notEmpty(configuration.getAuthProperty(PROP_ISSUER)) + ":"
            + CommonUtils.notEmpty(configuration.getAuthProperty(PROP_EMAIL)) + ":"
            + CommonUtils.notEmpty(configuration.getAuthProperty(PROP_CLIENT_ID));
    }

    @NotNull
    @Override
    protected ClickhouseJWTProvider getProvider(
        @NotNull DBPDataSourceContainer dataSource,
        @NotNull DBPConnectionConfiguration configuration
    ) throws DBException {
        String clientId = configuration.getAuthProperty(PROP_CLIENT_ID);
        if (CommonUtils.isEmpty(clientId)) {
            throw new DBException("Client ID must be specified in the connection settings");
        }
        String key = getProviderKey(dataSource, configuration);
        ClickhouseJWTProvider provider = ClickhouseJWTProviderRegistry.get(key);
        if (provider != null) {
            return provider;
        }
        ClickhouseOIDCSettings settings = new ClickhouseOIDCSettings(
            configuration.getAuthProperty(PROP_ISSUER),
            clientId,
            configuration.getAuthProperty(PROP_CLIENT_SECRET),
            configuration.getAuthProperty(PROP_AUDIENCE),
            configuration.getAuthProperty(PROP_SCOPES),
            configuration.getAuthProperty(PROP_EMAIL),
            CommonUtils.toInt(configuration.getAuthProperty(PROP_CALLBACK_PORT), ClickhouseOIDCProvider.DEFAULT_CALLBACK_PORT),
            CommonUtils.toBoolean(configuration.getAuthProperty(PROP_USE_DEVICE_CODE))
        );
        ClickhouseOIDCProvider oidcProvider = new ClickhouseOIDCProvider(settings);
        return ClickhouseJWTProviderRegistry.getOrCreate(key, id -> oidcProvider);
    }
}

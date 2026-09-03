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
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.impl.auth.AuthModelDatabaseNative;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.Properties;

/**
 * Base class of the ClickHouse auth models which authenticate with a JWT instead of a password.
 * <p>
 * The token is passed to the driver in the {@code bearer_token} property, which the ClickHouse JDBC driver
 * turns into an {@code Authorization: Bearer} header.
 */
public abstract class ClickhouseAuthModelJWTBase extends AuthModelDatabaseNative<ClickhouseJWTCredentials> {
    private static final Log log = Log.getLog(ClickhouseAuthModelJWTBase.class);

    /** ClickHouse JDBC driver property holding the JWT, see {@code ClientConfigProperties#BEARERTOKEN_AUTH} */
    public static final String PROP_BEARER_TOKEN = "bearer_token";

    /**
     * Identity provider tokens are kept in the auth properties, which are stored in the secure storage.
     * They are not passed to the driver.
     */
    private static final String AUTH_PROP_IDP_ACCESS_TOKEN = "idp-access-token";
    private static final String AUTH_PROP_IDP_REFRESH_TOKEN = "idp-refresh-token";

    @NotNull
    @Override
    public ClickhouseJWTCredentials createCredentials() {
        return new ClickhouseJWTCredentials();
    }

    @Override
    protected void loadCredentials(
        @NotNull DBPDataSourceContainer dataSource,
        @NotNull DBPConnectionConfiguration configuration,
        @NotNull ClickhouseJWTCredentials credentials
    ) {
        super.loadCredentials(dataSource, configuration, credentials);
        credentials.setIdpAccessToken(configuration.getAuthProperty(AUTH_PROP_IDP_ACCESS_TOKEN));
        credentials.setIdpRefreshToken(configuration.getAuthProperty(AUTH_PROP_IDP_REFRESH_TOKEN));
    }

    @Override
    public void saveCredentials(
        @NotNull DBPDataSourceContainer dataSource,
        @NotNull DBPConnectionConfiguration configuration,
        @NotNull ClickhouseJWTCredentials credentials
    ) {
        super.saveCredentials(dataSource, configuration, credentials);
        configuration.setAuthProperty(AUTH_PROP_IDP_ACCESS_TOKEN, credentials.getIdpAccessToken());
        configuration.setAuthProperty(AUTH_PROP_IDP_REFRESH_TOKEN, credentials.getIdpRefreshToken());
    }

    @Override
    public void refreshCredentials(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSourceContainer dataSource,
        @NotNull DBPConnectionConfiguration configuration,
        @NotNull ClickhouseJWTCredentials credentials
    ) {
        // Each connection gets a freshly minted token: the service scoped ones are short lived,
        // and a token rejected by the server must not be reused
        ClickhouseJWTProvider provider = ClickhouseJWTProviderRegistry.get(getProviderKey(dataSource, configuration));
        if (provider != null) {
            provider.invalidateServiceToken();
        }
    }

    @Override
    public boolean isUserNameApplicable() {
        return false;
    }

    @Override
    public boolean isUserPasswordApplicable() {
        return false;
    }

    @Override
    public Object initAuthentication(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSource dataSource,
        @NotNull ClickhouseJWTCredentials credentials,
        @NotNull DBPConnectionConfiguration configuration,
        @NotNull Properties connProperties
    ) throws DBException {
        DBPDataSourceContainer container = dataSource.getContainer();
        ClickhouseJWTProvider provider = getProvider(container, configuration);
        restoreTokens(provider, credentials);

        connProperties.put(PROP_BEARER_TOKEN, provider.getJWT(monitor));
        persistTokens(provider, container, configuration, credentials);
        // The token fully replaces the user credentials
        connProperties.remove(DBConstants.DATA_SOURCE_PROPERTY_USER);
        connProperties.remove(DBConstants.DATA_SOURCE_PROPERTY_PASSWORD);

        return credentials;
    }

    @Override
    public void endAuthentication(
        @NotNull DBPDataSourceContainer dataSource,
        @NotNull DBPConnectionConfiguration configuration,
        @NotNull Properties connProperties
    ) {
        connProperties.remove(PROP_BEARER_TOKEN);
    }

    @Override
    public void collectConnectionProperties(
        @NotNull DBPDataSourceContainer dataSourceContainer,
        @NotNull ClickhouseJWTCredentials credentials,
        @NotNull DBPConnectionConfiguration configuration,
        @NotNull Properties connectProps,
        boolean collectSecuredProps
    ) {
        // The token is acquired interactively in initAuthentication, there is nothing to collect here
    }

    /**
     * Removes the persisted identity provider tokens, so the next connection requires a new sign in.
     * Callers are expected to drop the cached providers as well.
     */
    public static void clearStoredTokens(@NotNull DBPDataSourceContainer dataSource) {
        DBPConnectionConfiguration configuration = dataSource.getConnectionConfiguration();
        if (configuration.getAuthProperty(AUTH_PROP_IDP_ACCESS_TOKEN) == null
            && configuration.getAuthProperty(AUTH_PROP_IDP_REFRESH_TOKEN) == null
        ) {
            return;
        }
        configuration.setAuthProperty(AUTH_PROP_IDP_ACCESS_TOKEN, null);
        configuration.setAuthProperty(AUTH_PROP_IDP_REFRESH_TOKEN, null);
        if (dataSource.isTemporary()) {
            return;
        }
        try {
            dataSource.persistConfiguration();
        } catch (Exception e) {
            log.debug("Cannot remove the stored ClickHouse sign-in tokens", e);
        }
    }

    /**
     * Seeds a fresh provider with the tokens loaded from the secure storage.
     */
    private static void restoreTokens(
        @NotNull ClickhouseJWTProvider provider,
        @NotNull ClickhouseJWTCredentials credentials
    ) {
        if (provider.getIdPAccessToken() == null) {
            provider.setIdPAccessToken(credentials.getIdpAccessToken());
        }
        if (provider.getRefreshToken() == null) {
            provider.setRefreshToken(credentials.getIdpRefreshToken());
        }
    }

    /**
     * Stores the identity provider tokens, so the next application start does not require a new sign in.
     * ClickHouse Cloud does not issue refresh tokens, hence the access token is persisted as well.
     */
    private void persistTokens(
        @NotNull ClickhouseJWTProvider provider,
        @NotNull DBPDataSourceContainer container,
        @NotNull DBPConnectionConfiguration configuration,
        @NotNull ClickhouseJWTCredentials credentials
    ) {
        String accessToken = provider.getIdPAccessToken();
        String refreshToken = provider.getRefreshToken();
        if (CommonUtils.equalObjects(accessToken, credentials.getIdpAccessToken())
            && CommonUtils.equalObjects(refreshToken, credentials.getIdpRefreshToken())
        ) {
            return;
        }
        credentials.setIdpAccessToken(accessToken);
        credentials.setIdpRefreshToken(refreshToken);
        if (container.isTemporary() || !container.isSavePassword()) {
            return;
        }
        configuration.setAuthProperty(AUTH_PROP_IDP_ACCESS_TOKEN, accessToken);
        configuration.setAuthProperty(AUTH_PROP_IDP_REFRESH_TOKEN, refreshToken);
        container.getConnectionConfiguration().setAuthProperty(AUTH_PROP_IDP_ACCESS_TOKEN, accessToken);
        container.getConnectionConfiguration().setAuthProperty(AUTH_PROP_IDP_REFRESH_TOKEN, refreshToken);
        try {
            container.persistConfiguration();
        } catch (Exception e) {
            log.debug("Cannot store ClickHouse Cloud sign-in tokens", e);
        }
    }

    /**
     * Key of the cached provider. Includes everything the tokens depend on.
     */
    @NotNull
    protected abstract String getProviderKey(
        @NotNull DBPDataSourceContainer dataSource,
        @NotNull DBPConnectionConfiguration configuration
    );

    /**
     * Returns the (cached) token provider for the given data source.
     */
    @NotNull
    protected abstract ClickhouseJWTProvider getProvider(
        @NotNull DBPDataSourceContainer dataSource,
        @NotNull DBPConnectionConfiguration configuration
    ) throws DBException;
}

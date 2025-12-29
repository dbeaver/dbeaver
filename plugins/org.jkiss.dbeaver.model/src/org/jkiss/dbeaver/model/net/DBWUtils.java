/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.net;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DatabaseURL;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.utils.CommonUtils;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class DBWUtils {


    public static final String LOOPBACK_HOST_NAME = "127.0.0.1";
    public static final String LOOPBACK_IPV6_HOST_NAME = ":1";
    public static final String LOOPBACK_IPV6_FULL_HOST_NAME = "0:0:0:0:0:0:0:1";
    public static final String LOCALHOST_NAME = "localhost";
    public static final String LOCAL_NAME = "local";
    public static final String SSH_TUNNEL = "ssh_tunnel";

    public static void updateConfigWithTunnelInfo(
        DBWHandlerConfiguration configuration,
        DBPConnectionConfiguration connectionInfo,
        String localHost,
        int localPort
    ) {
        // Replace database host/port and URL
        if (CommonUtils.isNotEmpty(localHost)) {
            connectionInfo.setHostName(localHost);
        } else if (!LOCALHOST_NAME.equals(connectionInfo.getHostName()) && !LOCAL_NAME.equals(connectionInfo.getHostName())) {
            connectionInfo.setHostName(LOOPBACK_HOST_NAME);
        }
        connectionInfo.setHostPort(Integer.toString(localPort));
        if (configuration.getDriver() != null) {
            // Driver can be null in case of orphan tunnel config (e.g. in network profile)
            String newURL = configuration.getDriver().getConnectionURL(connectionInfo);
            connectionInfo.setUrl(newURL);
        }
    }

    @NotNull
    public static String getTargetTunnelHostName(@Nullable DBPDataSourceContainer dataSourceContainer, @NotNull DBPConnectionConfiguration cfg) {
        String hostText = cfg.getHostName();
        // For localhost ry to get real host name from tunnel configuration
        if (isLocalAddress(hostText)) {
            DBWNetworkProfile networkProfile = dataSourceContainer == null ? null : getNetworkProfile(dataSourceContainer);
            for (DBWHandlerConfiguration hc : cfg.getHandlers()) {
                if (hc.isEnabled() && hc.getType() == DBWHandlerType.TUNNEL) {
                    String tunnelHost = null;
                    if (networkProfile != null) {
                        DBWHandlerConfiguration hCfg = networkProfile.getConfiguration(hc.getHandlerDescriptor());
                        if (hCfg != null) {
                            tunnelHost = getTunnelHostFromConfig(hCfg);
                        }
                    }
                    if (tunnelHost == null) {
                        tunnelHost = getTunnelHostFromConfig(hc);
                    }
                    if (!CommonUtils.isEmpty(tunnelHost)) {
                        hostText = tunnelHost;
                        break;
                    }
                }
            }
        }
        return CommonUtils.notEmpty(hostText);
    }

    public static @Nullable String getTunnelHostFromConfig(DBWHandlerConfiguration hc) {
        String host = hc.getStringProperty(DBWHandlerConfiguration.PROP_HOST);
        if (CommonUtils.isEmpty(host)) {
            return null;
        }
        return host;
    }

    public static boolean isLocalAddress(String hostText) {
        return CommonUtils.isEmpty(hostText) ||
            hostText.equals(LOCALHOST_NAME) ||
            hostText.equals(LOCAL_NAME) ||
            hostText.equals(LOOPBACK_HOST_NAME) ||
            hostText.equals(LOOPBACK_IPV6_HOST_NAME) ||
            hostText.equals(LOOPBACK_IPV6_FULL_HOST_NAME);
    }

    @Nullable
    public static DBWNetworkProfile getNetworkProfile(@NotNull DBPDataSourceContainer dataSourceContainer) {
        DBPConnectionConfiguration cfg = dataSourceContainer.getConnectionConfiguration();
        return CommonUtils.isEmpty(cfg.getConfigProfileName())
            ? null
            : dataSourceContainer.getRegistry().getNetworkProfile(cfg.getConfigProfileSource(), cfg.getConfigProfileName());
    }

    /**
     * Retrieves a list of effectively enabled network handlers
     * for a connection, possible from an active network profile.
     *
     * @param container data source container to retrieve network handlers for
     * @return a list of enabled network handlers
     */
    @NotNull
    public static List<DBWHandlerConfiguration> getActualNetworkHandlers(@NotNull DBPDataSourceContainer container) {
        DBWNetworkProfile profile = getNetworkProfile(container);

        List<DBWHandlerConfiguration> configurations;
        if (profile != null) {
            configurations = profile.getConfigurations();
        } else {
            configurations = container.getConnectionConfiguration().getHandlers();
        }

        return configurations.stream()
            .filter(DBWHandlerConfiguration::isEnabled)
            .toList();
    }


    public record ConnectivityParameters(
        @Nullable String hostName,
        @Nullable String hostPort,
        @Nullable String databaseName,
        @Nullable String userName,
        @Nullable String server
    ) {
    }

    @NotNull
    private static ConnectivityParameters getExplicitConnectivityParameters(@NotNull DBPConnectionConfiguration configuration) {
        String defaultCatalogName = configuration.getBootstrap().getDefaultCatalogName();
        return new ConnectivityParameters(
            configuration.getHostName(),
            configuration.getHostPort(),
            CommonUtils.isNotEmpty(defaultCatalogName) ? defaultCatalogName : configuration.getDatabaseName(),
            configuration.getUserName(),
            configuration.getServerName()
        );
    }

    /**
     * Returns information about connection by its configuration.
     * If the configuration type is URL, it extracts information
     * according to the sample URL template in the driver properties
     * or generic URL template, if sample URL template is empty.
     */
    @NotNull
    public static ConnectivityParameters getConnectivityParameters(
        @NotNull DBPConnectionConfiguration configuration,
        @NotNull DBPDriver driver
    ) throws DBException {
        ConnectivityParameters explicitConfiguration = getExplicitConnectivityParameters(configuration);
        return switch (configuration.getConfigurationType()) {
            case MANUAL -> explicitConfiguration;
            case URL -> {
                String activeUrl = driver.getConnectionURL(configuration);
                if (CommonUtils.isNotEmpty(activeUrl)) {
                    ConnectivityParameters urlConnectivityParams = null;
                    DBPConnectionConfiguration urlConfiguration = null;
                    DatabaseURL.MetaURL metaURL = null;
                    if (CommonUtils.isNotEmpty(driver.getSampleURL())) {
                        urlConfiguration = DatabaseURL.extractConfigurationFromUrl(driver.getSampleURL(), activeUrl);
                        if (urlConfiguration != null) {
                            metaURL = DatabaseURL.parseSampleURL(driver.getSampleURL());
                        }
                    }
                    if (urlConfiguration == null) {
                        urlConfiguration = DatabaseURL.extractConfigurationFromUrl(DatabaseURL.GENERIC_URL_TEMPLATE, activeUrl);
                        if (urlConfiguration != null) {
                            metaURL = DatabaseURL.parseSampleURL(DatabaseURL.GENERIC_URL_TEMPLATE);
                        }
                    }
                    if (urlConfiguration != null) {
                        urlConnectivityParams = getExplicitConnectivityParameters(urlConfiguration);
                    }
                    if (urlConnectivityParams == null) {
                        final String jdbcPrefix = "jdbc:";
                        URI url = URI.create(activeUrl.startsWith(jdbcPrefix) ? activeUrl.substring(jdbcPrefix.length()) : activeUrl);
                        urlConnectivityParams = new ConnectivityParameters(
                            url.getHost(),
                            url.getPort() != -1 ? Integer.toString(url.getPort()) : null,
                            url.getPath() != null && url.getPath().startsWith("/") ? url.getPath().substring(1) : url.getPath(),
                            url.getUserInfo(),
                            null
                        );
                    }
                    Set<String> requiredUrlParts = metaURL != null ? metaURL.getRequiredProperties() : Collections.emptySet();

                    String databaseName = requiredUrlParts.contains(DBConstants.PROP_DATABASE)
                        ? urlConnectivityParams.databaseName()
                        : CommonUtils.isNotEmpty(urlConnectivityParams.databaseName())
                            ? urlConnectivityParams.databaseName()
                            : explicitConfiguration.databaseName();
                    String userName = requiredUrlParts.contains(DBConstants.PROP_USER)
                        ? urlConnectivityParams.userName()
                        : CommonUtils.isNotEmpty(urlConnectivityParams.userName())
                            ? urlConnectivityParams.userName()
                            : explicitConfiguration.userName();
                    yield new ConnectivityParameters(
                        urlConnectivityParams.hostName(),
                        urlConnectivityParams.hostPort(),
                        databaseName,
                        userName,
                        urlConnectivityParams.server()
                    );
                } else {
                    yield new ConnectivityParameters(
                        driver.getDefaultHost(),
                        driver.getDefaultPort(),
                        explicitConfiguration.databaseName(),
                        explicitConfiguration.userName(),
                        null
                    );
                }
            }
        };
    }
}


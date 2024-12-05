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
package org.jkiss.dbeaver.model.net;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.utils.CommonUtils;

public class DBWUtils {


    public static final String LOOPBACK_HOST_NAME = "127.0.0.1";
    public static final String LOOPBACK_IPV6_HOST_NAME = ":1";
    public static final String LOOPBACK_IPV6_FULL_HOST_NAME = "0:0:0:0:0:0:0:1";
    public static final String LOCALHOST_NAME = "localhost";
    public static final String LOCAL_NAME = "local";

    public static void updateConfigWithTunnelInfo(
        DBWHandlerConfiguration configuration,
        DBPConnectionConfiguration connectionInfo,
        String localHost,
        int localPort
    ) {
        // Replace database host/port and URL
        if (CommonUtils.isEmpty(localHost)) {
            connectionInfo.setHostName(LOOPBACK_HOST_NAME);
        } else {
            connectionInfo.setHostName(localHost);
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

    public static @Nullable DBWNetworkProfile getNetworkProfile(@NotNull DBPDataSourceContainer dataSourceContainer) {
        DBPConnectionConfiguration cfg = dataSourceContainer.getConnectionConfiguration();
        return CommonUtils.isEmpty(cfg.getConfigProfileName())
            ? null
            : dataSourceContainer.getRegistry().getNetworkProfile(cfg.getConfigProfileSource(), cfg.getConfigProfileName());
    }
}


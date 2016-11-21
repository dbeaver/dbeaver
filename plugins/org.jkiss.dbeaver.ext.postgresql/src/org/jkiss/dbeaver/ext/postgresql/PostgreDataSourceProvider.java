/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.postgresql;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPClientHome;
import org.jkiss.dbeaver.model.connection.DBPClientManager;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceProvider;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.OSDescriptor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.WinRegistry;
import org.jkiss.utils.CommonUtils;

import java.util.*;

public class PostgreDataSourceProvider extends JDBCDataSourceProvider implements DBPClientManager {

    private static Map<String,String> connectionsProps;

    static {
        connectionsProps = new HashMap<>();

        // Prevent stupid errors "Cannot convert value '0000-00-00 00:00:00' from column X to TIMESTAMP"
        // Widely appears in MyISAM tables (joomla, etc)
        connectionsProps.put("zeroDateTimeBehavior", "convertToNull");
        // Set utf-8 as default charset
        connectionsProps.put("characterEncoding", GeneralUtils.UTF8_ENCODING);
        connectionsProps.put("tinyInt1isBit", "false");
    }

    public static Map<String,String> getConnectionsProps() {
        return connectionsProps;
    }

    public PostgreDataSourceProvider()
    {
    }

    @Override
    public long getFeatures()
    {
        return FEATURE_CATALOGS | FEATURE_SCHEMAS;
    }

    @Override
    public String getConnectionURL(DBPDriver driver, DBPConnectionConfiguration connectionInfo)
    {
        StringBuilder url = new StringBuilder();
        url.append("jdbc:postgresql://")
            .append(connectionInfo.getHostName());
        if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
            url.append(":").append(connectionInfo.getHostPort());
        }
        url.append("/");
        if (!CommonUtils.isEmpty(connectionInfo.getDatabaseName())) {
            url.append(connectionInfo.getDatabaseName());
        }
//        if (CommonUtils.toBoolean(connectionInfo.getProperty(PostgreConstants.PROP_USE_SSL))) {
//            url.append("?ssl=true");
//            if (CommonUtils.toBoolean(connectionInfo.getProperty(PostgreConstants.PROP_SSL_NON_VALIDATING))) {
//                url.append("&sslfactory=org.postgresql.ssl.NonValidatingFactory");
//            }
//        }
        return url.toString();
    }

    @NotNull
    @Override
    public DBPDataSource openDataSource(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSourceContainer container)
        throws DBException
    {
        return new PostgreDataSource(monitor, container);
    }

    ////////////////////////////////////////////////////////////////
    // Local client

    private static Map<String,PostgreServerHome> localServers = null;

    @Override
    public Collection<String> findClientHomeIds()
    {
        findLocalClients();
        Set<String> homes = new LinkedHashSet<>();
        for (PostgreServerHome home : localServers.values()) {
            homes.add(home.getHomeId());
        }
        return homes;
    }

    @Override
    public String getDefaultClientHomeId()
    {
        findLocalClients();
        return localServers.isEmpty() ? null : localServers.values().iterator().next().getHomeId();
    }

    @Override
    public DBPClientHome getClientHome(String homeId)
    {
        return getServerHome(homeId);
    }

    public static PostgreServerHome getServerHome(String homeId)
    {
        findLocalClients();
        PostgreServerHome home = localServers.get(homeId);
        return home == null ? new PostgreServerHome(homeId, homeId, null, null, null) : home;
    }

    public synchronized static void findLocalClients()
    {
        if (localServers != null) {
            return;
        }
        localServers = new LinkedHashMap<>();

        // find homes in Windows registry
        OSDescriptor localSystem = DBeaverCore.getInstance().getLocalSystem();
        if (localSystem.isWindows()) {
            try {
                List<String> homeKeys = WinRegistry.readStringSubKeys(WinRegistry.HKEY_LOCAL_MACHINE, PostgreConstants.PG_INSTALL_REG_KEY);
                if (homeKeys != null) {
                    for (String homeKey : homeKeys) {
                        Map<String, String> valuesMap = WinRegistry.readStringValues(WinRegistry.HKEY_LOCAL_MACHINE, PostgreConstants.PG_INSTALL_REG_KEY + "\\" + homeKey);
                        if (valuesMap != null) {
                            for (String key : valuesMap.keySet()) {
                                if (PostgreConstants.PG_INSTALL_PROP_BASE_DIRECTORY.equalsIgnoreCase(key)) {
                                    String baseDir = CommonUtils.removeTrailingSlash(valuesMap.get(PostgreConstants.PG_INSTALL_PROP_BASE_DIRECTORY));
                                    String version = valuesMap.get(PostgreConstants.PG_INSTALL_PROP_VERSION);
                                    String branding = valuesMap.get(PostgreConstants.PG_INSTALL_PROP_BRANDING);
                                    String dataDir = valuesMap.get(PostgreConstants.PG_INSTALL_PROP_DATA_DIRECTORY);
                                    localServers.put(homeKey, new PostgreServerHome(homeKey, baseDir, version, branding, dataDir));
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (Throwable e) {
                log.warn("Error reading Windows registry", e);
            }
        }
    }
}

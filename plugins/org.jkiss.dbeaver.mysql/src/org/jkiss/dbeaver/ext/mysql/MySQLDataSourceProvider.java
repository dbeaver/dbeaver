/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.mysql;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.core.Log;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceProvider;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.registry.OSDescriptor;
import org.jkiss.dbeaver.utils.WinRegistry;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.util.*;

public class MySQLDataSourceProvider extends JDBCDataSourceProvider implements DBPClientManager {

    static final Log log = Log.getLog(MySQLDataSourceProvider.class);

    private static final String REGISTRY_ROOT_32 = "SOFTWARE\\MySQL AB";
    private static final String REGISTRY_ROOT_64 = "SOFTWARE\\Wow6432Node\\MYSQL AB";
    private static final String SERER_LOCATION_KEY = "Location";
    //private static final String SERER_VERSION_KEY = "Version";

    private static Map<String,MySQLServerHome> localServers = null;
    private static Map<String,String> connectionsProps;

    static {
        connectionsProps = new HashMap<String, String>();

        // Prevent stupid errors "Cannot convert value '0000-00-00 00:00:00' from column X to TIMESTAMP"
        // Widely appears in MyISAM tables (joomla, etc)
        connectionsProps.put("zeroDateTimeBehavior", "convertToNull");
        // Set utf-8 as default charset
        connectionsProps.put("characterEncoding", "utf-8");
        connectionsProps.put("tinyInt1isBit", "false");
        // Auth plugins
//        connectionsProps.put("authenticationPlugins",
//            "com.mysql.jdbc.authentication.MysqlClearPasswordPlugin," +
//            "com.mysql.jdbc.authentication.MysqlOldPasswordPlugin," +
//            "org.jkiss.jdbc.mysql.auth.DialogAuthenticationPlugin");
    }

    public static Map<String,String> getConnectionsProps() {
        return connectionsProps;
    }

    public MySQLDataSourceProvider()
    {
    }

    @Override
    protected String getConnectionPropertyDefaultValue(String name, String value) {
        String ovrValue = connectionsProps.get(name);
        return ovrValue != null ? ovrValue : super.getConnectionPropertyDefaultValue(name, value);
    }

    @Override
    public long getFeatures()
    {
        return FEATURE_CATALOGS;
    }

    @Override
    public String getConnectionURL(DBPDriver driver, DBPConnectionInfo connectionInfo)
    {
        String trustStorePath = "D:/temp/ssl/test-cert-store";

        System.setProperty("javax.net.ssl.keyStore", trustStorePath);
        System.setProperty("javax.net.ssl.keyStorePassword", "password");
        System.setProperty("javax.net.ssl.trustStore", trustStorePath);
        System.setProperty("javax.net.ssl.trustStorePassword", "password");

        StringBuilder url = new StringBuilder();
        url.append("jdbc:mysql://")
            .append(connectionInfo.getHostName());
        if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
            url.append(":").append(connectionInfo.getHostPort());
        }
        url.append("/").append(connectionInfo.getDatabaseName());
        if (CommonUtils.toBoolean(connectionInfo.getProperty(MySQLConstants.PROP_USE_SSL))) {
            url.append("?useSSL=true&requireSSL=true");
        }
        return url.toString();
    }

    @NotNull
    @Override
    public DBPDataSource openDataSource(
        @NotNull DBRProgressMonitor monitor, @NotNull DBSDataSourceContainer container)
        throws DBException
    {
        return new MySQLDataSource(monitor, container);
    }

    //////////////////////////////////////
    // Client manager

    @Override
    public Collection<String> findClientHomeIds()
    {
        findLocalClients();
        Set<String> homes = new LinkedHashSet<String>();
        for (MySQLServerHome home : localServers.values()) {
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

    public static MySQLServerHome getServerHome(String homeId)
    {
        findLocalClients();
        MySQLServerHome home = localServers.get(homeId);
        return home == null ? new MySQLServerHome(homeId, homeId) : home;
    }

    public synchronized static void findLocalClients()
    {
        if (localServers != null) {
            return;
        }
        localServers = new LinkedHashMap<String, MySQLServerHome>();
        // read from path
        String path = System.getenv("PATH");
        if (path != null) {
            for (String token : path.split(System.getProperty("path.separator"))) {
                token = CommonUtils.removeTrailingSlash(token);
                File mysqlFile = new File(token, MySQLUtils.getMySQLConsoleBinaryName());
                if (mysqlFile.exists()) {
                    File binFolder = mysqlFile.getAbsoluteFile().getParentFile();//.getName()
                    if (binFolder.getName().equalsIgnoreCase("bin")) {
                    	String homeId = CommonUtils.removeTrailingSlash(binFolder.getParentFile().getAbsolutePath());
                        localServers.put(homeId, new MySQLServerHome(homeId, null));
                    }
                }
            }
        }

        // find homes in Windows registry
        OSDescriptor localSystem = DBeaverCore.getInstance().getLocalSystem();
        if (localSystem.isWindows()) {
            try {
                final String registryRoot = localSystem.is64() ? REGISTRY_ROOT_64 : REGISTRY_ROOT_32;
                List<String> homeKeys = WinRegistry.readStringSubKeys(WinRegistry.HKEY_LOCAL_MACHINE,
                    registryRoot);
                if (homeKeys != null) {
                    for (String homeKey : homeKeys) {
                        Map<String, String> valuesMap = WinRegistry.readStringValues(WinRegistry.HKEY_LOCAL_MACHINE, registryRoot + "\\" + homeKey);
                        if (valuesMap != null) {
                            for (String key : valuesMap.keySet()) {
                                if (SERER_LOCATION_KEY.equalsIgnoreCase(key)) {
                                    String serverPath = CommonUtils.removeTrailingSlash(valuesMap.get(key));
                                    localServers.put(serverPath, new MySQLServerHome(serverPath, homeKey));
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

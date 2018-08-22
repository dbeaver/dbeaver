/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.mysql;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPClientHome;
import org.jkiss.dbeaver.model.connection.DBPNativeClientLocationManager;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceProvider;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.OSDescriptor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.WinRegistry;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.StandardConstants;

import java.io.File;
import java.util.*;

public class MySQLDataSourceProvider extends JDBCDataSourceProvider implements DBPNativeClientLocationManager {

    private static final Log log = Log.getLog(MySQLDataSourceProvider.class);

    private static final String REGISTRY_ROOT_MYSQL_32 = "SOFTWARE\\MySQL AB";
    private static final String REGISTRY_ROOT_MYSQL_64 = "SOFTWARE\\Wow6432Node\\MYSQL AB";
    private static final String REGISTRY_ROOT_MARIADB = "SOFTWARE\\Monty Program AB";
    private static final String SERER_LOCATION_KEY = "Location";
    private static final String INSTALLDIR_KEY = "INSTALLDIR";
    //private static final String SERER_VERSION_KEY = "Version";

    private static Map<String,MySQLServerHome> localServers = null;
    private static Map<String,String> connectionsProps;

    static {
        connectionsProps = new HashMap<>();

        // Prevent stupid errors "Cannot convert value '0000-00-00 00:00:00' from column X to TIMESTAMP"
        // Widely appears in MyISAM tables (joomla, etc)
        //connectionsProps.put("zeroDateTimeBehavior", "CONVERT_TO_NULL");
        // Set utf-8 as default charset
        connectionsProps.put("characterEncoding", GeneralUtils.UTF8_ENCODING);
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
    public String getConnectionURL(DBPDriver driver, DBPConnectionConfiguration connectionInfo)
    {
/*
        String trustStorePath = System.getProperty(StandardConstants.ENV_USER_HOME) + "/.keystore";

        System.setProperty("javax.net.ssl.keyStore", trustStorePath);
        System.setProperty("javax.net.ssl.keyStorePassword", "changeit");
        System.setProperty("javax.net.ssl.trustStore", trustStorePath);
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
*/

        StringBuilder url = new StringBuilder();
        url.append("jdbc:mysql://")
            .append(connectionInfo.getHostName());
        if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
            url.append(":").append(connectionInfo.getHostPort());
        }
        url.append("/");
        if (!CommonUtils.isEmpty(connectionInfo.getDatabaseName())) {
            url.append(connectionInfo.getDatabaseName());
        }

        return url.toString();
    }

    @NotNull
    @Override
    public DBPDataSource openDataSource(
        @NotNull DBRProgressMonitor monitor, @NotNull DBPDataSourceContainer container)
        throws DBException
    {
        return new MySQLDataSource(monitor, container);
    }

    //////////////////////////////////////
    // Client manager

    @Override
    public Collection<String> findNativeClientHomeIds()
    {
        findLocalClients();
        Set<String> homes = new LinkedHashSet<>();
        for (MySQLServerHome home : localServers.values()) {
            homes.add(home.getHomeId());
        }
        return homes;
    }

    @Override
    public String getDefaultNativeClientHomeId()
    {
        findLocalClients();
        return localServers.isEmpty() ? null : localServers.values().iterator().next().getHomeId();
    }

    @Override
    public DBPClientHome getNativeClientHome(String homeId)
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
        localServers = new LinkedHashMap<>();
        // read from path
        String path = System.getenv("PATH");
        if (path != null) {
            for (String token : path.split(System.getProperty(StandardConstants.ENV_PATH_SEPARATOR))) {
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
                // Search MySQL entries
                {
                    final String registryRoot = localSystem.is64() ? REGISTRY_ROOT_MYSQL_64 : REGISTRY_ROOT_MYSQL_32;
                    List<String> homeKeys = WinRegistry.readStringSubKeys(WinRegistry.HKEY_LOCAL_MACHINE, registryRoot);
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
                }
                // Search MariaDB entries
                {
                    List<String> homeKeys = WinRegistry.readStringSubKeys(WinRegistry.HKEY_LOCAL_MACHINE, REGISTRY_ROOT_MARIADB);
                    if (homeKeys != null) {
                        for (String homeKey : homeKeys) {
                            Map<String, String> valuesMap = WinRegistry.readStringValues(WinRegistry.HKEY_LOCAL_MACHINE, REGISTRY_ROOT_MARIADB + "\\" + homeKey);
                            if (valuesMap != null) {
                                for (String key : valuesMap.keySet()) {
                                    if (INSTALLDIR_KEY.equalsIgnoreCase(key)) {
                                        String serverPath = CommonUtils.removeTrailingSlash(valuesMap.get(key));
                                        localServers.put(serverPath, new MySQLServerHome(serverPath, homeKey));
                                        break;
                                    }
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

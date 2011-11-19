/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.model.DBPClientHome;
import org.jkiss.dbeaver.model.DBPClientManager;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceProvider;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.registry.OSDescriptor;
import org.jkiss.dbeaver.utils.WinRegistry;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.util.*;

public class MySQLDataSourceProvider extends JDBCDataSourceProvider implements DBPClientManager {

    static final Log log = LogFactory.getLog(MySQLDataSourceProvider.class);

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

    public long getFeatures()
    {
        return FEATURE_CATALOGS;
    }

    public DBPDataSource openDataSource(
        DBRProgressMonitor monitor, DBSDataSourceContainer container)
        throws DBException
    {
        return new MySQLDataSource(container);
    }

    //////////////////////////////////////
    // Client manager

    public Collection<String> findClientHomeIds()
    {
        findLocalClients();
        Set<String> homes = new LinkedHashSet<String>();
        for (MySQLServerHome home : localServers.values()) {
            homes.add(home.getHomeId());
        }
        return homes;
    }

    public String getDefaultClientHomeId()
    {
        findLocalClients();
        return localServers.isEmpty() ? null : localServers.values().iterator().next().getHomeId();
    }

    public DBPClientHome getClientHome(String homeId)
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
                File mysqlFile = new File(token, getMySQLConsoleBinaryName());
                if (mysqlFile.exists()) {
                    localServers.put(token, new MySQLServerHome(token, null));
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

    private static String getMySQLConsoleBinaryName()
    {
        return DBeaverCore.getInstance().getLocalSystem().isWindows() ? "mysql.exe" : "mysql";
    }

}

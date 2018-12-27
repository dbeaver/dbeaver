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
package org.jkiss.dbeaver.ext.postgresql;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.impls.PostgreServerType;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPNativeClientLocation;
import org.jkiss.dbeaver.model.connection.DBPNativeClientLocationManager;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceProvider;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCURL;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.OSDescriptor;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.dbeaver.utils.WinRegistry;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.*;

public class PostgreDataSourceProvider extends JDBCDataSourceProvider implements DBPNativeClientLocationManager {

    private static Map<String, String> connectionsProps;

    static {
        connectionsProps = new HashMap<>();
    }

    public static Map<String, String> getConnectionsProps() {
        return connectionsProps;
    }

    public PostgreDataSourceProvider() {
    }

    @Override
    public long getFeatures() {
        return FEATURE_CATALOGS | FEATURE_SCHEMAS;
    }

    @Override
    public String getConnectionURL(DBPDriver driver, DBPConnectionConfiguration connectionInfo) {
        PostgreServerType serverType = PostgreUtils.getServerType(driver);
        if (serverType.supportsCustomConnectionURL()) {
            return JDBCURL.generateUrlByTemplate(driver, connectionInfo);
        }

        StringBuilder url = new StringBuilder();
        url.append("jdbc:postgresql://");

        url.append(connectionInfo.getHostName());
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
        throws DBException {
        return new PostgreDataSource(monitor, container);
    }

    ////////////////////////////////////////////////////////////////
    // Local client

    private static Map<String, PostgreServerHome> localServers = null;

    @Override
    public List<DBPNativeClientLocation> findLocalClientLocations() {
        findLocalClients();
        return new ArrayList<>(localServers.values());
    }

    @Override
    public DBPNativeClientLocation getDefaultLocalClientLocation() {
        findLocalClients();
        return localServers.isEmpty() ? null : localServers.values().iterator().next();
    }

    @Override
    public String getProductName(DBPNativeClientLocation location) throws DBException {
        if (location instanceof PostgreServerHome) {
            return ((PostgreServerHome) location).getProductName();
        }
        return "PostgreSQL";
    }

    @Override
    public String getProductVersion(DBPNativeClientLocation location) throws DBException {
        return getFullServerVersion(location.getPath());
    }

    public static PostgreServerHome getServerHome(String homeId) {
        findLocalClients();
        PostgreServerHome home = localServers.get(homeId);
        return home == null ? new PostgreServerHome(homeId, homeId, null, null, null) : home;
    }

    public synchronized static void findLocalClients() {
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

    static String getFullServerVersion(File path)
    {
        File binPath = path;
        File binSubfolder = new File(binPath, "bin");
        if (binSubfolder.exists()) {
            binPath = binSubfolder;
        }

        String cmd = new File(
            binPath,
            RuntimeUtils.getNativeBinaryName("psql")).getAbsolutePath();

        try {
            Process p = Runtime.getRuntime().exec(new String[] {cmd, "--version"});
            try {
                BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
                try {
                    String line;
                    if ((line = input.readLine()) != null) {
                        return line;
                    }
                } finally {
                    IOUtils.close(input);
                }
            } finally {
                p.destroy();
            }
        }
        catch (Exception ex) {
            log.warn("Error reading PostgreSQL native client version from " + cmd, ex);
        }
        return null;
    }

}

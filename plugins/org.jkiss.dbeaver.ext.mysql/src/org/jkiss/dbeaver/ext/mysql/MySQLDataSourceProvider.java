/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mysql.model.MySQLDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.*;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceProvider;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;
import org.jkiss.utils.StandardConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.sql.Driver;
import java.util.*;

public class MySQLDataSourceProvider extends JDBCDataSourceProvider implements DBPNativeClientLocationManager {
    private static final Log log = Log.getLog(MySQLDataSourceProvider.class);

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
        // Tell MySQL to use the (typically longer) interactive_timeout variable as the connection timeout
        // instead of wait_timeout.
        // This longer timeout is for connections directly in use by a human, who'd prefer MySQL not 
        // kill their connection while they were on a coffee break.
        connectionsProps.put("interactiveClient", "true");
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
        boolean needMariDBString = false;
        if (MySQLUtils.isMariaDB(driver)) {
            try {
                Object driverInstance = driver.getDriverInstance(new VoidProgressMonitor());
                if (driverInstance instanceof Driver && ((Driver) driverInstance).getMajorVersion() >= 3) {
                    // Since 3.0 version Maria DB driver only accept `jdbc:mariadb:` classpath by default.
                    needMariDBString = true;
                }
            } catch (DBException e) {
                log.error("Can't recognize MariaDB driver version", e);
            }
        }
        if (needMariDBString) {
            url.append("jdbc:mariadb://");
        } else {
            url.append("jdbc:mysql://");
        }
        url.append(connectionInfo.getHostName());
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
    public List<DBPNativeClientLocation> findLocalClientLocations()
    {
        findLocalClients();
        return new ArrayList<>(localServers.values());
    }

    @Override
    public DBPNativeClientLocation getDefaultLocalClientLocation()
    {
        findLocalClients();
        return localServers.isEmpty() ? null : localServers.values().iterator().next();
    }

    @Override
    public String getProductName(DBPNativeClientLocation location) {
        return "MySQL/MariaDB";
    }

    @Override
    public String getProductVersion(DBPNativeClientLocation location) {
        return getFullServerVersion(location.getPath());
    }

    public static MySQLServerHome getServerHome(String homeId)
    {
        findLocalClients();
        MySQLServerHome home = localServers.get(homeId);
        return home == null ? new MySQLServerHome(homeId, homeId) : home;
    }

    public synchronized static void findLocalClients() {
        if (localServers != null) {
            return;
        }
        localServers = new LinkedHashMap<>();
        // read from path
        String path = System.getenv("PATH");
        if (path != null && RuntimeUtils.isWindows()) {
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
        if (RuntimeUtils.isWindows()) {
            try {
                // Search MySQL entries
                {
                    if (Advapi32Util.registryKeyExists(WinReg.HKEY_LOCAL_MACHINE, REGISTRY_ROOT_MYSQL_64)) {
                        String[] homeKeys = Advapi32Util.registryGetKeys(WinReg.HKEY_LOCAL_MACHINE, REGISTRY_ROOT_MYSQL_64);
                        if (homeKeys != null) {
                            for (String homeKey : homeKeys) {
                                Map<String, Object> valuesMap = Advapi32Util.registryGetValues(WinReg.HKEY_LOCAL_MACHINE, REGISTRY_ROOT_MYSQL_64 + "\\" + homeKey);
                                for (String key : valuesMap.keySet()) {
                                    if (SERER_LOCATION_KEY.equalsIgnoreCase(key)) {
                                        String serverPath = CommonUtils.removeTrailingSlash(CommonUtils.toString(valuesMap.get(key)));
                                        if (new File(serverPath, "bin").exists()) {
                                            localServers.put(serverPath, new MySQLServerHome(serverPath, homeKey));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // Search MariaDB entries
                if (Advapi32Util.registryKeyExists(WinReg.HKEY_LOCAL_MACHINE, REGISTRY_ROOT_MARIADB)) {
                    String[] homeKeys = Advapi32Util.registryGetKeys(WinReg.HKEY_LOCAL_MACHINE, REGISTRY_ROOT_MARIADB);
                    if (homeKeys != null) {
                        for (String homeKey : homeKeys) {
                            Map<String, Object> valuesMap = Advapi32Util.registryGetValues(WinReg.HKEY_LOCAL_MACHINE, REGISTRY_ROOT_MARIADB + "\\" + homeKey);
                            for (String key : valuesMap.keySet()) {
                                if (INSTALLDIR_KEY.equalsIgnoreCase(key)) {
                                    String serverPath = CommonUtils.removeTrailingSlash(CommonUtils.toString(valuesMap.get(key)));
                                    if (new File(serverPath, "bin").exists()) {
                                        localServers.put(serverPath, new MySQLServerHome(serverPath, homeKey));
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Throwable e) {
                log.warn("Error reading Windows registry", e);
            }
        } else if (RuntimeUtils.isMacOS()) {
            Collection<File> mysqlDirs = new ArrayList<>();
            Collections.addAll(
                mysqlDirs,
                NativeClientLocationUtils.getSubdirectoriesWithNamesStartingWith("mysql", new File(NativeClientLocationUtils.USR_LOCAL)) //clients installed via installer downloaded from mysql site
            );
            Collections.addAll(
                mysqlDirs,
                NativeClientLocationUtils.getSubdirectories(NativeClientLocationUtils.getSubdirectoriesWithNamesStartingWith("mysql", new File(NativeClientLocationUtils.HOMEBREW_FORMULAE_LOCATION)))
            );
            Collections.addAll(
                mysqlDirs,
                NativeClientLocationUtils.getSubdirectories(NativeClientLocationUtils.getSubdirectoriesWithNamesStartingWith("mariadb", new File(NativeClientLocationUtils.HOMEBREW_FORMULAE_LOCATION)))
            );
            for (File dir: mysqlDirs) {
                File bin = new File(dir, NativeClientLocationUtils.BIN);
                File binary = new File(bin, MySQLUtils.getMySQLConsoleBinaryName());
                if (!bin.exists() || !bin.isDirectory() || !binary.exists() || !binary.canExecute()) {
                    continue;
                }
                String version = getFullServerVersion(dir);
                if (version == null) {
                    continue;
                }
                String canonicalPath = NativeClientLocationUtils.getCanonicalPath(dir);
                if (canonicalPath.isEmpty()) {
                    continue;
                }
                MySQLServerHome home = new MySQLServerHome(canonicalPath, "MySQL " + version);
                localServers.put(canonicalPath, home);
            }
        }
    }

    @Nullable
    private static String getFullServerVersion(File path) {
        File binPath = path;
        File binSubfolder = new File(binPath, "bin");
        if (binSubfolder.exists()) {
            binPath = binSubfolder;
        }

        String cmd = new File(
            binPath,
            MySQLUtils.getMySQLConsoleBinaryName()).getAbsolutePath();

        try {
            Process p = Runtime.getRuntime().exec(new String[]{cmd, MySQLConstants.FLAG_VERSION});
            try {
                BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
                try {
                    String line;
                    while ((line = input.readLine()) != null) {
                        int pos = line.indexOf("Distrib ");
                        if (pos != -1) {
                            pos += 8;
                            int pos2 = line.indexOf(",", pos);
                            return line.substring(pos, pos2);
                        }
                        pos = line.indexOf("Ver ");
                        if (pos != -1) {
                            pos += 4;
                            int pos2 = line.indexOf(" for ", pos);
                            return line.substring(pos, pos2);
                        }
                    }
                } finally {
                    IOUtils.close(input);
                }
            } finally {
                p.destroy();
            }
        }
        catch (Exception ex) {
            log.warn("Error reading MySQL server version from " + cmd, ex);
        }
        return null;
    }
}

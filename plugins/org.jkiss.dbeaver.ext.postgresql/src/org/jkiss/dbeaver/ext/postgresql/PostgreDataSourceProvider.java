/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.impls.PostgreServerType;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceURLProvider;
import org.jkiss.dbeaver.model.auth.DBAAuthModel;
import org.jkiss.dbeaver.model.connection.*;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceProvider;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCURL;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
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

        DBPPreferenceStore preferenceStore = DBWorkbench.getPlatform().getPreferenceStore();
        if (preferenceStore != null) {
            PrefUtils.setDefaultPreferenceValue(preferenceStore, PostgreConstants.PROP_DD_PLAIN_STRING, false);
            PrefUtils.setDefaultPreferenceValue(preferenceStore, PostgreConstants.PROP_DD_TAG_STRING, false);
        }
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
        DBAAuthModel authModel = connectionInfo.getAuthModel();
        if (authModel instanceof DBPDataSourceURLProvider) {
            return ((DBPDataSourceURLProvider) authModel).getConnectionURL(driver, connectionInfo);
        }
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
    public String getProductName(DBPNativeClientLocation location) {
        if (location instanceof PostgreServerHome) {
            return location.getDisplayName();
        }
        return "PostgreSQL";
    }

    @Override
    public String getProductVersion(DBPNativeClientLocation location) {
        return getFullServerVersion(location.getPath());
    }

    public synchronized static void findLocalClients() {
        if (localServers != null) {
            return;
        }
        localServers = new LinkedHashMap<>();

        // find homes in Windows registry
        if (RuntimeUtils.isWindows()) {
            try {
                if (Advapi32Util.registryKeyExists(WinReg.HKEY_LOCAL_MACHINE, PostgreConstants.PG_INSTALL_REG_KEY)) {
                    String[] homeKeys = Advapi32Util.registryGetKeys(WinReg.HKEY_LOCAL_MACHINE, PostgreConstants.PG_INSTALL_REG_KEY);
                    if (homeKeys != null) {
                        for (String homeKey : homeKeys) {
                            Map<String, Object> valuesMap = Advapi32Util.registryGetValues(WinReg.HKEY_LOCAL_MACHINE, PostgreConstants.PG_INSTALL_REG_KEY + "\\" + homeKey);
                            for (String key : valuesMap.keySet()) {
                                if (PostgreConstants.PG_INSTALL_PROP_BASE_DIRECTORY.equalsIgnoreCase(key)) {
                                    String baseDir = CommonUtils.removeTrailingSlash(CommonUtils.toString(valuesMap.get(PostgreConstants.PG_INSTALL_PROP_BASE_DIRECTORY)));
                                    String branding = CommonUtils.toString(valuesMap.get(PostgreConstants.PG_INSTALL_PROP_BRANDING));
                                    localServers.put(homeKey, new PostgreServerHome(homeKey, baseDir, branding));
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (Throwable e) {
                log.warn("Error reading Windows registry", e);
            }
        } else if (RuntimeUtils.isMacOS()) {
            Collection<File> postgresDirs = new ArrayList<>();
            Collections.addAll(
                postgresDirs,
                NativeClientLocationUtils.getSubdirectories(NativeClientLocationUtils.getSubdirectoriesWithNamesStartingWith("postgresql", new File(NativeClientLocationUtils.HOMEBREW_FORMULAE_LOCATION)))
            );
            Collections.addAll(
                postgresDirs,
                NativeClientLocationUtils.getSubdirectories(new File("/Library/PostgreSQL/")) //standard location for EDB installer
            );
            Collections.addAll(
                postgresDirs,
                NativeClientLocationUtils.getSubdirectories(new File("/Applications/Postgres.app/Contents/versions/"))
            );
            for (File dir: postgresDirs) {
                File bin = new File(dir, NativeClientLocationUtils.BIN);
                File psql = new File(bin, "psql");
                if (!bin.exists() || !bin.isDirectory() || !psql.exists() || !psql.canExecute()) {
                    continue;
                }
                String branding = getBranding(dir);
                if (branding.isEmpty()) {
                    continue;
                }
                String canonicalPath = NativeClientLocationUtils.getCanonicalPath(dir);
                if (canonicalPath.isEmpty()) {
                    continue;
                }
                PostgreServerHome home = new PostgreServerHome(branding, canonicalPath, branding);
                PostgreServerHome duplicate = localServers.putIfAbsent(branding, home);
                if (duplicate == null) {
                    continue;
                }
                localServers.remove(branding);
                home = new PostgreServerHome(canonicalPath, canonicalPath, canonicalPath);
                String duplicatePath = duplicate.getPath().getAbsolutePath();
                duplicate = new PostgreServerHome(duplicatePath, duplicatePath, duplicatePath);
                localServers.put(canonicalPath, home);
                localServers.put(duplicatePath, duplicate);
                //there is a possibility that there will be more that two duplicates. the code above does not account for that
            }
        }
    }

    private static String getBranding(File path) {
        String fullVersion = getFullServerVersion(path);
        if (fullVersion == null) {
            return "";
        }
        if (fullVersion.length() < 7) {
            log.warn("Unable to figure out PostgreSQL server branding from psql output!");
            return "";
        }
        return fullVersion.substring(6).replace(")", "").trim();
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

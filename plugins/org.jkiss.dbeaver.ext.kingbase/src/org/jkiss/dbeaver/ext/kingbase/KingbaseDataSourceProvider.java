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
package org.jkiss.dbeaver.ext.kingbase;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseDataSource;
import org.jkiss.dbeaver.ext.kingbase.model.impls.KingbaseServerType;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceURLProvider;
import org.jkiss.dbeaver.model.DatabaseURL;
import org.jkiss.dbeaver.model.access.DBAAuthModel;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverConfigurationType;
import org.jkiss.dbeaver.model.connection.DBPNativeClientLocation;
import org.jkiss.dbeaver.model.connection.DBPNativeClientLocationManager;
import org.jkiss.dbeaver.model.connection.NativeClientLocationUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceProvider;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.LocalSystemRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

public class KingbaseDataSourceProvider extends JDBCDataSourceProvider implements DBPNativeClientLocationManager {
    private static Map<String, String> connectionsProps;
    @Nullable
    private static Collection<DBPNativeClientLocation> localClients;

    static {
        connectionsProps = new HashMap<>();

        DBPPreferenceStore preferenceStore = DBWorkbench.getPlatform().getPreferenceStore();
        if (preferenceStore != null) {
            PrefUtils.setDefaultPreferenceValue(preferenceStore, KingbaseConstants.PROP_DD_PLAIN_STRING, false);
            PrefUtils.setDefaultPreferenceValue(preferenceStore, KingbaseConstants.PROP_DD_TAG_STRING, false);
        }
    }

    public static Map<String, String> getConnectionsProps() {
        return connectionsProps;
    }

    public KingbaseDataSourceProvider() {
    }

    @Override
    public long getFeatures() {
        return FEATURE_CATALOGS | FEATURE_SCHEMAS;
    }

    @Override
    public String getConnectionURL(DBPDriver driver, DBPConnectionConfiguration connectionInfo) {
        DBAAuthModel authModel = connectionInfo.getAuthModel();
        if (authModel instanceof DBPDataSourceURLProvider) {
            String connectionURL = ((DBPDataSourceURLProvider) authModel).getConnectionURL(driver, connectionInfo);
            if (CommonUtils.isNotEmpty(connectionURL)) {
                return connectionURL;
            }
        }
        if (connectionInfo.getConfigurationType() == DBPDriverConfigurationType.URL) {
            return connectionInfo.getUrl();
        }
        KingbaseServerType serverType = KingbaseUtils.getServerType(driver);
        if (serverType.supportsCustomConnectionURL()) {
            return DatabaseURL.generateUrlByTemplate(driver, connectionInfo);
        }

        StringBuilder url = new StringBuilder();
        url.append("jdbc:kingbase8://");

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
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSourceContainer container)
        throws DBException {
        return new KingbaseDataSource(monitor, container);
    }
    @Override
    public boolean providesDriverClasses() {
        return false;
    }

    @Override
    public List<DBPNativeClientLocation> findLocalClientLocations() {
        return new ArrayList<>(findLocalClients());
    }

    @Override
    public DBPNativeClientLocation getDefaultLocalClientLocation() {
        return CommonUtils.getFirstOrNull(findLocalClientLocations());
    }

    @Override
    public String getProductName(DBPNativeClientLocation location) {
        if (location instanceof KingbaseServerHome) {
            return location.getDisplayName();
        }
        return "Kingbase";
    }

    @Override
    public String getProductVersion(DBPNativeClientLocation location) {
        return getFullServerVersion(location.getPath());
    }

    private static synchronized Collection<DBPNativeClientLocation> findLocalClients() {
        if (localClients != null) {
            return localClients;
        }

        // find homes in Windows registry
        if (RuntimeUtils.isWindows()) {
            localClients = new HashSet<>();
            try {
                LocalSystemRegistry.Registry registry = LocalSystemRegistry.getInstance();
                if (registry.registryKeyExists("HKEY_LOCAL_MACHINE", KingbaseConstants.KB_INSTALL_REG_KEY)) {
                    String[] homeKeys = registry.registryGetKeys("HKEY_LOCAL_MACHINE", KingbaseConstants.KB_INSTALL_REG_KEY);
                    if (homeKeys != null) {
                        for (String homeKey : homeKeys) {
                            Map<String, Object> valuesMap = registry.registryGetValues("HKEY_LOCAL_MACHINE", KingbaseConstants.KB_INSTALL_REG_KEY + "\\" + homeKey);
                            for (String key : valuesMap.keySet()) {
                                if (KingbaseConstants.KB_INSTALL_PROP_BASE_DIRECTORY.equalsIgnoreCase(key)) {
                                    String baseDir = CommonUtils.removeTrailingSlash(CommonUtils.toString(valuesMap.get(KingbaseConstants.KB_INSTALL_PROP_BASE_DIRECTORY)));
                                    String branding = CommonUtils.toString(valuesMap.get(KingbaseConstants.KB_INSTALL_PROP_BRANDING));
                                    localClients.add(new KingbaseServerHome(homeKey, baseDir, branding));
                                    break;
                                }
                            }
                        }
                    }
                }
            } catch (Throwable e) {
                log.warn("Error reading Windows registry", e);
            }
        } else {
            localClients = NativeClientLocationUtils.findLocalClientsOnUnix(
                List.of("/Library/Kingbase", "/Applications/Kingbase.app/Contents/versions"),
                List.of("bin/ksql"),
                path -> {
                    String absolutePath = path.toAbsolutePath().toString();
                    return new KingbaseServerHome(absolutePath, absolutePath, absolutePath);
                }
            ).values();
        }

        return localClients;
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
            RuntimeUtils.getNativeBinaryName("ksql")).getAbsolutePath();

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
            log.warn("Error reading Kingbase local client version from " + cmd, ex);
        }
        return null;
    }
}

/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.oracle;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataSource;
import org.jkiss.dbeaver.ext.oracle.model.dict.OracleConnectionType;
import org.jkiss.dbeaver.ext.oracle.oci.OCIUtils;
import org.jkiss.dbeaver.ext.oracle.oci.OracleHomeDescriptor;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.access.DBAUserCredentialsProvider;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPNativeClientLocation;
import org.jkiss.dbeaver.model.connection.DBPNativeClientLocationManager;
import org.jkiss.dbeaver.model.impl.auth.AuthModelDatabaseNative;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceProvider;
import org.jkiss.dbeaver.model.net.DBWUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OracleDataSourceProvider extends JDBCDataSourceProvider implements
    DBAUserCredentialsProvider,
    DBPNativeClientLocationManager,
    DBPInformationProvider {

    public OracleDataSourceProvider()
    {
    }

    @Override
    public long getFeatures()
    {
        return FEATURE_SCHEMAS;
    }

    @Override
    public String getConnectionURL(DBPDriver driver, DBPConnectionConfiguration connectionInfo)
    {
        //boolean isOCI = OCIUtils.isOciDriver(driver);
        OracleConstants.ConnectionType connectionType = getConnectionType(connectionInfo);
        if (connectionType == OracleConstants.ConnectionType.CUSTOM) {
            return DatabaseURL.generateUrlByTemplate(connectionInfo.getUrl(), connectionInfo);
        }
        StringBuilder url = new StringBuilder(100);
        url.append("jdbc:oracle:thin:@"); //$NON-NLS-1$
        String databaseName = CommonUtils.notEmpty(connectionInfo.getDatabaseName());
        if (connectionType == OracleConstants.ConnectionType.TNS) {
            // TNS name specified
            // Try to get description from TNSNAMES
            File oraHomePath;
            boolean checkTnsAdmin;
            String tnsPathProp = CommonUtils.toString(connectionInfo.getProviderProperty(OracleConstants.PROP_TNS_PATH));
            if (!CommonUtils.isEmpty(tnsPathProp)) {
                oraHomePath = new File(tnsPathProp);
                checkTnsAdmin = false;
            } else {
                final String clientHomeId = connectionInfo.getClientHomeId();
                final OracleHomeDescriptor oraHome = CommonUtils.isEmpty(clientHomeId) ? null : OCIUtils.getOraHome(clientHomeId);
                oraHomePath = oraHome == null ? null : oraHome.getPath();
                checkTnsAdmin = true;
            }

            final Map<String, String> tnsNames = OCIUtils.readTnsNames(oraHomePath, checkTnsAdmin);
            final String tnsDescription = tnsNames.get(databaseName);
            if (!CommonUtils.isEmpty(tnsDescription)) {
                url.append(tnsDescription);
            } else {
                // TNS name not found.
                // Last chance - set TNS path and hope that Oracle driver find figure something out
                final File tnsNamesFile = OCIUtils.findTnsNamesFile(oraHomePath, checkTnsAdmin);
                if (tnsNamesFile != null && tnsNamesFile.exists()) {
                    System.setProperty(OracleConstants.VAR_ORACLE_NET_TNS_ADMIN, tnsNamesFile.getAbsolutePath());
                }
                url.append(databaseName);
            }
        } else {
            // Basic connection info specified
            boolean isSID = OracleConnectionType.SID.name().equals(connectionInfo.getProviderProperty(OracleConstants.PROP_SID_SERVICE));
            if (!isSID) {
                url.append("//"); //$NON-NLS-1$
            }
            if (!CommonUtils.isEmpty(connectionInfo.getHostName())) {
                url.append(connectionInfo.getHostName());
            }
            if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
                url.append(":"); //$NON-NLS-1$
                url.append(connectionInfo.getHostPort());
            }
            if (isSID) {
                url.append(":"); //$NON-NLS-1$
            } else {
                url.append("/"); //$NON-NLS-1$
            }
            if (!CommonUtils.isEmpty(databaseName)) {
                url.append(databaseName);
            }
        }
        return url.toString();
    }

    @NotNull
    private OracleConstants.ConnectionType getConnectionType(DBPConnectionConfiguration connectionInfo) {
        OracleConstants.ConnectionType connectionType;
        String conTypeProperty = connectionInfo.getProviderProperty(OracleConstants.PROP_CONNECTION_TYPE);
        if (conTypeProperty != null) {
            connectionType = OracleConstants.ConnectionType.valueOf(CommonUtils.toString(conTypeProperty));
        } else {
            connectionType = OracleConstants.ConnectionType.BASIC;
        }
        return connectionType;
    }

    @NotNull
    @Override
    public DBPDataSource openDataSource(
        @NotNull DBRProgressMonitor monitor, @NotNull DBPDataSourceContainer container)
        throws DBException
    {
        return new OracleDataSource(monitor, container);
    }

    //////////////////////////////////////
    // Client manager

    @Override
    public List<DBPNativeClientLocation> findLocalClientLocations()
    {
        List<DBPNativeClientLocation> homeIds = new ArrayList<>();
        for (OracleHomeDescriptor home : OCIUtils.getOraHomes()) {
            homeIds.add(home);
        }
        return homeIds;
    }

    @Override
    public DBPNativeClientLocation getDefaultLocalClientLocation()
    {
        return OCIUtils.getDefaultOraHome();
    }

    @Override
    public String getProductName(DBPNativeClientLocation location) {
        Integer oraVersion = getOracleVersion(location);
        return "Oracle" + (oraVersion == null ? "" : " " + oraVersion);
    }

    @Override
    public String getProductVersion(DBPNativeClientLocation location) {
        boolean isInstantClient = OCIUtils.isInstantClient(location.getName());
        return OCIUtils.getFullOraVersion(location.getName(), isInstantClient);
    }

    public static Integer getOracleVersion(DBPNativeClientLocation location)
    {
        File oraHome = location.getPath();
        boolean isInstantClient = OCIUtils.isInstantClient(location.getName());
        File folder = isInstantClient ? oraHome : new File(oraHome, "bin");
        if (!folder.exists()) {
            return null;
        }
        for (int counter = 7; counter <= 15; counter++) {
            String dllName = System.mapLibraryName("ocijdbc" + counter);
            File ociLibFile = new File(folder, dllName);
            if (ociLibFile.exists()) {
                return counter;
            }
        }
        return null;
    }

    @Nullable
    @Override
    public String getConnectionUserName(@NotNull DBPConnectionConfiguration connectionInfo) {
        String userName = connectionInfo.getUserName();
        String authModelId = connectionInfo.getAuthModelId();
        if (!CommonUtils.isEmpty(authModelId) && !AuthModelDatabaseNative.ID.equals(authModelId)) {
            return userName;
        }
        // FIXME: left for backward compatibility. Replaced by auth model. Remove in future.
        if (!CommonUtils.isEmpty(userName) && userName.contains(" AS ")) {
            return userName;
        }
        final String role = connectionInfo.getProviderProperty(OracleConstants.PROP_INTERNAL_LOGON);
        return role == null ? userName : userName + " AS " + role;
    }

    @Nullable
    @Override
    public String getConnectionUserPassword(@NotNull DBPConnectionConfiguration connectionInfo) {
        return connectionInfo.getUserPassword();
    }


    @Nullable
    @Override
    public String getObjectInformation(@NotNull DBPObject object, @NotNull String infoType) {
        if (object instanceof DBPDataSourceContainer ds && infoType.equals(INFO_TARGET_ADDRESS)) {
            DBPConnectionConfiguration connectionInfo = ds.getConnectionConfiguration();
            OracleConstants.ConnectionType connectionType = getConnectionType(connectionInfo);
            if (connectionType == OracleConstants.ConnectionType.CUSTOM) {
                return DatabaseURL.generateUrlByTemplate(connectionInfo.getUrl(), connectionInfo);
            }
            String databaseName = CommonUtils.notEmpty(connectionInfo.getDatabaseName());
            if (connectionType == OracleConstants.ConnectionType.TNS) {
                return databaseName;
            } else {
                String hostName = DBWUtils.getTargetTunnelHostName(ds, connectionInfo);
                String hostPort = connectionInfo.getHostPort();
                if (CommonUtils.isEmpty(hostName)) {
                    return null;
                } else if (CommonUtils.isEmpty(hostPort)) {
                    return hostName;
                } else {
                    return hostName + ":" + hostPort;
                }
            }
        }
        return null;
    }

}

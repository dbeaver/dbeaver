/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.oracle;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataSource;
import org.jkiss.dbeaver.ext.oracle.model.dict.OracleConnectionType;
import org.jkiss.dbeaver.ext.oracle.oci.OCIUtils;
import org.jkiss.dbeaver.ext.oracle.oci.OracleHomeDescriptor;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.connection.DBPClientHome;
import org.jkiss.dbeaver.model.connection.DBPClientManager;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceProvider;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.util.*;

public class OracleDataSourceProvider extends JDBCDataSourceProvider implements DBPClientManager {

    private static Map<String,String> connectionsProps;

    static {
        connectionsProps = new HashMap<>();

        // Program name
        connectionsProps.put("v$session.program", DBeaverCore.getProductTitle());
    }

    public static Map<String,String> getConnectionsProps() {
        return connectionsProps;
    }

    public OracleDataSourceProvider()
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
        return FEATURE_SCHEMAS;
    }

    @Override
    public String getConnectionURL(DBPDriver driver, DBPConnectionConfiguration connectionInfo)
    {
        //boolean isOCI = OCIUtils.isOciDriver(driver);
        OracleConstants.ConnectionType connectionType;
        String conTypeProperty = connectionInfo.getProviderProperty(OracleConstants.PROP_CONNECTION_TYPE);
        if (conTypeProperty != null) {
            connectionType = OracleConstants.ConnectionType.valueOf(CommonUtils.toString(conTypeProperty));
        } else {
            connectionType = OracleConstants.ConnectionType.BASIC;
        }
        if (connectionType == OracleConstants.ConnectionType.CUSTOM) {
            return connectionInfo.getUrl();
        }
        StringBuilder url = new StringBuilder(100);
        url.append("jdbc:oracle:thin:@"); //$NON-NLS-1$
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
                oraHomePath = oraHome == null ? null : oraHome.getHomePath();
                checkTnsAdmin = true;
            }

            final Map<String, String> tnsNames = OCIUtils.readTnsNames(oraHomePath, checkTnsAdmin);
            final String tnsDescription = tnsNames.get(connectionInfo.getDatabaseName());
            if (!CommonUtils.isEmpty(tnsDescription)) {
                url.append(tnsDescription);
            } else {
                // TNS name not found.
                // Last chance - set TNS path and hope that Oracle driver find figure something out
                final File tnsNamesFile = OCIUtils.findTnsNamesFile(oraHomePath, checkTnsAdmin);
                if (tnsNamesFile != null && tnsNamesFile.exists()) {
                    System.setProperty(OracleConstants.VAR_ORACLE_NET_TNS_ADMIN, tnsNamesFile.getAbsolutePath());
                }
                url.append(connectionInfo.getDatabaseName());
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
            if (!CommonUtils.isEmpty(connectionInfo.getDatabaseName())) {
                url.append(connectionInfo.getDatabaseName());
            }
        }
        return url.toString();
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
    public Collection<String> findClientHomeIds()
    {
        List<String> homeIds = new ArrayList<>();
        for (OracleHomeDescriptor home : OCIUtils.getOraHomes()) {
            homeIds.add(home.getHomeId());
        }
        return homeIds;
    }

    @Override
    public String getDefaultClientHomeId()
    {
        List<OracleHomeDescriptor> oraHomes = OCIUtils.getOraHomes();
        if (!oraHomes.isEmpty()) {
            return oraHomes.get(0).getHomeId();
        }
        return null;
    }

    @Override
    public DBPClientHome getClientHome(String homeId)
    {
        return new OracleHomeDescriptor(homeId);
    }

}

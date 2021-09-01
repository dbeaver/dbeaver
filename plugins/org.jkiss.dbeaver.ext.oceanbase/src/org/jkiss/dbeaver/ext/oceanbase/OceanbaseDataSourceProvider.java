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

package org.jkiss.dbeaver.ext.oceanbase;

import java.util.ArrayList;
import java.util.List;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mysql.MySQLDataSourceProvider;
import org.jkiss.dbeaver.ext.oceanbase.mysql.model.OceanbaseMySQLDataSource;
import org.jkiss.dbeaver.ext.oceanbase.oracle.model.OceanbaseOracleDataSource;
import org.jkiss.dbeaver.ext.oracle.OracleDataSourceProvider;
import org.jkiss.dbeaver.ext.oracle.oci.OCIUtils;
import org.jkiss.dbeaver.ext.oracle.oci.OracleHomeDescriptor;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPInformationProvider;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPNativeClientLocation;
import org.jkiss.dbeaver.model.connection.DBPNativeClientLocationManager;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCURL;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.registry.DataSourceUtils;
import org.jkiss.utils.CommonUtils;

public class OceanbaseDataSourceProvider extends MySQLDataSourceProvider
        implements DBPNativeClientLocationManager, DBPInformationProvider {

    private String tenantType;

    @Override
    public String getConnectionURL(DBPDriver driver, DBPConnectionConfiguration connectionInfo) {
        tenantType = connectionInfo.getProviderProperty("tenentType");
        return JDBCURL.generateUrlByTemplate(driver, connectionInfo);
    }

    @NotNull
    @Override
    public DBPDataSource openDataSource(@NotNull DBRProgressMonitor monitor, @NotNull DBPDataSourceContainer container)
            throws DBException {
        tenantType = container.getActualConnectionConfiguration().getProviderProperty("tenantType");
        if (isMySQLMode()) {
            return new OceanbaseMySQLDataSource(monitor, container);
        } else {
            return new OceanbaseOracleDataSource(monitor, container);
        }
    }

    @Override
    public long getFeatures() {
        try {
            if (isMySQLMode()) {
                return FEATURE_CATALOGS;
            } else {
                return FEATURE_SCHEMAS;
            }
        } catch (NullPointerException e) {
            return FEATURE_NONE;
        }
    }

    @Override
    public String getObjectInformation(DBPObject object, String infoType) {
        if (isMySQLMode()) {
            return null;
        } else if (object instanceof DBPDataSourceContainer && infoType.equals(INFO_TARGET_ADDRESS)) {
            DBPConnectionConfiguration connectionInfo = ((DBPDataSourceContainer) object).getConnectionConfiguration();
            String hostName = DataSourceUtils.getTargetTunnelHostName(connectionInfo);
            String hostPort = connectionInfo.getHostPort();
            if (CommonUtils.isEmpty(hostName)) {
                return null;
            } else if (CommonUtils.isEmpty(hostPort)) {
                return hostName;
            } else {
                return hostName + ":" + hostPort;
            }
        }
        return null;
    }

    @Override
    public List<DBPNativeClientLocation> findLocalClientLocations() {
        if (isMySQLMode()) {
            return super.findLocalClientLocations();
        } else {
            List<DBPNativeClientLocation> homeIds = new ArrayList<>();
            for (OracleHomeDescriptor home : OCIUtils.getOraHomes()) {
                homeIds.add(home);
            }
            return homeIds;
        }
    }

    @Override
    public DBPNativeClientLocation getDefaultLocalClientLocation() {
        List<OracleHomeDescriptor> oraHomes = OCIUtils.getOraHomes();
        if (!oraHomes.isEmpty()) {
            return oraHomes.get(0);
        }
        return super.getDefaultLocalClientLocation();
    }

    @Override
    public String getProductName(DBPNativeClientLocation location) {
        if (isMySQLMode()) {
            return super.getProductName(location);
        } else {
            Integer oraVersion = OracleDataSourceProvider.getOracleVersion(location);
            return "Oracle" + (oraVersion == null ? "" : " " + oraVersion);
        }
    }

    @Override
    public String getProductVersion(DBPNativeClientLocation location) {
        if (isMySQLMode()) {
            return super.getProductName(location);
        } else {
            boolean isInstantClient = OCIUtils.isInstantClient(location.getName());
            return OCIUtils.getFullOraVersion(location.getName(), isInstantClient);
        }
    }

    private boolean isMySQLMode() {
        return tenantType.equalsIgnoreCase("MySQL");
    }

}

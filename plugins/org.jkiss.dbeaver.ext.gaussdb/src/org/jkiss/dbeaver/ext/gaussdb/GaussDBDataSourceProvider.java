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

package org.jkiss.dbeaver.ext.gaussdb;

import java.util.HashMap;
import java.util.Map;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.gaussdb.model.GaussDBDataSource;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.ext.postgresql.model.impls.PostgreServerType;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceURLProvider;
import org.jkiss.dbeaver.model.DatabaseURL;
import org.jkiss.dbeaver.model.access.DBAAuthCredentials;
import org.jkiss.dbeaver.model.access.DBAAuthModel;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverConfigurationType;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceProvider;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.utils.CommonUtils;

public class GaussDBDataSourceProvider extends JDBCDataSourceProvider {

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

    @Override
    public long getFeatures() {
        return FEATURE_CATALOGS | FEATURE_SCHEMAS;
    }

    @Override
    public DBPDataSource openDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
        return new GaussDBDataSource(monitor, container);
    }

    @Override
    public String getConnectionURL(DBPDriver driver, DBPConnectionConfiguration connectionInfo) {
        DBAAuthModel<DBAAuthCredentials> authModel = connectionInfo.getAuthModel();
        if (authModel instanceof DBPDataSourceURLProvider) {
            String connectionURL = ((DBPDataSourceURLProvider) authModel).getConnectionURL(driver, connectionInfo);
            if (CommonUtils.isNotEmpty(connectionURL)) {
                return connectionURL;
            }
        }
        if (connectionInfo.getConfigurationType() == DBPDriverConfigurationType.URL) {
            return connectionInfo.getUrl();
        }
        PostgreServerType serverType = PostgreUtils.getServerType(driver);
        if (serverType.supportsCustomConnectionURL()) {
            return DatabaseURL.generateUrlByTemplate(driver, connectionInfo);
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
        return url.toString();
    }

    @Override
    public boolean providesDriverClasses() {
        return false;
    }
}

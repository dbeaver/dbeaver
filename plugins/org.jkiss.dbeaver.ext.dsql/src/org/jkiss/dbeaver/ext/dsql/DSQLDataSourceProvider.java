/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.dsql;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.impls.PostgreServerType;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceURLProvider;
import org.jkiss.dbeaver.model.DatabaseURL;
import org.jkiss.dbeaver.model.access.DBAAuthModel;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverConfigurationType;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceProvider;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

public class DSQLDataSourceProvider extends JDBCDataSourceProvider {

    @Override
    public long getFeatures() {
        return FEATURE_CATALOGS | FEATURE_SCHEMAS;
    }

    @Override
    public DBPDataSource openDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container) throws DBException {
        return new PostgreDataSource(monitor, container);
    }

    @Override
    public String getConnectionURL(DBPDriver driver, DBPConnectionConfiguration connectionInfo) {
        DBPConnectionConfiguration configToUse = connectionInfo;
        String databaseName = connectionInfo.getDatabaseName();

        if (databaseName != null && databaseName.contains("/")) {
            configToUse = new DBPConnectionConfiguration(connectionInfo);
            configToUse.setDatabaseName(databaseName.replace("/", "%2F"));
        }

        DBAAuthModel<?> authModel = configToUse.getAuthModel();

        if (authModel instanceof DBPDataSourceURLProvider sourceURLProvider) {
            String connectionURL = sourceURLProvider.getConnectionURL(driver, configToUse);
            if (CommonUtils.isNotEmpty(connectionURL)) {
                return connectionURL;
            }
        }

        if (configToUse.getConfigurationType() == DBPDriverConfigurationType.URL) {
            return configToUse.getUrl();
        }

        PostgreServerType serverType = PostgreUtils.getServerType(driver);
        if (serverType.supportsCustomConnectionURL()) {
            return DatabaseURL.generateUrlByTemplate(driver, configToUse);
        }

        StringBuilder url = new StringBuilder("jdbc:postgresql://");
        url.append(configToUse.getHostName());

        if (!CommonUtils.isEmpty(configToUse.getHostPort())) {
            url.append(":").append(configToUse.getHostPort());
        }

        url.append("/");

        if (!CommonUtils.isEmpty(configToUse.getDatabaseName())) {
            url.append(configToUse.getDatabaseName());
        }
        return url.toString();
    }
}

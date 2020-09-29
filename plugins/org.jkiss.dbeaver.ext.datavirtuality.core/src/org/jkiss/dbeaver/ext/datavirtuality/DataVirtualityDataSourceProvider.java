/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.datavirtuality;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.datavirtuality.model.DataVirtualityDataSource;
import org.jkiss.dbeaver.ext.datavirtuality.model.DataVirtualityMetaModel;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceProvider;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

public class DataVirtualityDataSourceProvider extends JDBCDataSourceProvider {

    private static final Log log = Log.getLog(DataVirtualityDataSourceProvider.class);

    public DataVirtualityDataSourceProvider()
    {
    }

    @Override
    public void init(@NotNull DBPPlatform platform) {

    }

    @Override
    public long getFeatures()
    {
        return FEATURE_SCHEMAS;
    }

    @Override
    public String getConnectionURL(DBPDriver driver, DBPConnectionConfiguration connectionInfo)
    {
        StringBuilder url = new StringBuilder();
        url.append("jdbc:DataVirtuality:");

        url.append(connectionInfo.getDatabaseName());
        url.append("@");
        if (!CommonUtils.isEmpty(connectionInfo.getProviderProperty(DataVirtualityConstants.PROP_SSL))) {
            url.append(connectionInfo.getProviderProperty(DataVirtualityConstants.PROP_SSL));
        }
        else {
            url.append("mm");
        }

        url.append("//");
        url.append(connectionInfo.getHostName());
        if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
            url.append(":").append(connectionInfo.getHostPort());
        }

        return url.toString();
    }

    private static boolean addParameter(StringBuilder url, String name, String value, boolean hasParam) {
        if (!CommonUtils.isEmpty(value)) {
            if (hasParam) url.append("&");
            url.append(name).append("=").append(value);
            return true;
        }
        return hasParam;
    }

    @NotNull
    @Override
    public DBPDataSource openDataSource(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSourceContainer container)
        throws DBException
    {
        return new DataVirtualityDataSource(monitor, container, new DataVirtualityMetaModel());
    }

}

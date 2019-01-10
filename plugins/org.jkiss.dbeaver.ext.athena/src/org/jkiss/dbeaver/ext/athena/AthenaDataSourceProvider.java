/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.athena;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.athena.model.AthenaConstants;
import org.jkiss.dbeaver.ext.athena.model.AthenaDataSource;
import org.jkiss.dbeaver.ext.athena.model.AthenaMetaModel;
import org.jkiss.dbeaver.ext.generic.GenericDataSourceProvider;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

public class AthenaDataSourceProvider extends GenericDataSourceProvider {

    private static final Log log = Log.getLog(AthenaDataSourceProvider.class);

    public AthenaDataSourceProvider()
    {
    }

    @Override
    public void init(@NotNull DBPPlatform platform) {

    }

    @NotNull
    @Override
    public DBPDataSource openDataSource(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSourceContainer container)
        throws DBException
    {
        return new AthenaDataSource(monitor, container, new AthenaMetaModel());
    }

    @Override
    public String getConnectionURL(DBPDriver driver, DBPConnectionConfiguration connectionInfo) {
        //jdbc:awsathena://AwsRegion=us-east-1;
        StringBuilder url = new StringBuilder();
        url.append(AthenaConstants.JDBC_URL_PREFIX)
            .append(AthenaConstants.DRIVER_PROP_REGION).append("=").append(connectionInfo.getServerName()).append(";");
        return url.toString();
    }
}

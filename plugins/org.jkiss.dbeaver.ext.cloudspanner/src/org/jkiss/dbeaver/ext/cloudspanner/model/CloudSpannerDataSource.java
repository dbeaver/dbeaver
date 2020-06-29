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
package org.jkiss.dbeaver.ext.cloudspanner.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.HashMap;
import java.util.Map;

public class CloudSpannerDataSource extends GenericDataSource {

    public CloudSpannerDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container, CloudSpannerMetaModel metaModel)
        throws DBException
    {
        super(monitor, container, metaModel, new CloudSpannerSQLDialect());
    }

    @Override
	protected String getConnectionURL(DBPConnectionConfiguration connectionInfo) {
		StringBuilder url = new StringBuilder(super.getConnectionURL(connectionInfo));
		url.append("/projects/").append(connectionInfo.getServerName());
		url.append("/instances/").append(connectionInfo.getHostName());
		url.append("/databases/").append(connectionInfo.getDatabaseName());
		return url.toString();
	}

	@Override
    protected Map<String, String> getInternalConnectionProperties(DBRProgressMonitor monitor, DBPDriver driver, JDBCExecutionContext context, String purpose, DBPConnectionConfiguration connectionInfo) throws DBCException {
        Map<String, String> props = new HashMap<>();
        props.put(CloudSpannerConstants.DRIVER_PROP_CREDENTIALS_FILE, connectionInfo.getProviderProperty(CloudSpannerConstants.DRIVER_PROP_CREDENTIALS_FILE));

        return props;
    }

}

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
package org.jkiss.dbeaver.ext.athena.model;

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

/**
 * Athena datasource
 */
public class AthenaDataSource extends GenericDataSource {

    public AthenaDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container, AthenaMetaModel metaModel)
        throws DBException
    {
        super(monitor, container, metaModel, new AthenaSQLDialect());
    }

    @Override
    protected Map<String, String> getInternalConnectionProperties(DBRProgressMonitor monitor, DBPDriver driver, JDBCExecutionContext context, String purpose, DBPConnectionConfiguration connectionInfo) throws DBCException {
        Map<String, String> props = new HashMap<>();
        props.put(AthenaConstants.DRIVER_PROP_S3_OUTPUT_LOCATION, connectionInfo.getDatabaseName());
        props.put(AthenaConstants.DRIVER_PROP_AWS_CREDENTIALS_PROVIDER_CLASS, "com.amazonaws.auth.DefaultAWSCredentialsProviderChain");

        return props;
    }

    @Override
    public boolean isOmitCatalog() {
        return true;
    }

    @Override
    protected boolean isPopulateClientAppName() {
        return false;
    }
}

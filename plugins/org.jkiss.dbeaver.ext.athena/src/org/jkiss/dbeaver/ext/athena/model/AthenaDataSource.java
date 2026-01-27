/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

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
    protected Map<String, String> getInternalConnectionProperties(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDriver driver,
        @NotNull JDBCExecutionContext context,
        @NotNull String purpose,
        @NotNull DBPConnectionConfiguration connectionInfo
    ) throws DBCException {
        Map<String, String> props = new HashMap<>();
        if (CommonUtils.isEmpty(connectionInfo.getDatabaseName())) {
            String s3OutputLocation = connectionInfo.getProviderProperty(AthenaConstants.DRIVER_PROP_S3_OUTPUT_LOCATION);
            if (s3OutputLocation == null) {
                s3OutputLocation = connectionInfo.getProviderProperty(AthenaConstants.DRIVER_PROP_S3_OUTPUT_LOCATION_OLD);
            }
            connectionInfo.setDatabaseName(s3OutputLocation);
        }
        if (!CommonUtils.isEmpty(connectionInfo.getDatabaseName())) {
            props.put(AthenaConstants.DRIVER_PROP_S3_OUTPUT_LOCATION, connectionInfo.getDatabaseName());
        }

        boolean useCatalogs = CommonUtils.toBoolean(connectionInfo.getProviderProperty(AthenaConstants.PROP_SHOW_CATALOGS));
        if (useCatalogs) {
            props.put(AthenaConstants.DRIVER_PROP_METADATA_RETRIEVAL_METHOD, "ProxyAPI");
        }

        // Hack to fix update from v2 -> v3 driver version https://github.com/dbeaver/dbeaver/issues/39947
        // https://docs.aws.amazon.com/athena/latest/ug/jdbc-v3-driver-aws-configuration-profile-credentials.html
        String credentialsProviderClass = connectionInfo.getProperties()
            .get(AthenaConstants.PROP_AWS_CREDENTIALS_PROVIDER_CLASS);
        if (AthenaConstants.PROP_OLD_VALUE_AWS_CREDENTIALS_PROVIDER_CLASS.equals(credentialsProviderClass)) {
            connectionInfo.getProperties().put(
                AthenaConstants.PROP_AWS_CREDENTIALS_PROVIDER_CLASS,
                AthenaConstants.PROP_NEW_VALUE_AWS_CREDENTIALS_PROVIDER_CLASS
            );
        }
        return props;
    }

    @Override
    public boolean isOmitCatalog() {
        return !CommonUtils.toBoolean(
            getContainer().getConnectionConfiguration().getProviderProperty(AthenaConstants.PROP_SHOW_CATALOGS));
    }

    @Override
    protected boolean isPopulateClientAppName() {
        return false;
    }
}

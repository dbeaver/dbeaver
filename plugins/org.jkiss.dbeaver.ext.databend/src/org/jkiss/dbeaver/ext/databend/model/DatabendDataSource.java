/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.databend.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.ext.databend.model.DatabendConstants;
import org.jkiss.dbeaver.ext.databend.model.DatabendMetaModel;
import org.jkiss.dbeaver.ext.databend.model.DatabendSQLDialect;
import org.jkiss.utils.CommonUtils;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Databend datasource
 */
public class DatabendDataSource extends GenericDataSource {

    public DatabendDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container, DatabendMetaModel metaModel)
            throws DBException {
        super(monitor, container, metaModel, new DatabendSQLDialect());
    }


    @Override
    public boolean isOmitCatalog() {
        return true;
    }

    @Override
    protected boolean isPopulateClientAppName() {
        return false;
    }

    @Nullable
    @Override
    protected Properties getAllConnectionProperties(@NotNull DBRProgressMonitor monitor, JDBCExecutionContext context, String purpose, DBPConnectionConfiguration connectionInfo)
            throws DBCException {
        Properties properties = super.getAllConnectionProperties(monitor, context, purpose, connectionInfo);

        final DBWHandlerConfiguration sslConfig = getContainer().getActualConnectionConfiguration().getHandler("databend-ssl");

        if (sslConfig != null && sslConfig.isEnabled()) {
            try {
                initSSL(monitor, properties, sslConfig);
            } catch (Exception e) {
                throw new DBCException("Error configuring SSL certificates", e);
            }
        }
        return properties;
    }

    private void initSSL(DBRProgressMonitor monitor, Properties properties, DBWHandlerConfiguration sslConfig) throws DBException {
        monitor.subTask("Initialising SSL configuration");
        properties.put(DatabendConstants.SSL_PARAM, "true");
    }
}

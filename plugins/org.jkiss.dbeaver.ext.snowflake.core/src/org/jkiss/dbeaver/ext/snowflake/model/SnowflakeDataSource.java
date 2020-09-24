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
package org.jkiss.dbeaver.ext.snowflake.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.snowflake.SnowflakeConstants;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

public class SnowflakeDataSource extends GenericDataSource {

    public SnowflakeDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container, SnowflakeMetaModel metaModel)
        throws DBException
    {
        super(monitor, container, metaModel, new SnowflakeSQLDialect());
    }

    @Override
    protected Map<String, String> getInternalConnectionProperties(DBRProgressMonitor monitor, DBPDriver driver, JDBCExecutionContext context, String purpose, DBPConnectionConfiguration connectionInfo) throws DBCException {
        Map<String, String> props = new HashMap<>();
        String authProp = connectionInfo.getProviderProperty(SnowflakeConstants.PROP_AUTHENTICATOR);
        if (!CommonUtils.isEmpty(authProp)) {
            props.put("authenticator", authProp);
        }

        return props;
    }

    @Override
    protected boolean isPopulateClientAppName() {
        return false;
    }

    @Override
    protected void initializeContextState(DBRProgressMonitor monitor, JDBCExecutionContext context, JDBCExecutionContext initFrom) throws DBException {
        super.initializeContextState(monitor, context, initFrom);
        ((SnowflakeExecutionContext) context).setDefaultSchema(monitor);
    }

    @Override
    protected JDBCExecutionContext createExecutionContext(JDBCRemoteInstance instance, String type) {
        return new SnowflakeExecutionContext(instance, type);
    }
}

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
package org.jkiss.dbeaver.ext.spanner.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.spanner.SpannerDataSourceProvider;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SpannerDataSource extends GenericDataSource {

    public SpannerDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container, SpannerMetaModel metaModel)
        throws DBException
    {
        super(monitor, container, metaModel, new SpannerSQLDialect());
    }

    @Override
	protected String getConnectionURL(DBPConnectionConfiguration connectionInfo) {
		String url = super.getConnectionURL(connectionInfo);
		if (url != null && url.startsWith("jdbc:cloudspanner:/projects/%s/instances/%s/databases/%s")) {
			// Official driver.
			return String.format(url, connectionInfo.getServerName(), connectionInfo.getHostName(), connectionInfo.getDatabaseName());
		}
		return url;
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
        if (driver.getId().equals(SpannerDataSourceProvider.COMMUNITY_DRIVER_ID)) {
	        props.put(SpannerConstants.DRIVER_PROP_PROJECT_ID, connectionInfo.getServerName());
	        props.put(SpannerConstants.DRIVER_PROP_INSTANCE_ID, connectionInfo.getHostName());
	        props.put(SpannerConstants.DRIVER_PROP_DATABASE_ID, connectionInfo.getDatabaseName());
	        props.put(SpannerConstants.DRIVER_PROP_PVTKEYPATH, connectionInfo.getProviderProperty(SpannerConstants.DRIVER_PROP_PVTKEYPATH));
        } else if (connectionInfo.getProviderProperty(SpannerConstants.DRIVER_PROP_PVTKEYPATH) != null) {
            props.put(SpannerConstants.DRIVER_PROP_CREDENTIALS_FILE, connectionInfo.getProviderProperty(SpannerConstants.DRIVER_PROP_PVTKEYPATH));
        }
        return props;
    }

    @Override
    public GenericTableBase findTable(
        @NotNull DBRProgressMonitor monitor,
        @Nullable String catalogName,
        @Nullable String schemaName,
        @NotNull String tableName
    ) throws DBException {
        if (CommonUtils.isEmpty(schemaName) &&  !CommonUtils.isEmpty(getSchemas())) {
            // This is very strange case from Spanner. He supports one main empty name schema and system schemas.
            // So use default schema as container for this case.
            List<GenericSchema> nonSystemSchemas = getSchemas()
                .stream()
                .filter(e -> !e.isSystem())
                .collect(Collectors.toList());
            if (nonSystemSchemas.size() == 1) {
                GenericSchema schema = nonSystemSchemas.get(0);
                if (GenericConstants.DEFAULT_NULL_SCHEMA_NAME.equals(schema.getName())) {
                    return schema.getTable(monitor, tableName);
                }
            }
        }
        return super.findTable(monitor, catalogName, schemaName, tableName);
    }
}

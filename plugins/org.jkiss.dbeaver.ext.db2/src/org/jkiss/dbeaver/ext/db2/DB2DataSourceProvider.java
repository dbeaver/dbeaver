/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceProvider;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * DB2 DataSource provider ddd
 */
public class DB2DataSourceProvider extends JDBCDataSourceProvider {

    private static Map<String, String> connectionsProps = new HashMap<>();

    // ------------
    // Constructors
    // ------------

    public DB2DataSourceProvider()
    {
    }

    public static Map<String, String> getConnectionsProps()
    {
        return connectionsProps;
    }

    @Override
    protected String getConnectionPropertyDefaultValue(String name, String value)
    {
        String ovrValue = connectionsProps.get(name);
        return ovrValue != null ? ovrValue : super.getConnectionPropertyDefaultValue(name, value);
    }

    @Override
    public long getFeatures()
    {
        return FEATURE_SCHEMAS;
    }

    @Override
    public String getConnectionURL(DBPDriver driver, DBPConnectionConfiguration connectionInfo)
    {
        StringBuilder url = new StringBuilder(128);
        url.append("jdbc:db2://").append(connectionInfo.getHostName());
        if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
            url.append(":").append(connectionInfo.getHostPort());
        }
        url.append("/");
        if (!CommonUtils.isEmpty(connectionInfo.getDatabaseName())) {
            url.append(connectionInfo.getDatabaseName());
        }
        Map<String, String> properties = connectionInfo.getProviderProperties();
        boolean traceEnabled = CommonUtils.getBoolean(properties.get(DB2Constants.PROP_TRACE_ENABLED), false);
        if (traceEnabled) {
            url.append(":traceDirectory=").append(CommonUtils.toString(properties.get(DB2Constants.PROP_TRACE_FOLDER)));
            url.append(";traceFile=")
                .append(CommonUtils.escapeFileName(CommonUtils.toString(properties.get(DB2Constants.PROP_TRACE_FILE))));
            url.append(";traceFileAppend=").append(CommonUtils.getBoolean(properties.get(DB2Constants.PROP_TRACE_APPEND), false));
            url.append(";traceLevel=")
                .append(CommonUtils.toInt(properties.get(DB2Constants.PROP_TRACE_LEVEL), DB2Constants.TRACE_ALL));
            url.append(";");
        }
        return url.toString();
    }

    @NotNull
    @Override
    public DBPDataSource openDataSource(@NotNull DBRProgressMonitor monitor, @NotNull DBPDataSourceContainer container)
        throws DBException
    {
        return new DB2DataSource(monitor, container);
    }

}

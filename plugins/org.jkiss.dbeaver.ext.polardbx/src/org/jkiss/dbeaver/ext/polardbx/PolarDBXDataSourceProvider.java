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
package org.jkiss.dbeaver.ext.polardbx;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mysql.MySQLDataSourceProvider;
import org.jkiss.dbeaver.ext.polardbx.mysql.model.PolarDBXMySQLDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DatabaseURL;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class PolarDBXDataSourceProvider extends MySQLDataSourceProvider {

    private static final Log log = Log.getLog(PolarDBXDataSourceProvider.class);

    @NotNull
    @Override
    public String getConnectionURL(
        @NotNull DBPDriver driver,
        @NotNull DBPConnectionConfiguration connectionInfo
    ) throws DBException {
        // Use a unified PolarDB-X URL format, compatible with both the Standard Edition and the Enterprise Edition.
        // PolarDB-X URL format: jdbc:polardbx://[host]:[port],[host]:[port],...[/database]?[property=<value>]&[property=<value>]
        String polardbxUrlTemplate = "jdbc:polardbx://{host}[:{port}][/{database}]";
        String connectionUrl = DatabaseURL.generateUrlByTemplate(polardbxUrlTemplate, connectionInfo);

        // Add PolarDB-X default parameters, compatible with both the Standard Edition and the Enterprise Edition.
        // socketTimeout is set to 60 seconds to prevent network reads from hanging indefinitely.
        String defaultParams = "ignoreVip=false&socketTimeout=60000&useSSL=false&characterEncoding=UTF-8";

        // Check whether the URL already contains parameters.
        if (connectionUrl.contains("?")) {
            // If parameters already exist, append to the existing parameters.
            connectionUrl = connectionUrl + "&" + defaultParams;
        } else {
            // If there are no parameters, add the parameter separator.
            connectionUrl = connectionUrl + "?" + defaultParams;
        }

        return connectionUrl;
    }



    @NotNull
    @Override
    public PolarDBXMySQLDataSource openDataSource(@NotNull DBRProgressMonitor monitor, @NotNull DBPDataSourceContainer container)
            throws DBException {

        return new PolarDBXMySQLDataSource(monitor, container);
    }
}

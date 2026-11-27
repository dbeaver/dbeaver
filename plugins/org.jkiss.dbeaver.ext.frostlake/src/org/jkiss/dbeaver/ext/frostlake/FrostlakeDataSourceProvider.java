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
package org.jkiss.dbeaver.ext.frostlake;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.frostlake.model.FrostlakeDataSource;
import org.jkiss.dbeaver.ext.generic.GenericDataSourceProvider;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.utils.CommonUtils;

/**
 * Builds a Frostlake JDBC URL from the connection settings.
 *
 * <p>Frostlake speaks three URL forms and they are not interchangeable — one talks to a server, the
 * other two run the engine inside this process:
 *
 * <pre>
 *   jdbc:frostlake://host:port/db   a running DatabaseHttpServer
 *   jdbc:frostlake:file:&lt;dir&gt;       in-process, persisted to a directory
 *   jdbc:frostlake:direct:&lt;name&gt;    in-process, nothing persisted
 * </pre>
 *
 * An embedded connection is recognised by having a database/path but no host, which is how DBeaver
 * presents a driver marked {@code embedded="true"}.
 */
public class FrostlakeDataSourceProvider extends GenericDataSourceProvider<FrostlakeDataSource> {

    public FrostlakeDataSourceProvider() {
        super(FrostlakeDataSource.class);
    }

    @Override
    public long getFeatures() {
        return FEATURE_CATALOGS | FEATURE_SCHEMAS;
    }

    @NotNull
    @Override
    public String getConnectionURL(@NotNull DBPDriver driver, @NotNull DBPConnectionConfiguration connectionInfo) {
        if (CommonUtils.isEmpty(connectionInfo.getHostName())) {
            // No host: an in-process engine. A path means persist there; otherwise a named
            // throwaway engine shared for the life of the JVM.
            final String path = CommonUtils.notEmpty(
                CommonUtils.isEmpty(connectionInfo.getDatabaseName())
                    ? connectionInfo.getServerName()
                    : connectionInfo.getDatabaseName());
            if (CommonUtils.isEmpty(path)) {
                return FrostlakeConstants.URL_PREFIX_DIRECT + "dbeaver";
            }
            return FrostlakeConstants.URL_PREFIX_FILE + path;
        }

        final StringBuilder url = new StringBuilder(FrostlakeConstants.URL_PREFIX_SERVER);
        url.append(connectionInfo.getHostName());
        if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
            url.append(":").append(connectionInfo.getHostPort());
        }
        url.append("/");
        if (!CommonUtils.isEmpty(connectionInfo.getDatabaseName())) {
            url.append(connectionInfo.getDatabaseName());
        }
        return url.toString();
    }
}

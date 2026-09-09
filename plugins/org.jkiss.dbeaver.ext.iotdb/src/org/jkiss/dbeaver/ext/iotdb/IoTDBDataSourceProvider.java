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

package org.jkiss.dbeaver.ext.iotdb;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.GenericDataSourceProvider;
import org.jkiss.dbeaver.ext.iotdb.model.IoTDBDataSource;
import org.jkiss.dbeaver.ext.iotdb.model.meta.IoTDBMetaModel;
import org.jkiss.dbeaver.ext.iotdb.model.meta.IoTDBTableMetaModel;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DatabaseURL;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

public class IoTDBDataSourceProvider extends GenericDataSourceProvider<IoTDBDataSource> {

    public IoTDBDataSourceProvider() {
        super(IoTDBDataSource.class);
    }

    @NotNull
    @Override
    public IoTDBDataSource openDataSource(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSourceContainer container
    ) throws DBException {
        String url = container.getConnectionConfiguration().getUrl();
        if (url.endsWith("?sql_dialect=table")) {
            return new IoTDBDataSource(monitor, container, new IoTDBTableMetaModel(), false);
        }
        return new IoTDBDataSource(monitor, container, new IoTDBMetaModel(), true);
    }

    private boolean useRawUrl(@NotNull DBPConnectionConfiguration connectionInfo) {
        return !CommonUtils.isEmpty(connectionInfo.getUrl()) &&
                CommonUtils.isEmpty(connectionInfo.getHostPort()) &&
                CommonUtils.isEmpty(connectionInfo.getHostName()) &&
                CommonUtils.isEmpty(connectionInfo.getServerName());
    }

    @NotNull
    private String removeTrailingPathSlash(@NotNull String url) {
        int index = url.indexOf("?");
        if (index > 0 && url.charAt(index - 1) == '/') {
            return url.substring(0, index - 1).concat(url.substring(index));
        }
        return url;
    }

    @NotNull
    @Override
    public String getConnectionURL(
        @NotNull DBPDriver driver,
        @NotNull DBPConnectionConfiguration connectionInfo) {
        String urlTemplate = driver.getSampleURL();
        String connectionUrl = connectionInfo.getUrl();
        String result;

        if ((useRawUrl(connectionInfo) || CommonUtils.isEmptyTrimmed(urlTemplate)) && CommonUtils.isNotEmpty(connectionUrl)) {
            result = removeTrailingPathSlash(connectionUrl);
        } else if (CommonUtils.isNotEmpty(urlTemplate)) {
            try {
                Map<String, String> extraParams = Map.of("sqlDialect", connectionInfo.getServerName());
                String url = DatabaseURL.generateUrlByTemplate(urlTemplate, connectionInfo, extraParams);
                result = url == null ? null : removeTrailingPathSlash(url);
            } catch (Throwable ex) {
                log.error(ex);
                result = null;
            }
        }  else {
            result = null;
        }

        return result;
    }
}

/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DatabaseURL;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

public class IoTDBDataSourceProvider extends GenericDataSourceProvider {

    @NotNull
    @Override
    public DBPDataSource openDataSource(@NotNull DBRProgressMonitor monitor,
                                        @NotNull DBPDataSourceContainer container) throws DBException {
        String url = container.getConnectionConfiguration().getUrl();
        if (url.endsWith("?sql_dialect=table")) {
            return new IoTDBDataSource(monitor, container, new IoTDBTableMetaModel(), false);
        }
        return new IoTDBDataSource(monitor, container, new IoTDBMetaModel(), true);
    }

    private static String makePropPattern(String prop) {
        return "{" + prop + "}";
    }

    private boolean useRawUrl(DBPConnectionConfiguration connectionInfo) {
        return !CommonUtils.isEmpty(connectionInfo.getUrl()) &&
                CommonUtils.isEmpty(connectionInfo.getHostPort()) &&
                CommonUtils.isEmpty(connectionInfo.getHostName()) &&
                CommonUtils.isEmpty(connectionInfo.getServerName());
    }

    private String buildUrlFromTemplate(DBPConnectionConfiguration connectionInfo, String urlTemplate) throws DBException {
        DatabaseURL.MetaURL metaURL = DatabaseURL.parseSampleURL(urlTemplate);
        StringBuilder url = new StringBuilder();
        for (String component : metaURL.getUrlComponents()) {
            String newComponent = component;
            if (!CommonUtils.isEmpty(connectionInfo.getHostName())) {
                newComponent = newComponent.replace(makePropPattern(DBConstants.PROP_HOST), connectionInfo.getHostName());
            }
            if (!CommonUtils.isEmpty(connectionInfo.getHostPort())) {
                newComponent = newComponent.replace(makePropPattern(DBConstants.PROP_PORT), connectionInfo.getHostPort());
            }
            if (!CommonUtils.isEmpty(connectionInfo.getServerName())) {
                newComponent = newComponent.replace(makePropPattern("sqlDialect"), connectionInfo.getServerName());
            }
            newComponent = newComponent.replace(makePropPattern(DBConstants.PROP_USER), CommonUtils.notEmpty(connectionInfo.getUserName()));

            if (newComponent.startsWith("[")) {
                if (!newComponent.equals(component)) {
                    url.append(newComponent.substring(1, newComponent.length() - 1));
                }
            } else {
                url.append(newComponent);
            }
        }
        return url.toString();
    }

    @NotNull
    @Override
    public String getConnectionURL(
        @NotNull DBPDriver driver,
        @NotNull DBPConnectionConfiguration connectionInfo) {
        String urlTemplate = driver.getSampleURL();
        if (useRawUrl(connectionInfo)) {
            return connectionInfo.getUrl();
        }
        if (CommonUtils.isEmptyTrimmed(urlTemplate)) {
            return connectionInfo.getUrl();
        }

        try {
            return buildUrlFromTemplate(connectionInfo, urlTemplate);
        } catch (DBException e) {
            log.error(e);
            return null;
        }
    }
}

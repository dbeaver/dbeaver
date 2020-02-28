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
package org.jkiss.dbeaver.ext.h2.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.Connection;

/**
 * H2 datasource
 */
public class H2DataSource extends GenericDataSource {

    public static final String H2_URL_PREFIX_TCP = "jdbc:h2:tcp:";
    public static final String H2_URL_PREFIX = "jdbc:h2:";
    public static final String H2_DB_FILE_EXTENSION = ".mv.db";

    public H2DataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container, GenericMetaModel metaModel)
        throws DBException {
        super(monitor, container, metaModel, new H2SQLDialect());
    }

    @Override
    protected String getConnectionURL(DBPConnectionConfiguration connectionInfo) {
        String url = connectionInfo.getUrl();
        if (url == null || url.startsWith(H2_URL_PREFIX_TCP)) {
            return url;
        }
        if (url.startsWith(H2_URL_PREFIX)) {
            String filePath = url.substring(H2_URL_PREFIX.length());
            String params = null;
            int divPos = filePath.indexOf('?');
            if (divPos != -1) {
                params = filePath.substring(divPos);
                filePath = filePath.substring(0, divPos);
            }
            if (filePath.endsWith(H2_DB_FILE_EXTENSION)) {
                // Remove extension from database name
                url = H2_URL_PREFIX + filePath.substring(0, filePath.length() - H2_DB_FILE_EXTENSION.length());
                if (params != null) {
                    url += params;
                }
                return url;
            }
        }
        return super.getConnectionURL(connectionInfo);
    }

    @Override
    protected Connection openConnection(@NotNull DBRProgressMonitor monitor, @Nullable JDBCExecutionContext context, @NotNull String purpose) throws DBCException {
        return super.openConnection(monitor, context, purpose);
    }
}

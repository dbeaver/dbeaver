/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.netezza.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.meta.ForTest;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.BeanUtils;

public class NetezzaDataSource extends GenericDataSource {

    private static final Log log = Log.getLog(NetezzaDataSource.class);

    public NetezzaDataSource(@NotNull DBRProgressMonitor monitor, @NotNull DBPDataSourceContainer container, @NotNull GenericMetaModel metaModel) throws DBException {
        super(monitor, container, metaModel, new NetezzaSQLDialect());
    }

    // Constructor for tests
    @ForTest
    public NetezzaDataSource(@NotNull DBRProgressMonitor monitor, @NotNull GenericMetaModel metaModel, @NotNull DBPDataSourceContainer container) throws DBException {
        super(monitor, metaModel, container, new NetezzaSQLDialect());
    }

    @Override
    protected void initializeContextState(@NotNull DBRProgressMonitor monitor, @NotNull JDBCExecutionContext context, JDBCExecutionContext initFrom) throws DBException {
        final String name = getContainer().getActualConnectionConfiguration().getUserName();
        if (getSQLDialect().isQuotedIdentifier(name)) {
            // Special case for quoted user names.
            // Netezza inside check the user name with "select current_user", but this select returns and sets user name in lowercase without quotes
            try {
                final var connection = context.getConnection(monitor);
                final var datasource = BeanUtils.invokeObjectMethod(connection, "getDatasource");
                BeanUtils.invokeObjectMethod(datasource, "setUser", new Class[]{String.class}, new Object[]{name});
            } catch (Throwable e) {
                log.debug("Error setting user", e);
            }
        }
    }

}

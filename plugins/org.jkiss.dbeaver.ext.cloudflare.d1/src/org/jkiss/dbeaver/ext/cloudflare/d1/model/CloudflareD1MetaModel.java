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
package org.jkiss.dbeaver.ext.cloudflare.d1.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.sqlite.model.SQLiteMetaModel;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class CloudflareD1MetaModel extends SQLiteMetaModel {
    @NotNull
    @Override
    public GenericTableBase createTableOrViewImpl(
        @NotNull GenericStructContainer container, @NotNull String tableName, @Nullable String tableType, @Nullable JDBCResultSet resultSet
    ) {
        if (tableType != null && this.isView(tableType)) {
            return super.createTableOrViewImpl(container, tableName, tableType, resultSet);
        }
        return new CloudflareD1Table(container, tableName, tableType, resultSet);
    }

    @NotNull
    @Override
    public GenericDataSource createDataSourceImpl(@NotNull DBRProgressMonitor monitor, @NotNull DBPDataSourceContainer container)
        throws DBException {
        return new CloudflareD1DataSource(monitor, container, this);
    }

    @NotNull
    @Override
    public GenericCatalog createCatalogImpl(@NotNull GenericDataSource dataSource, @NotNull String catalogName) {
        CloudflareD1DataSource d1 = (CloudflareD1DataSource) dataSource;
        return new CloudflareD1Catalog(d1, d1.getDatabase(catalogName));
    }

    @Override
    public boolean useCatalogInObjectNames() {
        return false;
    }

    @Override
    public boolean isCatalogsOptional() {
        return false;
    }
}

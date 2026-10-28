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
package org.jkiss.dbeaver.ext.duckdb.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.GenericDataSourceProvider;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class DuckDBDataSourceProvider extends GenericDataSourceProvider<DuckDBDataSource> {
    public DuckDBDataSourceProvider() {
        super(DuckDBDataSource.class);
    }

    @NotNull
    @Override
    public DBPPropertyDescriptor[] getConnectionProperties(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDriver driver,
        @Nullable DBPDataSourceContainer dataSourceContainer,
        @NotNull DBPConnectionConfiguration connectionInfo
    ) throws DBException {
        if (DuckDBConstants.DRIVER_MOTHER_DUCK_ID.equals(driver.getId())) {
            // The driver lists its settings by opening a real connection, which for MotherDuck means logging in
            // without the token. Read the same DuckDB settings from an in-memory database instead.
            DBPConnectionConfiguration localInfo = new DBPConnectionConfiguration();
            localInfo.setUrl(DuckDBConstants.IN_MEMORY_URL);
            return super.getConnectionProperties(monitor, driver, null, localInfo);
        }
        return super.getConnectionProperties(monitor, driver, dataSourceContainer, connectionInfo);
    }
}

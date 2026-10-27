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
package org.jkiss.dbeaver.ext.cloudflare.d1;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cloudflare.d1.model.CloudflareD1DataSource;
import org.jkiss.dbeaver.ext.sqlite.model.SQLiteDataSourceProvider;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Arrays;
import java.util.Set;

public class CloudflareD1DataSourceProvider extends SQLiteDataSourceProvider {
    private static final Set<String> AUTH_PROPERTIES = Set.of("accountId", "apiToken");

    public CloudflareD1DataSourceProvider() {
        super(CloudflareD1DataSource.class);
    }

    @NotNull
    @Override
    public DBPPropertyDescriptor[] getConnectionProperties(
        @NotNull DBRProgressMonitor monitor, @NotNull DBPDriver driver, @NotNull DBPDataSourceContainer dataSourceContainer,
        @NotNull DBPConnectionConfiguration connectionInfo
    ) throws DBException {
        return Arrays.stream(super.getConnectionProperties(monitor, driver, dataSourceContainer, connectionInfo))
            .filter(property -> !AUTH_PROPERTIES.contains(property.getId()))
            .toArray(DBPPropertyDescriptor[]::new);
    }
}

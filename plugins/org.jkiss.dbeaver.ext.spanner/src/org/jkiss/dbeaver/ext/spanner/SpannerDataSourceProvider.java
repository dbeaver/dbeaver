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
package org.jkiss.dbeaver.ext.spanner;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.GenericDataSourceProvider;
import org.jkiss.dbeaver.ext.spanner.model.SpannerDataSource;
import org.jkiss.dbeaver.ext.spanner.model.SpannerMetaModel;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class SpannerDataSourceProvider extends GenericDataSourceProvider {
    public static final String COMMUNITY_DRIVER_ID = "spanner_jdbc";
    public static final String OFFICIAL_DRIVER_ID = "spanner_jdbc_official";

    public SpannerDataSourceProvider()
    {
    }

    @Override
    public void init(@NotNull DBPPlatform platform) {

    }

    @NotNull
    @Override
    public DBPDataSource openDataSource(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSourceContainer container)
        throws DBException
    {
        return new SpannerDataSource(monitor, container, new SpannerMetaModel());
    }

    @Override
    public String getConnectionURL(DBPDriver driver, DBPConnectionConfiguration connectionInfo) {
        if ( COMMUNITY_DRIVER_ID.equals(driver.getId())) {
            return "jdbc:cloudspanner://localhost;";
        } else {
            return "jdbc:cloudspanner:/projects/%s/instances/%s/databases/%s";
        }
    }
}

/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Data source provider
 */
public interface DBPDataSourceProvider extends DBPDataSourceURLProvider, DBPObject
{
    long FEATURE_NONE        = 0;
    long FEATURE_CATALOGS    = 1;
    long FEATURE_SCHEMAS     = 2;

    /**
     * Initializes data source provider
     * @param platform application
     */
    void init(@NotNull DBPPlatform platform);

    /**
     * Supported features
     * @return features
     */
    long getFeatures();
    
    /**
     * Supported connection properties.
     *
     * @param monitor progress monitor
     * @param driver driver
     * @param connectionInfo connection information   @return property group which contains all supported properties
     * @throws DBException on any error
     */
    DBPPropertyDescriptor[] getConnectionProperties(
        DBRProgressMonitor monitor,
        DBPDriver driver,
        DBPConnectionConfiguration connectionInfo)
        throws DBException;

    /**
     * Opens new data source
     * @param monitor progress monitor
     * @param container data source container
     * @return new data source object
     * @throws DBException on any error
     */
    @NotNull
    DBPDataSource openDataSource(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPDataSourceContainer container)
        throws DBException;

}

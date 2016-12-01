/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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
public interface DBPDataSourceProvider extends DBPObject
{
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
     * Constructs connection URL
     * @param driver driver descriptor
     * @param connectionInfo connection info
     * @return valid connection URL or null (if URLs not supported by driver)
     */
    String getConnectionURL(DBPDriver driver, DBPConnectionConfiguration connectionInfo);

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

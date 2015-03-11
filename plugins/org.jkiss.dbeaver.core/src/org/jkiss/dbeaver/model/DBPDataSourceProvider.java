/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.model;

import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;

/**
 * Data source provider
 */
public interface DBPDataSourceProvider
{
    public static final long FEATURE_CATALOGS    = 1;
    public static final long FEATURE_SCHEMAS     = 2;

    /**
     * Initializes data source provider
     * @param application application
     */
    void init(@NotNull DBPApplication application);

    /**
     * Supported features
     * @return features
     */
    long getFeatures();
    
    /**
     * Supported connection properties.
     *
     * @param runnableContext runnable context. May be used by implementation to load necessary driver artifacts.
     * @param driver driver
     * @param connectionInfo connection information
     * @return property group which contains all supported properties
     * @throws DBException on any error
     */
    IPropertyDescriptor[] getConnectionProperties(
        IRunnableContext runnableContext,
        DBPDriver driver,
        DBPConnectionInfo connectionInfo)
        throws DBException;

    /**
     * Constructs connection URL
     * @param driver driver descriptor
     * @param connectionInfo connection info
     * @return valid connection URL or null (if URLs not supported by driver)
     */
    String getConnectionURL(DBPDriver driver, DBPConnectionInfo connectionInfo);

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
        @NotNull DBSDataSourceContainer container)
        throws DBException;

    /**
     * Closes data source provider
     */
    void close();

}

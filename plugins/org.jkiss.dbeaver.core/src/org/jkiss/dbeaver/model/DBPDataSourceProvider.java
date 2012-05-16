/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;

import java.util.Collection;

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
    void init(DBPApplication application);

    /**
     * Supported features
     * @return features
     */
    long getFeatures();

    /**
     * Supported connection properties.
     * @param driver driver
     * @param connectionInfo connection information
     * @return property group which contains all supported properties
     * @throws DBException on any error
     */
    Collection<IPropertyDescriptor> getConnectionProperties(
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
    DBPDataSource openDataSource(
        DBRProgressMonitor monitor,
        DBSDataSourceContainer container)
        throws DBException;

    /**
     * Closes data source provider
     */
    void close();

}

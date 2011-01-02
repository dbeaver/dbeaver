/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;

/**
 * DBPDataSourceProvider
 */
public interface DBPDataSourceProvider
{
    public static final long FEATURE_CATALOGS    = 1;
    public static final long FEATURE_SCHEMAS     = 2;

    void init(DBPApplication application);

    long getFeatures();

    DBPPropertyGroup getConnectionProperties(
        DBPDriver driver,
        DBPConnectionInfo connectionInfo)
        throws DBException;

    DBPDataSource openDataSource(
        DBRProgressMonitor monitor,
        DBSDataSourceContainer container)
        throws DBException;

    void close();

}

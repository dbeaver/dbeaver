/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
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
    void init(DBPApplication application);

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

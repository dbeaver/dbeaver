/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;

/**
 * DBPDataSourceProvider
 */
public interface DBPDataSourceProvider
{
    void init(DBPApplication application);

    void close();

    DBPDataSource openDataSource(
        DBSDataSourceContainer container)
        throws DBException;

}

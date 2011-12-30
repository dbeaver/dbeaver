/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * MySQL informational object
 */
public abstract class MySQLInformation implements DBSObject {

    private MySQLDataSource dataSource;

    protected MySQLInformation(MySQLDataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    public DBSObject getParentObject()
    {
        return getDataSource().getContainer();
    }

    public MySQLDataSource getDataSource()
    {
        return dataSource;
    }

    public boolean isPersisted()
    {
        return true;
    }

    @Override
    public String toString()
    {
        return getName();
    }
}

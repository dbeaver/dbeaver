/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
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

    @Override
    public DBSObject getParentObject()
    {
        return getDataSource().getContainer();
    }

    @Override
    public MySQLDataSource getDataSource()
    {
        return dataSource;
    }

    @Override
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

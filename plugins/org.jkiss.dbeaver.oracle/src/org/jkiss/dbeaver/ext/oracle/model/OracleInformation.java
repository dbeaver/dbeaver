/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Oracle informational object
 */
public abstract class OracleInformation implements DBSObject {

    private OracleDataSource dataSource;

    protected OracleInformation(OracleDataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    public DBSObject getParentObject()
    {
        return getDataSource().getContainer();
    }

    public OracleDataSource getDataSource()
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

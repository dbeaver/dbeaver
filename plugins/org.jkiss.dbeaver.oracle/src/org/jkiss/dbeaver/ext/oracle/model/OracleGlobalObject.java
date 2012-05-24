/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Abstract oracle schema object
 */
public abstract class OracleGlobalObject implements DBSObject, DBPSaveableObject
{
    static final Log log = LogFactory.getLog(OracleGlobalObject.class);

    private final OracleDataSource dataSource;
    private boolean persisted;

    protected OracleGlobalObject(
        OracleDataSource dataSource,
        boolean persisted)
    {
        this.dataSource = dataSource;
        this.persisted = persisted;
    }

    @Override
    public String getDescription()
    {
        return null;
    }

    @Override
    public DBSObject getParentObject()
    {
        return dataSource.getContainer();
    }

    @Override
    public OracleDataSource getDataSource()
    {
        return dataSource;
    }

    @Override
    public boolean isPersisted()
    {
        return persisted;
    }

    @Override
    public void setPersisted(boolean persisted)
    {
        this.persisted = persisted;
    }

}

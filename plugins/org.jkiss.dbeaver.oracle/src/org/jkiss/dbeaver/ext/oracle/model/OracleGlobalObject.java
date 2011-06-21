/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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

    public String getDescription()
    {
        return null;
    }

    public DBSObject getParentObject()
    {
        return dataSource.getContainer();
    }

    public OracleDataSource getDataSource()
    {
        return dataSource;
    }

    public boolean isPersisted()
    {
        return persisted;
    }

    public void setPersisted(boolean persisted)
    {
        this.persisted = persisted;
    }

}

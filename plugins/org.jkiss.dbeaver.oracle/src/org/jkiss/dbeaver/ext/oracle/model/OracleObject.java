/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Abstract oracle object
 */
public abstract class OracleObject<PARENT extends DBSObject> implements DBSObject, DBPSaveableObject
{
    static final Log log = LogFactory.getLog(OracleObject.class);


    protected final PARENT parent;
    protected String name;
    private boolean persisted;

    protected OracleObject(
        PARENT parent,
        String name,
        boolean persisted)
    {
        this.parent = parent;
        this.name = name;

        this.persisted = persisted;
    }

    public String getDescription()
    {
        return null;
    }

    public PARENT getParentObject()
    {
        return parent;
    }

    public OracleDataSource getDataSource()
    {
        return (OracleDataSource) parent.getDataSource();
    }

    @Property(name = "Name", viewable = true, editable = true, order = 1)
    public String getName()
    {
        return name;
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

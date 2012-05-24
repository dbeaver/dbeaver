/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Entity container
 */
public abstract class WMIContainer extends WMIPropertySource implements DBSObject
{
    protected final WMINamespace parent;

    protected WMIContainer(WMINamespace parent)
    {
        this.parent = parent;
    }

    @Override
    public String getDescription()
    {
        return null;
    }

    @Override
    public DBSObject getParentObject()
    {
        return parent;
    }

    @Override
    public WMIDataSource getDataSource()
    {
        return parent.getDataSource();
    }

    @Override
    public boolean isPersisted()
    {
        return true;
    }

}

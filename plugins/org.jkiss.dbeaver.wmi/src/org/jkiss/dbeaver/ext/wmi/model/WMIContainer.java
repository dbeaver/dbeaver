/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.wmi.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Entity container
 */
public abstract class WMIContainer implements DBSEntity
{
    static final Log log = LogFactory.getLog(WMIContainer.class);

    protected final WMIContainer parent;

    protected WMIContainer(WMIContainer parent)
    {
        this.parent = parent;
    }

    public String getDescription()
    {
        return null;
    }

    public DBSObject getParentObject()
    {
        return parent instanceof WMIDataSource ? ((WMIDataSource) parent).getContainer() : parent;
    }

    public WMIDataSource getDataSource()
    {
        for (WMIContainer p = this; p != null; p = p.parent) {
            if (p instanceof WMIDataSource) {
                return (WMIDataSource)p;
            }
        }
        return null;
    }

    public boolean isPersisted()
    {
        return true;
    }

    public boolean refreshEntity(DBRProgressMonitor monitor) throws DBException
    {
        return false;
    }

}

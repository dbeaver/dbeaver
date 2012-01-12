/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.wmi.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPCloseableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Entity container
 */
public abstract class WMIContainer implements DBSEntity, DBSEntityContainer
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

    public void cacheStructure(DBRProgressMonitor monitor, int scope) throws DBException
    {
    }

    public DBSEntity getChild(DBRProgressMonitor monitor, String childName) throws DBException
    {
        return DBUtils.findObject(getChildren(monitor), childName);
    }

    public boolean refreshEntity(DBRProgressMonitor monitor) throws DBException
    {
        return false;
    }

}

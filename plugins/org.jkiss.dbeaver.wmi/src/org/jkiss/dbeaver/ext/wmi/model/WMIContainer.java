/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Collection;

/**
 * Entity container
 */
public class WMIContainer implements DBSEntityContainer {

    public Collection<? extends DBSEntity> getChildren(DBRProgressMonitor monitor) throws DBException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public DBSEntity getChild(DBRProgressMonitor monitor, String childName) throws DBException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Class<? extends DBSEntity> getChildType(DBRProgressMonitor monitor) throws DBException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void cacheStructure(DBRProgressMonitor monitor, int scope) throws DBException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getDescription()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public DBSObject getParentObject()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public DBPDataSource getDataSource()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getName()
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isPersisted()
    {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}

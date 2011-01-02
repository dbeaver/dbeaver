/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSIndex;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Collection;

/**
 * AbstractIndex
 */
public abstract class AbstractIndex implements DBSIndex
{
    public Collection<? extends DBSObject> getChildren(DBRProgressMonitor monitor)
        throws DBException
    {
        return getColumns(monitor);
    }

    public boolean isPersisted()
    {
        return true;
    }

    public boolean refreshEntity(DBRProgressMonitor monitor)
        throws DBException
    {
        return false;
    }

}

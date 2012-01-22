/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSTableIndex;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Collection;

/**
 * AbstractTableIndex
 */
public abstract class AbstractTableIndex implements DBSTableIndex
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

    @Override
    public DBSEntityConstraintType getConstraintType()
    {
        return DBSEntityConstraintType.INDEX;
    }
}

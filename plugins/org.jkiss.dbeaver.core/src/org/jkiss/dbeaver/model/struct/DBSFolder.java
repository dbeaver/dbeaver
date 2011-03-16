/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Collection;

/**
 * DBSFolder
 */
public interface DBSFolder extends DBSObject
{
    Collection<DBSObject> getChildrenObjects(DBRProgressMonitor monitor)
        throws DBException;
}

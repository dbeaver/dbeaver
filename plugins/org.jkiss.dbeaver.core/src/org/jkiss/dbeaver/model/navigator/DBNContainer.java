/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.navigator;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * DBNDatabaseFolder
 */
public interface DBNContainer //extends DBSFolder
{
    Object getValueObject();

    Class<?> getItemsClass();

    DBNNode addChildItem(DBRProgressMonitor monitor, Object childObject) throws DBException;

    void removeChildItem(DBNNode item) throws DBException;

}
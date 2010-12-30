/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.navigator;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSFolder;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.tree.DBXTreeFolder;
import org.jkiss.dbeaver.ui.ICommandIds;

/**
 * DBNTreeFolder
 */
public interface DBNContainer extends DBSFolder
{
    Object getValueObject();

    Class<? extends DBSObject> getItemsClass();

    //Class getChildrenType();

    DBNNode addChildItem(DBRProgressMonitor monitor, DBSObject childObject) throws DBException;

    void removeChildItem(DBNNode item) throws DBException;
}